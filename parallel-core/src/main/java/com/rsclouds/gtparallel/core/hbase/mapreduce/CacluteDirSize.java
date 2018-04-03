package com.rsclouds.gtparallel.core.hbase.mapreduce;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class CacluteDirSize extends Configured implements Tool{
	
	private static final Logger LOG = LoggerFactory.getLogger(CacluteDirSize.class);
	
	static class MapperRead extends TableMapper<Text, IntWritable> {
		
		public IntWritable size = new IntWritable();
		public Text keyPath = new Text();
		private byte[] familyByte = "atts".getBytes();
		private byte[] qualifierSize = "size".getBytes();
		private String dirPath;
		
		protected void setup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			dirPath = conf.get("dirpath", "/");
			keyPath.set(dirPath);
		}
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException{
			System.out.println(new String(key.copyBytes()));
			byte[] sizeBytes = value.getValue(familyByte, qualifierSize);
			int sizeInt = Integer.parseInt(new String(sizeBytes));
			if (sizeInt > 0) {
				size.set(sizeInt);
				context.write(keyPath, size);
			}
		}
	}
	
	static class ReducerMerge extends Reducer<Text, IntWritable, Text, LongWritable> {
		public LongWritable sumLongWritable = new LongWritable();
		public long sumLong = 0;
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			for (IntWritable value : values) {
				sumLong += value.get();
			}
			sumLongWritable.set(sumLong);
			context.write(key, sumLongWritable);
		}
	}
	
	public int run(String[] args)throws Exception{
		if (args.length < 2) {
			usage();
			return 0;
		}
		Configuration conf = HBaseConfiguration.create();
		String tablename = args[0];
		String prefix = args[1];
		
		
		while (prefix.endsWith("/")) {
			prefix = prefix.substring(0, prefix.length()-1);
		}
		
		conf.set("dirPath", prefix);
		conf.set(TableInputFormat.INPUT_TABLE, tablename);
		conf.set(TableInputFormat.SCAN_ROW_START, prefix + "/L00/");
		conf.set(TableInputFormat.SCAN_ROW_STOP, prefix + "/{");
		Job job = Job.getInstance(conf);
		job.setJarByClass(CacluteDirSize.class);
		job.setJobName("caclute titles size");
		job.setInputFormatClass(TableInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setMapperClass(MapperRead.class);
		job.setReducerClass(ReducerMerge.class);	
		Path path = new Path(GtDataConfig.HDFS_ROOT_PATH + "temp/" + System.currentTimeMillis());
		FileOutputFormat.setOutputPath(job, path);
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public void usage() {
		LOG.info("usage: <metatable name> <dir path>");
	}
	
	public static void main(String[] args)throws Exception {
		int status = ToolRunner.run(new CacluteDirSize(), args);
		System.exit(status);
	}
	
}
