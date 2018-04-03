package com.rsclouds.gtparallel.core.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.core.entity.Job;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

/**
 * gt-parallel整体工具类
 * @author wugq
 *
 */
public class Utils {
	
	public static List<Job> result2Jobs(Result... results){
		List<Job> jobs = new ArrayList<Job>();
		if(results!= null){
			for(Result rs : results){
				Job job = new Job();
				job.setRowKey(Bytes.toString(rs.getRow()));
				job.setPid(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PID.byteVal)));
				job.setType(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.TYPE.byteVal)));
				job.setNode(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.NODE.byteVal)));
				job.setInPath(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.IN_PATH.byteVal)));
				job.setOutPath(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.OUT_PATH.byteVal)));
				job.setProgress(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PROGRESS.byteVal)));
				job.setState(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.STATE.byteVal)));
				job.setJid(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.JID.byteVal)));
				job.setPart(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PART.byteVal)));
				job.setStartTime(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.START_TIME.byteVal)));
				job.setEndTime(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.END_TIME.byteVal)));
				jobs.add(job);	
			}
		}	
		return jobs;
	}
	
	public static List<Job> result2Jobs(List<Result> results){
		List<Job> jobs = new ArrayList<Job>();
		if(results!= null){
			for(Result rs : results){
				Job job = new Job();
				job.setRowKey(Bytes.toString(rs.getRow()));
				job.setPid(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PID.byteVal)));
				job.setType(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.TYPE.byteVal)));
				job.setNode(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.NODE.byteVal)));
				job.setInPath(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.IN_PATH.byteVal)));
				job.setOutPath(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.OUT_PATH.byteVal)));
				job.setProgress(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PROGRESS.byteVal)));
				job.setState(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.STATE.byteVal)));
				job.setJid(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.JID.byteVal)));
				job.setPart(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PART.byteVal)));
				job.setStartTime(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.START_TIME.byteVal)));
				job.setEndTime(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.END_TIME.byteVal)));
				jobs.add(job);	
			}
		}	
		return jobs;
	}
	
	public static Job result2Job(Result rs){
		Job job = new Job();
		if(!rs.isEmpty()){
			job.setRowKey(Bytes.toString(rs.getRow()));
			job.setPid(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PID.byteVal)));
			job.setType(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.TYPE.byteVal)));
			job.setNode(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.NODE.byteVal)));
			job.setInPath(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.IN_PATH.byteVal)));
			job.setOutPath(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.OUT_PATH.byteVal)));
			job.setProgress(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PROGRESS.byteVal)));
			job.setState(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.STATE.byteVal)));
			job.setJid(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.JID.byteVal)));
			job.setPart(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.PART.byteVal)));
			job.setStartTime(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.START_TIME.byteVal)));
			job.setEndTime(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.END_TIME.byteVal)));
			job.setMapName(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.MAP_NAME.byteVal)));
			job.setLog(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.LOG.byteVal)));
			job.setGeoRange(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.GEO_RANGE.byteVal)));
			job.setCurrent(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.CURRNT.byteVal)));
			job.setTotal(Bytes.toString(rs.getValue(CoreConfig.JOB.FAMILY.byteVal, CoreConfig.JOB.TOTAL.byteVal)));
		}else{
			return null;
		}
		return job;
	}
	
	public static String timeStrFillZero(String timeStr){
		if(timeStr.length() < 13){
			String str = "0000000000000";	
			return timeStr + str.substring(0,13-timeStr.length());
		}
		return timeStr;
	}
	
	public static Map<String,Object> result2MetaMap(Result rs){
		if(!rs.isEmpty()){
			Map<String, Object> metaMap = new HashMap<String,Object>();
			NavigableMap<byte[], byte[]> map = rs.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
	    	if(map != null){
	    		for(Entry<byte[], byte[]> entry : map.entrySet()){
	    			if(Arrays.equals(entry.getKey(), GtDataConfig.META.SIZE.byteVal)){
	    				String size = Bytes.toString(entry.getValue());
	    				if(size.length() < 13){
	    					String str = "0000000000000";
	    					size = size + str.substring(0,13-size.length());
	    				}
	    				metaMap.put(Bytes.toString(entry.getKey()), size);
	    			}else{
	    				metaMap.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
	    			}	    			
	    		}
	    	}  
	    	return metaMap;
		}else{
			return null;	
		}			
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		System.out.println(Bytes.toString(null));
//		String size = "123456789";
//		String str = "0000000000000";
//		size = size + str.substring(0,13-size.length());
//		System.out.println(size);
//		Bytes.toString(null);
		System.out.println(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		//HTable resTable = new HTable(HBaseConfiguration.create(), GtDataConfig.TABLE_NAME.MAP_TABLE.getStrVal());
	}

}
