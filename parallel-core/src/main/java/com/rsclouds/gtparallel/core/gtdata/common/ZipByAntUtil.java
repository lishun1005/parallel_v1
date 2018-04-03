package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

public class ZipByAntUtil {
	static final int BUFFER = 8192;
	
	/** 压缩一个文件 */  
    public  static void compressFile(File file, ZipOutputStream out, String filePath) {
        if (!file.exists()) {  
            return;  
        }  
        try {  
            BufferedInputStream bis = new BufferedInputStream(  
                    new FileInputStream(file));  
            compressFile(bis,out,filePath);
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }
    
    public static void compressFile(InputStream bis, ZipOutputStream out, String filePath) {
        try {  
            ZipEntry entry = new ZipEntry(filePath);
            out.putNextEntry(entry);  
            int count;  
            byte data[] = new byte[BUFFER];            
            while ((count = bis.read(data, 0, BUFFER)) != -1) {  
                out.write(data, 0, count);  
            }      
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }
    
    public static void compressFile(byte[] data, ZipOutputStream out, String filePath) {
        try {  
            ZipEntry entry = new ZipEntry(filePath);
            out.putNextEntry(entry);        
            out.write(data, 0, data.length);  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }
    
    public static void compressDir(ZipOutputStream out, String filePath) {
    	if(!filePath.endsWith("/")){
    		filePath += "/";
    	}
        try {  
            ZipEntry entry = new ZipEntry(filePath);
            entry.setSize (0);
        	entry.setMethod (ZipEntry.STORED);
        	entry.setCrc (0);
        	entry.setUnixMode(16877);
			out.putNextEntry(entry);
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }
    
    
    
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
