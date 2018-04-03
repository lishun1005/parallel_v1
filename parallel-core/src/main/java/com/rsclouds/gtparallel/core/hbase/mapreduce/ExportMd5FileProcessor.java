package com.rsclouds.gtparallel.core.hbase.mapreduce;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class ExportMd5FileProcessor {
	private final static Logger LOG = LoggerFactory.getLogger(ExportMd5FileProcessor.class);
	ThreadPoolExecutor threadPool = null;
//	private static String md5Path = HbaseBase.getHbaseConf().get("fs.defaultFS")
	
	// 似有静态内部类, 只有当有引用时, 该类才会被装载
    private static class LazyProcessorService {
       public static ExportMd5FileProcessor instance = new ExportMd5FileProcessor();
    }
    
    public static ExportMd5FileProcessor getInstance() {
        return LazyProcessorService.instance;
    }
	
	private ExportMd5FileProcessor(){
		threadPool = new ThreadPoolExecutor(0, 3, 2, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
	
	public void processExport(String md5,String outpath){
		Runnable task = new ExportThread(md5,outpath);
        threadPool.execute(task);
	}
	
	class ExportThread implements Runnable{

		private String md5;
		private String outpath;
		
		ExportThread(String md5,String outpath){
			this.md5 = md5;
			this.outpath = outpath;
		}
		
		@Override
		public void run() {
			OutputStream out;
			try {			
				FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
				if(!fs.isFile(new Path(GtDataConfig.HDFS_MD5_PATH, md5))){
					LOG.info("NOT EXIST md5 file :" + md5);
					return;
				}
				out = new FileOutputStream(new File(outpath, md5));
				FSDataInputStream in = fs.open(new Path(GtDataConfig.HDFS_MD5_PATH, md5));
				int readLen;
				byte[] value = new byte[1024];
				while ((readLen = in.read(value, 0, 1024)) != -1) {
					out.write(value, 0, readLen);
				}
				in.close();
				out.close();
				LOG.info("SUCCESS export md5 file :" + md5 + " to " + outpath);
			} catch (IOException e) { 
				e.printStackTrace();
				LOG.error("ERROR in export md5 file :" + md5,e);
			}finally{
				
			}	
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
