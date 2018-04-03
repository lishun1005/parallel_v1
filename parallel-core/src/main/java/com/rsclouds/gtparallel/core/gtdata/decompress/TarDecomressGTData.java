package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.RedisUtils;
import com.rsclouds.gtparallel.core.gtdata.common.UntarGTDataUtil;
import com.rsclouds.gtparallel.core.hadoop.io.MD5FileSize;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class TarDecomressGTData extends Configured implements Tool{

	public final static  Log LOG = LogFactory.getLog(TarDecomressGTData.class);
	private final static String OUTPATH = "outpath";
	
	static class TableRead extends TableMapper<Text, MD5FileSize> {
		private Text filePath = new Text();
		private MD5FileSize md5FileSize = new MD5FileSize(); 
		
		protected void setup(Context context) throws IOException,
		InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			Map<String,String> map = new HashMap<String,String>();
			map.put(CoreConfig.JOB.JID.strVal, context.getJobID().toString());
			map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.RUNNING.toString());
			String rowkey = conf.get(CoreConfig.JOBID);
			HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, rowkey, CoreConfig.JOB.FAMILY.strVal, map);
		}
		
		public void map(ImmutableBytesWritable key, Result values,
				Context context) throws IOException, InterruptedException {
			String keyStr = Bytes.toString(key.copyBytes());
			if(!keyStr.endsWith(".tar.gz")){
				LOG.info("pass : " + keyStr);
				return;
			}
			String fileSizeStr = new String (values.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
			String md5Str = new String (values.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal));
			long fileSize = Long.parseLong(fileSizeStr);
			if ( !fileSizeStr.equals("-1")){
				filePath.set(key.copyBytes());
				md5FileSize.setMd5(md5Str);
				md5FileSize.setFileSize(fileSize);
				context.write(filePath, md5FileSize);
				LOG.info("map : " + fileSizeStr + " " + keyStr);
			}		
		}
	}
	
	static class DecompressReduce extends Reducer<Text, MD5FileSize, Text, NullWritable> {
		private HTable resTable;
		private HTable metaTable;
		private String outputPath;
	
		protected void setup(Context context)throws IOException, InterruptedException{
			Configuration conf = context.getConfiguration();
			resTable = new HTable(conf, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			outputPath = conf.get(OUTPATH);
		}
		
		public void reduce(Text key, Iterable<MD5FileSize> values, Context context)throws
				IOException, InterruptedException {
			LOG.info("reduce key : " +  Bytes.toString(key.copyBytes()));
			GtPath path = new GtPath( Bytes.toString(key.copyBytes()));
			String filename = path.getDisplayFileName();
			filename = GtDataUtils.replaceLast(filename, ".tar.gz", "");
			for (MD5FileSize val : values) {			
				String url = val.getMd5().toString();
				if ( val.getFileSize().get() < GtDataConfig.HBASE_FILESIZE_MAX) {
					UntarGTDataUtil.unTarHbase(metaTable, resTable, url, filename, outputPath);
				}else {
					UntarGTDataUtil.unTarHDFS(metaTable, resTable, url, filename, outputPath);
				}
			}
		}
		
		public String trimExtension(String filename) {
			if ((filename != null) && (filename.length() > 0)) {
				int i = filename.lastIndexOf('.');
				if ((i > -1) && (i < (filename.length()))) {
					return filename.substring(0, i);
				}
			}
			return filename;
		}
	}
	
	public int run(String[] args) throws Exception {
		if ( args.length < 3) {
			LOG.info("usage: <jobid> <gt-data inputPath> <gt-data outputPath>");
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set(TableInputFormat.INPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		String startRow = GtDataUtils.format2GtPath(args[1]).replace("//", "/");
		String stopRow = startRow + "/{";
		conf.set(TableInputFormat.SCAN_ROW_START, startRow+"/");
		conf.set(TableInputFormat.SCAN_ROW_STOP, stopRow);
		conf.set(CoreConfig.JOBID, args[0]);
		GtPath outpath = new GtPath(args[2]);
		conf.set(OUTPATH,outpath.getGtPath());
		Job job = Job.getInstance(conf);
		job.setJarByClass(TarDecomressGTData.class);
		FileOutputFormat.setOutputPath(job, new Path("/temp/output/"+ Math.random()));
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapperClass(TableRead.class);
		job.setReducerClass(DecompressReduce.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(MD5FileSize.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setNumReduceTasks(15);
		job.setJobName("TarDecompress");
		
		boolean flag = job.waitForCompletion(true);
		if(flag){
			// 生成gtdata输出目录结构
			if (!GtDataUtils.genterGtdataDir(args[2])) {
				LOG.info("ERROR <Storage_path> : " + args[2]);
				return 1;
			}
			// redis check
			RedisUtils.redisDirCheck(outpath.getGtPath(), GtDataConfig.REDIS_HOST);
		}
		return job.waitForCompletion(true) ? 0 : 1;
	}
	

	
	public static void main(String[] args) throws Exception{
//		args = new String[]{"/projects/rscloudmart/data/GF1/20140826/GF1_PMS1_E110.3_N20.5_20131115_L1A0000111501","/download/test2"};
		int status = ToolRunner.run(new TarDecomressGTData(), args);
		System.exit(status);
//		String s = "/dfe/asdf//dfe";
//		System.out.println(s.substring(s.lastIndexOf("//")+2));
	}
	

}
