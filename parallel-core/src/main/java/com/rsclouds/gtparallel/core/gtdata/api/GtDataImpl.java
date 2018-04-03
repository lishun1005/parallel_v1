package com.rsclouds.gtparallel.core.gtdata.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.core.common.Utils;
import com.rsclouds.gtparallel.core.exception.ParException;
import com.rsclouds.gtparallel.core.gtdata.common.RedisUtils;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.ConfProperty;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

public class GtDataImpl {
	
	public  static List<Map<String,String>> list(String gtPath) throws IOException{
		List<Map<String,String>> fileMapList = new ArrayList<Map<String,String>>();
		try {
			gtPath = GtDataUtils.format2GtPath(gtPath).replace("//", "/");	
			if("/".equals(gtPath)){
				gtPath = "";
			}
			List<Result> results = HbaseBase.Scan(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0, gtPath + "//", gtPath + "//{", null, null);
			for(Result rs : results){
				Map<String,String> fileMap = new HashMap<String,String>();
				String sizeStr = Bytes.toString(rs.getValue(
						GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
				String timeStr = Bytes.toString(rs.getValue(
						GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal));
				String rowKey = Bytes.toString(rs.getRow());
				if(rowKey.contains("%")){
					rowKey = TransCoding.decode(rowKey, "utf-8");
				}
				fileMap.put("path", rowKey.replace("//", "/"));
				fileMap.put("size", sizeStr);
				fileMap.put("time", Utils.timeStrFillZero(timeStr));
				fileMapList.add(fileMap);
			}
		} catch (IOException e) {
			e.printStackTrace(); 
			throw e;
		}	
		return fileMapList;	
	}
	
	public static boolean mkdir(String path) throws IOException {
		return mkdir(path,true);
	}
	
	public static boolean mkdir(String path,boolean keepTwoFile) throws IOException {
		try {
			path = GtDataUtils.format2GtPath(path);			
			if (GtDataUtils.getFileSzie(path) != null){
				if(!keepTwoFile){
					return false;
				}else{
					String renamePath = path;
					int times = 0;
					boolean isFileExist = true;
					do{
						times++;
						renamePath = GtDataUtils.genterNewPath(path,times);
						if (GtDataUtils.getFileSzie(renamePath) == null){
							isFileExist = false;
						}else{
							isFileExist = true;
						}			
					}while(isFileExist);
					path = renamePath;
				}	
			}	
//			Map<String, String> map = new HashMap<String, String>();
			HbaseBase.writeMetaDataRow(path, "", System.currentTimeMillis(), -1);
//			map.put(GtDataConfig.META.SIZE.strVal, "-1");
//			map.put(GtDataConfig.META.URL.strVal, "");
//			map.put(GtDataConfig.META.DFS.strVal, "0");
//			map.put(GtDataConfig.META.TIME.strVal, "" + System.currentTimeMillis());
//			HbaseBase.writeRows(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), path,GtDataConfig.META.FAMILY.strVal, map);
			//redis check
			RedisUtils.redisDirCheck(path,GtDataConfig.REDIS_HOST);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}		
	}
	
	public static Map<String,String> reanme(String dirPath,String oldFileName,String newFileName,boolean keepTwoFile) throws Exception {
		try {
			while(dirPath.endsWith("/") && dirPath.length()>1){
				dirPath = dirPath.substring(0, dirPath.length()-1);
			}
			String oldPath = TransCoding.UrlEncode(dirPath + "//" + oldFileName,"utf-8");
			String newPath = TransCoding.UrlEncode(dirPath + "//" + newFileName,"utf-8");
			String oldFileSize = GtDataUtils.getFileSzie(oldPath);
			//检查旧文件地址是否存在
			if (oldFileSize == null){
				throw new ParException("2003", oldFileName + " does not exist");
			}
			//检查新文件地址是否存在
			if (GtDataUtils.getFileSzie(newPath) != null){
				if(!keepTwoFile){
					throw new ParException("2002", newFileName + " already exist");
				}else{
					String renamePath = newPath;
					int times = 0;
					boolean isFileExist = true;
					do{
						times++;
						renamePath = GtDataUtils.genterNewPath(newPath,times);
						if (GtDataUtils.getFileSzie(renamePath) == null){
							isFileExist = false;
						}else{
							isFileExist = true;
						}			
					}while(isFileExist);
					newPath = renamePath;
				}	
			}
			return renameImpl(oldPath,newPath,true);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}		
	}
	
	public static Map<String,String> copyOrMove(String oldDirPath,String newDirPath,String filename,boolean isDel,boolean overwrite,boolean keepTwoFile) throws Exception {
		try {
			while(oldDirPath.endsWith("/") && oldDirPath.length()>1){
				oldDirPath = oldDirPath.substring(0, oldDirPath.length()-1);
			}
			while(newDirPath.endsWith("/") && newDirPath.length()>1){
				newDirPath = newDirPath.substring(0, newDirPath.length()-1);
			}		
			String oldPath = TransCoding.UrlEncode(oldDirPath + "//" + filename,"utf-8");
			String newPath = TransCoding.UrlEncode(newDirPath + "//" + filename,"utf-8");
			if(newPath.startsWith(oldPath)){
				throw new ParException("2001", "Invalid value of parameter");
			}
			String oldFileSize = GtDataUtils.getFileSzie(oldPath);
			//检查旧文件地址是否存在
			if (oldFileSize == null){
				throw new ParException("2003", filename + " does not exist");
			}
			//检查新文件地址是否存在
			if (GtDataUtils.getFileSzie(newPath) != null){
				if(!keepTwoFile && !overwrite){
					throw new ParException("2002", filename + " already exist");
				}else if(!overwrite && keepTwoFile){
					String renamePath = newPath;
					int times = 0;
					boolean isFileExist = true;
					do{
						times++;
						renamePath = GtDataUtils.genterNewPath(newPath,times);
						if (GtDataUtils.getFileSzie(renamePath) == null){
							isFileExist = false;
						}else{
							isFileExist = true;
						}			
					}while(isFileExist);
					newPath = renamePath;
				}
			}
			return renameImpl(oldPath,newPath,isDel);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}		
	}
	
	private static Map<String,String> renameImpl(String oldPath,String newPath,boolean isDel) throws ParException, IOException{
		Map<String,String> back = new HashMap<String,String>();
		Result result = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), oldPath);
		if(!result.isEmpty()){
			HTable metaTable = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			byte[] sizeByte = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
			List<byte[]> deletes = new ArrayList<byte[]>();
			NavigableMap<byte[], byte[]> map = result.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
	    	Put metaPut = new Put(Bytes.toBytes(newPath));
	    	if(map != null){
	    		for(Entry<byte[], byte[]> entry : map.entrySet()){
//	    			System.out.println(Bytes.toString(entry.getKey()) + ":" + Bytes.toString(entry.getValue()));
	    			metaPut.add(GtDataConfig.META.FAMILY.byteVal, entry.getKey(), entry.getValue());
	    		}
	    	}    	    		
			if(Arrays.equals(sizeByte, GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)){
				String startRow  = oldPath.replace("//", "/");
				List<Result> results = HbaseBase.Scan(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 1000, startRow+ "/", startRow+"/{", null, null);
				String replacement = newPath.replace("//", "/");
				if(results.size() < 1000){				
					for(Result rs : results){
						String key = Bytes.toString(rs.getRow());
						String newKey = key.replace(startRow, replacement);
						System.out.println(key + " : " + newKey);
						NavigableMap<byte[], byte[]> map2 = rs.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
				    	Put metaPut2 = new Put(Bytes.toBytes(newKey));
				    	if(map2 != null){
				    		for(Entry<byte[], byte[]> entry : map2.entrySet()){
//				    			System.out.println(Bytes.toString(entry.getKey()) + ":" + Bytes.toString(entry.getValue()));
				    			metaPut2.add(GtDataConfig.META.FAMILY.byteVal, entry.getKey(), entry.getValue());
				    		}
				    	}
				    	metaTable.put(metaPut2);
				    	if(isDel)
				    		deletes.add(rs.getRow());
					}
				}else{
					//使用mapreduce处理
					
					back.put("oldpath", startRow);
					back.put("newpath", replacement);
					back.put("isdel", isDel+"");
					back.put("result", "mapreduce");
					return back;
				}
			}
	    	metaTable.put(metaPut);
			if(isDel){
				deletes.add(Bytes.toBytes(oldPath));
				List<Delete> list = new ArrayList<Delete>();
				for(byte[] del : deletes){
					list.add(new Delete(del));
				}
				metaTable.delete(list);
				RedisUtils.redisDel(GtDataConfig.REDIS_HOST, deletes);
			}		
	    	metaTable.flushCommits();
	    	metaTable.close();
	    	RedisUtils.redisDirCheck(newPath, GtDataConfig.REDIS_HOST);
	    	back.put("result", "true");
	    	return back;
		}else{
			throw new ParException("2003", oldPath + " does not exist");
		}
	}
	
	
	
	
	public static boolean delete(String gtPath) throws IOException {
		try {
			gtPath = GtDataUtils.format2GtPath(gtPath);
			Result  result = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtPath);
			if(!result.isEmpty()){
				byte[] size = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
				List<byte[]> rowList = new ArrayList<byte[]>();
				if(Arrays.equals(GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal,size)){
					String startRow = gtPath.replace("//", "/");
					List<Result> resultList = HbaseBase.Scan(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0, startRow + "/", startRow + "/{", null, null);
					for(Result rs : resultList){
						String key = Bytes.toString(rs.getRow());
						NavigableMap<byte[], byte[]> map = rs.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
						HbaseBase.writeRows(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), Bytes.toBytes("-//"+key), GtDataConfig.META.FAMILY.byteVal, map);
						HbaseBase.deleteRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), Bytes.toString(rs.getRow()));
						rowList.add(rs.getRow());
					}
					RedisUtils.redisDel(GtDataConfig.REDIS_HOST,rowList);
				}
				NavigableMap<byte[], byte[]> map = result.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
				HbaseBase.writeRows(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), Bytes.toBytes("-//"+gtPath), GtDataConfig.META.FAMILY.byteVal, map);
				HbaseBase.deleteRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtPath);
				RedisUtils.redisDel( GtDataConfig.REDIS_HOST,gtPath);
				//redis check
				RedisUtils.redisDirCheck(gtPath,GtDataConfig.REDIS_HOST);			
				return true;
			}else{
				return false;
			}			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}		
	}
	
	public  static List<Map<String,String>> search(String gtPath,String keyword) throws IOException{
		List<Map<String,String>> fileMapList = new ArrayList<Map<String,String>>();
		try {
			gtPath = GtDataUtils.format2GtPath(gtPath).replace("//", "/");
			if("/".equals(gtPath)){
				gtPath = "";
			}
			keyword = TransCoding.UrlEncode(keyword, "utf-8");
			List<Result> results = HbaseBase.Scan(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0, gtPath+"/", gtPath+"/{", "\\/\\/.*("+keyword+")", null);
			for(Result rs : results){
				Map<String,String> fileMap = new HashMap<String,String>();
				String sizeStr = Bytes.toString(rs.getValue(
						GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
				String timeStr = Bytes.toString(rs.getValue(
						GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal));
				String rowKey = Bytes.toString(rs.getRow());
				if(rowKey.contains("%")){
					rowKey = TransCoding.decode(rowKey, "utf-8");
				}
				fileMap.put("path", rowKey.replace("//", "/"));
				fileMap.put("size", sizeStr);
				fileMap.put("time", Utils.timeStrFillZero(timeStr));
				fileMapList.add(fileMap);
			}
		} catch (IOException e) {
			e.printStackTrace(); 
			throw e;
		}	
		return fileMapList;	
	}
	
	public static boolean export(String gtpath,OutputStream out) throws IOException{
		try {
			gtpath = GtDataUtils.format2GtPath(gtpath);
			System.out.println("[export]::========"+gtpath);
			System.out.println("[GtDataConfig.TABLE_NAME.META_TABLE]::="+GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			Result result = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtpath);
			if (!result.isEmpty()) {
				String dfsStr = new String(result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal));
				int dfsInt = Integer.parseInt(dfsStr);
				byte[] md5Bytes = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
				String md5Str = Bytes.toString(md5Bytes);
				if (dfsInt == 0) {
					Result resultRes = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), md5Str);
					if ( resultRes == null) {
						out.close();
						return false;
					}else{
						byte[] value = resultRes.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.DATA.byteVal);
						out.write(value, 0, value.length);
						out.flush();
						out.close();
					}					
				}else if (dfsInt == 1){
					FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
					Result resultRes = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), md5Str);
					if ( resultRes == null) {
						out.close();
						return false;
					}else {
						byte[] value = resultRes.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.PREFIX.byteVal);
						if ( value != null && value.length > 0) {
							md5Str = new String(value) + md5Str;
						}else {
							md5Str = ConfProperty.getInstance().getStringValue("hdfs.md5.path") + "/" + md5Str;
						}
					}
					Path path = new Path(GtDataConfig.HDFS_ROOT_PATH , md5Str); 
					System.out.println("[export]::========"+path.toString());
					FSDataInputStream in = fs.open(path);
					int readLen;
					byte[] value = new byte[1024];
					while ( (readLen = in.read(value, 0, 1024)) != -1){
						out.write(value, 0, readLen);
					}
					in.close();
				}else {
					out.close();
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace(); 
			throw e;
		}
		return true;	
	}
	
	public static boolean importStream(String outPath,InputStream in,String md5) throws IOException{
		outPath = GtDataUtils.format2GtPath(outPath);
		return false;		
	}
	
	public  static List<Map<String,String>> trashList(String gtPath) throws IOException{
		List<Map<String,String>> fileMapList = new ArrayList<Map<String,String>>();
		try {
			gtPath = GtDataUtils.format2GtPath(gtPath).replace("//", "/");	
			if("/".equals(gtPath)){
				gtPath = "";
			}
			gtPath = "-//" + gtPath;
			List<Result> results = HbaseBase.Scan(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0, gtPath + "//", gtPath + "//{", null, null);
			for(Result rs : results){
				Map<String,String> fileMap = new HashMap<String,String>();
				String sizeStr = Bytes.toString(rs.getValue(
						GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
				String timeStr = Bytes.toString(rs.getValue(
						GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal));
				String rowKey = Bytes.toString(rs.getRow());
				if(rowKey.contains("%")){
					rowKey = TransCoding.decode(rowKey, "utf-8");
				}
				fileMap.put("path", rowKey.replace("//", "/"));
				fileMap.put("size", sizeStr);
				fileMap.put("time", Utils.timeStrFillZero(timeStr));
				fileMapList.add(fileMap);
			}
		} catch (IOException e) {
			e.printStackTrace(); 
			throw e;
		}	
		return fileMapList;	
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		System.out.println(HbaseBase.selectRow(GtDataConfig.META_TABLENAME, "/import//%E6%88%91%E6%98%AF%E4%B8%AD%E6%96%872"));
//		for(Map<String,String> map : search("/import","目录")){
//			System.out.println(map);
//		}
		for(Map<String,String> map : trashList("-///")){
			System.out.println(map);
		}
//		mkdir("/import");
//		System.out.println(reanme("/import3/new6/test4","123(1).txt","456.txt",true));
//		System.out.println(copyOrMove("/import3/new6/test3/_alllayers/55555","/import3/new6/test4","123.txt",false,true,false));
	}


}
