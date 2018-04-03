package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class FileOperate {

	public static boolean copyTemplateFile(String templateFile, String destinationFile) {
		File file = new File(destinationFile);
		InputStream in = FileOperate.class.getClassLoader().getResourceAsStream(templateFile);	
		try {
			if(in == null){
				return false; 
			}
			OutputStream out = new FileOutputStream(file);
			int readbyte = 0;
			byte[] data = new byte[1024];
			while ((readbyte = in.read(data, 0, 1024)) != -1) {
				System.out.println(readbyte);
				out.write(data, 0, readbyte);
			}
			out.close();
			in.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
    public static  void deleteDir(File dir) {
    	if (dir == null || !dir.exists() || !dir.isDirectory())
    		return; // 检查参数
    	for (File file : dir.listFiles()) {
    		if (file.isFile()) {
    			file.delete(); // 删除所有文件
    		}
    		else if (file.isDirectory()) {
    			deleteDir(file); // 递规的方式删除文件夹
    		}
    	}
    	dir.delete();// 删除目录本身
    }
    
    public static byte[] readSmalFile(String inputPath){
    	File file = new File(inputPath);
    	long fileSize;
		if (file.isDirectory()) {
			return null;
		} else {
			fileSize = file.length();
		}
    	byte[] value = new byte[(int) fileSize];
		InputStream in = null; 
		try {
			in = new FileInputStream(file);
			int length = value.length;
			int readLen = 0;
			int off = 0;
			while ((readLen = in.read(value, off, length)) != 0) {
				off += readLen;
				length -= readLen;
			}
		} catch (IOException e) {
			return null;
		}finally{
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return value;
    }
    
    public static String mkdirs(String header, String tail) {
    	if (header == null) {
    		header = "/";
    	}else if(header.endsWith("/")){
    		header = header.substring(0, header.length()-1);
    	}
    	String[] paths = StringUtils.splitByWholeSeparator(tail, "/");
    	StringBuilder pathBuilder = new StringBuilder(header);
    	for (int i = 0; i < paths.length; i ++) {
    		pathBuilder.append("/");
    		pathBuilder.append(paths[i]);
    		File dir = new File(pathBuilder.toString());
    		if (!dir.exists()) {
    			dir.mkdir();
    		}
    		
    	}
    	return pathBuilder.toString();  	
    }
    
    public static void dirCheck(String str){
    	
    }
    
    /**
	 * 遍历hdfs路径下的所有文件
	 * @param fs
	 * @param dir
	 * @param paths
	 * @return
	 */
	public static boolean getPaths(FileSystem fs, Path dir, List<String> paths) {
		try {
			FileStatus[] fileStatus = fs.listStatus(dir);
			for ( int i = 0; i < fileStatus.length; i++) {
				Path p = fileStatus[i].getPath();
				if(fs.isFile(p)){
					paths.add(p.toString());
				}
				else {
					if (!getPaths(fs, p, paths)) {
						return false;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 遍历本地路径下的所有文件
	 * @param file			File
	 * @param filePaths		List<String>
	 */
	public static void listLocalFiles(File file, List<String> filePaths) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			int count = files.length;
			for (int i = 0; i < count; i ++) {
				File tmpFile = files[i];
				if (tmpFile.isFile()) {
					filePaths.add(tmpFile.getPath());
				}else {
					listLocalFiles(tmpFile, filePaths);
				}
			}
		}else {
			filePaths.add(file.getPath());
		}
		
	}
	
	/**
	 * 遍历所有文件，并返回其路径
	 * @param inputPath   遍历根路径
	 * @param filePaths   返回遍历的所有文件路径List<String>
	 * @return
	 * @throws IOException 
	 */
	public static boolean listFiles(String inputPath, List<String> filePaths) throws IOException {
		if (filePaths == null) {
			filePaths = new ArrayList<String>();
		}
		if (inputPath.startsWith("hdfs://")) {
			Configuration conf = new Configuration();
			FileSystem fs  = FileSystem.get(conf);
			getPaths(fs, new Path(inputPath), filePaths);
			fs.close();
		}else if (inputPath.startsWith("gtdata://")) {
			Configuration conf = HBaseConfiguration.create();
			HTable metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getByteVal());
			String rowkey = inputPath.substring("gtdata://".length());
			String gtpath = GtDataUtils.format2GtPath(rowkey);
			Get get = new Get(gtpath.getBytes());
			Result result = metaTable.get(get);
			if (!result.isEmpty()) {
				byte[] sizeBytes = result.getValue(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.SIZE.byteVal);
				if (sizeBytes != null && sizeBytes.length > 0) {
					String sizeStr = new String(sizeBytes);
					if (sizeStr.equals("-1")) {
						String startRowkey = gtpath.replace("//", "/") + "/%";
						String stopRowkey = gtpath.replace("//", "/") + "/{";
						Scan scan = new Scan();
						scan.setStartRow(startRowkey.getBytes());
						scan.setStopRow(stopRowkey.getBytes());
						ResultScanner scanner = metaTable.getScanner(scan);
						for (Result res : scanner) {
							sizeBytes = res.getValue(GtDataConfig.META.FAMILY.byteVal, 
									GtDataConfig.META.SIZE.byteVal);
							if (sizeBytes != null && sizeBytes.length > 0) {
								sizeStr = new String(sizeBytes);
								if (sizeStr.equals("-1")) {
									continue;
								}
								String rowkeyPath = new String(res.getRow());
								rowkeyPath = "gtdata://" + GtDataUtils.format2DisplayPath(rowkeyPath);
								if (rowkeyPath.endsWith("tif") || rowkeyPath.endsWith("tiff")
										|| rowkeyPath.endsWith("TIFF") || rowkeyPath.endsWith("TIF")
										|| rowkeyPath.endsWith("img")) {
									filePaths.add(rowkeyPath);
								}
							}
						}
					}else {
						filePaths.add(inputPath);
					}
					
				}
			} 
			if (metaTable != null) {
				metaTable.close();
			}
			
		}else {
			listLocalFiles(new File(inputPath), filePaths);
		}
		return filePaths.size() > 0;
	}
    
}
