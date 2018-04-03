package com.rsclouds.gtparallel.core.mutilcenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper.Context;

public class ResTableMapperFilter extends
		TableMapper<ImmutableBytesWritable, Result> {

	private String strOldCenter;
	private String strNewCenter;

	/**
	 * Use this before submitting a TableMap job. It will appropriately set up
	 * the job.
	 * 
	 * @param table
	 *            The table name.
	 * @param scan
	 *            The scan with the columns to scan.
	 * @param mapper
	 *            The mapper class.
	 * @param job
	 *            The job configuration.
	 * @throws IOException
	 *             When setting up the job fails.
	 */
	@SuppressWarnings("rawtypes")
	public static void initJob(String table, Scan scan,
			Class<? extends TableMapper> mapper, Job job) throws IOException {
		TableMapReduceUtil.initTableMapperJob(table, scan, mapper,
				ImmutableBytesWritable.class, Result.class, job);
	}

	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		strOldCenter = conf.get(ChangeMetaCenter.KEY_OLD_CENTER, "");
		strNewCenter = conf.get(ChangeMetaCenter.KEY_NEW_CENTER, "");
	}

	List<Cell> cellList = new ArrayList<Cell>();

	/**
	 * Pass the key, value to reduce.
	 * 
	 * @param key
	 *            The current key.
	 * @param value
	 *            The current value.
	 * @param context
	 *            The current context.
	 * @throws IOException
	 *             When writing the record fails.
	 * @throws InterruptedException
	 *             When the job is aborted.
	 */
	public void map(ImmutableBytesWritable key, Result value, Context context)
			throws IOException, InterruptedException {
		byte[] dfs = value.getValue(ChangeResCenter.RES_FAMILY_BYTES,
				ChangeResCenter.RES_QUALIFIER_DFS_BYTES);
		if (dfs != null && dfs.length > 0) {
			String strDfs = new String(dfs);
			if (strDfs.startsWith(strOldCenter)) {
				strDfs = strDfs.replace(strOldCenter, strNewCenter);
				cellList.clear();
				cellList = value.listCells();
				int length = cellList.size();

				for (int i = 0; i < length; i++) {
					Cell cellTemp = cellList.get(i);
					String strQualifierName = new String(
							cellTemp.getQualifierArray());
					if (strQualifierName.equalsIgnoreCase("dfs")) {
						cellList.remove(i);
						i--;
						length --;
						Cell cellDfsNew = CellUtil.createCell(key.get(), ChangeResCenter.RES_FAMILY_BYTES, ChangeResCenter.RES_QUALIFIER_DFS_BYTES,
								cellTemp.getTimestamp(), cellTemp.getTypeByte(), strDfs.getBytes());
						cellList.add(cellDfsNew);
					}
				}
				Result valueNew = Result.create(cellList);
				context.write(key, valueNew);
			}
		}

	}
	
	public static void main(String[] args) {
		Configuration conf = HBaseConfiguration.create();
		try {
			HTable table = new HTable(conf, "");
			Scan s = new Scan();
			ResultScanner res = table.getScanner(s);
			Result sult = res.next();
			KeyValue key;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
