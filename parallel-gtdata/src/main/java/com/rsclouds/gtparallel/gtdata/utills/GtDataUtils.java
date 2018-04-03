package com.rsclouds.gtparallel.gtdata.utills;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.GtDataImpl;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

/**
 * gt-data工具类
 * @author wugq
 *
 */
public class GtDataUtils {
	public final static Configuration hbaseConfig = HBaseConfiguration.create();

	/**
	 * 转换为gt-data的路径格式 before : /new/test/中文/123.tif after :
	 * /new/test/%E4%B8%AD%E6%96%87//123.tif
	 * 
	 * @param path
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String format2GtPath(String path){
		while (path.endsWith("/") && path.length() > 1) {
			path = path.substring(0, path.length() - 1);
		}
		String[] paths = StringUtils.splitByWholeSeparator(path, "/");
		String out = "";
		for (int i = 0; i < paths.length - 1; i++) {
			out = out + "/" + paths[i];
		}
		out = out + "//" + paths[paths.length - 1];
		if (!out.contains("%")) {
			out = TransCoding.UrlEncode(out, "utf-8");
		}
		return out;
	}

	public static String genterNewPath(String path, int num) {
		int index = path.lastIndexOf(".");
		if (index > 0) {
			return path.substring(0, index) + "(" + num + ")"
					+ path.substring(index);
		} else {
			return path + "(" + num + ")";
		}
	}

	public static String format2DisplayPath(String gtPath){
		if (gtPath != null && gtPath.length() > 0) {
			while (gtPath.endsWith("/") && gtPath.length() > 1) {
				gtPath = gtPath.substring(0, gtPath.length() - 1);
			}
			gtPath = gtPath.replace("//", "/");
			if (gtPath.contains("%")) {
				gtPath = TransCoding.decode(gtPath, "utf-8");
			}
		}
		return gtPath;
	}

	public static String replaceLast(String string, String toReplace,
			String replacement) {
		int pos = string.lastIndexOf(toReplace);
		if (pos > -1) {
			return string.substring(0, pos)
					+ replacement
					+ string.substring(pos + toReplace.length(),
							string.length());
		} else {
			return string;
		}
	}

	/**
	 * 获取gt-data文件大小
	 * 
	 * @param gtPath
	 * @return
	 * @throws IOException
	 */
	public static String getFileSzie(String gtPath) throws IOException {
		Result rs = HbaseBase.selectRow(
				GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtPath);
		if (rs != null) {
			byte[] sizeByte = rs.getValue(GtDataConfig.META.FAMILY.byteVal,
					GtDataConfig.META.SIZE.byteVal);
			if (sizeByte != null && sizeByte.length > 0) {
				return Bytes.toString(sizeByte);
			}
		}
		return "-1";
	}

	/**
	 * 自动生成目录结构文件夹
	 * 
	 * @param path
	 *            such as : /map/new/132
	 * @return
	 * @throws IOException
	 */
	public static boolean genterGtdataDir(String path) throws IOException {
		GtPath pathObj = new GtPath(path);
		String encodedPath = pathObj.getGtPath();
		String size = getFileSzie(encodedPath);
		if (size == null) {
			GtDataImpl.getInstance().mkdir(encodedPath, false);
			if (encodedPath.lastIndexOf("//") > 0) {
				return genterGtdataDir(pathObj.getGtParent());
			} else {
				return true;
			}
		} else if (size.equals("-1")) {
			return true;
		}
		return false;
	}

    /**
     * 字符串向右补零到13位，主要用于时间处理
     * @param timeStr
     * @return
     */
	public static String timeStrFillZero(String timeStr){
		if(timeStr.length() < 13){
			String str = "0000000000000";	
			return timeStr + str.substring(0,13-timeStr.length());
		}
		return timeStr;
	}
	
	
	public static void importMeta(String url, long size, String dfs,
			String rowKey) throws IOException {
		long time = System.currentTimeMillis();
		HbaseBase.writeMetaDataRow(rowKey, url, time, size);
//			Map<byte[], byte[]> meta = new HashMap<byte[], byte[]>();
//			meta.put(GtDataConfig.META.URL.byteVal, Bytes.toBytes(url));
//			meta.put(GtDataConfig.META.SIZE.byteVal, Bytes.toBytes(size + ""));
//			meta.put(GtDataConfig.META.DFS.byteVal, Bytes.toBytes(dfs));
//			meta.put(GtDataConfig.META.TIME.byteVal,Bytes.toBytes(System.currentTimeMillis() + ""));
//			HbaseBase.writeRows(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
//					Bytes.toBytes(rowKey), GtDataConfig.META.FAMILY.byteVal,
//					meta);
	}

	public static void importResource(byte[] contents,String url,long size) throws IOException{
			Result rs = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), url);
			if (!rs.isEmpty()) {
				String links = Bytes.toString(rs.getValue(
						GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.LINKS.byteVal));
				long linksLong = Long.parseLong(links);
				byte[] newLinks = Bytes.toBytes((linksLong + 1) + "");
				HbaseBase.writeRow(
						GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
						Bytes.toBytes(url),
						GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.LINKS.byteVal, newLinks);
			} else {
				Map<byte[], byte[]> kv = new HashMap<byte[], byte[]>();
				if (size < GtDataConfig.HBASE_FILESIZE_MAX)
					kv.put(GtDataConfig.RESOURCE.DATA.byteVal, contents);
				else {
					FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
					FSDataOutputStream out = fs.create(new Path(GtDataConfig.HDFS_MD5_PATH, url));
					out.write(contents, 0, (int) size);
					out.close();
				}
				kv.put(GtDataConfig.RESOURCE.LINKS.byteVal,GtDataConfig.CONSTANT.ONE.byteVal);
				kv.put(GtDataConfig.RESOURCE.SIZE.byteVal,Bytes.toBytes(size + ""));
				HbaseBase.writeRows(
						GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
						Bytes.toBytes(url),
						GtDataConfig.RESOURCE.FAMILY.byteVal, kv);
			}
		
	}
	
	public static byte[] getByteFileSzie(String gtPath) throws IOException{
		Result rs = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtPath);
		if(rs!= null){
			byte[] sizeByte = rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
			if(sizeByte != null && sizeByte.length>0){
				return sizeByte;
			}
		}
		return null;
	}
	
	public static boolean isDir(String gtPath) throws IOException{
		GtPath geojsonPathObj = new GtPath(gtPath);	
		byte[] filesize = getByteFileSzie(geojsonPathObj.getGtPath());
		if(Arrays.equals(filesize, GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)){
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean isFile(String gtPath) throws IOException{
		GtPath geojsonPathObj = new GtPath(gtPath);	
		byte[] filesize = getByteFileSzie(geojsonPathObj.getGtPath());
		if(filesize!=null && !Arrays.equals(filesize, GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)){
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean isExist(String gtPath) throws IOException{
		GtPath geojsonPathObj = new GtPath(gtPath);	
		return getByteFileSzie(geojsonPathObj.getGtPath()) == null?false:true;
	}
	
	public static boolean export(String metaTable,String resTable,String gtpath,OutputStream out) throws IOException{
		try {
			gtpath = GtDataUtils.format2GtPath(gtpath);
			Result result = HbaseBase.selectRow(metaTable, gtpath);
			if (!result.isEmpty()) {
				String dfsStr = new String(result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal));
				int dfsInt = Integer.parseInt(dfsStr);
				byte[] md5Bytes = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
				String md5Str = Bytes.toString(md5Bytes);
				if (dfsInt == 0) {
					Result resultRes = HbaseBase.selectRow(resTable, md5Str);;
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
					FSDataInputStream in = fs.open(new Path(GtDataConfig.HDFS_MD5_PATH , md5Str));
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
			else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace(); 
			throw e;
		}
		return true;	
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
	}

}
