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

public class ScanPrefix extends Configured implements Tool{
	
	private static final Logger LOG = LoggerFactory.getLogger(ScanPrefix.class);
	public static final String PATHFILTER = "pathfilter";
	public static final String META_TABLENAME = "meta";
	public static final String KEYWORD = "keywords";

	public static class TableReaderMap extends TableMapper<Text, NullWritable> {
		public Text pathText = new Text();
		private String keyWords = null;
		
		protected void setup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			keyWords = conf.get(KEYWORD, null);
		}
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException{
			if(keyWords == null) {
				return;
			}
			String keyStr = new String(key.copyBytes());
			//keyStr = GtDataUtils.format2DisplayPath(keyStr);
			if(keyStr.indexOf(keyWords) != -1) {
				pathText.set(keyStr);
				context.write(pathText, NullWritable.get());
			}
		}
	}
	
	@Override
	public int run(String[] args) throws Exception {
		if(args.length < 3) {
			usage();
			return 0;
		}
		Configuration conf = HBaseConfiguration.create();
		String prefix = args[1];
		while(prefix.endsWith("/")) {
			prefix = prefix.substring(0, prefix.length()-1);
		}
		//conf.set(PATHFILTER, args[1]);
		conf.set(KEYWORD, args[2]);
		conf.set(TableInputFormat.INPUT_TABLE, args[0]);
		conf.set(TableInputFormat.SCAN_ROW_START, prefix + "//");
		conf.set(TableInputFormat.SCAN_ROW_STOP, prefix + "/{");
		
		Job job = Job.getInstance(conf, "scan");
		job.setJarByClass(ScanPrefix.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapperClass(TableReaderMap.class);
		Path path = new Path(GtDataConfig.HDFS_ROOT_PATH + "temp/" + System.currentTimeMillis());
		FileOutputFormat.setOutputPath(job, path);
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	
	
	public void  usage() {
		LOG.info("usage: <meta table> <prefix path> <keywords>");
		LOG.info("date: the date of image, and it's format is yyyyMMdd");
		LOG.info("test: true or false, if true, will be not deleted file, else will be deleted file");
	}
	
	public static void main(String[] args)throws Exception {
		int status = ToolRunner.run(new ScanPrefix(), args);
		System.exit(status);
	}

}
