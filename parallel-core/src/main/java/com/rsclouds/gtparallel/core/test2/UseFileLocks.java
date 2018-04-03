package com.rsclouds.gtparallel.core.test2;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class UseFileLocks {

	static private final int start = 10;
	  static private final int end = 20;
	  
	  static public void main( String args[] ) throws Exception {
	    // Get file channel  
	    RandomAccessFile raf = new RandomAccessFile( "C:/Users/Administrator/Desktop/idc配置文件/hosts", "rw" );//如果要获取一个排它锁，您必须以写方式打开文件。  
	    FileChannel fc = raf.getChannel();  
	  
	    // Get lock  
	    System.out.println( "trying to get lock" );  
	    FileLock lock = fc.lock( start, end, false );//排它锁  
	    System.out.println( "got lock!" );  
	  
	    // Pause  
	    System.out.println( "pausing" );  
	    try { Thread.sleep( 30000 ); } catch( InterruptedException ie ) {}  
	  
	    // Release lock  
	    System.out.println( "going to release lock" );  
	    lock.release();//释放锁  
	    System.out.println( "released lock" );  
	  
	    raf.close();  
	  }  
}
