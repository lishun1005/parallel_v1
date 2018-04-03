package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;


public class ZipDecompress extends Configured implements Tool{
	private static final Logger LOG = LoggerFactory.getLogger(ZipDecompress.class);
	private static final String restableName = "restable_name";
	
	static class ZipDecompressMapper extends Mapper<Text, BytesWritable, Text, Text> {
		final String ONE = "1";
		final String NEGATIVE_ONE = "-1";
		final String ZERO = "0";
		String storagePath = "storage_path";
		
		Configuration conf;
		Configuration hbaseConfig;
		HTable resTable = null;
		StringBuilder filePathBuild = new StringBuilder("");
		Text keyOut = new Text();
		Text valueOut = new Text();
		

		public void setup(Context context) throws IOException,
				InterruptedException {
			conf = context.getConfiguration();
			storagePath = conf.get("storage_path");
			hbaseConfig = HBaseConfiguration.create();
			String resName = conf.get(restableName, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			System.out.println("output restablename: " + resName);
			resTable = new HTable(hbaseConfig, resName);
			while(storagePath.length()>1 && storagePath.endsWith("/")){
				storagePath = storagePath.substring(0, storagePath.length()-1);
			}
		}

		public void map(Text key, BytesWritable value, Context context)
				throws IOException, InterruptedException {
//			LOG.info(key.toString() +" : " + value.getLength());			
//			String filePath = "";		
			try {	
				
//				filePath = storagePath + "/" + key.toString();
				filePathBuild.append(storagePath + "/" + key.toString());
//				if(filePath.endsWith("/")){
//					filePath = filePath.substring(0, filePath.length()- 1);
//				}
				if(filePathBuild.lastIndexOf("/") == filePathBuild.length() - 1){
					filePathBuild.deleteCharAt(filePathBuild.length() - 1);
				}
				//处理瓦片L层级命名问题
//				if(filePath.contains("_alllayers/L")){
//					int index = filePath.indexOf("_alllayers/L")+11;
//					if(index + 3 <= filePath.length()){
//						int x = Integer.valueOf(filePath.substring(index+1, index+3));
//						if(x >= 10){
//							String s = Integer.toHexString(x);
//							if(s.length()<2){
//								s = "0" + s;
//							}
//							filePath = filePath.replace("/L"+x, "/L"+s);
//						}					
//					}					
//				}
				//处理瓦片L层级命名问题
				if(filePathBuild.indexOf("_alllayers/L") != -1){
					int index = filePathBuild.indexOf("_alllayers/L")+11;
					if(index + 3 <= filePathBuild.length()){
						int x = Integer.valueOf(filePathBuild.substring(index+1, index+3));
						if(x >= 10){
							String s = Integer.toHexString(x);
							if(s.length()<2){
								s = "0" + s;
							}
							filePathBuild.replace(index+1, index+3, s);
						}					
					}					
				}
				//中文编码
//				filePath = TransCoding.UrlEncode(filePath, "utf-8");
				filePathBuild.replace(0, filePathBuild.length(), TransCoding.UrlEncode(filePathBuild.toString(), "utf-8"));
//				filePath = RedisUtils.replaceLast(filePath, "/", "//");
				filePathBuild.replace(0, filePathBuild.length(), GtDataUtils.replaceLast(filePathBuild.toString(), "/", "//"));
				byte[] md5Bytes = null;
				String md5Str = "";
				long fileSize = value.getLength();
				if(key.toString().endsWith("/")){
					//写入文件夹	
//					LOG.info("========写入文件夹:"+filePath+"========");
				}else if(value.getLength() == 0){
					//空文件使用一样的MD5
					md5Str = "d41d8cd98f00b204e9800998ecf8427e";
					md5Bytes = Bytes.toBytes(md5Str);
//					LOG.info("========写入空文件:"+filePath+"========MD5:"+md5Str);
				}else{
					md5Str = MD5Calculate.fileByteMD5(value.copyBytes());
					md5Bytes = Bytes.toBytes(md5Str);
//					LOG.info("========写入文件:"+filePath+"========MD5:"+md5Str+"========size:"+fileSize);
				}	
//				String size = "";
				String url = "";
//				String dfs = "";
				if(md5Bytes == null){
//					size = NEGATIVE_ONE;
					fileSize = -1;
					url = "";
//					dfs = ZERO;
				}else{
//					size = ""+fileSize;
					url = md5Str;
					
					Put resPut = new Put(md5Bytes);				
					Get get = new Get(md5Bytes);		
					Result result = resTable.get(get);
					if (result != null && !result.isEmpty()) {
//						LOG.info("======file exists on gt-data:"+md5Str);
//						String links_count = new String(result.getValue(
//								ParameterDefine.RESOURCE_FAMILY,
//								ParameterDefine.RESOURCE_LINKS));
//						int linksInt = Integer.parseInt(links_count) + 1;
//						resPut.add(ParameterDefine.RESOURCE_FAMILY,
//								ParameterDefine.RESOURCE_LINKS,
//								Bytes.toBytes("" + linksInt));
//						resTable.put(resPut);
					} else {// file doesn't exists on gt-data
//						LOG.info("======file doesn't exists on gt-data:"+md5Str);
						resPut.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
								GtDataConfig.RESOURCE.LINKS.byteVal, GtDataConfig.CONSTANT.ONE.byteVal);
						resPut.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
								GtDataConfig.RESOURCE.SIZE.byteVal, Bytes.toBytes(fileSize+""));
						if (fileSize < 16777216) {// less than 16MB,input hbase
							resPut.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
									GtDataConfig.RESOURCE.DATA.byteVal, value.copyBytes());
						} else {// more than 16MB, input hdfs
							FileSystem fs = FileSystem.get(hbaseConfig);
							FSDataOutputStream out = fs.create(new Path(GtDataConfig.HDFS_MD5_PATH , new String(md5Bytes)));
							out.write(value.copyBytes(), 0, value.getLength());
							out.close();
							fs.close();
						}
						resTable.put(resPut);
					}
				}
				keyOut.set(filePathBuild.toString());
				if(fileSize < 16777216){
					valueOut.set(fileSize+","+url+","+ZERO);
				}else{
					valueOut.set(fileSize+","+url+","+ONE);
				}
				
				context.write(keyOut, valueOut);
			}catch (IOException e) {
				e.printStackTrace();
				throw e;
			} finally {
				filePathBuild.delete(0, filePathBuild.length());
			}
		}

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			if (resTable != null) {
				resTable.flushCommits();
				resTable.close();
			}
		}
	}
	
	static class ZipDecompressReducer extends TableReducer<Text, Text, NullWritable> {
		Configuration hbaseConfig = HBaseConfiguration.create(); 
		
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			Iterator<Text> it = values.iterator();
			while (it.hasNext()) {
				Text value = it.next();
				LOG.info(key.toString() + " : " + value.toString());
				String[] args = value.toString().split(",");
				if (args.length != 3) {
					// error
				}
				byte[] rowkey = Bytes.toBytes(key.toString());
				GtPath gtpath = new GtPath(new String(rowkey));
				GtDataUtils.genterGtdataDir(gtpath.getDisplayParent());
				Put metaPut = new Put(rowkey);
				if (args[0].equals("-1")) {
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.PERMISSON.byteVal, Bytes.toBytes("1110000000110"));
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.CAPACITY.byteVal, Bytes.toBytes("00"));
				}else {
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.PERMISSON.byteVal, Bytes.toBytes("0110000000110"));
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.CAPACITY.byteVal, Bytes.toBytes("10"));
				}
				metaPut.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.SIZE.byteVal, Bytes.toBytes(args[0]));
				metaPut.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.URL.byteVal, Bytes.toBytes(args[1]));
				metaPut.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.DFS.byteVal, Bytes.toBytes(args[2]));
				metaPut.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.TIME.byteVal,
						Bytes.toBytes("" + System.currentTimeMillis()));
				context.write(NullWritable.get(), metaPut);
			}
		}
	}
	
//	public static boolean createTable(String tablename, String[] cfs) throws IOException {
//		Configuration conf = HBaseConfiguration.create();
//		HBaseAdmin admin = new HBaseAdmin(conf);
//		if (admin.tableExists(tablename)) {
//			System.out.println("table is already exist : " + tablename);
//			return false;
//		} else {
//			HTableDescriptor tableDesc = new HTableDescriptor(tablename);
//			for (int i = 0; i < cfs.length; i++) {
//				tableDesc.addFamily(new HColumnDescriptor(cfs[i]));
//			}
//			admin.createTable(tableDesc);
//			System.out.println("createTable success : " + tablename);
//			return true;
//		}	
//	}
	
	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 4) {
			LOG.info("usage: <metatable> <restable> <inputpath> <Storage_path>");
			return 0;
		}
		Configuration conf = getConf() == null ? HBaseConfiguration.create()
				: getConf();
		conf.set("mapred.child.java.opts", "-Xmx4096m");
		conf.setInt("mapred.tasktracker.map.tasks.maximum", 1);

		conf.set("storage_path", args[3]);
		conf.set(restableName, args[1]);
		conf.set(TableOutputFormat.OUTPUT_TABLE, args[0]);
		Job job = Job.getInstance(conf);

		//createTable(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), new String[]{GtDataConfig.META.FAMILY.strVal});
		//createTable(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), new String[]{GtDataConfig.RESOURCE.FAMILY.strVal});
	
		job.setJarByClass(ZipDecompress.class);
		job.setMapperClass(ZipDecompressMapper.class);
		job.setReducerClass(ZipDecompressReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setInputFormatClass(ZipInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setNumReduceTasks(6);

		FileSystem fs = FileSystem.get(conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(args[2]));
		List<Path> paths = new ArrayList<Path>();
		for ( int i = 0; i < fileStatus.length; i++) {
			Path p = fileStatus[i].getPath();
			if(p.getName().endsWith(".zip")){
				paths.add(p);
			}
		}
		FileInputFormat.setInputPaths(job, paths.toArray(new Path[paths.size()]));
		//生成gtdata输出目录结构
		if (!GtDataUtils.genterGtdataDir(args[3])) {
			LOG.info("ERROR <Storage_path> : " + args[3]);
			return 1;
		}
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
//		args = new String[2];
//		args[0] = "hdfs://node03.rsclouds.cn:8020/nanlin_root/L16.zip";
//		args[1] = "/import/test16/_alllayers/";
		int exitCode = ToolRunner.run(new ZipDecompress(), args);
		long end = System.currentTimeMillis();
		LOG.info("ZIP decompress time : " + (end - start) + "(ms)");
		System.exit(exitCode);
	}

}
