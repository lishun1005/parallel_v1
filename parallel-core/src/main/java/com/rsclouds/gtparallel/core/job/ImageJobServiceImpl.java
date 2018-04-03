package com.rsclouds.gtparallel.core.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

/**
 * 处理裁剪相关job操作
 * @author wugq
 *
 */
@Service("imageJobServiceImpl")
public class ImageJobServiceImpl {

	public void createJob(String jobid,String type,String inpath,String outpath,String aoipath,Integer extend) throws IOException{
		Map<String,String> map = new HashMap<String,String>();
		map.put(CoreConfig.JOB.TYPE.strVal, type);
		map.put(CoreConfig.JOB.STATUS.strVal, CoreConfig.JOB_STATE.ACCEPTED.toString());
		if(inpath!=null)
		map.put(CoreConfig.JOB.IN_PATH.strVal, inpath);
		map.put(CoreConfig.JOB.PROGRESS.strVal, "0");
		if(outpath!= null)
		map.put(CoreConfig.JOB.OUT_PATH.strVal, outpath);
		if(aoipath != null)
		map.put(CoreConfig.JOB.AOI_PATH.strVal, aoipath);
		if(extend != null)
		map.put(CoreConfig.JOB.EXTEND.strVal, extend+"");
		map.put(CoreConfig.JOB.ACCEPT_TIME.strVal, System.currentTimeMillis()+"");
		HbaseBase.writeRows(CoreConfig.IMAGE_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, map);
	}
	
	public void updateJobCallBackUrl(String jobid,String callBackUrl) throws IOException{ 
		HbaseBase.writeRow(CoreConfig.IMAGE_JOB_TABLE, jobid,CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.CALLBACK_URL.strVal, callBackUrl);
	}
	
	public Map<String, Object> getJob(String jobid) throws IOException{
		Map<String, Object> map = new HashMap<String, Object>();
		Result rs = HbaseBase.selectRow(CoreConfig.IMAGE_JOB_TABLE, jobid);
		if(rs.isEmpty()){
			return null;
		}else{
			for(Entry<byte[], byte[]> entry : rs.getFamilyMap(Bytes.toBytes(CoreConfig.JOB.FAMILY.strVal)).entrySet()){
				map.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
			}
		}
		return map;
	}
	
	public void completeJob(String jobid) throws IOException{
		HbaseBase.writeRow(CoreConfig.IMAGE_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.COMPLETE_TIME.strVal, System.currentTimeMillis()+"");
	}
	public void updateJobState(String jobid,String stete) throws IOException{
		HbaseBase.writeRow(CoreConfig.IMAGE_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.STATUS.strVal, stete+"");
	}

	public void setJobLog(String jobid, String log) throws IOException {
		HbaseBase.writeRow(CoreConfig.IMAGE_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.LOG.strVal, log);
	}

}
