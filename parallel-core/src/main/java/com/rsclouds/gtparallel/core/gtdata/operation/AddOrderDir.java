package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class AddOrderDir {
	public static void main(String[] args) {
		args = new String[2];
		args[0] = "auth";
		args[1] = "meta";
		byte[] authFamily = "atts".getBytes();
		byte[] autfAttsPrefix = "prefix".getBytes();
		byte[] dirSizeValue = "-1".getBytes();
		byte[] dirUrlValue = "".getBytes();
		byte[] dirDfsValue = "0".getBytes();
		byte[] dirTimeValue = Bytes.toBytes(System.currentTimeMillis()+"");
		byte[] dirPermissionValue = "1100000000110".getBytes();
		byte[] dirCapacatyValue = "00".getBytes();
		
		
		String authTableNameStr = args[0];
		String metaTableNameStr = args[1];
		String dirName = "付费订单数据";
		if(args.length == 3)
			dirName = args[2];
		String urlDirName = GtDataUtils.format2GtPath(dirName).replace("//", "/");
		Configuration conf = HBaseConfiguration.create();
		try {
			HTable authTable = new HTable(conf, authTableNameStr);
			HTable metaTable = new HTable(conf, metaTableNameStr);
			Scan scan = new Scan();
			ResultScanner scanner = authTable.getScanner(scan);
			for(Result result : scanner) {
				byte[] prefixByte = result.getValue(authFamily, autfAttsPrefix);
				if(prefixByte == null) {
					System.out.println(new String(result.getRow()));
					break;
				}
				String prefixStr = new String(prefixByte);

				String rowkey = prefixStr + urlDirName;
				Put put = new Put(rowkey.getBytes());
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal, dirSizeValue);
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal, dirTimeValue);
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal, dirDfsValue);
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, dirUrlValue);
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal, dirCapacatyValue);
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal, dirPermissionValue);
				metaTable.put(put);	
			}
			if(authTable != null)
				authTable.close();
			if(metaTable != null) {
				metaTable.flushCommits();
				metaTable.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
