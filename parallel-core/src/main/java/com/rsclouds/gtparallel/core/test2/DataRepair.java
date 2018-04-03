package com.rsclouds.gtparallel.core.test2;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class DataRepair {
	public static void main(String[] args) throws IOException {
//		args = new String[]{""};
		System.out.println(HbaseBase.listTable());
		System.out.println("搜索rowkey包含指定关键字的记录:");
		//for(Result rs : HbaseBase.selectByRegions("job", "69d85588-49df-4a7a-97ee-7a6271d20cf!","69d85588-49df-4a7a-97ee-7a6271d20cfz")){
		for(Result rs : HbaseBase.Scan("meta",0, args[0]+"/",args[0]+"/{", null, null)){
			System.out.println("==================");
//			System.out.println(HbaseUtils.result2Job(rs).toString());
			System.out.println(Bytes.toString(rs.getRow()));
			String url = Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url")));
			if(url == null || url.isEmpty()){
				System.out.println("size:"  + Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("size"))));
				System.out.println("url:"  +url);
				System.out.println("dfs:"  +Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("dfs"))));
				System.out.println("time:"  +Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("time"))));
			}			
			System.out.println("==================");
//			HbaseBase.deleteRow("job", Bytes.toString(rs.getRow()));
			//del redis
//			RedisUtils.redisDel(GtDataConfig.REDIS_HOST, Bytes.toString(rs.getRow()));
			
		}
	}
}
