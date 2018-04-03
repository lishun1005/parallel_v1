package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.IOException;
import java.util.Date;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

public class GeowebcacheTool {

	public static String getLayerName(int layers) {
		String layersNameStr = Integer.toHexString(layers);
		if (layersNameStr.length() == 1) {
			layersNameStr = "L0" + layersNameStr;
		} else if(layersNameStr.length() == 2){
			layersNameStr = "L" + layersNameStr;
		}else {
			layersNameStr = null;
		}
		return layersNameStr;
	}
	
	public static boolean isNewestTime(HTable meta, String prefix, long time) throws IOException {
		Scan scan = new Scan();
		while(prefix.endsWith("/"))
			prefix.substring(0, prefix.length()-1);
		String startRowkey = prefix + "//";
		String stopRowkey = prefix + "/{";
		scan.setStartRow(startRowkey.getBytes());
		scan.setStopRow(stopRowkey.getBytes());
		ResultScanner scanner = meta.getScanner(scan);
		if(scanner == null)
			return true;
		Result result = scanner.next();
		if(result == null || result.isEmpty())
			return true;
		else {
			String rowkey = new String(result.getRow());
			System.out.println("isNewestTime rowkey= " + rowkey);
			int indexof = rowkey.indexOf("//");
			String timeStr = rowkey.substring(indexof+2, rowkey.length()-4);
			long timeLong = Long.parseLong(timeStr);
			timeLong = 9999999999999L - timeLong;
			System.out.println("isNewestTime timeLong= " + timeLong + "; time= " + time);
			if (timeLong <= time) {
				return true;
			}else {
				return false;
			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println(new Date(Long.parseLong("1412006400000")));
	}
}
