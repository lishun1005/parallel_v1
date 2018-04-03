package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class DeleteMeta extends Configured implements Tool{
	public static Logger LOG = LoggerFactory.getLogger(DeleteMeta.class);
	public static final String PATHFILTER = "pathfilter";
	public static final String META_TABLENAME = "meta";
	public static final String RES_TABLENAME = "res";
	public static final String TEST_BOOL = "test";
	public static final String DATE_BEFORE = "date";
	
	public static class MapperRead extends TableMapper<Text, NullWritable> {
		public long interval_times;
		public long current_time;
		public long time;
		public Text pathText = new Text();
		private HTable metaTable;
		private String prefix;
		private boolean testFlag;
		
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			int date = conf.getInt(DATE_BEFORE, 0);
			String tablename = conf.get(META_TABLENAME, "meta");
			metaTable = new HTable(conf, tablename);
			testFlag = conf.getBoolean(TEST_BOOL, true);
			prefix = conf.get(PATHFILTER, null);
			metaTable.setAutoFlushTo(false);
			interval_times = date * 24 * 60 * 60;
			current_time = System.currentTimeMillis();
		}
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException{
			time = value.getColumnCells(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal).get(0).getTimestamp();
			if(current_time - time > interval_times) {
				String rowkerStr = new String(key.copyBytes());
				if(prefix == null || !rowkerStr.startsWith(prefix)) {
					System.out.println(rowkerStr);
					return;
				}
				if (!testFlag) {
					Delete del = new Delete(key.copyBytes());
					metaTable.delete(del);
				}
				pathText.set(key.copyBytes());
				context.write(pathText, NullWritable.get());
			}else {
				return;
			}
		}
		
		public void cleanup(Context context) throws IOException, InterruptedException {
			if(metaTable != null) {
				metaTable.flushCommits();
				metaTable.close();
			}
		}
	}
	
	
	
	public int run(String[] args) throws Exception {
		if(args.length < 3) {
			usage();
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set(META_TABLENAME, args[0]);
		conf.set(PATHFILTER, args[1]);
		conf.setInt(DATE_BEFORE, Integer.parseInt(args[2]));
		if(args.length == 4) {
			if(args[3].equalsIgnoreCase("true"))
				conf.setBoolean(TEST_BOOL, true);
			else {
				conf.setBoolean(TEST_BOOL, false);
			}
		}else {
			conf.setBoolean(TEST_BOOL, true);
		}
		String delePath = args[1];
		while(delePath.endsWith("/"))
			delePath = delePath.substring(0, delePath.length()-1);
		conf.set(TableInputFormat.INPUT_TABLE, args[0]);
		conf.set(TableInputFormat.SCAN_ROW_START, delePath + "/");
		conf.set(TableInputFormat.SCAN_ROW_STOP, delePath + "/{");
		Job job = Job.getInstance(conf, "delete redundancy data");
		job.setJarByClass(DeleteMeta.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapperClass(MapperRead.class);
		Path path = new Path(GtDataConfig.HDFS_ROOT_PATH + "temp/" + System.currentTimeMillis());
		FileOutputFormat.setOutputPath(job, path);
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public static void main(String[] args)throws Exception {
//		args = new String[5];
//		args[0] = "meta_2.6";
//		args[1] = "res_2.6";
//		args[2] = "/users/yongkun/xiaoshaolin";
//		args[3] = "1";
//		args[4] = "true";
		int status = ToolRunner.run(new DeleteMeta(), args);
		System.exit(status);
	}
	
	public void usage() {
		LOG.info("<meta table name> <path delete>  [days before] [test flag]");
	}
}
