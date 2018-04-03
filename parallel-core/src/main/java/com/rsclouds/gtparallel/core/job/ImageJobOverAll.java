package com.rsclouds.gtparallel.core.job;

import java.io.IOException;

public class ImageJobOverAll {
	/*private ImageJobServiceImpl imageJobServiceImpl;
	private JobHbase managerJobServiceImpl;
	
	public static int ALL_JOB = 0;
	public static int PROD_LING_JOBTYPE = 1;
	public static int SEGMENT_JOBTYPE = 2;
	
	public ImageJobOverAll() {
		imageJobServiceImpl = new ImageJobServiceImpl();
		managerJobServiceImpl = new JobHbase();
	}
	
	
	*//**
	 * 保存父亲jobID 和 子任务生产线、切片等的关系
	 * @param jobid，hbase key值
	 * @param atts 存放hbase的列值名和对应该列的value，map的key值为列名，value为列的值
	 * @throws IOException 
	 *//*
	public void creteOverAllJob(String jobid, Map<String, String>atts) throws IOException {
		HbaseBase.writeRows(CoreConfig.IMAGEJOB_OVERALL_TABLENAME, jobid, CoreConfig.IMAGEJOB_OVERALL.FAMILY.strVal, atts);
	}
	
	public Map<String, Object> getOverAllJob(String jobid) throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		Result rs = HbaseBase.selectRow(CoreConfig.IMAGEJOB_OVERALL_TABLENAME, jobid);
		if(rs.isEmpty()){
			return null;
		}else{
			for(Entry<byte[], byte[]> entry : rs.getFamilyMap(Bytes.toBytes(CoreConfig.IMAGEJOB_OVERALL.FAMILY.strVal)).entrySet()){
				map.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
			}
		}
		return map;
	}
	
	
	public void createProdLineJob(String jobid,String type,String inpath,String outpath,String aoipath,Integer extend) throws IOException{
		imageJobServiceImpl.createJob(jobid, type, inpath, outpath, aoipath, extend);
	}
	
	@SuppressWarnings("static-access")
	public boolean createSegmentJob(Job job)throws IOException{
		return managerJobServiceImpl.createJob(job);	
	}
	
	public boolean createSegmentJob(String jobid, String mapLayerName, String inputPath)throws IOException {
		return createSegmentJob(jobid, mapLayerName, inputPath, "CUTTING_DEFAULT");
	}
	*//**
	 * 
	 * @param jobid
	 * @param mapLayerName
	 * @param inputPath
	 * @param jobType
	 * @return
	 * @throws IOException
	 *//*
	@SuppressWarnings("static-access")
	public boolean createSegmentJob(String jobid, String mapLayerName, String inputPath, String jobType) throws IOException {
		Job job = new Job();
		job.setRowKey(jobid);
		job.setMapName(mapLayerName);
		job.setType(jobType);
		job.setInPath(inputPath);
		job.setStartTime(""+System.currentTimeMillis());
		job.setState(CoreConfig.JOB_STATE.RUNNING.toString());
		return managerJobServiceImpl.createJob(job);
	}
	
	*//*
	 * 获取生产线进度和状态
	 *//*
	public Map<String, Object> getProdLineJob(String jobid) throws IOException{
		return imageJobServiceImpl.getJob(jobid);
	}
	
	public void completeProdLineJob(String jobid) throws IOException{
		HbaseBase.writeRow(CoreConfig.IMAGE_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.COMPLETE_TIME.strVal, System.currentTimeMillis()+"");
	}
	
	*//*
	 * 获取切片进度和状态
	 *//*
	@SuppressWarnings("static-access")
	public Job getSegementJob(String jobid) throws IOException{
		return managerJobServiceImpl.getJob(jobid);
	}
	
	*//**
	 * 设置切片任务状态
	 * @param jobid
	 * @param state
	 * @return
	 *//*
	@SuppressWarnings("static-access")
	public boolean setSegementJobState(String jobid, CoreConfig.JOB_STATE state) {
		return managerJobServiceImpl.setJobState(jobid, state,0);
	}
	
	*//**
	 * 设置切片任务状态
	 * @param jobid
	 * @param state
	 * @param endTime
	 * @return
	 *//*
	@SuppressWarnings("static-access")
	public boolean setSegementJobState(String jobid, CoreConfig.JOB_STATE state,long endTime) {
		return managerJobServiceImpl.setJobState(jobid, state,0);
	}
	
	
	
	*//**
	 * 设置切片日志信息
	 * @param jobid
	 * @param log
	 * @return
	 *//*
	@SuppressWarnings("static-access")
	public boolean setSegementJobLog(String jobid, String log) {
		return managerJobServiceImpl.setJobLog(jobid, log);
	}
	
	public void setProdLineJobLog(String jobid, String log) throws IOException {
		imageJobServiceImpl.setJobLog(jobid, log);
	}


	public Map<String, Object> getProgress(String jobid, int jobType) throws IOException {
		Map<String, Object> result = new HashMap<String,Object>();
		Map<String, Object> jobAll = getOverAllJob(jobid);
		if (jobAll != null) {
			if (jobType == 0 || jobType == 1) {
				Map<String, Object> prodLineJob = new HashMap<String,Object>();
				prodLineJob = getProdLineJob((String)jobAll.get(CoreConfig.IMAGEJOB_OVERALL.PROD_LING.strVal));
				if (prodLineJob != null)
					result.put("prod_line", prodLineJob);
			}
			if (jobType == 0 || jobType == 2) {
				Map<String, String> cuttingLineJob = new HashMap<String,String>();
				Job job = getSegementJob((String)jobAll.get(CoreConfig.IMAGEJOB_OVERALL.SEGEMENT.strVal));
				if (job != null) {
					cuttingLineJob = job.toMap();
					result.put("segment", cuttingLineJob);
				}
			}
		}else {
			return null;
		}
		return result;
	}*/
	
	
}
