package com.rsclouds.gtparallel.core.hbase;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.RowFilter;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

public class UpdateHBaseWidthPermission {
	private byte[] capacity = "capacity".getBytes();
	private byte[] dirCapacity = "00".getBytes();
	private byte[] fileCapacity = "10".getBytes();
	private byte[] permisson = "permisson".getBytes();
	private byte[] filePermisson = "0110000000110".getBytes();
	private byte[] dirPermisson = "1110000000110".getBytes();
	private String keyWorkFilter = TransCoding.UrlEncode("付费订单数据", "utf-8");
	
	public boolean updateHbasePermission(String metaTable, String prefix, String operate){
		Configuration conf = HBaseConfiguration.create();
		HTable meta = null;
		try {
			meta = new HTable(conf, metaTable);
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
			ResultScanner result = meta.getScanner(scan);
			int count = 0;
			
			if (operate.equals("search"))
			{
				for(Result res : result) {
//					count ++;
//					System.out.println(new String(res.getRow()));
//					byte[] permissonByte = res.getValue(GtDataConfig.META.FAMILY.byteVal, permisson);
//					if(permissonByte == null) {
//						System.out.println("permisson is null");
//					}else {
//						System.out.println("permisson= " + new String(permissonByte));
//					}
					byte[] rowkey = res.getRow();
					String rowkeyStr = new String(rowkey);
					if(rowkeyStr.contains(keyWorkFilter)) {
						System.out.println("filter special path");
						continue;
					}
					String permission = new String(res.getValue(GtDataConfig.META.FAMILY.byteVal, permisson));
					if (permission.equals("0000000000110")) {
						count ++;
						System.out.println(new String(rowkey) + " permisson= " + permission);
					}else {
						System.out.println("permisson is right");
					}
				}
			}else if (operate.equals("put")){
				for(Result res : result) {
					byte[] rowkey = res.getRow();
					String rowkeyStr = new String(rowkey);
					if(rowkeyStr.contains(keyWorkFilter))
						continue;
					String permission = new String(res.getValue(GtDataConfig.META.FAMILY.byteVal, permisson));
					if (permission.equals("0000000000110")) {
						count ++;
						System.out.println(rowkeyStr + " permisson= " + permission);
						Put put = new Put(rowkey);
						put.add(GtDataConfig.META.FAMILY.byteVal, permisson, filePermisson);
						meta.put(put);
					}
				}
			}else if(operate.equals("remove")) {
				for(Result res : result) {
					count ++;
					if(res.getValue(GtDataConfig.META.FAMILY.byteVal, capacity) != null) {
						byte[] rowkey = res.getRow();
						Delete delete = new Delete(rowkey);
						delete.deleteColumn(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal);
						delete.deleteColumn(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal);
						meta.delete(delete);
					}
				}
			}
			
			System.out.println("count= " + count);
			meta.flushCommits();
			return true;
		} catch (IOException e) {
			return false;
		}finally{
			if(meta != null) {
				try {
					meta.close();
				} catch (IOException e) {}
			}
		}
	}
	
	public static void main(String[] args){
		if(args.length < 2) {
			System.out.println("usage <metadata tablename> <gtdata path> <put or search or remove>");
		}
		UpdateHBaseWidthPermission test = new UpdateHBaseWidthPermission();
//		if(args[2].equals("put")) {
//			System.out.println("opetate is put");
//		}else if(args[2].equals("search")) {
//			System.out.println("opetate is search");
//		}else if(args[2].equals("remove")) {
//			System.out.println("opetate is remove");
//		}else {
//			System.out.println("usage <metadata tablename> <gtdata path> <put or search or remove>");
//			return ;
//		}
		test.updateHbasePermission(args[0], args[1], args[2]);
//		test.updateHbasePermission("meta", "/gdal/fusion/guangzhou", "search");
	}
}
