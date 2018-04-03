package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.IOUtils;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.operation.Import;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;

public class UnzipGTDataUtil {
	private final static Logger LOG = LoggerFactory.getLogger(UnzipGTDataUtil.class);
	
	public static boolean unZipFromLocal(String localFile,String outPath){
		try {
			File file = new File(localFile);
			if(file.isFile()){		
					FileInputStream in = new FileInputStream(file);	
					ZipInputStream zipInputStream = new ZipInputStream(in);
					unZipMix(zipInputStream, outPath,file.getName().replace(".zip", ""));
					return true;
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}	
	
	public static boolean unZipFromGTData(String inpath,String outPath,String zipName) throws IOException{
		try {
			GtPath inpathObj = new GtPath(inpath);
			Result meta = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), inpathObj.getGtPath());
			if(!meta.isEmpty()){
				byte[] size = meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
				if(!Arrays.equals(GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal,size)){
					byte[] dfs = meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal);
					String url = Bytes.toString(meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal));
					if(Arrays.equals(GtDataConfig.CONSTANT.ZERO.byteVal, dfs)){
						//hbase
						return unZipHbase(url, outPath,zipName);
					}else{
						//hdfs
						return unZipHDFS(url, outPath,zipName);
					}
				}			
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;	
	}
	
	public static boolean unZipHbase(String url, String outputPath,String zipName) {
		Result result;
		try {
			result = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), url);
			if(!result.isEmpty()){
				byte[] data = result.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal,GtDataConfig.RESOURCE.DATA.byteVal);
				ByteArrayInputStream in = new ByteArrayInputStream(data); 
				ZipInputStream zip = new ZipInputStream(in);
				return unZipMix(zip, outputPath,zipName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean unZipHDFS(String url, String outputPath,String zipName) {
		FileSystem fs = null;
		FSDataInputStream in = null;
		ZipInputStream zip = null;
		try {
			fs = FileSystem.get(HbaseBase.getHbaseConf());
			Path path = new Path(GtDataConfig.HDFS_MD5_PATH , url);
			if(fs.isFile(path)){
				in = fs.open(path);
				zip = new ZipInputStream(in);
				return unZipMix(zip,outputPath,zipName);
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if (zip != null)
					zip.close();
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}			
	
	public static boolean unZipMix(ZipInputStream zipInputStream,String outputPath,String zipName) throws IOException {
			ZipEntry zipEntry = null;
			GtPath outPathObj = new GtPath(outputPath);
			String tempDir = CoreConfig.LOCA_LTEMP_DIR;
			if (tempDir.isEmpty()) {
				LOG.error("LOCA_LTEMP_DIR is not exist!");
				return false;
			}
			String uid = UUID.randomUUID().toString();
			File tempFile = new File(tempDir,uid);
			if(!tempFile.isDirectory()){
				tempFile.mkdirs();		
			}
			boolean idRootDirBuilt = false;
			boolean isUseLocalTemp = false;
			do{
				zipEntry = zipInputStream.getNextEntry();
				if(zipEntry!= null){
					String entryName = zipEntry.getName();
					if(!entryName.startsWith(zipName+"/")){
						entryName = zipName+"/"+entryName;
						if(!idRootDirBuilt){							
							GtDataUtils.importMeta("", -1, "0", new GtPath(outPathObj.getDisplayPath() +"/"+zipName).getGtPath());
							idRootDirBuilt = true;
						}
					}
					String url = "";
					String dfs = GtDataConfig.CONSTANT.ZERO.strVal;
					long size = zipEntry.getSize();
					boolean isMemoryCache = true;
					if(size > GtDataConfig.HBASE_FILESIZE_MAX || size < 0){
						isMemoryCache = false;
					}
					if (!zipEntry.isDirectory()) {
						if(isMemoryCache){
							byte[] contents = new byte[(int) size];
							contents = IOUtils.readFully(zipInputStream, (int) size,false);	
							if(size == 0){
								//空文件使用一样的MD5
								url = "d41d8cd98f00b204e9800998ecf8427e";
							}else{
								url = MD5Calculate.fileByteMD5(contents);
							}
							GtDataUtils.importResource(contents, url, size);
						}else{
							File file = new File(tempFile.getPath(),entryName);	
							if(!file.getParentFile().isDirectory())
								file.getParentFile().mkdirs();
							FileOutputStream bos = new FileOutputStream(file);
							byte[] buffer = new byte[GtDataConfig.BUFFER];
							int len = 0;
							while (-1 != (len = zipInputStream.read(buffer, 0 , GtDataConfig.BUFFER))) {
								bos.write(buffer, 0, len);
							}
							bos.close();
							isUseLocalTemp = true;
						}
					}else{
						isMemoryCache = true;
						size = -1;
					}
					if(isMemoryCache){
						String rowKey = new GtPath(outPathObj.getDisplayPath() +"/"+entryName).getGtPath();
						GtDataUtils.importMeta(url, size, dfs, rowKey);
					}
				}
			}while(zipEntry != null);
			if(isUseLocalTemp){
				HTable resTable = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
				HTable metaTable = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
				try{					
					Import importDir = new Import();
					importDir.ImportDirFileFilter(metaTable, resTable, tempFile.getPath(), outputPath, null);
					FileOperate.deleteDir(tempFile);
				}finally{
					if(resTable != null)
						resTable.close();
					if(metaTable != null)
						metaTable.close();
				}	
			}			
			RedisUtils.redisFileCheck(outPathObj.getGtPath(), GtDataConfig.REDIS_HOST);
			return true;
	}
	
	
	public static void main(String[] args) throws IOException, ArchiveException {
//		String localPath = "E:/myJob/ziptest.zip";
//		String outPath = "/src/test/abc";
//		File file = new File(localPath);
//		if(file.isFile() && file.getName().endsWith(".zip")){		
//				FileInputStream in = new FileInputStream(file);	
//				ZipInputStream zipInputStream = new ZipInputStream(in);
//				unZipMix(zipInputStream, outPath,file.getName().replace(".zip", ""));
//		}	
		
//		unZipFromGTData("/src/test/ziptest.zip","/src/test/efg","ziptest");
	}
}
