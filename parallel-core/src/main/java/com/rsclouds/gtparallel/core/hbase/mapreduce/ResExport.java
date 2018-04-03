package com.rsclouds.gtparallel.core.hbase.mapreduce;

/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

/**
 * Export an HBase table. Writes content to sequence files up in HDFS. Use
 * {@link Import} to read it back in again.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class ResExport {
	private static final Log LOG = LogFactory.getLog(ResExport.class);
	final static String NAME = "export";
	final static String RAW_SCAN = "hbase.mapreduce.include.deleted.rows";
	final static String EXPORT_BATCHING = "hbase.export.scanner.batch";

	/**
	 * Sets up the actual job.
	 * 
	 * @param conf
	 *            The current configuration.
	 * @param args
	 *            The command line parameters.
	 * @return The newly created job.
	 * @throws IOException
	 *             When setting up the job fails.
	 */
	public static Job createSubmittableJob(Configuration conf, String[] args)
			throws IOException {
		String tableName = args[0];
		Path outputDir = new Path(GtDataConfig.HDFS_ROOT_PATH,args[2]);
		conf.set("resourceTable", args[1]);
		conf.set("md5OutputDir", new Path(GtDataConfig.HDFS_ROOT_PATH,args[3]).toString());
		Job job = Job.getInstance(conf, NAME + "_" + tableName);
		job.setJobName(NAME + "_" + tableName);
		job.setJarByClass(ResExport.class);
		// Set optional scan parameters
		Scan s = getConfiguredScanForJob(conf, args);
		// MetaTableMapper.initJob(tableName, s, MetaTableMapper.class, job);
		// No reducers. Just write straight to output files.
		TableMapReduceUtil.initTableMapperJob(tableName, s, MetaMapper.class,
				BytesWritable.class, Text.class, job);
		job.setReducerClass(ResReducer.class);
		int reduceNum = 10;
		if (args.length > 7) {
			reduceNum = Integer.valueOf(args[7]);
		}
		job.setNumReduceTasks(reduceNum);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setOutputKeyClass(ImmutableBytesWritable.class);
		job.setOutputValueClass(Result.class);
		FileOutputFormat.setOutputPath(job, outputDir);
		return job;
	}

	private static Scan getConfiguredScanForJob(Configuration conf,
			String[] args) throws IOException {
		Scan s = new Scan();
		// Optional arguments.
		// Set Scan Range
		String startRow = args.length > 4 ? args[4] : null;
		String stopRow = args.length > 5 ? args[5] : null;
		if (startRow != null)
			s.setStartRow(Bytes.toBytes(startRow));
		if (stopRow != null)
			s.setStopRow(Bytes.toBytes(stopRow));
		// Set cache blocks
		s.setCacheBlocks(false);
		// Set Scan Column Family
		boolean raw = Boolean.parseBoolean(conf.get(RAW_SCAN));
		if (raw) {
			s.setRaw(raw);
		}
		long startTime = 0;
	    if(args.length > 6){
	    	startTime = Long.parseLong(args[6]);
	    	LOG.info("startTime=" + startTime );
	    	s.setTimeRange(startTime, System.currentTimeMillis());
	    }
		if (conf.get(TableInputFormat.SCAN_COLUMN_FAMILY) != null) {
			LOG.info("SCAN_COLUMN_FAMILY : "
					+ conf.get(TableInputFormat.SCAN_COLUMN_FAMILY));
			s.addFamily(Bytes.toBytes(conf
					.get(TableInputFormat.SCAN_COLUMN_FAMILY)));
		}
		// Set RowFilter or Prefix Filter if applicable.
		LOG.info("startRow=" + startRow + ", stopRow=" + stopRow);
		return s;
	}

	@SuppressWarnings("unused")
	private static Filter getExportFilter(String[] args) {
		Filter exportFilter = null;
		String filterCriteria = (args.length > 5) ? args[5] : null;
		if (filterCriteria == null)
			return null;
		if (filterCriteria.startsWith("^")) {
			String regexPattern = filterCriteria.substring(1,
					filterCriteria.length());
			exportFilter = new RowFilter(CompareOp.EQUAL,
					new RegexStringComparator(regexPattern));
		} else {
			exportFilter = new PrefixFilter(Bytes.toBytes(filterCriteria));
		}
		return exportFilter;
	}

	static class MetaMapper extends TableMapper<BytesWritable, Text> {
		BytesWritable MD5 = new BytesWritable();
		Text dfsText = new Text();
		byte[] url;
		byte[] dfs;

		public void map(ImmutableBytesWritable key, Result value,
				Context context) throws IOException, InterruptedException {
			String row = new String(key.copyBytes());
			if (row.startsWith("/projects/rscloudmart/data/GF1/20141117Rar")) {
				return;
			} else if (row.startsWith("/projects/rscloudmart/data/GF1/20140826")) {
				return;
			} else if (row.startsWith("/projects/rscloudmart/data/GF1/20140828Src")) {
				return;
			} else if (row.startsWith("/projects/rscloudmart/data/GF1/20140901Src")) {
				return;
			} else if (row.startsWith("/projects/rscloudmart/data/GF1/20140919Src")) {
				return;
			} else if (row.startsWith("/projects/rscloudmart/data/GF1/20141103Src")) {
				return;
			} else if (row.startsWith("/projects/rscloudmart/data/LC8/20140909Src")) {
				return;
			}else if (row.startsWith("/projects/rscloudmart/userftp")) {
				return;
			}else if (row.startsWith("/users/xiaoshaolin")) {
				return;
			 } else {
				url = value.getValue(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.URL.byteVal);
				dfs = value.getValue(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.DFS.byteVal);
				if (url != null && url.length > 0) {
					MD5.set(url, 0, url.length);
					dfsText.set(dfs);
					context.write(MD5, dfsText);
				}
			}
		}
	}

	static class ResReducer extends
			Reducer<BytesWritable, Text, ImmutableBytesWritable, Result> {
		HTable resTable;
		FileSystem fs;
		FSDataOutputStream out;
		ImmutableBytesWritable rowkey = new ImmutableBytesWritable();

		protected void setup(Context context) throws IOException,
				InterruptedException {
			Configuration conf = context.getConfiguration();
			String MD5Dir = conf.get("md5OutputDir");
			String restableName = conf.get("resourceTable",
					GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			resTable = new HTable(conf, restableName);
			fs = FileSystem.get(conf);
			TaskID task = context.getTaskAttemptID().getTaskID();
			String taskStr = task.toString();
			String partStr = taskStr.substring(taskStr.lastIndexOf('_') + 1);
			Path path = new Path(MD5Dir, partStr);
			out = fs.create(path, true);
		}

		public void reduce(BytesWritable key, Iterable<Text> value,
				Context context) throws IOException, InterruptedException {
			byte[] md5Byte = key.copyBytes();
			Get get = new Get(md5Byte);
			Result result = resTable.get(get);
			rowkey.set(md5Byte);
			context.write(rowkey, result);
			System.out.println(Bytes.toString(md5Byte));
			if (value.iterator().next().toString().equalsIgnoreCase("1")) {
				out.write(md5Byte);
				out.write("\n".getBytes());
			}
		}

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			if (resTable != null)
				resTable.close();
			if (out != null)
				out.close();
			if (fs != null)
				fs.close();
		}
	}

	/*
	 * @param errorMsg Error message. Can be null.
	 */
	private static void usage(final String errorMsg) {
		if (errorMsg != null && errorMsg.length() > 0) {
			System.err.println("ERROR: " + errorMsg);
		}
		System.err
				.println("Usage: Export <meta-tablename> <res-tablename> <outputdir> [<start_row> <stop_row> <start_time> <reduce_num>]\n");
	}

	/**
	 * Main entry point.
	 * 
	 * @param args
	 *            The command line parameters.
	 * @throws Exception
	 *             When running the job fails.
	 */
	public static void main(String[] args) throws Exception {
		// args = new String[] { "meta", "res",
		// "/nanlin_root/export-test",
		// "/nanlin_root/export-md5",
		// "/download/", "/download/{" ,"1420300860000" , 10};
		Configuration conf = HBaseConfiguration.create();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		if (otherArgs.length < 4) {
			usage("Wrong number of arguments: " + otherArgs.length);
			System.exit(-1);
		}
		Job job = createSubmittableJob(conf, otherArgs);
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
