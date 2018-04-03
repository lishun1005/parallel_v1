package com.rsclouds.gtparallel.core.hbase;

import java.io.IOException;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class UpdateHbaseWidthResourceSize {
	private byte[] capacity = "capacity".getBytes();
	private byte[] dirCapacity = "00".getBytes();
	private byte[] fileCapacity = "10".getBytes();
	private byte[] permisson = "permisson".getBytes();
	private byte[] filePermisson = "0110000000110".getBytes();
	private byte[] dirPermisson = "1110000000110".getBytes();
	private String[] rowkeyArray = {
			"/users/rscloudmart/pldata/fulls/20150110_050319_0801//20150110_050319_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150110_050319_1_0801//20150110_050319_1_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150110_050320_0801//20150110_050320_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150110_050321_0801//20150110_050321_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150110_050322_0801//20150110_050322_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150110_050325_0801//20150110_050325_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150110_050326_0801//20150110_050326_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150110_050327_0801//20150110_050327_0801_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150416_031609_090b//20150416_031609_090b_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150416_031609_090b//20150416_031609_090b_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150416_031613_090b//20150416_031613_090b_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150416_031613_090b//20150416_031613_090b_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031729_0905//20150420_031729_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031729_0905//20150420_031729_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031730_0905//20150420_031730_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031730_0905//20150420_031730_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031925_0905//20150420_031925_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031925_0905//20150420_031925_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031926_0905//20150420_031926_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031926_0905//20150420_031926_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031929_0905//20150420_031929_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031929_0905//20150420_031929_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031930_0905//20150420_031930_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031930_0905//20150420_031930_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031932_0905//20150420_031932_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031933_0905//20150420_031933_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031933_0905//20150420_031933_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031935_0905//20150420_031935_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031935_0905//20150420_031935_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031936_0905//20150420_031936_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150420_031936_0905//20150420_031936_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150509_060056_0823//20150509_060056_0823_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150525_025743_0905//20150525_025743_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150525_025743_0905//20150525_025743_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150525_025744_0905//20150525_025744_0905_analytic.tif",
			"/users/rscloudmart/pldata/fulls/20150525_025744_0905//20150525_025744_0905_visual.tif",
			"/users/rscloudmart/pldata/fulls/20150525_025746_0905//20150525_025746_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20141020_021236_0908_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20141027_045724_0906_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150416_031613_090b_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031729_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031730_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031926_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031929_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031930_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031932_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031933_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031935_0905_visual.tif",
			"/users/rscloudmart/pldata/pl-disk-20150611/scenes//20150420_031936_0905_visual.tif",
			"/users/ywy1//WVS+1.24.121.exe"
	};
	
	public void updateSizebyRowkey(String metaTableName, String resTableName, String[] rowkey, boolean flag){
		String[] rowkeyTemp;
		if (rowkey == null) {
			rowkeyTemp = rowkeyArray;
		}else {
			rowkeyTemp = rowkey;
		}
		
		Configuration conf = HBaseConfiguration.create();
		HTable meta = null;
		HTable resTable = null;
		try {
			meta = new HTable(conf, metaTableName);
			resTable = new HTable(conf, resTableName);
			resTable.setAutoFlushTo(false);
			meta.setAutoFlushTo(false);
			for(int i = 0; i < rowkeyTemp.length; i ++) {
				Get get = new Get(rowkeyTemp[i].getBytes());
				Result result = meta.get(get);
				byte[] md5Byte = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
				if(md5Byte == null || md5Byte.length == 0) {
					continue;
				}		
				byte[] sizeMeta = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
				Result resultRes = resTable.get(new Get(md5Byte));
				byte[] sizeRes = resultRes.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.SIZE.byteVal);
				if(null == sizeRes) {
					System.out.println(new String(result.getRow()) + " size is null" );
					if(flag) {
						Put put = new Put(md5Byte);
						put.add(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.SIZE.byteVal, sizeMeta);
						resTable.put(put);
					}
				}else if(sizeRes.equals(sizeMeta)) {
					System.out.println(new String(result.getRow()) + " size is error" );
				}
			}
			resTable.flushCommits();
		} catch (IOException e) {}
		finally{		
			try {
				if(meta != null) {
					meta.close();
				}
				if(resTable != null) {
					resTable.close();
				}
			} catch (IOException e) {}
		}
	}
	
	public boolean updateResourceSize(String metaTableName, String resTableName, String prefix, boolean flag){
		Configuration conf = HBaseConfiguration.create();
		HTable meta = null;
		HTable resTable = null;
		try {
			meta = new HTable(conf, metaTableName);
			resTable = new HTable(conf, resTableName);
			resTable.setAutoFlushTo(false);
			meta.setAutoFlushTo(false);
			if(!prefix.endsWith("/")) {
				prefix += "/";
			}
			Scan scan = new Scan();
			RowFilter filter = new RowFilter(CompareOp.EQUAL, new BinaryPrefixComparator(prefix.getBytes()));
			scan.setFilter(filter);
			scan.setStartRow(prefix.getBytes());
			prefix += "{";
			scan.setStopRow(prefix.getBytes());
			ResultScanner resultScanner = meta.getScanner(scan);
			int count = 0;
			for(Result result : resultScanner) {
				byte[] md5Byte = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
				if(md5Byte == null || md5Byte.length == 0) {
					continue;
				}
				
				byte[] sizeMeta = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
				Result resultRes = resTable.get(new Get(md5Byte));
				byte[] sizeRes = resultRes.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.SIZE.byteVal);
				if(null == sizeRes) {
					count ++;
					System.out.println(new String(result.getRow()) + " size is null" );
					if(flag) {
						Put put = new Put(md5Byte);
						put.add(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.SIZE.byteVal, sizeMeta);
						resTable.put(put);
					}
				}else if(sizeRes.equals(sizeMeta)) {
					count ++;
					System.out.println(new String(result.getRow()) + " size is error" );
				}

			}
			System.out.println("count= " + count);
			resTable.flushCommits();
			return true;
		} catch (IOException e) {
			return false;
		}finally{
			try {
				if(meta != null) {
					meta.close();
				}
				if(resTable != null) {
					resTable.close();
				}
			} catch (IOException e) {}
		}
	}
	
	public static void main(String[] args){
		if(args.length < 3) {
			System.out.println("usage <metadata tablename> <restable tablename> <gtdata path> <put or search>");
		}
		UpdateHbaseWidthResourceSize test = new UpdateHbaseWidthResourceSize();
		boolean flag = false;
		if(args[2].equals("true")) {
			flag = true;
			System.out.println("flag is true");
		}
//		test.updateResourceSize(args[0], args[1], args[2], flag);
		test.updateSizebyRowkey(args[0], args[1], null, flag);
//		System.out.println(new Date(Long.parseLong("1434128622833")));
	}
}
