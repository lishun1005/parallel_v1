package com.rsclouds.gtparallel.core.gtdata.producer2consumer;

import java.io.File;
import java.util.concurrent.BlockingQueue;

public class ImportProducer<T> {
	@SuppressWarnings("unused")
	private String nameID = null;
	private BlockingQueue<String> storage = null;
	int count = 0;
	
	public ImportProducer(String nameID, BlockingQueue<String> storage){
		this.nameID = nameID;
		this.storage = storage;
	}
	
	public boolean pathCheck(String inputPath) {
		if (inputPath.startsWith("/data/sdi/GF1") || inputPath.startsWith("/data/sdi/GF2")
				|| inputPath.startsWith("/data/sdi/ZY3") || inputPath.startsWith("/data/sdi/ZY02C")
				|| inputPath.startsWith("/data/sdi/HJ") || inputPath.startsWith("/data/sdi/GF3")) {
			return true;
		}
		return false;
	}
	
	public void run(String inputpath, String keyword){
		File file = new File(inputpath);
		File[] files = file.listFiles();
		System.out.println("[ImportProducer::run]start");
		for (int k = 0; k< files.length; k ++) {
			String filename = files[k].getPath();
			System.out.println(filename);
			if (!pathCheck(filename)) {
				System.out.println("=====nanlin debug====Filter: " + filename);
				continue;
			}
			if (files[k].isDirectory()) {
				run(filename, keyword);
			}else {
				if (filename.endsWith(keyword)) {
					try {
						storage.put(filename);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					count++;
				}
			}
		} 
	}
	
	public int getCount(){
		return count;
	}
}
