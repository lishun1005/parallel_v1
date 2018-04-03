//package com.rsclouds.gtparallel.core.gtdata.producer2consumer;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class ImportProducerConsummer {
//	private static int CONSUMERMAX = 3;
//	private int consumerThreadNum;
//	private Map<String, String> jobidProgress;
//	
//	public Map<String, String> getJobidProgress() {
//		return jobidProgress;
//	}
//	public ImportProducerConsummer(){
//		Map<String, String> map = new HashMap<String, String>();
//		setup(CONSUMERMAX, map);
//	}
//	public ImportProducerConsummer (int consumerThreadNum, Map<String, String> map) {
//		setup(consumerThreadNum, map);
//	}
//	public void setup(int consumerThreadNum, Map<String, String> map){
//		this.consumerThreadNum = consumerThreadNum;
//		this.jobidProgress = map;
//	}
//	
//	public  void importDirRun(String jobid, String inputDir, String outputDir, String keyword){
//		List<ImportConsumer<String>> listConsumer = new ArrayList<ImportConsumer<String>>(consumerThreadNum);
//		List<thread> listThread = new ArrayList<thread>();
//		Storage<String> storage = new Storage<String>();
//		for (int i = 1; i <= consumerThreadNum; i ++) {
//			ImportConsumer<String> consumer = new ImportConsumer<String>("consumer"+i, outputDir, storage, null);
//			thread con = new thread(consumer);
//			con.start();
//			listConsumer.add(consumer);
//			listThread.add(con);
//			
//		}		
//		ImportProducer<String> producer = new ImportProducer<String>("producer", storage);
//		producer.run(inputDir, keyword);
//		System.out.println("total record: " + producer.getCount());
//		int count = consumerThreadNum;
//		float totalRecord = producer.getCount();
//		int finishRecord = 0;
//		int curentRecord = 0;
//		while(true) {
//			try {
//				curentRecord = 0;
//				thread.sleep(1000);
//				for (int i = 0; i < count; i ++) {
//					if (listConsumer.get(i).getFinishFlag()){
//						finishRecord += listConsumer.get(i).getCount();
////						listThread.get(i).stop();
//						listConsumer.get(i).interrupted();
//						listThread.remove(i);
//						listConsumer.remove(i);
//						count --;					
//					}else {
//						curentRecord += listConsumer.get(i).getCount();
//					}
//				}
//				curentRecord += finishRecord;
//				String progress = "" + curentRecord/totalRecord;
//				jobidProgress.put(jobid, progress);
//				if ( count == 0) {
//					break;
//				}
//			} catch (InterruptedException e) {
//				break;
//			}
//		}
//	}
//	
//	public static void main(String[] args) {
//		if (args == null || args.length == 0) {
//			args = new String[5];
//			args[0] = "1";
//			args[1] = "D://nanlin";
//			args[2] = "/import/test2";
//			args[3] = "xml";
//			args[4] = "2";
//		}
//		int num = Integer.parseInt(args[4]);
//		Map<String, String> map = new HashMap<String, String>();
//		ImportProducerConsummer test = new ImportProducerConsummer(num, map);		
//		test.importDirRun(args[0], args[1], args[2], args[3]);
//	}
//
//
//	
//	
//}
