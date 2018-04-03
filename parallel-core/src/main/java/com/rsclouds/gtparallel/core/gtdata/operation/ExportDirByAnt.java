package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.tools.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.ZipByAntUtil;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.TableRegionSplitInputFormat;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ZipFileOutputFormat;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;



public class ExportDirByAnt extends Configured implements Tool{
	private static Logger LOG = LoggerFactory
			.getLogger(ExportDirByAnt.class);
	public static final String OUTPUTPATH = "outputpath";
	public static final String INPUTPATH = "inputpath";
	public static final String MINSPLIT = "minSplit";
	public static final String KEY_KEYWORDS_STRING = "keywords";
	public int PERSPLIT_RECORDS_MAX = Integer.MAX_VALUE/3*2;

	static class MapperCompress extends TableMapper<Text, BytesWritable> {
		private HTable htable;
		private String inputPath = "";
		private FileSystem fs = null;
		private OutputStream out = null;
		private ZipOutputStream zos;
		private File tempFile = null;
		private String outGtPath = null;
		private String keywords = null;

		public void setup(Context context) {
			Configuration conf = context.getConfiguration();
			int spiltSize = conf.getInt("splits", 0);
			String jobid = conf.get(CoreConfig.JOBID);
			keywords = conf.get(KEY_KEYWORDS_STRING, null);
			File tempDir = new File(CoreConfig.DOWNLOAD_TEMP_PATH,jobid);
			TaskID task = context.getTaskAttemptID().getTaskID();
			String taskStr = task.toString();
			String partStr = taskStr.substring(taskStr.lastIndexOf('_')+1);
			int partNumber = Integer.parseInt(partStr);
			try {
				if(!tempDir.exists()){
					tempDir.mkdirs();
				}
				GtPath inPathObj = new GtPath(conf.get(INPUTPATH));
				String filename = inPathObj.getDisplayFileName();
				if ( 0 == spiltSize) {
					filename = filename  + ".zip";
				}else {
					filename = filename + "_" + partNumber + ".zip";
				}
				outGtPath = new GtPath(conf.get(OUTPUTPATH) +"/" + filename).getGtPath();
				inputPath = inPathObj.getDisplayPath();
				fs = FileSystem.get(conf);
				tempFile = new File(tempDir.getPath(),filename);
				zos = new ZipOutputStream(tempFile);
				htable = new HTable(conf,GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void map(ImmutableBytesWritable key, Result values,
				Context context) throws IOException, InterruptedException {
			String size = new String(values.getValue(GtDataConfig.META.FAMILY.byteVal,GtDataConfig.META.SIZE.byteVal));
			String rowkey = Bytes.toString(key.copyBytes());
			System.out.println("key is : " + rowkey);
			if (keywords != null) {
				if (!rowkey.contains(keywords)) {
					return;
				}
			}
			GtPath pathObj = new GtPath(Bytes.toString(key.copyBytes())); 
			String zipKey = pathObj.getDisplayPath().replace(inputPath+"/", "");
			if (0 <= Long.parseLong(size)) {
				byte[] url = values.getValue(Bytes.toBytes("atts"),
						Bytes.toBytes("url"));
				byte[] dfs = values.getValue(Bytes.toBytes("atts"),
						Bytes.toBytes("dfs"));
				if ( dfs[0] == '0') {
					Get get = new Get(url);
					get.addColumn(Bytes.toBytes("img"), Bytes.toBytes("data"));
					Result result = htable.get(get);
					if ( !result.isEmpty()) {
						byte[] data =result.getValue(Bytes.toBytes("img"), Bytes.toBytes("data"));
						ZipByAntUtil.compressFile(data, zos, zipKey);
					}
				} else {
					Path path = new Path(GtDataConfig.HDFS_MD5_PATH ,new String(url));
					FSDataInputStream in = fs.open(path);
					ZipByAntUtil.compressFile(in, zos, zipKey);
					in.close();
				}
			}else{
				ZipByAntUtil.compressDir(zos, zipKey+"/");
			}
//			zos.closeEntry();
		}
		
		public void cleanup(Context context)throws IOException, InterruptedException{	
			super.cleanup(context);
			if(htable!= null)
				htable.close();
			if(zos != null){
				zos.close();
			}				
			if(out!=null){
				out.close();
			}			
			if(fs != null)
				fs.close();
			Import imp = new Import();
			if(imp.ImportFromLocal(tempFile.getPath(), outGtPath)){
				LOG.info("delete "  +tempFile.getPath() + " :" + tempFile.delete());
			}
		}
	}

	public int run(String[] args) throws Exception {
		if (args.length < 4) {
			return 1;
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set(CoreConfig.JOBID, args[0]);
		String prefix = GtDataUtils.format2GtPath(args[1]).replace("//", "/");
//		conf.setBoolean("mapreduce.map.speculative", false);
//		conf.setInt("yarn.scheduler.minimum-allocation-mb", 3072);
//		conf.setFloat("yarn.nodemanager.vmem-pmem-ratio", 3.0f);		
//		conf.setStrings("mapred.child.java.opts", "-Xmx2048m");
//		conf.setInt("mapreduce.map.memory.mb", 3072);
		if ( "0".equalsIgnoreCase(args[3])) {
			conf.setInt(MINSPLIT, PERSPLIT_RECORDS_MAX);
		} else {
			conf.setInt(MINSPLIT, CoreConfig.PERSPLIT_ROKEYS_NUM);
		}
		if(args.length == 5) {
			conf.set(KEY_KEYWORDS_STRING, args[4]);
		}
		conf.set(INPUTPATH, prefix);
		conf.set(OUTPUTPATH, args[2]);
		conf.set(TableInputFormat.INPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		conf.set(TableInputFormat.SCAN_ROW_START, prefix +"/");
		conf.set(TableInputFormat.SCAN_ROW_STOP, prefix + "/{");
	
		Job job = Job.getInstance(conf, "ExportDir");
		job.setNumReduceTasks(0);
		job.setJarByClass(ExportDirByAnt.class);
		Path outputPath = new Path(GtDataConfig.HDFS_TEMP_PATH,args[0]);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		job.setInputFormatClass(TableRegionSplitInputFormat.class);
		job.setMapperClass(MapperCompress.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(BytesWritable.class);
		job.setOutputFormatClass(ZipFileOutputFormat.class);
		boolean status = job.waitForCompletion(true);
		if(status){
			//delete hdfs output file
			FileSystem.get(conf).delete(outputPath, true);
			//delete local temp file
			File tempDir = new File(CoreConfig.DOWNLOAD_TEMP_PATH,args[0]);
			if(tempDir.exists()){
				tempDir.delete();
			}
		}		
		return  status? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
//		args = new String[]{"test_jonid_"+RandomUtils.nextInt(999),"/src/test/new","/src/test","1"};
		if (args.length < 4) {
			LOG.info("usage: <jobid> <source_path> <outputpath_gtdata> <zip_flag> <keywords>");
			LOG.info("jobid");
			LOG.info("source_path:source path(gt-data) to compress.");
			LOG.info("outputpath_hdfs: outputpath.");
			LOG.info("zip_flag: 0->only one zip file output! 1->severl zip file output!");
		} else {
			int exitcode = ToolRunner.run(new ExportDirByAnt(), args);
			System.exit(exitcode);
		}		
	}
}
