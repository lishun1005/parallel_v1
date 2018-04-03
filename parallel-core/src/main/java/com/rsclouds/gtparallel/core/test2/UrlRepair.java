package com.rsclouds.gtparallel.core.test2;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class UrlRepair {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		HTable table = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		Scan scan = new Scan();
		scan.setMaxVersions();
		ResultScanner rsscan = table.getScanner(scan);
		System.out.println("repair start!");
		long count = 0;
		for(Result rs : rsscan){
			count++;
			byte[] size = rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
			byte[] md5 = rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);		
			if(size == null || Bytes.toString(size).isEmpty()){
				System.out.println("size error row :" + Bytes.toString(rs.getRow()));
				System.out.println("===========================================");
				continue;
			}
			if(!Bytes.equals(size, GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)){
				byte[] url2 = rs.getValue(GtDataConfig.META.FAMILY.byteVal,Bytes.toBytes("URL"));
				if(url2 != null){
					System.out.println("URL is "+ Bytes.toString(url2));
					if(md5 == null || Bytes.toString(md5).isEmpty()){
						HbaseBase.writeRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
								rs.getRow(), GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, url2);
						System.out.println("write url" +Bytes.toString(url2) );
					}
					Delete del = new Delete(rs.getRow());
					del.deleteColumn(GtDataConfig.META.FAMILY.byteVal, Bytes.toBytes("URL"));
					table.delete(del);
					System.out.println("delete URL:"+Bytes.toString(url2));
					System.out.println("===========================================");
					continue;
				}
				if(md5 == null || Bytes.toString(md5).isEmpty()){
					System.out.println("url error row :" + Bytes.toString(rs.getRow()));
					System.out.println("===========================================");
				}			
			}
		}
		System.out.println("repair over!count="+count);
		rsscan.close();
	}

}
