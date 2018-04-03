package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
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

public class ScanTarGzFile extends Configured implements Tool{
	
	private static final Logger LOG = LoggerFactory.getLogger(ScanTarGzFile.class);

	public static class TableReaderMap extends TableMapper<Text, Text> {
		public Text urlText = new Text();
		public Text pathText = new Text();
		
		
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException{
			String keyStr = new String(key.copyBytes());
			if(!keyStr.endsWith("tar.gz"))
				return;
			byte[] url = value.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
			pathText.set(keyStr);
			if(url == null || url.length == 0)
				return;
			urlText.set(new String(url));
			context.write(urlText, pathText);
		}
	}
	
	public static class TableDelete extends Reducer<Text, Text, Text, Text> {
		public Text pathText = new Text();
		private StringBuilder strBuilder = new StringBuilder();
		private int count;
		
		public void reduce(Text key, Iterable<Text>value, Context context)throws
				IOException, InterruptedException {
			count = 0;
			strBuilder.delete(0, strBuilder.length());
			for(Text val : value) {
				strBuilder.append(val.toString() + ", ");
				count ++;
			}
			strBuilder.insert(0, count+", ");
			pathText.set(strBuilder.toString());
			context.write(key, pathText);
			
		}
	}
	
	
	@Override
	public int run(String[] args) throws Exception {
		if(args.length < 1) {
			usage();
			return 0;
		}
		Configuration conf = HBaseConfiguration.create();

		conf.set(TableInputFormat.INPUT_TABLE, args[0]);
		Job job = Job.getInstance(conf, "delete file of tar.gz ");
		job.setJarByClass(ScanTarGzFile.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapperClass(TableReaderMap.class);
		job.setReducerClass(TableDelete.class);
		job.setNumReduceTasks(6);
		Path path = new Path(GtDataConfig.HDFS_ROOT_PATH + "temp/" + System.currentTimeMillis());
		FileOutputFormat.setOutputPath(job, path);
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	
	
	public void  usage() {
		LOG.info("usage: <meta table>");
	}
	
	public static void main(String[] args)throws Exception {
		int status = ToolRunner.run(new ScanTarGzFile(), args);
		System.exit(status);
	}
}