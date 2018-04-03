package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class ResSizeRepairByMR extends Configured implements Tool {
	private static Logger LOG = LoggerFactory.getLogger(ResSizeRepairByMR.class);

	public static class Map extends TableMapper<Text, Text> {
		Configuration conf;

		@Override
		public void setup(Context context) {
			conf = context.getConfiguration();
		}

		public void map(ImmutableBytesWritable key, Result result,Context context) throws IOException, InterruptedException
		{
			byte[] size = result.getValue(GtDataConfig.META.FAMILY.byteVal,GtDataConfig.META.SIZE.byteVal);
			byte[] md5 = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);			
			if(size == null || Bytes.toString(size).isEmpty()){
				LOG.info("error row :" + Bytes.toString(result.getRow()));			
//				HbaseBase.deleteRow(GtDataConfig.META_TABLENAME, Bytes.toString(result.getRow()));
				return;
			}
			if(!Bytes.equals(size, GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)){
				if(md5 == null || Bytes.toString(md5).isEmpty()){
					LOG.info("error row :" + Bytes.toString(result.getRow()));		
					return;
				}
//				LOG.info("rowkey : " + Bytes.toString(key.copyBytes()));
				Result res = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), Bytes.toString(md5));
				if(!res.isEmpty()){	
					byte[] sizeGet = res.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.SIZE.byteVal);
					if(sizeGet == null || sizeGet.length == 0){
//						LOG.info("write " +Bytes.toString(md5)+ " : " + Bytes.toString(size) );				
						HbaseBase.writeRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
								md5,GtDataConfig.RESOURCE.FAMILY.byteVal,
								GtDataConfig.RESOURCE.SIZE.byteVal, size);
					}
				}
			}
//			context.write(new Text(user), new Text(size));
		}
	}

//	public static class Reduce extends TableReducer<Text, Text, NullWritable> {
//		Configuration conf;
//		
//		@Override
//		public void setup(Context context) {
//			conf = context.getConfiguration();
//			
//		}
//		
//		public void reduce(Text key, Iterable<Text> values, Context context)throws IOException{
//		}
//		
//	}

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = HBaseConfiguration.create();
		conf.set(TableInputFormat.INPUT_TABLE,GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		conf.set(TableOutputFormat.OUTPUT_TABLE,GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		conf.set(TableInputFormat.SCAN_COLUMN_FAMILY,GtDataConfig.META.FAMILY.strVal);
		conf.set(TableInputFormat.SCAN_BATCHSIZE, "100");
		conf.set(TableInputFormat.SCAN_CACHEBLOCKS, "false");
		conf.setStrings("mapred.child.java.opts", "-Xmx6000m");
		conf.setInt("mapreduce.map.memory.mb", 3072);
		if(args!= null && args.length > 0 ){
			conf.set(TableInputFormat.SCAN_ROW_START, args[0]);
		}
		if(args!= null && args.length > 1 ){
			conf.set(TableInputFormat.SCAN_ROW_STOP, args[1]);
		}
		Job job = Job.getInstance(conf, "ResSizeRepair");		
		job.setJarByClass(ResSizeRepairByMR.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setMapperClass(Map.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
//		job.setReducerClass(Reduce.class);
		boolean status = job.waitForCompletion(true);
		return  status? 0 : 1;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int status = ToolRunner.run(new ResSizeRepairByMR(), args);
		System.exit(status);
	}

}
