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
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
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
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.util.GenericOptionsParser;

import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

/**
 * Export an HBase table. Writes content to sequence files up in HDFS. Use
 * {@link Import} to read it back in again.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class UserSizeExport {
	private static final Log LOG = LogFactory.getLog(UserSizeExport.class);
	final static String NAME = "userSizeExport";
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
//		Path outputDir = new Path(GtDataConfig.HDFS_ROOT_PATH,args[1]);
		GtPath userPathObj = new GtPath(args[2]);
		String userPath = userPathObj.getGtPath().replace("//", "/");
		conf.set("USER_PATH", userPath);
		conf.set("USER_SIZE_PATH", new Path(GtDataConfig.HDFS_ROOT_PATH,args[1]).toString());
		conf.set(TableOutputFormat.OUTPUT_TABLE, tableName);
		Job job = Job.getInstance(conf, NAME + "_" + tableName);
		job.setJobName(NAME + "_" + tableName);
		job.setJarByClass(UserSizeExport.class);
		// Set optional scan parameters
		Scan s = getConfiguredScanForJob(conf, args);
		// MetaTableMapper.initJob(tableName, s, MetaTableMapper.class, job);
		// No reducers. Just write straight to output files.
//		TableMapReduceUtil.initTableMapperJob(tableName, s, MetaMapper.class,
//				Text.class, LongWritable.class, job);
		TableMapReduceUtil.initTableMapperJob(tableName, s, MetaMapper.class, Text.class, LongWritable.class, job,false,true,TableInputFormat.class);
		job.setReducerClass(UserSizeReducer.class);
		int reduceNum = 1;
		if (args.length > 4) {
			reduceNum = Integer.valueOf(args[4]);		
		}
		job.setNumReduceTasks(reduceNum);
		job.setOutputFormatClass(TableOutputFormat.class);		
//		job.setOutputFormatClass(SequenceFileOutputFormat.class);
//		job.setOutputKeyClass(Text.class);
//		job.setOutputValueClass(LongWritable.class);
//		FileOutputFormat.setOutputPath(job, outputDir);
		return job;
	}

	private static Scan getConfiguredScanForJob(Configuration conf,
			String[] args) throws IOException {
		Scan s = new Scan();
		// Optional arguments.
		// Set Scan Range
		GtPath userPathObj = new GtPath(args[2]);
		String userPath = userPathObj.getGtPath().replace("//", "/");	
		String startRow = userPath + "/";
		String stopRow = userPath + "/{";
		s.setStartRow(Bytes.toBytes(startRow));
		s.setStopRow(Bytes.toBytes(stopRow));
		// Set cache blocks
		s.setCacheBlocks(false);
		// Set Scan Column Family
		boolean raw = Boolean.parseBoolean(conf.get(RAW_SCAN));
		if (raw) {
			s.setRaw(raw);
		}
		long startTime = 0;
	    if(args.length > 3){
	    	startTime = Long.parseLong(args[3]);
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

	static class MetaMapper extends TableMapper<Text, LongWritable> {
		Text sizeText = new Text();
		LongWritable size = new LongWritable();
		String userPath;
		static long count = 0;
		
		protected void setup(Context context) throws IOException,
		InterruptedException {
			Configuration conf = context.getConfiguration();
			userPath = conf.get("USER_PATH");
		}
		public void map(ImmutableBytesWritable key, Result value,
				Context context) throws IOException, InterruptedException {
			String row = new String(key.copyBytes());
			byte[] sizeGet = value.getValue(GtDataConfig.META.FAMILY.byteVal,
					GtDataConfig.META.SIZE.byteVal);
			count++;
			try{
				if(!Arrays.equals(sizeGet, GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)){
					String userName  = path2userName(row,userPath);
					size.set(Long.parseLong(Bytes.toString(sizeGet)));
					context.write(new Text(userName), size);
				}
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("error: " + count + ":" + row);
			}		
		}
	}

	static class UserSizeReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
		FileSystem fs;
		FSDataOutputStream out;
		Text username = new Text();
		LongWritable size = new LongWritable();
		static long count = 0;

		protected void setup(Context context) throws IOException,
				InterruptedException {
			Configuration conf = context.getConfiguration();
			fs = FileSystem.get(conf);
			TaskID task = context.getTaskAttemptID().getTaskID();
			String taskStr = task.toString();
			String partStr = taskStr.substring(taskStr.lastIndexOf('_') + 1);
			String useSizePath = conf.get("USER_SIZE_PATH");
			Path path = new Path(useSizePath, partStr);
			out = fs.create(path, true);
		}

		public void reduce(Text key, Iterable<LongWritable> value,
				Context context) throws IOException, InterruptedException {
			String userName = Bytes.toString(key.copyBytes());
			long totalSize = 0;
			Iterator<LongWritable> it = value.iterator();
			while(it.hasNext()){
				totalSize += it.next().get();
			}
			System.out.println(userName + ":" + totalSize);
			out.write((userName + ":" + totalSize).getBytes());
			out.write("\n".getBytes());
//			username.set(userName);
//			size.set(totalSize);
//			context.write(username, size);
		}

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			if (out != null)
				out.close();
			if (fs != null)
				fs.close();
		}
	}
	
	private static String path2userName(String path,String userPath){
		String userName = path.replace(userPath, "");
		if(userName.startsWith("/")){
			userName = userName.substring(1);
		}
		int index = userName.indexOf("/");
		userName = userName.substring(0, index);
		return userName;	
	}

	/*
	 * @param errorMsg Error message. Can be null.
	 */
	private static void usage(final String errorMsg) {
		if (errorMsg != null && errorMsg.length() > 0) {
			System.err.println("ERROR: " + errorMsg);
		}
		System.err
				.println("Usage: Export <meta-tablename> <outputdir> <user path> <start time>\n");
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
//		 args = new String[] { "meta",
//		 "nanlin_root/export-usersize",
//		 "/users" , "1420300860000" };
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
