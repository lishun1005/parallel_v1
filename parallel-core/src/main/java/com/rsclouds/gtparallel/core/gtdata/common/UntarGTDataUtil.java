package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.decompress.TarDecomressGTData;
import com.rsclouds.gtparallel.core.gtdata.operation.Import;
import com.rsclouds.gtparallel.core.hadoop.io.MD5FileSize;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class UntarGTDataUtil {
	public static void unTarGTData(HTable metaTable, HTable resTable,
			 String filename, MD5FileSize md5FileSize,
			String outputPath) {
		long fileSize = md5FileSize.getFileSize().get();
		String url = md5FileSize.getMd5().toString();
		if (fileSize < GtDataConfig.HBASE_FILESIZE_MAX) {
			unTarHbase(metaTable, resTable, url, filename, outputPath);
		} else {
			unTarHDFS(metaTable, resTable, url, filename, outputPath);
		}
	}
	
	public static boolean unTarFromGTData(String inpath,String outPath,String fileName){
		HTable resTable = null;
		HTable metaTable = null;
		try {
			resTable = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			metaTable = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			GtPath inpathObj = new GtPath(inpath);
			GtPath outPathObj = new GtPath(outPath);
			Result meta = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), inpathObj.getGtPath());
			if(!meta.isEmpty()){
				byte[] size = meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
				if(!Arrays.equals(GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal,size)){
					byte[] dfs = meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal);
					String url = Bytes.toString(meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal));
					if(Arrays.equals(GtDataConfig.CONSTANT.ZERO.byteVal, dfs)){
						//hbase
						unTarHbase(metaTable, resTable, url, fileName, outPathObj.getGtPath());
						return  true;
					}else{
						//hdfs
						unTarHDFS(metaTable, resTable, url, fileName, outPathObj.getGtPath());
						return  true;
					}
				}			
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if (metaTable != null)
					metaTable.close();
				if (resTable != null) {
					resTable.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;	
	}

	public static void unTarHbase(HTable metaTable, HTable resTable,
			 String url, String filename, String outputPath) {
		Get get = new Get(Bytes.toBytes(url));
		Result result;
		try {
			result = resTable.get(get);
			if(result.isEmpty()){
				return;
			}
			byte[] data = result.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal,
					GtDataConfig.RESOURCE.DATA.byteVal);
			ByteArrayInputStream in = new ByteArrayInputStream(data); 
			GZIPInputStream zip = new GZIPInputStream(in);
			TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
					.createArchiveInputStream("tar", zip);
			unTar(metaTable, resTable,tarInputStream,filename, outputPath);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ArchiveException e) {
			e.printStackTrace();
		}

	}

	public static void unTarHDFS(HTable metaTable, HTable resTable,
			 String url, String filename, String outputPath) {
		try {
			FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
			FSDataInputStream in = fs.open(new Path(GtDataConfig.HDFS_MD5_PATH, url));
			GZIPInputStream zip = new GZIPInputStream(in);
			TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
					.createArchiveInputStream("tar", zip);
			unTar(metaTable, resTable,tarInputStream,filename, outputPath);
			if(tarInputStream!=null)
				tarInputStream.close();
			if(in != null)
				in.close();
			if(fs!=null)
				fs.close();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ArchiveException e) {
			e.printStackTrace();
		}
	}

	public static boolean unTar(HTable metaTable, HTable resTable,
			TarArchiveInputStream tarInputStream, String tarFilename,
			String outputPath) {
		try {
			TarArchiveEntry entry = tarInputStream.getNextTarEntry();
			String tempDir = CoreConfig.LOCA_LTEMP_DIR;
			if (tempDir.isEmpty()) {
				return false;
			}
			UUID uid = UUID.randomUUID();
			File tempFile = new File(tempDir,uid.toString());
			File tarDir = new File(tempFile.getPath(),tarFilename);
			if(!tarDir.exists()){
				tarDir.mkdirs();
				TarDecomressGTData.LOG.info("temp mkdirs : " + tarDir.getPath());				
			}
			String uuidPath = tempDir;
			if (uuidPath.endsWith("/")) {
				uuidPath += uid + "/";
				
			}else {
				uuidPath += "/" + uid + "/";
			}			
			BufferedInputStream bufferedInputStream = new BufferedInputStream(tarInputStream);
			while (entry != null) {
				String keyStr = entry.getName();
				keyStr = stringFormat(keyStr, tarFilename);
				if (entry.isFile()) {				
					File file = new File(uuidPath + keyStr);					
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
					TarDecomressGTData.LOG.info("write over to : " + file.getPath());
				}else{
					new File(uuidPath + keyStr).mkdirs();
					TarDecomressGTData.LOG.info("mkdirs : " + uuidPath + keyStr);
				}
				entry = tarInputStream.getNextTarEntry();
			}
			bufferedInputStream.close();
			Import importDir = new Import();
			importDir.ImportDirFileFilter(metaTable, resTable, uuidPath, outputPath, null);
			FileOperate.deleteDir(new File(uuidPath));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static String stringFormat(String keyStr, String filename) {
		String prefix = filename + "/";
		if(!keyStr.startsWith(prefix)){
			keyStr = prefix + keyStr;
		}
		return 	keyStr;	
	}
	
	public static void main(String[] args) throws IOException, ArchiveException {
		// TODO Auto-generated method stub
		Configuration conf = HbaseBase.getHbaseConf();
		FileSystem fs = FileSystem.get(conf);
		HTable resTable = new HTable(conf, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
		HTable metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		Result meta = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), "/rar//new.tar.gz");
		String url  = Bytes.toString(meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal));
		String size  = Bytes.toString(meta.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
		Result values = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), url);		
		if(!values.isEmpty()){
			MD5FileSize md5FileSize = new MD5FileSize(url,Long.parseLong(size));
//			unTarGTData(metaTable, resTable,fs, "test", md5FileSize,"/hello");
//			byte[] data = values.getValue(GtDataConfig.RESOURCE_FAMILY,GtDataConfig.RESOURCE_DATA);
//			ByteArrayInputStream in = new ByteArrayInputStream(data); 
//			GZIPInputStream zip = new GZIPInputStream(in);
//			TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
//					.createArchiveInputStream("tar", zip);
//			TarArchiveEntry entry = null;
//			do{
//				entry = tarInputStream.getNextTarEntry();
//				if(entry != null)
//					System.out.println(entry.getName());
//			}while(entry != null);
			
		}	
	}
}
