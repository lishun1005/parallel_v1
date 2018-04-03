package com.rsclouds.gtparallel.core.gtdata.producer2consumer;

import java.io.File;

public class DeleteFileTimerTask implements Runnable{
	private String dirPath;
	private String dirPathBack;
	private long  intervalTime;
	
	public DeleteFileTimerTask(String dirPath, String dirPathBack, long intervalTime) {
		this.dirPath = dirPath;
		this.dirPathBack = dirPathBack;
		this.intervalTime = intervalTime;
	}
	
	/**
     * Deletes all files and subdirectories under "dir".
     * @param dir Directory to be deleted
     * @return boolean Returns "true" if all deletions were successful.
     *                 If a deletion fails, the method stops attempting to
     *                 delete and returns "false".
     */
    private void deleteFile(File dir, long currentTime) { 
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                deleteFile(new File(dir, children[i]), currentTime);
            }
        }else {
        	long time = dir.lastModified();
        	if(time + intervalTime <= currentTime) {
        		if( !dir.delete() ) {
        			System.out.println("delete file error : " + dir.getPath());
        		}
        	}	
        }
    }
    
    /**
     * Deletes all leaf node of directory(is empty) under "dir".
     * @param dir
     * @param currentTime
     */
    private void deleteLeafNodeDir(File dir, long currentTime) {
    	if(dir.isDirectory()) {
    		File[] dirs = dir.listFiles();
    		for(int i = 0; i < dirs.length; i ++) {
    			if(dirs[i].isFile())
    				continue;
    			File[] dateDirs = dirs[i].listFiles();
    			for( int j = 0; j < dateDirs.length; j ++) {
    				if(dateDirs[j].isFile())
        				continue;
    				if(dateDirs[j].list().length == 0) {
    					if(dateDirs[j].lastModified() + intervalTime < currentTime) {
    						if (!dateDirs[j].delete()) {
    							System.out.println("delete dir error : " + dateDirs[j].getPath());
    						}
    					}
    				}
    			}
    		}
    	}
    }
	
	
	@Override
	public void run() {
		while(true) {
			long currentTime = System.currentTimeMillis();
			File dir = new File(dirPath);
			File dirBack = new File(dirPathBack);
			deleteFile(dirBack, currentTime);
			deleteLeafNodeDir(dir, currentTime);
			deleteLeafNodeDir(dirBack, currentTime);
			
			try {
				Thread.sleep(24*60*60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
