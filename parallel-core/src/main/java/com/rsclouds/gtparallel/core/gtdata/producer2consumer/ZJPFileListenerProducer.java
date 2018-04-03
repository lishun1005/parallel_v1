package com.rsclouds.gtparallel.core.gtdata.producer2consumer;

import java.io.File;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;


public class ZJPFileListenerProducer implements FileAlterationListener{  
	  
	private BlockingQueue<String> storage = null;
	private String  inputformat = null;
	private String  finishFlagStr = null;
	
	public ZJPFileListenerProducer(BlockingQueue<String> storage, String inputformat, String finishFlagStr) {
		this.storage = storage;
		this.inputformat = inputformat;
		this.finishFlagStr = finishFlagStr;
	}
 
    @Override  
    public void onStart(FileAlterationObserver observer) {  
        //System.out.println("onStart");  
    }  
    @Override  
    public void onDirectoryCreate(File directory) {  
//        System.out.println("onDirectoryCreate:" +  directory.getName());  
    }  
  
    @Override  
    public void onDirectoryChange(File directory) {  
//        System.out.println("onDirectoryChange:" + directory.getName());  
    }  
  
    @Override  
    public void onDirectoryDelete(File directory) {  
//        System.out.println("onDirectoryDelete:" + directory.getName());  
    }  
  
    public boolean pathCheck(String inputPath) {
		if (inputPath.startsWith("/data/sdi/GF1") || inputPath.startsWith("/data/sdi/GF2")
				|| inputPath.startsWith("/data/sdi/ZY3") || inputPath.startsWith("/data/sdi/ZY02C")
				|| inputPath.startsWith("/data/sdi/HJ") || inputPath.startsWith("/data/sdi/GF3")) {
			return true;
		}
		return false;
	}
    
    @Override  
    public void onFileCreate(File file) {
    	String path = file.getPath();
    	if(path.endsWith(this.inputformat+"." + this.finishFlagStr)) {
    		path = path.substring(0, path.length()-this.finishFlagStr.length()-1);
    		try {
    			if (pathCheck(path)) {
    				//System.out.println("onFileCreate : " + file.getName());  
    				storage.put(path);
    			}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	//System.out.println("onFileCreate : " + file.getName());  
    }  
  
    @Override  
    public void onFileChange(File file) {  
//        System.out.println("onFileChange : " + file.getName());  
    }  
  
    @Override  
    public void onFileDelete(File file) {  
//        System.out.println("onFileDelete :" + file.getName());  
    }  
  
    @Override  
    public void onStop(FileAlterationObserver observer) {  
        //System.out.println("onStop");  
    }  
  
}  
