package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class DeleteHbase {
	public static void main(String[] args) throws IOException {
//		args = new String[]{"map_meta","/map/auto_proc/img/warter/RealtimeChinaTest20150923/Layers/_alllayers//",
//				"/map/auto_proc/img/warter/RealtimeChinaTest20150923/Layers/_alllayers/{","","search"};
		if(args.length !=5){
			System.out.println("usage: <tablename> <startrow> <stoprow> <keyword> <search or delete>");
			System.exit(1);
		}
		String tableName = args[0];
		String startRow = args[1];
		String stopRow = args[2];
		String op = args[4];
		boolean isDelete = false;
		if(op.equalsIgnoreCase("delete")){
			isDelete = true;
		}
		List<String> dels = new ArrayList<String>();
		for(Result rs : HbaseBase.Scan(tableName, 0, startRow, stopRow, args[3], null)){
			System.out.println("==================");		
			if(isDelete){
				dels.add(Bytes.toString(rs.getRow()));
				HbaseBase.deleteRow(tableName, rs.getRow());
			}else{
				System.out.println(Bytes.toString(rs.getRow()));
			}
//			System.out.println(Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("size"))));
//			System.out.println(Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"))));
//			System.out.println(Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("dfs"))));
//			System.out.println(Bytes.toString(rs.getValue(Bytes.toBytes("atts"), Bytes.toBytes("time"))));
			System.out.println("==================");
		}
		//if(isDelete)
			//HbaseBase.deleteRow(tableName, dels.toArray(new String[dels.size()]));
		System.out.println("=============repair over==================");
	}
}
