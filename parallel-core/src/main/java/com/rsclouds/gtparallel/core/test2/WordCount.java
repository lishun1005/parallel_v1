package com.rsclouds.gtparallel.core.test2;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.operation.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordCount extends Configured{


	public static class TokenizerMapper extends Mapper<LongWritable, Text, Text, IntWritable>{
		private final static Logger logger = LoggerFactory.getLogger(TokenizerMapper.class);

		private final static IntWritable one = new IntWritable(1);

		private Text word = new Text();
		public void setup(Context context)throws IOException, InterruptedException {
			logger.info("22222222222222222222222222222222");
		}
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(value.toString());
			logger.info("33333333333333333333333333333333");
			while (itr.hasMoreTokens()) {
				word.set(itr.nextToken());
				context.write(word, one);
			}
		}
	}

	public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
		private IntWritable result = new IntWritable(); 
		public void reduce(Text key, Iterable<IntWritable> values,Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			result.set(sum);
			context.write(key, result);
		}
	}

	public static void main(String[] args) throws Exception {
		//args = new String[]{"hdfs://192.168.2.3:8020/nanlin_root/yongkun/wordcount/input","hdfs://192.168.2.3:8020/nanlin_root/yongkun/wordcount/output"};
		//args = new String[]{"G:\\wc\\i","G:\\wc\\o"};
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		Job job = Job.getInstance(conf, "word count");
		job.setJarByClass(WordCount.class);
		job.setMapperClass(TokenizerMapper.class);
		//job.setCombinerClass(IntSumReducer.class);
		job.setReducerClass(IntSumReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
//		Import importFile = new Import();
//		importFile.ImportFromLocal(CoreConfig.MAP_META_TABLE, CoreConfig.MAP_RES_TABLE, "D://nanlin//conf.xml", "/2015051401/Layers/conf.xml");
//		importFile.ImportFromLocal(CoreConfig.MAP_META_TABLE, CoreConfig.MAP_RES_TABLE, "D://nanlin//conf.cdi", "/2015051401/Layers/conf.cdi");
		/*File dir = new File("D://nanlin//image//MSS-ENHANCE");
		File[] files = dir.listFiles();
		for(int i = 0; i < files.length; i++) {
			File tmpfile = files[i].listFiles()[0];
			String filename = tmpfile.getName();
			if(filename.equals("GF1_PMS1_E116.5_N23.3_20140418_L1A0000205963-MSS1.tiff")) {
				System.out.println(i);
				break;
			}
		}*/
		StringTokenizer itr = new StringTokenizer("lishun wdwd lishun");
		int i = 0;
		IntWritable one = new IntWritable(1);
		while (itr.hasMoreTokens()) {
			i = i + 1;
			System.out.println(itr.nextToken());
			System.out.println(i + "," + one);
		}
		//OutputCollector<Text, IntWritable> output

	}
}
