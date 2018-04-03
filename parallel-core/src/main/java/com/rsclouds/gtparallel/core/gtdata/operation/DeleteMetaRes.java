package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
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
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class DeleteMetaRes extends Configured implements Tool{
	public static Logger LOG = LoggerFactory.getLogger(DeleteMetaRes.class);
	public static final String PATHFILTER = "pathfilter";
	public static final String META_TABLENAME = "meta";
	public static final String RES_TABLENAME = "res";
	public static final String TEST_BOOL = "test";
	public static final String DATE_BEFORE = "date";
	
	public static class MapperRead extends TableMapper<Text, Text> {
		public long interval_times;
		public long current_time;
		public long time;
		public Text urlText = new Text();
		public Text pathText = new Text();
		
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			int date = conf.getInt(DATE_BEFORE, 0);
			interval_times = date * 24 * 60 * 60;
			current_time = System.currentTimeMillis();
		}
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException{
			byte[] url = value.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
			byte[] size = value.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
			if(size == null || size.length == 0){
				System.out.println(new String(key.get()));
				return;
			}
			String sizeStr = new String(size);
			pathText.set(new String(key.copyBytes()));
			if(url == null || url.length == 0)
				return;
			time = value.getColumnCells(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal).get(0).getTimestamp();
			if(current_time - time > interval_times)
				urlText.set(new String(url) + "," + sizeStr);
			context.write(urlText, pathText);
		}
	}
	
	public static class ReducerFilter extends Reducer<Text, Text, Text, Text> {
		public Text pathText = new Text();
		public String pathFilter = null;
		public HTable metaTable = null;
		public HTable resTable = null;
		public boolean deleteFlag = false;
		public boolean notDeleteFlag = false;
		public boolean testFlag;
		public long totalSize = 0l;
		public long fileSize;
		public Jedis jedis;
		
		protected void setup(Context context)throws
				IOException, InterruptedException{
			Configuration conf = context.getConfiguration();
			String metaTableStr = conf.get(META_TABLENAME, "meta");
			String resTableStr = conf.get(RES_TABLENAME, "res");
			testFlag = conf.getBoolean(TEST_BOOL, true);
			metaTable = new HTable(conf, metaTableStr);
			resTable = new HTable(conf, resTableStr);
			pathFilter = conf.get(PATHFILTER, null);
			jedis = new Jedis(GtDataConfig.REDIS_HOST, GtDataConfig.REDIS_PORT, 30000);
//			jedis = new Jedis("192.168.2.6", GtDataConfig.REDIS_PORT, 30000);
		}
		
		public boolean deleteMetaTable(HTable metaTable, String key) {
			if(metaTable == null || StringUtils.isEmpty(key))
				return false;
			Delete delete = new Delete(key.getBytes());
			try {
				metaTable.delete(delete);
				metaTable.flushCommits();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		public boolean deleteFile(HTable resTable, String key) {
			if(resTable == null || StringUtils.isEmpty(key)) {
				return false;
			}
			Delete delete = new Delete(key.getBytes());
			try {
				resTable.delete(delete);
				resTable.flushCommits();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		public void reduce(Text key, Iterable<Text>value, Context context)throws
				IOException, InterruptedException {
			deleteFlag = false;
			notDeleteFlag = false;
			if(pathFilter == null) {
				return;
			}
			String[] splits = key.toString().split(",");
			fileSize = Long.parseLong(splits[1]);
			for(Text val : value) {
				if(val.find(pathFilter) == 0 ) {//匹配需要删除的路径
					deleteFlag = true;
					if(!testFlag) { //不是测试状态，真实删除数据
						if( deleteMetaTable(metaTable, val.toString()) ){
							if(jedis != null) {
								if(jedis.del(val.toString()) != 1){
									pathText.set("delete redis failed: " + val.toString());
									context.write(key, pathText);
								}
//								System.out.println(jedis.del(splits[0]));
							}
							pathText.set(val.toString() + " sucessed");
							context.write(key, pathText);
						}
						else {
							pathText.set(val.toString() + " failed");
							context.write(key, pathText);
						}
					} else {//测试状态，直接输出需要删除记录
						pathText.set(val.toString());
						context.write(key, pathText);
					}
				}else {
					notDeleteFlag = true;
				}
			}
			if(deleteFlag && notDeleteFlag == false && testFlag == false) {
				if (deleteFile(resTable, splits[0])) {
					pathText.set("resource delete sucessed");
					context.write(key, pathText);
					totalSize += fileSize;
				}
				else {
					pathText.set("hdfs delete failed");
					context.write(key, pathText);
				}
			}
			
		}
		
		protected void cleanup(Context context)throws IOException,
				InterruptedException {
			context.write(new Text(pathFilter), new Text(totalSize + ""));
		}
	}
	
	public int run(String[] args) throws Exception {
		if(args.length < 3) {
			usage();
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set(META_TABLENAME, args[0]);
		conf.set(RES_TABLENAME, args[1]);
		conf.set(PATHFILTER, args[2]);
		conf.setInt(DATE_BEFORE, Integer.parseInt(args[3]));
		if(args.length == 5) {
			if(args[4].equalsIgnoreCase("true"))
				conf.setBoolean(TEST_BOOL, true);
			else {
				conf.setBoolean(TEST_BOOL, false);
			}
		}else {
			conf.setBoolean(TEST_BOOL, true);
		}
		conf.set(TableInputFormat.INPUT_TABLE, args[0]);
		Job job = Job.getInstance(conf, "delete redundancy data");
		job.setJarByClass(DeleteMetaRes.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapperClass(MapperRead.class);
		job.setReducerClass(ReducerFilter.class);
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
		int status = ToolRunner.run(new DeleteMetaRes(), args);
		System.exit(status);
	}
	
	public void usage() {
		LOG.info("<meta table name> <resource table name> <path delete>  [days before] [test flag]");
	}
}
