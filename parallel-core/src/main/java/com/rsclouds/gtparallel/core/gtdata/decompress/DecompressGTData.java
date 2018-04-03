package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
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

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.api.GtDataImpl;
import com.rsclouds.gtparallel.core.gtdata.common.RedisUtils;
import com.rsclouds.gtparallel.core.gtdata.common.UntarGTDataUtil;
import com.rsclouds.gtparallel.core.gtdata.common.UnzipGTDataUtil;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;


public class DecompressGTData extends Configured implements Tool{
	private static final Logger LOG = LoggerFactory.getLogger(DecompressGTData.class);
	private static final String OUTPATH = "out_path";
	
	public static class ZipDecompressMapper extends TableMapper<Text, Text> {
		Configuration conf;
		Configuration hbaseConfig;
		
		public void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			Map<String,String> map = new HashMap<String,String>();
			map.put(CoreConfig.JOB.JID.strVal, context.getJobID().toString());
			map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.RUNNING.toString());
			String rowkey = conf.get(CoreConfig.JOBID);
			HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, rowkey, CoreConfig.JOB.FAMILY.strVal, map);
		}

		public void map(ImmutableBytesWritable key, Result result, Context context)
				throws IOException, InterruptedException {	
			try {	
				String keyStr = Bytes.toString(key.copyBytes());
				if(!keyStr.endsWith(".zip") && !keyStr.endsWith(".tar.gz")){
					LOG.info("pass : " + keyStr);
					return;
				}
				String fileSizeStr = new String (result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
				String md5Str = new String (result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal));
				if ( !fileSizeStr.equals("-1") && !StringUtils.isEmpty(md5Str)){
					context.write(new Text(key.copyBytes()), new Text(md5Str+","+fileSizeStr));
					LOG.info("map : " + keyStr  + ":"  + md5Str +":" +  fileSizeStr );
				}
			}catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
		}

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
		}
	}
	
	public static class ZipDecompressReducer extends TableReducer<Text, Text, NullWritable> {
		Configuration hbaseConfig = HBaseConfiguration.create(); 
		Configuration conf;
		String outPath;
		private HTable resTable;
		private HTable metaTable;
	
		public void setup(Context context) throws IOException,
		InterruptedException {
			super.setup(context);
			conf = context.getConfiguration();
			outPath = conf.get(OUTPATH);
			resTable = new HTable(conf, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		}

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			byte[] rowkey = key.copyBytes();
			GtPath filepath = new GtPath( Bytes.toString(rowkey));
			String fileName = filepath.getDisplayFileName();
			GtPath outPathObj = new GtPath(outPath);
			Iterator<Text> it = values.iterator();
			if(it.hasNext()) {
				Text value = it.next();
				LOG.info(key.toString() + "{" + value.toString() + "}");
				String[] args = value.toString().split(",");	
				String md5 = args[0];
				long size = 0;
				try{
					size = Long.valueOf(args[1]);
				}catch(NumberFormatException nfe){
					nfe.printStackTrace();
					return;
				}
				
				if(size > GtDataConfig.HBASE_FILESIZE_MAX){
					if(fileName.endsWith(".zip")){
						fileName = fileName.replace(".zip", "");
						UnzipGTDataUtil.unZipHDFS(md5, outPathObj.getGtPath(),fileName);
					}else if(fileName.endsWith(".tar.gz")){
						fileName = fileName.replace(".tar.gz", "");
						UntarGTDataUtil.unTarHDFS(metaTable, resTable, md5, fileName, outPathObj.getGtPath());
					}
				}else{
					if(fileName.endsWith(".zip")){
						fileName = fileName.replace(".zip", "");
						UnzipGTDataUtil.unZipHbase(md5, outPathObj.getGtPath(),fileName);
					}else if(fileName.endsWith(".tar.gz")){
						fileName = fileName.replace(".tar.gz", "");
						UntarGTDataUtil.unTarHbase(metaTable, resTable, md5, fileName, outPathObj.getGtPath());
					}		
				}
				GtDataImpl.mkdir(outPath+"/"+fileName, false);
			}
		}
		
		public void cleanup(Context context) throws IOException,
		InterruptedException {
			if(metaTable!= null)
				metaTable.close();
			if(resTable!= null){
				resTable.close();
			}
		}
	}
	
	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 3) {
			LOG.info("usage: <jobid> <inputpath> <outputpath> <recursive(default:true)>");
			return 0;
		}
		GtPath inPathObj = new GtPath(args[1]);
		String fileSize = GtDataUtils.getFileSzie(inPathObj.getGtPath());
		boolean isFile = false;
		if(fileSize == null){
			return 1;
		}else if(!fileSize.equals(GtDataConfig.CONSTANT.NEGATIVE_ONE.strVal)){
			//单文件解压
//			boolean flag = false;
//			if(inPathObj.getDisplayFileName().endsWith(".zip")){
//				flag = UnzipGTDataUtil.unZipFromGTData(inPathObj.getGtPath(), args[2], inPathObj.getDisplayFileName().replace(".zip", ""));
//			}else if(inPathObj.getDisplayFileName().endsWith(".tar.gz")){
//				flag = UntarGTDataUtil.unTarFromGTData(inPathObj.getGtPath(),args[2],inPathObj.getDisplayFileName().replace(".tar.gz", ""));
//			}
//			return flag?0:1;
			isFile = true;
		}
		boolean recursive = false;
		if(args.length>3){
			recursive = Boolean.valueOf(args[3]);
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set("mapred.child.java.opts", "-Xmx4096m");
		conf.setInt("mapred.tasktracker.map.tasks.maximum", 1);
		
		conf.set(TableInputFormat.INPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		conf.set(TableOutputFormat.OUTPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		conf.set(CoreConfig.JOBID, args[0]);
		String startRow = inPathObj.getGtPath().replace("//", "/");
		if(isFile){
			conf.set(TableInputFormat.SCAN_ROW_START, inPathObj.getGtPath());
			conf.set(TableInputFormat.SCAN_ROW_STOP,  inPathObj.getGtPath());
		}else{
			if(recursive){
				//递归检索
				conf.set(TableInputFormat.SCAN_ROW_START, startRow+"/");
				conf.set(TableInputFormat.SCAN_ROW_STOP,  startRow + "/{");
			}else{
				conf.set(TableInputFormat.SCAN_ROW_START, startRow+"//");
				conf.set(TableInputFormat.SCAN_ROW_STOP,  startRow + "//{");
			}	
		}	
		conf.set(OUTPATH, args[2]);
		
		
		
		Job job = Job.getInstance(conf,"DecompressGTData");

		HbaseBase.createTable(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), new String[]{GtDataConfig.META.FAMILY.strVal});
		HbaseBase.createTable(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), new String[]{GtDataConfig.RESOURCE.FAMILY.strVal});
	
		job.setJarByClass(DecompressGTData.class);
		job.setMapperClass(ZipDecompressMapper.class);
		job.setReducerClass(ZipDecompressReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setNumReduceTasks(6);

		
		boolean flag = job.waitForCompletion(true);
		if(flag){
			//生成gtdata输出目录结构
			if (!GtDataUtils.genterGtdataDir(args[2])) {
				LOG.info("ERROR <outputpath> : " + args[2]);
				return 1;
			}
			// redis check
			RedisUtils.redisDirCheck(new GtPath(args[2]).getGtPath(), GtDataConfig.REDIS_HOST);
		}
		return  flag? 0 : 1;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
//		args = new String[]{"jobid","/test/zipfiles","/test/zipout"};
		int exitCode = ToolRunner.run(new DecompressGTData(), args);
		long end = System.currentTimeMillis();
		LOG.info("decompress time : " + (end - start) + "(ms)");
		System.exit(exitCode);
	}

}
