package com.rsclouds.gtparallel.core.mutilcenter;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeMetaCenter extends Configured implements Tool {
	public static Logger LOG = LoggerFactory.getLogger(ChangeMetaCenter.class);
	public static final String KEY_OLD_CENTER = "old_center";
	public static final String KEY_NEW_CENTER = "new_center";
	public static final byte[] META_FAMILY_BYTES = "atts".getBytes();
	public static final byte[] META_QUALIFIER_DFS_BYTES = "dfs".getBytes();
	
	public static class MapperRead extends TableMapper<ImmutableBytesWritable, Text> {
		public String strOldCenter;
		public String strNewCenter;
		public Text dfsValue = new Text();
		
		
		protected void setup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			strOldCenter = conf.get(KEY_OLD_CENTER, "");
			strNewCenter = conf.get(KEY_NEW_CENTER, "");
		}
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException {
			byte[] dfsBytes = value.getValue(META_FAMILY_BYTES, META_QUALIFIER_DFS_BYTES);
			if (dfsBytes != null && dfsBytes.length > 0) {
				String strDfs = new String(dfsBytes);
				if (strDfs.startsWith(strOldCenter)) {
					strDfs = strDfs.replace(strOldCenter, strNewCenter);
					dfsValue.set(strDfs);
					context.write(key, dfsValue);
				}
			}
		}
	}

	public static class ReducerWrite extends TableReducer<ImmutableBytesWritable, Text, NullWritable> {
		public String strNewCenter;
		public Text valueText = new Text();
		
		protected void setup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			strNewCenter = conf.get(KEY_NEW_CENTER, "");
		}
		
		public void reduce(ImmutableBytesWritable key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException{
			Put put = new Put(key.get());
			Iterator<Text> iterator = values.iterator();
			valueText.set(iterator.next());
			put.add(META_FAMILY_BYTES, META_QUALIFIER_DFS_BYTES, valueText.toString().getBytes());
			context.write(NullWritable.get(), put);
		}
	}
	
	public int run(String[] args)throws Exception {
		if (args.length != 3) {
			usage();
			return 0;
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set(KEY_OLD_CENTER, args[1]);
		conf.set(KEY_NEW_CENTER, args[2]);
		
		conf.set(TableInputFormat.INPUT_TABLE, args[0]);
		conf.set(TableOutputFormat.OUTPUT_TABLE, args[0]);
		Job job = Job.getInstance(conf);
		
		job.setJarByClass(ChangeMetaCenter.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setMapOutputKeyClass(ImmutableBytesWritable.class);
		job.setMapOutputValueClass(Text.class);
		
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public void usage() {
		LOG.info("usage: <meta table name> <odl center> <new center>");
	}
	
	public static void main(String[] args) throws Exception {
		int status = ToolRunner.run(new ChangeMetaCenter(), args);
		System.exit(status);
	}
}
