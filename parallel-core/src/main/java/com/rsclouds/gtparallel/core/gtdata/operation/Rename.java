package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.core.gtdata.common.RedisUtils;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class Rename extends Configured implements Tool {
	private static final Logger LOG = LoggerFactory.getLogger(Rename.class);
	public static HTable metaTable = null;
	public static final String OLD_PATH = "old.path";
	public static final String NEW_PATH = "new.path";
	public static final String IS_DEL = "isdel";
	
	public static class Map extends TableMapper<Text, Text>{
		private  String oldPath = "";
		private  String newPath = "";
		private  boolean isdel = false;
		Configuration conf;
		
		@Override  
	    protected void setup(Context context) throws IOException,  
	        InterruptedException { 
			conf = context.getConfiguration();
			oldPath = conf.get(OLD_PATH);
			newPath = conf.get(NEW_PATH);
			isdel = Boolean.valueOf(conf.get(IS_DEL));
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			LOG.info("oldPath : " + oldPath);
			LOG.info("newPath : " + newPath); 
	    } 
		
		@Override 
		protected void cleanup(Context context) throws IOException,  
	        InterruptedException { 
			 if(metaTable != null){
				 metaTable.flushCommits();
				 metaTable.close();
			 }
		 }
		
		//实现map函数
        public void map(ImmutableBytesWritable rowKey, Result result,Context context)throws IOException,InterruptedException{
        	String key = Bytes.toString(rowKey.get());
        	LOG.info("from : " + key);
        	LOG.info(" to  : " + key.replace(oldPath, newPath));
        	NavigableMap<byte[], byte[]> map = result.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
        	Put metaPut = new Put(Bytes.toBytes(key.replace(oldPath, newPath)));
        	if(map != null){
        		for(Entry<byte[], byte[]> entry : map.entrySet()){
//        			System.out.println(Bytes.toString(entry.getKey()) + ":" + Bytes.toString(entry.getValue()));
//        			if(Arrays.equals(entry.getKey(), GtDataConfig.META.TIME.byteVal)){
//        				metaPut.add(GtDataConfig.META.FAMILY.byteVal, entry.getKey(), Bytes.toBytes(System.currentTimeMillis()+""));
//        			}else{
        				metaPut.add(GtDataConfig.META.FAMILY.byteVal, entry.getKey(), entry.getValue());
//        			}    			
        		}
        	}
        	metaTable.put(metaPut);
        	if(isdel)
        		context.write(new Text(key), new Text("1"));
        }
	}
	
	public static class Reduce extends TableReducer<Text, Text,  NullWritable> {
		Configuration conf;
		HTable metaTable;
		
		@Override  
	    protected void setup(Context context) throws IOException,  
	        InterruptedException { 
			conf = context.getConfiguration();
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
	    }
		
		@Override 
		protected void cleanup(Context context) throws IOException,  
	        InterruptedException { 
			 if(metaTable != null){
				 metaTable.flushCommits();
				 metaTable.close();
			 }
		 }
		
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
//			System.out.println("reduce delete : " + key.toString()+",values : " + values);
//			HTable metaTable = new HTable(conf, ParameterDefine.META_TABLENAME);
			metaTable.delete(new Delete(Bytes.toBytes(key.toString())));
			RedisUtils.redisDel(GtDataConfig.REDIS_HOST, key.toString());
		}
	}


	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = HBaseConfiguration.create();
		GtPath oldPathObj = new GtPath(args[0]);
		GtPath newPathObj = new GtPath(args[1]);
		String startRow = oldPathObj.getGtPath().replace("//", "/");
		conf.set(TableInputFormat.SCAN_ROW_START, startRow + "/");
		conf.set(TableInputFormat.SCAN_ROW_STOP, startRow + "/{");
		conf.set(OLD_PATH, startRow);
		conf.set(NEW_PATH, newPathObj.getGtPath().replace("//", "/"));
		boolean isDel = false;
		if(args.length > 2){
			isDel = Boolean.valueOf(args[2]);
		}
		conf.set(IS_DEL, isDel+"");
		
		
//		Scan scan = new Scan();
//		scan.addFamily(ParameterDefine.META_FAMILY);
//		Filter filter = new RowFilter(CompareOp.EQUAL,new BinaryPrefixComparator(args[1].getBytes()));
//		scan.setFilter(filter);
//		scan.setBatch(400);
//		scan.setCacheBlocks(false);
//		scan.setStartRow(Bytes.toBytes(args[0] + "!"));
//		scan.setStopRow(Bytes.toBytes(args[0] + "z"));
		
		conf.set(TableInputFormat.INPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal()); 
		conf.set(TableInputFormat.SCAN_COLUMN_FAMILY, GtDataConfig.META.FAMILY.strVal);
//		conf.set(TableInputFormat.SCAN_ROW_START, args[0] + "!");
//		conf.set(TableInputFormat.SCAN_ROW_STOP, args[0] + "{");
		conf.set(TableOutputFormat.OUTPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());

		Job job = Job.getInstance(conf, "RenameMapReduce");
		job.setJarByClass(Rename.class);

		job.setOutputFormatClass(TableOutputFormat.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapperClass(Map.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setReducerClass(Reduce.class);
		job.setNumReduceTasks(3); 

//		TableMapReduceUtil.initTableMapperJob(Bytes.toBytes(ParameterDefine.META_TABLENAME), scan,
//				Map.class,Writable.class, Writable.class,job, true);
//		TableMapReduceUtil.initTableReducerJob(
//				ParameterDefine.META_TABLENAME_DEFAULT, ResampleReducer.class,
//				job);
		boolean status = job.waitForCompletion(true);
		if(status){
			GtDataUtils.genterGtdataDir(newPathObj.getGtPath());
			if(isDel)
				HbaseBase.deleteRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), oldPathObj.getGtPath());
			RedisUtils.redisFileCheck(newPathObj.getGtPath(), GtDataConfig.REDIS_HOST);
		}
		return  status? 0 : 1;
	}
	
	

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		args = new String[2];
		args[0] = "/cutting/test";
		args[1] = "/cutting/开源软甲";
		int status = 1;
		if ( args.length < 2){
			System.out.println("usage:<oldPath> <newPath> <isDel>");
		}else{
			status = ToolRunner.run(new Rename(), args);
		}
		System.exit(status);

	}
}
