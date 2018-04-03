package com.rsclouds.gtparallel.core.test2;

import java.io.IOException;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class AppDataRepair {
	
	private static byte[] attsByte = Bytes.toBytes("atts");
	private static Configuration conf = HBaseConfiguration.create();

	public static void main(String[] args) throws IOException {

//		args = new String[]{"-///users/rscloudmart/thumbnail"};
//		boolean flag = false;
//		if(args.length > 1 && args[1].equals("repair")){
//			flag = true;
//		}
//		System.out.println("搜索rowkey包含指定关键字的记录:");

//		args = new String[]{"",".*"};
		long startTime = System.currentTimeMillis();
		System.out.println("搜索rowkey包含指定关键字的记录:");

		for(Result rs : HbaseBase.Scan(args[0],0, args[1], null, args[2], null)){
			System.out.println("==================");
			System.out.println(Bytes.toString(rs.getRow()));
////			String url = Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url")));
////			if(url == null || url.isEmpty()){
////				System.out.println("size:"  + Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("size"))));
////				System.out.println("url:"  +url);
////				System.out.println("dfs:"  +Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("dfs"))));
////				System.out.println("time:"  +Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("time"))));
////			}	
//			String row = Bytes.toString(rs.getRow()); 
//			if(row.startsWith("-//") && flag){
//				String rowKey = row.replaceFirst("-//", "");
//				NavigableMap<byte[], byte[]> qv = rs.getFamilyMap(attsByte);
//				HbaseBase.writeRows("meta", Bytes.toBytes(rowKey), attsByte, qv);
//				System.out.println("repair ok: " + rowKey);
//			}

//			System.out.println("==================");			
//		}
//		String startRow = args[0]+"/";
//		String stopRow = args[0]+"{";
		HTable table = new HTable(conf, "map_meta");
		Scan scan = new Scan();
		RowFilter filter = new RowFilter(CompareFilter.CompareOp.EQUAL,new RegexStringComparator(".*f0L/sreyallla_/sreyaL/m2_1fg_4102_3102_nanuh/nanuh/fg/gmi/corp_otua/atad/hss/pam/"));
//		scan.setMaxVersions();
//		if(startRow!=null)
//			scan.setStartRow(Bytes.toBytes(startRow));
//		if(stopRow!=null)
//			scan.setStopRow(Bytes.toBytes(stopRow));
		scan.setFilter(filter);
		long time = System.currentTimeMillis();
		ResultScanner result = table.getScanner(scan);
		for (Result rs1 : result) {
			rs1.getRow();
//			System.out.println(Bytes.toString(rs.getRow()));
//			String row = Bytes.toString(rs.getRow()); 
//			if(row.startsWith("-//") && flag){
//				String rowKey = row.replaceFirst("-//", "");
//				NavigableMap<byte[], byte[]> qv = rs.getFamilyMap(attsByte);
//				HbaseBase.writeRows("meta", Bytes.toBytes(rowKey), attsByte, qv);
//				System.out.println("repair ok: " + rowKey);
//			}

			System.out.println("==================");		

		}
		System.out.println("cost:" + (System.currentTimeMillis() - startTime));
		System.out.println("==================");
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("==================");
	}
}
}
