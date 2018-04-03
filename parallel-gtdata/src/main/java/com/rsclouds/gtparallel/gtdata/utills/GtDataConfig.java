package com.rsclouds.gtparallel.gtdata.utills;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

/**
 * gt-data配置文件
 * @author wugq
 *
 */
public class GtDataConfig {
	/**
	 * if the file size is small than HBASE_FILESIZE_MAX(it dose not include equal), this file will be save to hbase, Otherwise will be save to hdfs.
	 */
	public static long HBASE_FILESIZE_MAX= Long.parseLong(ConfProperty.getInstance().getStringValue("hbase.value.size.max"));
	public static String HDFS_ROOT_PATH = HbaseBase.getHbaseConf().get("fs.defaultFS")+"/";
	public static String HDFS_MD5_PATH = new Path(HDFS_ROOT_PATH ,ConfProperty.getInstance().getStringValue("hdfs.md5.path")).toString();
	public static String HDFS_TEMP_PATH = new Path(HDFS_ROOT_PATH,ConfProperty.getInstance().getStringValue("hdfs.temp.path")).toString();
	public static String REDIS_HOST = ConfProperty.getInstance().getStringValue("redis.host");
	public static int REDIS_PORT = ConfProperty.getInstance().getIntValue("redis.port");
	public static int HBASE_POOL_SIZE = ConfProperty.getInstance().getIntValue("hbase.pool.size");
	public static final int BUFFER = 8192;
		
	public enum CONSTANT{
		ONE("1"),
		ZERO("0"),
		NEGATIVE_ONE("-1"),
		FILE_CAPACITY("00"),
		DIR_CAPACITY("10"),
		FILE_PERMISSON("0110000000110"),
		DIR_PERMISSON("1110000000110")
		;	
		public final String strVal;
		public final byte[] byteVal;
		CONSTANT(String str){
			strVal = str;
			byteVal = Bytes.toBytes(str);
		}
	}
	
	public enum TABLE_NAME{
		META_TABLE(ConfProperty.getInstance().getStringValue("meta.table")),
		RES_TABLE(ConfProperty.getInstance().getStringValue("resource.table")),
		MAP_META_TABLE(ConfProperty.getInstance().getStringValue("map.meta.table")),
		MAP_RES_TABLE(ConfProperty.getInstance().getStringValue("map.res.table")),
		;
		private String strVal;
		private byte[] byteVal;
		TABLE_NAME(String str){
			strVal = str;
			byteVal = Bytes.toBytes(str);
		}
		public String getStrVal() {
			return strVal;
		}
		public byte[] getByteVal() {
			return byteVal;
		}
		
	}

	
	public enum META{
		FAMILY("atts"),
		URL("url"),
		DFS("dfs"),
		SIZE("size"),
		TIME("time"),
		CAPACITY("capacity"),
		PERMISSON("permisson")
		;
		
		public final String strVal;
		public final byte[] byteVal;		
		META(String str){
			strVal = str;
			byteVal = Bytes.toBytes(str);
		}
	}
	
	public enum RESOURCE{
		FAMILY("img"),
		LINKS("links"),
		DATA("data"),
		SIZE("size"),
		PREFIX("prefix"),
		;
		public final String strVal;
		public final byte[] byteVal;		
		RESOURCE(String str){
			strVal = str;
			byteVal = Bytes.toBytes(str);
		}

	}
	
	public static void  main(String[] args) throws IOException{
		System.out.println(META.DFS.strVal);
		System.out.println(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		System.out.println(new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.META_TABLE.getStrVal()));
		System.out.println(HDFS_ROOT_PATH);
		System.out.println(HDFS_MD5_PATH); 
		System.out.println(new Path(HDFS_ROOT_PATH,"md5Str"));
	}
	
	
}
