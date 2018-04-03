package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class ResSizeRepairByThread {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		HTable table = new HTable(HbaseBase.getHbaseConf(), GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		Scan scan = new Scan();
		scan.setStartRow(Bytes.toBytes("/users/"));
		scan.setStopRow(Bytes.toBytes("/users/{"));
		ResultScanner rsscan = table.getScanner(scan);
		for(Result rs : rsscan){
			byte[] size = rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
			byte[] md5 = rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);		
			if(size == null || Bytes.toString(size).isEmpty()){
				System.out.println("error size row :" + Bytes.toString(rs.getRow()));
				continue;
			}
			if(!Bytes.equals(size, GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)){
				if(md5 == null || Bytes.toString(md5).isEmpty()){
					System.out.println("error url row :" + Bytes.toString(rs.getRow()));
					continue;
				}
//				System.out.println("rowkey : " + Bytes.toString(rs.getRow()));
//				System.out.println("md5 : " + Bytes.toString(md5));
//				System.out.println("size : " + Bytes.toString(size));
				Result res = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), Bytes.toString(md5));
				if(!res.isEmpty()){	
					byte[] sizeGet = res.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.SIZE.byteVal);
					if(sizeGet == null || sizeGet.length == 0){
						System.out.println("write " +Bytes.toString(md5)+ " : " + Bytes.toString(size) );				
						HbaseBase.writeRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
								Bytes.toString(md5), GtDataConfig.RESOURCE.FAMILY.strVal,
								GtDataConfig.RESOURCE.SIZE.strVal, Bytes.toString(size));
					}				
				}else{
					System.out.println("can not find url row :" + Bytes.toString(rs.getRow()));
					continue;
				}
			}
//			System.out.println();
		}
		System.out.println("repair over!");
		rsscan.close();
	}

}
