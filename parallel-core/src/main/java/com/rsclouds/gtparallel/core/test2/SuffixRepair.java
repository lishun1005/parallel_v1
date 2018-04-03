package com.rsclouds.gtparallel.core.test2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class SuffixRepair {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
//		args = new String[]{"/download/test2",".jar",".test"};
		if(args.length < 3){
			System.out.println("Usage : <dir-path> <old-suffix> <new-suffix>");
		}else{
			List<String> delRows = new ArrayList<String>();
			GtPath path = new GtPath(args[0]);
			String startRow = path.getGtPath().replace("//", "/")+"/";
			for(Result rs : HbaseBase.Scan(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0, startRow, startRow+"{", args[1], null)){
				String row = Bytes.toString(rs.getRow());
				if(!row.endsWith(args[1]))
					continue;
				String newRow = GtDataUtils.replaceLast(row, args[1], args[2]);
				NavigableMap<byte[], byte[]> map = rs.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
				HbaseBase.writeRows(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), Bytes.toBytes(newRow), GtDataConfig.META.FAMILY.byteVal, map);
				delRows.add(row);
				System.out.println(row +" to " + newRow);
			}
			HbaseBase.deleteRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), delRows.toArray(new String[delRows.size()]));		
		}	
	}

}
