package com.rsclouds.gtparallel.core.test2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import com.rsclouds.gtparallel.core.common.CoreConfig;

public class ZipCompressorByAnt {
	static final int BUFFER = 8192;
    private File zipFile;  
  
    public ZipCompressorByAnt(String pathName) {  
        zipFile = new File(pathName);  
    } 
    
    public void compress2(String srcPathName) {  
        File srcdir = new File(srcPathName);  
        if (!srcdir.exists())  
            throw new RuntimeException(srcPathName + "不存在！");  
          
        Project prj = new Project();  
        Zip zip = new Zip();  
        zip.setProject(prj);  
        zip.setDestFile(zipFile);  
        FileSet fileSet = new FileSet();  
        fileSet.setProject(prj);  
        fileSet.setDir(srcdir);  
        //fileSet.setIncludes("**/*.java"); 包括哪些文件或文件夹 eg:zip.setIncludes("*.java");  
        //fileSet.setExcludes(...); 排除哪些文件或文件夹  
        zip.addFileset(fileSet);  
          
        zip.execute();  
    }
      
    public void compress(String srcPathName) {  
        File file = new File(srcPathName);  
        if (!file.exists())  
            throw new RuntimeException(srcPathName + "不存在！");
        if(!file.canRead()){
        	throw new RuntimeException(srcPathName + "不不可读！");
        }
        try {  
            ZipOutputStream zOut = new ZipOutputStream(zipFile);           
            String basedir = "";  
            compress(file, zOut, basedir);  
            zOut.close();  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }  
    
    private void compress(File file, ZipOutputStream out, String basedir) {  
        /* 判断是目录还是文件 */  
        if (file.isDirectory()) {  
            System.out.println("压缩：" + basedir + file.getName()); 
            ZipEntry entry = new ZipEntry(basedir + file.getName() + "/");
            try {
            	entry.setSize (0);
            	entry.setMethod (ZipEntry.STORED);
            	entry.setCrc (0);
            	entry.setUnixMode(ArchiveFileSet.DEFAULT_DIR_MODE);
    			out.putNextEntry(entry);
    		} catch (IOException e) {
    			throw new RuntimeException(e);
    		}
            this.compressDirectory(file, out, basedir);  
        } else {  
            System.out.println("压缩：" + basedir + file.getName());  
            this.compressFile(file, out, basedir);  
        }  
    }  
  
    /** 压缩一个目录 */  
    private void compressDirectory(File dir, ZipOutputStream out, String basedir) {  
        if (!dir.exists())  
            return;      
        File[] files = dir.listFiles();  
        for (int i = 0; i < files.length; i++) {  
            /* 递归 */  
            compress(files[i], out, basedir + dir.getName() + "/");  
        }    
    }  
  
    /** 压缩一个文件 */  
    private void compressFile(File file, ZipOutputStream out, String basedir) {  
        if (!file.exists()) {  
            return;  
        }  
        try {  
            BufferedInputStream bis = new BufferedInputStream(  
                    new FileInputStream(file));  
            ZipEntry entry = new ZipEntry(basedir + file.getName());
//            entry.setTime(file.lastModified());
//            entry.setMethod (ZipEntry.DEFLATED);
//            entry.setUnixMode(ArchiveFileSet.DEFAULT_FILE_MODE);         
            out.putNextEntry(entry);  
            int count;  
            byte data[] = new byte[BUFFER];  
            while ((count = bis.read(data, 0, BUFFER)) != -1) {  
                out.write(data, 0, count);  
            }  
            bis.close();  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }
    
    public static boolean unZip(ZipInputStream zipInputStream,String zipName) {
		try {
			java.util.zip.ZipEntry zipEntry = null;
			String tempDir = CoreConfig.LOCA_LTEMP_DIR;
			if (tempDir.isEmpty()) {
				return false;
			}
			String uid = UUID.randomUUID().toString();
			File tempFile = new File(tempDir,uid);
			if(!tempFile.isDirectory()){
				tempFile.mkdirs();		
			}
			BufferedInputStream bufferedInputStream = new BufferedInputStream(zipInputStream);
			do{
				zipEntry = zipInputStream.getNextEntry();
				if(zipEntry!= null){
					String entryName = zipEntry.getName();
					if(!entryName.startsWith(zipName+"/")){
						entryName = zipName+"/"+entryName;
					}
					if (!zipEntry.isDirectory()) {				
						File file = new File(tempFile.getPath(),entryName);	
						if(!file.getParentFile().isDirectory())
							file.getParentFile().mkdirs();
						FileOutputStream bos = new FileOutputStream(file);
						int buf_size = 1024;
						byte[] buffer = new byte[buf_size];
						int len = 0;
						while (-1 != (len = bufferedInputStream.read(buffer, 0,
								buf_size))) {
							bos.write(buffer, 0, len);
						}
						bos.close();
						System.out.println("write over to : " + file.getPath());
					}else{
						File file = new  File(tempFile.getPath(),entryName);
						file.mkdirs();
						System.out.println("mkdirs : " +file.getPath());
					}
				}			
			}while(zipEntry != null);
			return true;		
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;	
	}
    
    public static void main(String[] args) throws FileNotFoundException {
        ZipCompressorByAnt zca = new ZipCompressorByAnt("E:\\myJob\\ziptest.zip");  
        zca.compress("E:/myJob/test2");
//        
//        File file = new File("E:\\myJob\\szhzipant.zip");
//        FileInputStream in = new FileInputStream(file);	
//		ZipInputStream zipInputStream = new ZipInputStream(in);
//        unZip(zipInputStream,file.getName().replace(".zip", ""));
    } 
}  