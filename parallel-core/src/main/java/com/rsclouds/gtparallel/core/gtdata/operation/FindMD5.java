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
import org.apache.hadoop.hbase.util.Bytes;
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

public class FindMD5 extends Configured implements Tool{

	private static final Logger LOG = LoggerFactory.getLogger(FindMD5.class);
	private static final String KEY_MD5_STR = "md5";
	
	public static class TableReader extends TableMapper<Text, Text> {
		private String md5 = null;
		private byte[] familly = Bytes.toBytes("atts");
		private byte[] col_url = Bytes.toBytes("url");
		private Text keyText = new Text();
		private Text valueText = new Text();
		
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			md5 = conf.get(KEY_MD5_STR, null);
			if(md5 != null) {
				keyText.set(md5);
			}
		}
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException {
			if(md5 == null)
				return;
			byte[] md5Bytes = value.getValue(familly, col_url);
			if(md5Bytes == null || md5Bytes.length == 0) {
				return;
			}
			
			String md5Current = new String(md5Bytes);
			if(md5Current.equals(md5)) {
				valueText.set(key.copyBytes());
				context.write(keyText, valueText);
			}
		}
	}
	
	public static class Md5Ouput extends Reducer<Text, Text, Text, Text> {
		public void reduce(Text key, Iterable<Text> values, Context context)throws
				IOException, InterruptedException {
			for(Text val : values) {
				context.write(key, val);
			}
		}
	}
	
	public int run(String[] args) throws Exception {
		if(args.length < 2) {
			LOG.info("usage <meta table> <md5>");
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set(TableInputFormat.INPUT_TABLE, args[0]);
		conf.set(KEY_MD5_STR, args[1]);
		Job job = Job.getInstance(conf, "md5 find");
		job.setJarByClass(FindMD5.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapperClass(TableReader.class);
		job.setReducerClass(Md5Ouput.class);
		
		Path path = new Path(GtDataConfig.HDFS_ROOT_PATH + "temp/" + System.currentTimeMillis());
		FileOutputFormat.setOutputPath(job, path);
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public static void main(String[] args)throws Exception {
		int status = ToolRunner.run(new FindMD5(), args);
		System.exit(status);
	}
}
