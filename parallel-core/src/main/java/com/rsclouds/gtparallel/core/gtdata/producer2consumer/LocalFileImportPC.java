package com.rsclouds.gtparallel.core.gtdata.producer2consumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.rsclouds.gtparallel.core.common.CoreConfig;

public class LocalFileImportPC {

	public static  void importDirRun(String inputDir, String inputDirBack, String outputDir, String keyword, String finishFlagStr, short replacation) throws Exception{

		BlockingQueue<String> queue = new LinkedBlockingQueue<String>(100);
		while(outputDir.endsWith("/")) {
			outputDir = outputDir.substring(0, outputDir.length()-1);
		}
		ImportConsumer consumer = new ImportConsumer(inputDirBack, outputDir, queue, finishFlagStr, 3000, replacation);
		consumer.init();
		Thread con = new Thread(consumer);
		con.start();
		
		ZJPFileMonitor m = new ZJPFileMonitor(100);  
        m.monitor(inputDir, new ZJPFileListenerProducer(queue, keyword, finishFlagStr));  
        m.start();
        
		ImportProducer<String> producer = new ImportProducer<String>("producer", queue);
		producer.run(inputDir, keyword);
		
		long intervalTime = CoreConfig.INTERVAL_DAY * 24 * 60 * 60 * 1000;
        DeleteFileTimerTask deleteTask = new DeleteFileTimerTask(inputDir, inputDirBack, intervalTime);
        Thread deleteTherad = new Thread(deleteTask);
        deleteTherad.start();
	}
	
	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[5];
			args[0] = "D://test";
			args[1] = "F://test_successed";
			args[2] = "/users/rscloudmart/data_temp";
			args[3] = "tar.gz";
			args[4] = "fin";
		}
		if(args.length < 5) {
			System.out.println("usage: <inputPath(local)> <inputPath_back> <outputPath(gt-data)> <format> <finishFlag> [replacation]");
			System.exit(0);
		}
		try {
			short replacation = 3;
			if(args.length == 5)
				importDirRun(args[0], args[1], args[2], args[3], args[4], replacation);
			else {
				replacation = Short.parseShort(args[5]);
				importDirRun(args[0], args[1], args[2], args[3], args[4], replacation);
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
}
