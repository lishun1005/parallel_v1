package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
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

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.HttpClientBase;
import com.rsclouds.gtparallel.core.gtdata.cutting.ParserTime;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

/**
 * 删除过期的影像tar.gz包
 * @author root
 *
 */
public class DeleteTarGzFile extends Configured implements Tool{
	
	private static final Logger LOG = LoggerFactory.getLogger(DeleteTarGzFile.class);
	private static final String DATE_BEFORE = "date";
	public static final String PATHFILTER = "pathfilter";
	public static final String META_TABLENAME = "meta";
	public static final String RES_TABLENAME = "res";
	public static final String TEST_BOOL = "test";

	public static class TableReaderMap extends TableMapper<Text, Text> {
		public Text urlText = new Text();
		public Text pathText = new Text();
		
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException{
			String keyStr = new String(key.copyBytes());
			byte[] url = value.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
			byte[] size = value.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
			if (size == null || size.length == 0) {
				System.out.println(new String(key.get()));
				return;
			}
			String sizeStr = new String(size);
			pathText.set(keyStr);
			if(url == null || url.length == 0)
				return;
			urlText.set(new String(url) + "," + sizeStr);
			context.write(urlText, pathText);
		}
	}
	
	public static class TableDelete extends Reducer<Text, Text, Text, Text> {
		private long dateMills;
		private long imagedate;
		private boolean curlStatus;
		
		public Text pathText = new Text();
		public Text keyDefault = new Text("=====nanlin===== debug");
		public String prefix = null;
		public HTable metaTable = null;
		public HTable resTable = null;
		public FileSystem fs = null;
		public boolean deleteFlag;
		public boolean testFlag;
		public long totalSize = 0l;
		public long fileSize;
		public Jedis jedis;
		public List<String> pathList;
		public List<String> pathListTemp;
		
		protected void setup(Context context)throws IOException, InterruptedException{
			Configuration conf = context.getConfiguration();
			String metaTableStr = conf.get(META_TABLENAME, "meta");
			String resTableStr = conf.get(RES_TABLENAME, "res");
			testFlag = conf.getBoolean(TEST_BOOL, true);
			dateMills = conf.getLong(DATE_BEFORE, 0);
			fs = FileSystem.get(conf);
			metaTable = new HTable(conf, metaTableStr);
			resTable = new HTable(conf, resTableStr);
			prefix = conf.get(PATHFILTER, null);
			jedis = new Jedis(GtDataConfig.REDIS_HOST, GtDataConfig.REDIS_PORT, 30000);
			pathList = new ArrayList<String>();
			pathListTemp = new ArrayList<String>();
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
		
		public boolean deleteFile(HTable resTable, FileSystem fs, String key) {
			if(resTable == null || fs == null || StringUtils.isEmpty(key)) {
				return false;
			}
			Delete delete = new Delete(key.getBytes());
			try {
				resTable.delete(delete);
				resTable.flushCommits();
				Path path = new Path(GtDataConfig.HDFS_MD5_PATH + "/" + key);
				if (fs.exists(path)) {
					fs.delete(path, false);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		private String getFileName(String path) {
			String filename = GtDataUtils.format2DisplayPath(path);
			int indexof = filename.lastIndexOf("/");
			filename = filename.substring(indexof+1);
			return filename;
		}
		
		public void reduce(Text key, Iterable<Text>value, Context context)throws
				IOException, InterruptedException {
			
			if(prefix == null || dateMills == 0) {
				return;
			}
			pathList.clear();
			pathListTemp.clear();
			String[] splits = key.toString().split(",");
			fileSize = Long.parseLong(splits[1]);
			deleteFlag = true;
			for(Text val : value) {
				String path = val.toString();
				
				if (path.endsWith(".tar.gz") && path.startsWith(prefix)) {
					try {
						if(path.contains("/PL/")) {
							imagedate = Long.parseLong(ParserTime.parserPLTimeMilliSeconds(path));
						}else {
							imagedate = Long.parseLong(ParserTime.parserTimeMilliSeconds(path));
						}
						if(imagedate < dateMills) {
							pathList.add(path);
						}
					}catch(NumberFormatException e) {
						System.out.println("=====nanlin====degbug parsertime error");
						deleteFlag = false;
						break;
					}
					
				}else if(path.startsWith("/users/rscloudmart/data_temp/") || path.startsWith("/projects/rscloudmart/data/")
						|| path.startsWith("-///users/rscloudmart/data/")) {
					pathListTemp.add(path);
				}else {
					deleteFlag = false;
					break;
				}
			}
			if(deleteFlag) {
				if(pathList.size() > 0) {
					for(int i = 0; i < pathList.size(); i ++) {
						if (testFlag) {
							pathText.set(key.toString() + " " + pathList.get(i));
							context.write(keyDefault, pathText);
						}
						String pathStr = pathList.get(i);
						String param = "name=" + getFileName(pathStr);
						curlStatus = HttpClientBase.httpGetStatus(CoreConfig.URL_DELETE_ACK, param);

						if(curlStatus && testFlag == false) {
							if (deleteMetaTable(metaTable, pathStr) ){
								jedis.del(pathStr);
								pathText.set(pathStr + CoreConfig.URL_DELETE_ACK + " " + param + " " + curlStatus);
								context.write(key, pathText);
							}
							else {
								pathText.set(pathStr + " delete metaTable failed");
								context.write(key, pathText);
								deleteFlag = false;
								break;
							}				
						}else if(curlStatus == false){
							pathText.set(pathStr + CoreConfig.URL_DELETE_ACK + " " + param + " " + curlStatus);
							context.write(key, pathText);
							deleteFlag = false;
							break;
						}
						if(testFlag == true) {
							pathText.set(pathStr + " test " + CoreConfig.URL_DELETE_ACK + " " + param + " " + curlStatus);
							context.write(key, pathText);
						}
					}
				}else {
					deleteFlag = false;
				}
				
				if(deleteFlag) {
					for(int i = 0; i < pathListTemp.size(); i ++) {
						String pathStr = pathListTemp.get(i);						
						if(testFlag == false) {
							if (deleteMetaTable(metaTable, pathStr) ){
								jedis.del(pathStr);
								pathText.set(pathStr + " delete metaTable sucessed");
								context.write(key, pathText);
							}else {
								pathText.set(pathStr + " delete metaTable failed");
								context.write(key, pathText);
								deleteFlag = false;
								break;
							}				
						}
						if(deleteFlag && testFlag == true) {
							pathText.set(pathStr + " test");
							context.write(key, pathText);
						}
					}
				}
				
				if (deleteFlag && testFlag == false) {
					if (deleteFile(resTable, fs, splits[0])) {
						pathText.set("delete resource sucessed");
						context.write(key, pathText);
						totalSize += fileSize;
					}
					else {
						pathText.set("delete resource failed");
						context.write(key, pathText);
					}
				}else if (testFlag == false) {
					
				}
			}	
		}
		
		protected void cleanup(Context context)throws IOException,
				InterruptedException {
			context.write(new Text(prefix), new Text(totalSize + ""));
		}
	}
	
	
	@Override
	public int run(String[] args) throws Exception {
		if(args.length < 4) {
			usage();
			return 0;
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set(META_TABLENAME, args[0]);
		conf.set(RES_TABLENAME, args[1]);
		conf.set(PATHFILTER, args[2]);
		String time = ParserTime.parserTimeMilliSeconds(args[3]+"_");
		conf.setLong(DATE_BEFORE, Long.parseLong(time));
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
		Job job = Job.getInstance(conf, "delete file of tar.gz ");
		job.setJarByClass(DeleteTarGzFile.class);
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
		LOG.info("usage: <meta table> <res table> <delete path> <date> [test]");
		LOG.info("date: the date of image, and it's format is yyyyMMdd");
		LOG.info("test: true or false, if true, will be not deleted file, else will be deleted file");
	}
	
	public static void main(String[] args)throws Exception {
//		args = new String[5];
//		args[0] = "meta";
//		args[1] = "res";
//		args[2] = "/users/rscloudmart/data/GF1/";
//		args[3] = "20141010";
//		args[4] = "true";
		int status = ToolRunner.run(new DeleteTarGzFile(), args);
		System.exit(status);
	}

}
