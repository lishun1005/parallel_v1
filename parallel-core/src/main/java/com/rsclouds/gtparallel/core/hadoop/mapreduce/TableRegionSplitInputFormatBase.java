package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableRecordReader;
import org.apache.hadoop.hbase.mapreduce.TableSplit;
import org.apache.hadoop.hbase.util.Addressing;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.hadoop.hbase.util.Strings;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.net.DNS;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.operation.ExportDir;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

/**
 * A base for {@link TableInputFormat}s. Receives a {@link HTable}, an
 * {@link Scan} instance that defines the input columns etc. Subclasses may use
 * other TableRecordReader implementations.
 * <p>
 * An example of a subclass:
 * 
 * <pre>
 * class ExampleTIF extends TableInputFormatBase implements JobConfigurable {
 * 
 * 	public void configure(JobConf job) {
 * 		HTable exampleTable = new HTable(HBaseConfiguration.create(job),
 * 				Bytes.toBytes(&quot;exampleTable&quot;));
 * 		// mandatory
 * 		setHTable(exampleTable);
 * 		Text[] inputColumns = new byte[][] { Bytes.toBytes(&quot;cf1:columnA&quot;),
 * 				Bytes.toBytes(&quot;cf2&quot;) };
 * 		// mandatory
 * 		setInputColumns(inputColumns);
 * 		RowFilterInterface exampleFilter = new RegExpRowFilter(&quot;keyPrefix.*&quot;);
 * 		// optional
 * 		setRowFilter(exampleFilter);
 * 	}
 * 
 * 	public void validateInput(JobConf job) throws IOException {
 * 	}
 * }
 * </pre>
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public abstract class TableRegionSplitInputFormatBase extends
		InputFormat<ImmutableBytesWritable, Result> {

	class IntRow {
		private int index;
		private byte[] rowkey;

		public IntRow() {
			index = 0;
			rowkey = "".getBytes();
		}

		public IntRow(int index, byte[] rowkey) {
			this.index = index;
			this.rowkey = rowkey;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public byte[] getRowkey() {
			return rowkey;
		}

		public void setRowkey(byte[] rowkey) {
			this.rowkey = rowkey;
		}
	}

	final Log LOG = LogFactory.getLog(TableRegionSplitInputFormatBase.class);

	/** Holds the details for the internal scanner. */
	private Scan scan = null;
	/** The table to scan. */
	private HTable table = null;
	/** The reader scanning the table, can be a custom one. */
	private TableRecordReader tableRecordReader = null;

	/** The reverse DNS lookup cache mapping: IPAddress => HostName */
	private HashMap<InetAddress, String> reverseDNSCacheMap = new HashMap<InetAddress, String>();

	/** The NameServer address */
	private String nameServer = null;

	/**
	 * Builds a TableRecordReader. If no TableRecordReader was provided, uses
	 * the default.
	 * 
	 * @param split
	 *            The split to work with.
	 * @param context
	 *            The current context.
	 * @return The newly created record reader.
	 * @throws IOException
	 *             When creating the reader fails.
	 * @see org.apache.hadoop.mapreduce.InputFormat#createRecordReader(org.apache.hadoop.mapreduce.InputSplit,
	 *      org.apache.hadoop.mapreduce.TaskAttemptContext)
	 */
	@Override
	public RecordReader<ImmutableBytesWritable, Result> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException {
		if (table == null) {
			throw new IOException(
					"Cannot create a record reader because of a"
							+ " previous error. Please look at the previous logs lines from"
							+ " the task's full log for more details.");
		}
		TableSplit tSplit = (TableSplit) split;
		TableRecordReader trr = this.tableRecordReader;
		// if no table record reader was provided use default
		if (trr == null) {
			trr = new TableRecordReader();
		}
		Scan sc = new Scan(this.scan);
		sc.setStartRow(tSplit.getStartRow());
		sc.setStopRow(tSplit.getEndRow());
		trr.setScan(sc);
		trr.setHTable(table);
		return trr;
	}

	/**
	 * Calculates the splits that will serve as input for the map tasks. The
	 * number of splits matches the number of regions in a table.
	 * 
	 * @param context
	 *            The current job context.
	 * @return The list of input splits.
	 * @throws IOException
	 *             When creating the list of splits fails.
	 * @see org.apache.hadoop.mapreduce.InputFormat#getSplits(org.apache.hadoop.mapreduce.JobContext)
	 */
	@Override
	public List<InputSplit> getSplits(JobContext context) throws IOException {
		Configuration conf = context.getConfiguration();
		Map<String,String> map = new HashMap<String,String>();
		map.put(CoreConfig.JOB.JID.strVal, context.getJobID().toString());
		map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.RUNNING.toString());
		String rowkey = conf.get(CoreConfig.JOBID);
		HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, rowkey, CoreConfig.JOB.FAMILY.strVal, map);
		if (table == null) {
			throw new IOException("No table was provided.");
		}
		// Get the name server address and the default value is null.
		this.nameServer = context.getConfiguration().get(
				"hbase.nameserver.address", null);

		Pair<byte[][], byte[][]> keys = table.getStartEndKeys();
		if (keys == null || keys.getFirst() == null
				|| keys.getFirst().length == 0) {
			HRegionLocation regLoc = table.getRegionLocation(
					HConstants.EMPTY_BYTE_ARRAY, false);
			if (null == regLoc) {
				throw new IOException("Expecting at least one region.");
			}
			List<InputSplit> splits = new ArrayList<InputSplit>(1);
			InputSplit split = new TableSplit(table.getName(),
					HConstants.EMPTY_BYTE_ARRAY, HConstants.EMPTY_BYTE_ARRAY,
					regLoc.getHostnamePort().split(
							Addressing.HOSTNAME_PORT_SEPARATOR)[0]);
			splits.add(split);
			return splits;
		}
		List<InputSplit> splits = new ArrayList<InputSplit>();
		byte[] startRow = scan.getStartRow();
		byte[] stopRow = scan.getStopRow();
		int minSplit = context.getConfiguration().getInt(ExportDir.MINSPLIT,
				CoreConfig.PERSPLIT_ROKEYS_NUM);
		byte[] remainStartRow = startRow;
		byte[] remainStopRow = stopRow;
		int remainCount = 0;
		for (int i = 0; i < keys.getFirst().length; i++) {
			if (!includeRegionInSplit(keys.getFirst()[i], keys.getSecond()[i])) {
				continue;
			}
			HRegionLocation location = table.getRegionLocation(
					keys.getFirst()[i], false);
			// The below InetSocketAddress creation does a name resolution.
			InetSocketAddress isa = new InetSocketAddress(
					location.getHostname(), location.getPort());
			if (isa.isUnresolved()) {
				LOG.warn("Failed resolve " + isa);
			}
			InetAddress regionAddress = isa.getAddress();
			String regionLocation;
			try {
				regionLocation = reverseDNS(regionAddress);
			} catch (NamingException e) {
				LOG.error("Cannot resolve the host name for " + regionAddress
						+ " because of " + e);
				regionLocation = location.getHostname();
			}

			// determine if the given start an stop key fall into the region
			if ((startRow.length == 0 || keys.getSecond()[i].length == 0 || Bytes
					.compareTo(startRow, keys.getSecond()[i]) < 0)
					&& (stopRow.length == 0 || Bytes.compareTo(stopRow,
							keys.getFirst()[i]) > 0)) {
				byte[] splitStart = startRow.length == 0
						|| Bytes.compareTo(keys.getFirst()[i], startRow) >= 0 ? keys
						.getFirst()[i] : startRow;
				byte[] splitStop = (stopRow.length == 0 || Bytes.compareTo(
						keys.getSecond()[i], stopRow) <= 0)
						&& keys.getSecond()[i].length > 0 ? keys.getSecond()[i]
						: stopRow;
				List<IntRow> rowList = getSplitRows(splitStart, splitStop,
						minSplit);

				if (rowList == null || rowList.size() == 0) {
					throw new IOException("Expecting at least one record.");
				} else {
					if (remainCount != 0) {
						int temp = rowList.get(1).getIndex()
								- rowList.get(0).getIndex();
						if (temp + remainCount > (minSplit * 4 / 3)) {
							InputSplit split = new TableSplit(table.getName(),
									remainStartRow, remainStopRow,
									regionLocation);
							splits.add(split);
							remainCount = 0;
						} else {
							rowList.get(0).setRowkey(remainStartRow);
							remainCount += rowList.get(1).getIndex();
							rowList.get(1).setIndex(remainCount);
						}
					}
					if(rowList.size() < 2){
						remainStartRow = splitStart;
						remainStopRow = splitStop;
					}
					int listSize = rowList.size() - 1;				
					int startIndex = 0;
					int stopIndex = 0;
					byte[] start;
					byte[] stop;
					for (int k = 0; k < listSize; k++) {
						start = rowList.get(k).getRowkey();
						stop = rowList.get(k + 1).getRowkey();
						startIndex = rowList.get(k).getIndex();
						stopIndex = rowList.get(k + 1).getIndex();
						remainCount = stopIndex - startIndex;
						if (remainCount < minSplit) {
							remainStartRow = start;
							remainStopRow = stop;
						} else {
							InputSplit split = new TableSplit(table.getName(),
									start, stop, regionLocation);	
							splits.add(split);
							remainCount = 0;
						}
					}
				}
			}
			if (splits.isEmpty()) {
				InputSplit split = new TableSplit(table.getName(),
						remainStartRow, remainStopRow, regionLocation);				
				splits.add(split);
			} else if (0 != remainCount) {
				if (remainCount > (minSplit / 3)) {
					InputSplit split = new TableSplit(table.getName(),
							remainStartRow, remainStopRow, regionLocation);					
					splits.add(split);
					remainCount = 0;
				} else {
					TableSplit split = (TableSplit) splits
							.get(splits.size() - 1);
					splits.remove(splits.size() - 1);
					TableSplit split_last = new TableSplit(table.getName(),
							split.getStartRow(), remainStopRow,
							split.getRegionLocation());				
					splits.add(split_last);
				}
			}
			if (splits.size() == 1) {
				context.getConfiguration().setInt("splits", 0);
			} else {
				context.getConfiguration().setInt("splits", 1);
			}
		}		
		return splits;
	}

	/**
	 * split the region to m splits
	 * 
	 * @param splitStart
	 * @param splitStop
	 * @param perSplit
	 * @param directoryCompressFlag
	 * @return return the rowkeys which to use to split
	 * @throws IOException
	 */
	private List<IntRow> getSplitRows(byte[] startRow, byte[] stopRow, int per) {
		List<IntRow> rows = new ArrayList<IntRow>();

		Scan scan1 = new Scan();
		scan1.setStartRow(startRow);
		scan1.setStopRow(stopRow);
		scan1.setCacheBlocks(false);
		long scarnTime = System.currentTimeMillis();
		ResultScanner resultscanner = null;
		int i = 0;
		try {
			resultscanner = table.getScanner(scan1);
			for (Result rs : resultscanner) {
				if (rs.isEmpty())
					continue;
				if (i % per == 0) {
					IntRow intRow = new IntRow(i, rs.getRow());
					rows.add(intRow);
				}
				i++;
			}
			i = i - 1;
			if (i % per != 0) {
				IntRow intRow = new IntRow(i, stopRow);
				rows.add(intRow);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			resultscanner.close();
		}
		System.out.println("Scan Time in TableSplitInputFormatBase cost : "
				+ (System.currentTimeMillis() - scarnTime) + "(ms)");
		return rows;
	}

	private String reverseDNS(InetAddress ipAddress) throws NamingException {
		String hostName = this.reverseDNSCacheMap.get(ipAddress);
		if (hostName == null) {
			hostName = Strings.domainNamePointerToHostName(DNS.reverseDns(
					ipAddress, this.nameServer));
			this.reverseDNSCacheMap.put(ipAddress, hostName);
		}
		return hostName;
	}

	/**
	 * 
	 * 
	 * Test if the given region is to be included in the InputSplit while
	 * splitting the regions of a table.
	 * <p>
	 * This optimization is effective when there is a specific reasoning to
	 * exclude an entire region from the M-R job, (and hence, not contributing
	 * to the InputSplit), given the start and end keys of the same. <br>
	 * Useful when we need to remember the last-processed top record and revisit
	 * the [last, current) interval for M-R processing, continuously. In
	 * addition to reducing InputSplits, reduces the load on the region server
	 * as well, due to the ordering of the keys. <br>
	 * <br>
	 * Note: It is possible that <code>endKey.length() == 0 </code> , for the
	 * last (recent) region. <br>
	 * Override this method, if you want to bulk exclude regions altogether from
	 * M-R. By default, no region is excluded( i.e. all regions are included).
	 * 
	 * 
	 * @param startKey
	 *            Start key of the region
	 * @param endKey
	 *            End key of the region
	 * @return true, if this region needs to be included as part of the input
	 *         (default).
	 * 
	 */
	protected boolean includeRegionInSplit(final byte[] startKey,
			final byte[] endKey) {
		return true;
	}

	/**
	 * Allows subclasses to get the {@link HTable}.
	 */
	protected HTable getHTable() {
		return this.table;
	}

	/**
	 * Allows subclasses to set the {@link HTable}.
	 * 
	 * @param table
	 *            The table to get the data from.
	 */
	protected void setHTable(HTable table) {
		this.table = table;
	}

	/**
	 * Gets the scan defining the actual details like columns etc.
	 * 
	 * @return The internal scan instance.
	 */
	public Scan getScan() {
		if (this.scan == null)
			this.scan = new Scan();
		return scan;
	}

	/**
	 * Sets the scan defining the actual details like columns etc.
	 * 
	 * @param scan
	 *            The scan to set.
	 */
	public void setScan(Scan scan) {
		this.scan = scan;
	}

	/**
	 * Allows subclasses to set the {@link TableRecordReader}.
	 * 
	 * @param tableRecordReader
	 *            A different {@link TableRecordReader} implementation.
	 */
	protected void setTableRecordReader(TableRecordReader tableRecordReader) {
		this.tableRecordReader = tableRecordReader;
	}
}