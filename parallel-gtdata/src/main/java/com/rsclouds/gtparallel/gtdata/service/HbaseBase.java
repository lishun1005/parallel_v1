package com.rsclouds.gtparallel.gtdata.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableFactory;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.HTableUtil;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class HbaseBase {

	private static Configuration conf = HBaseConfiguration.create();


	private final static Logger logger = LoggerFactory.getLogger(HbaseBase.class);

	private static HTablePool hTablePool= new HTablePool(conf, GtDataConfig.HBASE_POOL_SIZE);
	

	public static Configuration getHbaseConf(){
		return conf;
	}


	/**
	 * createTable
	 * 
	 * @throws IOException
	 */
	public static boolean createTable(String tablename, String[] cfs) throws IOException {
		HBaseAdmin admin = new HBaseAdmin(conf);
		if (admin.tableExists(tablename)) {
			logger.info("table is already exist : " + tablename);
			return false;
		} else {
			HTableDescriptor tableDesc = new HTableDescriptor(tablename);
			for (int i = 0; i < cfs.length; i++) {
				tableDesc.addFamily(new HColumnDescriptor(cfs[i]));
			}
			admin.createTable(tableDesc);
			logger.info("createTable success : " + tablename);
			return true;
		}	
	}

	/**
	 * deleteTable
	 * 
	 * @param tablename
	 * @throws IOException
	 */
	public static boolean deleteTable(String tablename) throws IOException {
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			admin.disableTable(tablename);
			admin.deleteTable(tablename);
			logger.info("deleteTable success : " + tablename);
		} catch (MasterNotRunningException e) {
			e.printStackTrace();
			throw e;
		} catch (ZooKeeperConnectionException e) {
			e.printStackTrace();
			throw e;
		}
		return true;
	}

	/**
	 * write a Row
	 * 
	 * @param tablename
	 * @param cfs
	 * @throws IOException 
	 */
	public static void writeRow(String tablename, String rowkey, String family,String qualifier, String value) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		Put put = new Put(Bytes.toBytes(rowkey));
		put.add(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
		table.put(put);	
	}
	
	public static void writeRow(String tablename, byte[] rowkey, byte[] family,byte[] qualifier, byte[] value) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		Put put = new Put(rowkey);
		put.add(family, qualifier, value);
		table.put(put);	

	}
	
	
	
	/**
	 * 地图瓦片入库
	 * @param rowkey
	 * @param md5
	 * @param time
	 * @param filesize
	 * @throws IOException
	 */
	public static void writeMapMetaDataRow(String rowkey, String md5, long time, long filesize) throws IOException {
		byte[] dfsByte = null;
		if(filesize < GtDataConfig.HBASE_FILESIZE_MAX) {
			dfsByte = "0".getBytes();
		}else {
			dfsByte = "1".getBytes();
		}
		writeMapMetaDataRow(rowkey.getBytes(), md5.getBytes(), Bytes.toBytes("" +time), Bytes.toBytes("" +filesize), dfsByte);
	}
	
	/**
	 * 地图瓦片入库
	 * @param rowkeyByte
	 * @param md5Byte
	 * @param timeByte
	 * @param filesizeByte
	 * @param dfsByte
	 * @throws IOException
	 */
	public static void writeMapMetaDataRow(byte[] rowkeyByte, byte[] md5Byte, byte[] timeByte,
			byte[] filesizeByte, byte[] dfsByte) throws IOException {
		HTableInterface table =  hTablePool.getTable(GtDataConfig.TABLE_NAME.MAP_META_TABLE.getStrVal()); 
		Put put = new Put(rowkeyByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, md5Byte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal, timeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal, filesizeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal, dfsByte);
		table.put(put);
		table.flushCommits();	
	}
	
	
	/**
	 * 地图瓦片入库
	 * @param rowkey
	 * @param md5
	 * @param time
	 * @param filesize
	 * @throws IOException
	 */
	public static void writeMapMetaDataRow(HTable mapMeta, String rowkey, String md5, long time, long filesize) throws IOException {
		byte[] dfsByte = null;
		if(filesize < GtDataConfig.HBASE_FILESIZE_MAX) {
			dfsByte = "0".getBytes();
		}else {
			dfsByte = "1".getBytes();
		}
		writeMapMetaDataRow(mapMeta, rowkey.getBytes(), md5.getBytes(), Bytes.toBytes("" +time), Bytes.toBytes("" +filesize), dfsByte);
	}
	
	/**
	 * 地图瓦片入库
	 * @param rowkeyByte
	 * @param md5Byte
	 * @param timeByte
	 * @param filesizeByte
	 * @param dfsByte
	 * @throws IOException
	 */
	public static void writeMapMetaDataRow(HTable mapMeta, byte[] rowkeyByte, byte[] md5Byte, byte[] timeByte,
			byte[] filesizeByte, byte[] dfsByte) throws IOException { 
		Put put = new Put(rowkeyByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, md5Byte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal, timeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal, filesizeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal, dfsByte);
		mapMeta.put(put);
		mapMeta.flushCommits();	
	}
	
	
	
	
	
		
	/**
	 * 插入一条rowkey 到 metadata 表，该记录包含了rowkey对应的所有列值
	 * @param htable
	 * @param rowkey
	 * @param md5
	 * @param time
	 * @param filesize
	 * @throws IOException
	 */
	public static void writeMetaDataRow(HTable htable, String rowkey, String md5, long time, long filesize) throws IOException {
		byte[] dfsByte = null;
		boolean isDirectory = false;
		if(filesize < GtDataConfig.HBASE_FILESIZE_MAX) {
			dfsByte = "0".getBytes();
			if(filesize == -1) {
				isDirectory = true;
			}
		}else {
			dfsByte = "1".getBytes();
		}
		writeMetaDataRow(htable, rowkey.getBytes(), md5.getBytes(), Bytes.toBytes("" +time), Bytes.toBytes("" +filesize), dfsByte, isDirectory);
	}
	
	/**
	 * 插入一条rowkey 到 metadata 表，该记录包含了rowkey对应的所有列值
	 * 容量列默认值为00 （目录）或 10（文件）
	 * 权限默认值为  1 110 000 000 110 （目录） 或 0 110 000 000 110（文件）
	 * @param htable
	 * @param rowkeyByte
	 * @param md5Byte
	 * @param timeByte
	 * @param filesizeByte
	 * @param dfsByte
	 * @param isDirectory
	 * @throws IOException
	 */
	public static void writeMetaDataRow(HTable htable, byte[] rowkeyByte, byte[] md5Byte, byte[] timeByte,
			byte[] filesizeByte, byte[] dfsByte, boolean isDirectory) throws IOException {
		Put put = new Put(rowkeyByte);
		if(isDirectory) {
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal, GtDataConfig.CONSTANT.DIR_CAPACITY.byteVal);
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal, GtDataConfig.CONSTANT.DIR_PERMISSON.byteVal);
		} else {
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal, GtDataConfig.CONSTANT.FILE_CAPACITY.byteVal);
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal, GtDataConfig.CONSTANT.FILE_PERMISSON.byteVal);
		}
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, md5Byte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal, timeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal, filesizeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal, dfsByte);
		htable.put(put);
	}
	
	/**
	 * 插入一条rowkey 到 metadata 表，该记录包含了rowkey对应的所有列值
	 * @param rowkey
	 * @param md5
	 * @param time
	 * @param filesize
	 * @throws IOException
	 */
	public static void writeMetaDataRow(String rowkey, String md5, long time, long filesize) throws IOException {
		byte[] dfsByte = null;
		boolean isDirectory = false;
		String md5Temp = md5;
		if(filesize < GtDataConfig.HBASE_FILESIZE_MAX) {
			dfsByte = "0".getBytes();
			if(filesize == -1) {
				isDirectory = true;
				md5Temp = "";
			}
		}else {
			dfsByte = "1".getBytes();
		}
		writeMetaDataRow(rowkey.getBytes(), md5Temp.getBytes(), Bytes.toBytes("" +time), Bytes.toBytes("" +filesize), dfsByte, isDirectory);
	}
	
	/**
	 * 插入一条rowkey 到 metadata 表，该记录包含了rowkey对应的所有列值
	 * 容量列默认值为00 （目录）或 10（文件）
	 * 权限默认值为  1 110 000 000 110 （目录） 或 0 110 000 000 110（文件）
	 * @param rowkeyByte
	 * @param md5Byte      
	 * @param timeByte
	 * @param filesizeByte
	 * @param dfsByte
	 * @param isDirectory
	 * @throws IOException
	 */
	public static void writeMetaDataRow(byte[] rowkeyByte, byte[] md5Byte, byte[] timeByte,
			byte[] filesizeByte, byte[] dfsByte, boolean isDirectory) throws IOException {
		HTableInterface table =  hTablePool.getTable(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal()); 
		Put put = new Put(rowkeyByte);
		if(isDirectory) {
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal, GtDataConfig.CONSTANT.DIR_CAPACITY.byteVal);
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal, GtDataConfig.CONSTANT.DIR_PERMISSON.byteVal);
		} else {
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal, GtDataConfig.CONSTANT.FILE_CAPACITY.byteVal);
			put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal, GtDataConfig.CONSTANT.FILE_PERMISSON.byteVal);
		}
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, md5Byte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal, timeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal, filesizeByte);
		put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal, dfsByte);
		table.put(put);
		table.flushCommits();
	}
	
	
	/**
	 * write Rows
	 * 
	 * @param tablename
	 * @param cfs
	 * @throws IOException 
	 */
	public static void writeRows(String tablename, String rowkey, String family,Map<String,String> qv) throws IOException {

		HTableInterface table =  hTablePool.getTable(tablename);
		Put put = new Put(Bytes.toBytes(rowkey));

		for(String qualifier : qv.keySet()){
			String value = String.valueOf(qv.get(qualifier));
			put.add(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
		}
		table.put(put);
	}
	
	public static void writeRows(String tablename, byte[] rowkey, byte[] family,Map<byte[],byte[]> qv) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		Put put = new Put(rowkey);
		for(byte[] qualifier : qv.keySet()){
			byte[] value = qv.get(qualifier);
			put.add(family, qualifier, value);
		}	
		table.put(put);	
	}

	/**
	 * delete a Row
	 * 
	 * @param tablename
	 * @param rowkey
	 * @throws IOException
	 */
	public static void deleteRow(String tablename, String rowkey) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		table.delete(new Delete(Bytes.toBytes(rowkey)));
		logger.info("delete a Row success : " + tablename + " : " + rowkey);
	}
	
	public static void deleteRow(String tablename, byte[] rowkey) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		table.delete(new Delete(rowkey));
		logger.info("delete a Row success : " + tablename + " : " + Bytes.toString(rowkey));
	}
	
	public static void deleteRow(String tablename, String... rowkey) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		List<Delete> list = new ArrayList<Delete>();
		if(rowkey!= null){
			for(String str : rowkey){
				list.add(new Delete(Bytes.toBytes(str)));
				logger.info("delete a Row : " + tablename + " : " + str);
			}
		}		
		table.delete(list);
		logger.info("delete Rows success ");
	}
	
	public static void deleteRow(String tablename, byte[]... rowkey) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		List<Delete> list = new ArrayList<Delete>();
		if(rowkey!= null){
			for(byte[] str : rowkey){
				list.add(new Delete(str));
				logger.info("delete a Row : " + tablename + " : " + str);
			}
		}		
		table.delete(list);
		logger.info("delete Rows success ");
	}
	
	public static void deleteRow(String tablename, List<byte[]> rowkey) throws IOException {
		HTableInterface table =  hTablePool.getTable(tablename);
		List<Delete> list = new ArrayList<Delete>();
		if(rowkey!= null){
			for(byte[] str : rowkey){
				list.add(new Delete(str));
				logger.info("delete a Row : " + tablename + " : " + str);
			}
		}		
		table.delete(list);
		logger.info("delete Rows success ");
	}

	/**
	 * select Row
	 * 
	 * @param tablename
	 * @param rowkey
	 */
	public static Result selectRow(String tablename, String rowKey)
			throws IOException {
		return selectRow(tablename,Bytes.toBytes(rowKey));
	}
	
	public static Result selectRow(String tablename, byte[] rowKey)
			throws IOException {
		HTable table = new HTable(conf, tablename);
		Get g = new Get(rowKey);
		Result rs = table.get(g);
		table.close();
		return rs;
	}

	/**
	 * 根据rowkey的关键字搜索一条记录
	 * @param tablename
	 * @param keyWord
	 * @return
	 * @throws IOException
	 */
	public static List<Result> selectByRowFilter(String tablename, String keyWord)
			throws IOException {
		List<Result> results = new ArrayList<Result>();
		HTableInterface table =  hTablePool.getTable(tablename);
		Scan scan = new Scan();
		Filter filter = new RowFilter(CompareFilter.CompareOp.EQUAL,new RegexStringComparator(keyWord));//such as : "resource/XTYY/public/map/cia/china_cia/Layers/_alllayers/L../"
		scan.setFilter(filter);
		ResultScanner rs = table.getScanner(scan);
		for (Result res : rs) {
			 results.add(res);
		}
		rs.close();
		return results;
	}

	/**
	 * 根据范围搜索相关记录
	 * @param tablename
	 * @param startRow
	 * @param stopRow
	 * @return
	 * @throws IOException
	 */
	public static List<Result> selectByRegions(String tablename,String startRow,String stopRow) throws IOException {
		List<Result> results = new ArrayList<Result>();
		HTableInterface table =  hTablePool.getTable(tablename);
		Scan scan = new Scan();
		scan.setMaxVersions();
		if(startRow!=null)
			scan.setStartRow(Bytes.toBytes(startRow));
		if(stopRow!=null)
			scan.setStopRow(Bytes.toBytes(stopRow));
		ResultScanner rs = table.getScanner(scan);
		for (Result res : rs) {
			 results.add(res);
		}
		rs.close();
		return results;
	}

	
	public static List<Result> selectByFilter(String tablename, List<String> arr)
			throws IOException {
		List<Result> results = new ArrayList<Result>();
		HTableInterface table =  hTablePool.getTable(tablename);
		FilterList filterList = new FilterList();
		Scan scan = new Scan();
		for (String v : arr) { // 各个条件之间是“与”的关系
			String[] s = v.split(",");
			filterList.addFilter(new SingleColumnValueFilter(Bytes
					.toBytes(s[0]), Bytes.toBytes(s[1]), CompareOp.EQUAL, Bytes
					.toBytes(s[2])));
			// 添加下面这一行后，则只返回指定的cell，同一行中的其他cell不返回
			// s1.addColumn(Bytes.toBytes(s[0]), Bytes.toBytes(s[1]));
		}
		scan.setFilter(filterList);
		ResultScanner rs = table.getScanner(scan);
		for (Result res : rs) {
			 results.add(res);
		}
		rs.close();
		return results;
	}
	

	public static List<Result> Scan(String tablename,int size,String startRow,String stopRow,String keyWord,List<String> arr) throws IOException{
		List<Result> results = new ArrayList<Result>();
		HTableInterface table =  hTablePool.getTable(tablename);
		Scan scan = new Scan();
		scan.setMaxVersions();
		if(size<0)
			size=50;
		if(startRow!=null)
			scan.setStartRow(Bytes.toBytes(startRow));
		if(stopRow!=null)
			scan.setStopRow(Bytes.toBytes(stopRow));
		FilterList filterList = new FilterList();
		if(keyWord != null)
			filterList.addFilter(new RowFilter(CompareFilter.CompareOp.EQUAL,new RegexStringComparator(keyWord)));
		if(arr != null){
			for (String v : arr) { // 各个条件之间是“与”的关系
				String[] s = v.split(",");
				filterList.addFilter(new SingleColumnValueFilter(Bytes
						.toBytes(s[0]), Bytes.toBytes(s[1]), CompareOp.EQUAL, Bytes
						.toBytes(s[2])));
				// 添加下面这一行后，则只返回指定的cell，同一行中的其他cell不返回
				// s1.addColumn(Bytes.toBytes(s[0]), Bytes.toBytes(s[1]));
			}
		}
		if(keyWord!=null || arr != null)
			scan.setFilter(filterList);		
		ResultScanner rs = table.getScanner(scan);
		if(startRow!=null || stopRow == null){
			int count = 0;
			for (Result res : rs) {
				 results.add(res);
				 count++;
				 if(size != 0 && count == size){
					 break;
				 }
			}
		}else if(stopRow!=null){		
			for (Result res : rs) {
				 results.add(res);
			}
			int length = results.size();
			if(size != 0  && length - size > 0 ){
				results = results.subList(length - size, length);
			}
		}
		rs.close();
		return results;
	}
	
	public static List<Result> ScanForTest(String tablename,Long offset,Long size,String startRow,String stopRow,String keyWord) throws IOException{
		List<Result> results = new ArrayList<Result>();
		HTableInterface table =  hTablePool.getTable(tablename);
		Scan scan = new Scan();
		scan.setMaxVersions();
		if(startRow!=null)
			scan.setStartRow(Bytes.toBytes(startRow));
		if(stopRow!=null)
			scan.setStopRow(Bytes.toBytes(stopRow));
		FilterList filterList = new FilterList();
		if(keyWord != null)
			filterList.addFilter(new RowFilter(CompareFilter.CompareOp.EQUAL,new RegexStringComparator(keyWord)));
		if(keyWord!=null )
			scan.setFilter(filterList);		
		ResultScanner rs = table.getScanner(scan);
		long count = 0;
		long ignore = 0;
		for (Result res : rs) {
			if(offset != null && ignore < offset){
				ignore++;
				continue;
			}
			 results.add(res);
			 count++;
			 if(size != null && count == size){
				 break;
			 }
		}
		rs.close();
		return results;
	}

	/**
	 * 列出所有table
	 * @return
	 * @throws IOException
	 */
	public static List<String> listTable() throws IOException {
		List<String> result = new ArrayList<String>();
		HBaseAdmin admin = new HBaseAdmin(conf);
		for (TableName name : admin.listTableNames()) {
			result.add(name.getNameAsString());
			logger.info(name.getNameAsString());
		}
		return result;
	}
	
	public static boolean isTableExist(String tableName) throws IOException{
		HBaseAdmin admin = new HBaseAdmin(conf);
		return admin.isTableEnabled(tableName);
	}
	

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		listTable();
	}


	

}
