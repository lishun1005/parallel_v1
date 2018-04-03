package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hbase.client.HTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class ImportMultiThread implements Runnable{
	private final static Logger LOG = LoggerFactory.getLogger(ImportMultiThread.class);
	private String inpath;
	private String outpath;
	private String keyword;
	private HTable metaTable = null;
	private HTable resTable = null;
	ThreadPoolExecutor threadPool = null;
	private long count = 0;		
	
	public ImportMultiThread(String inpath,String outpath,String keyword){
		this.inpath = inpath;
		this.outpath = outpath;
		this.keyword = keyword==null?"":keyword;
		threadPool = new ThreadPoolExecutor(0, 5, 1, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(100),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	@Override
	public void run() {
		try {
			metaTable = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());		
			resTable = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			File file = new File(inpath);
			if(file.isFile()){
				if(file.getName().contains(keyword)){
					ImportThread task = new ImportThread(file.getPath(), outpath);
					threadPool.execute(task);
					count++;
				}
			}else if(file.isDirectory()){
				File[] files = file.listFiles();
				long length = files.length;
				for (int k = 0; k < length; k ++) {
					System.out.println(files[k].getPath());
					String currPath = files[k].getPath();
					if (files[k].isDirectory()) {
						run(currPath);
					}else {
						if (files[k].getName().contains(keyword)) {
							ImportThread task = new ImportThread(currPath, outpath);
							threadPool.execute(task);
							count++;
						}
					}
				} 
			}	
			System.out.println("all task inserted !");
			Thread.sleep(1000);
			while(threadPool.getActiveCount() != 0){
				//写入进度
				int progress = (int) (threadPool.getCompletedTaskCount()*100/count);
				LOG.info("progress :" + progress + "%");
				Thread.sleep(10000);
			}
			System.out.println("all task completet ! count:"+count);
			threadPool.shutdown();
			if(threadPool.awaitTermination(10, TimeUnit.SECONDS)){					
			}else{
				System.out.println(" thread stop error !");
			}
		} catch (Exception  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				if (metaTable != null) {
					metaTable.flushCommits();
					metaTable.close();
				}
				if (resTable != null) {
					resTable.flushCommits();
					resTable.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void run(String dirpath){
		File file = new File(dirpath);
		File[] files = file.listFiles();
		long length = files.length;
		for (int k = 0; k < length; k ++) {
			String currPath = files[k].getPath();
			if (files[k].isDirectory()) {
				run(currPath);
			}else {
				if (files[k].getName().contains(keyword)) {
					ImportThread task = new ImportThread(currPath, outpath);
					threadPool.execute(task);
					count++;
				}
			}
		}
	}
	
	class ImportThread implements Runnable{

		private String inpath;
		private String outpath;
		private Import importFile;
		
		ImportThread(String inpath,String outpath){
			this.inpath = inpath;
			this.outpath = outpath;
			this.importFile = new Import();
		}
		
		@Override
		public void run() {
			boolean rs = false;
			try{
				rs = importFile.ImportFileToDir(metaTable, resTable, inpath, outpath);
			}catch(Exception e){
				e.printStackTrace();
			}
			if(!rs){
				System.out.println("======ERROR======:"+inpath +" to " + outpath);
			}		
		}
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		args = new String[]{"E:\\myJob\\项目农情综合服务平台\\tif","/sotcut/test1/inputmap"};
		// TODO Auto-generated method stub
		if(args.length < 2){
			System.out.println("Usage : <local_path> <out_gt_path> <keyword>");
			return ;
		}
		String loaclPath = args[0];//"C:/Users/zkyg0710/我的文档/Tencent Files/273438964/FileRecv";
		String outpath = args[1];//"/src/src";
		String keyword = null;
		if(args.length > 2){
			keyword = args[2];//".xlsx";
		}
		ImportMultiThread process = new ImportMultiThread(loaclPath,outpath,keyword);
		Thread thread = new Thread(process);
		thread.start();
	}

}
