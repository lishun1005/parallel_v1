package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;
import java.util.Date;

public class MosaicJobDto  implements Serializable {
	private String id;
	
	public String algorithmType;//算法类型 PL(pl镶嵌),GF1-2m,GF1-16m,GF2-0.8m,pl-quality(质量评估)
	
	private String ownerUserId;
	
	private String inPath;

	public String outPath;//输出路径
	
	private String status;

	private String jobid;

	private String operationUserId;

	private Date startTime;

	private Date acceptTime;

	private Date endTime;

	public String project;//投影:WGS84,MECATOR

	public Integer outBand;//输出波段:3,4
	
	public Integer outImage;//输出影像字节:8,16
	
	public Integer progress;

	private String log;
	
	public Integer isRestart = 0;
	
	public String jobName;

	private Integer isDel;

	private String modelId;

	private String gearmanFunc;

	private String gearmanParms; //按','分隔, end结束标志位



	public Integer getIsDel() {
		return isDel;
	}

	public void setIsDel(Integer isDel) {
		this.isDel = isDel;
	}
	
	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	public Integer getIsRestart() {
		return isRestart;
	}

	public void setIsRestart(Integer isRestart) {
		this.isRestart = isRestart;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getInPath() {
		return inPath;
	}

	public void setInPath(String inPath) {
		this.inPath = inPath;
	}


	public String getAlgorithmType() {
		return algorithmType;
	}

	public void setAlgorithmType(String algorithmType) {
		this.algorithmType = algorithmType;
	}

	public String getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}


	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}

	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

	public String getOperationUserId() {
		return operationUserId;
	}

	public void setOperationUserId(String operationUserId) {
		this.operationUserId = operationUserId;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getAcceptTime() {
		return acceptTime;
	}

	public void setAcceptTime(Date acceptTime) {
		this.acceptTime = acceptTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
	public String getOutPath() {
		return outPath;
	}

	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public Integer getOutBand() {
		return outBand;
	}

	public void setOutBand(Integer outBand) {
		this.outBand = outBand;
	}

	public Integer getOutImage() {
		return outImage;
	}

	public void setOutImage(Integer outImage) {
		this.outImage = outImage;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public String getGearmanFunc() {
		return gearmanFunc;
	}

	public void setGearmanFunc(String gearmanFunc) {
		this.gearmanFunc = gearmanFunc;
	}

	public String getGearmanParms() {
		return gearmanParms;
	}

	public void setGearmanParms(String gearmanParms) {
		this.gearmanParms = gearmanParms;
	}

	@Override
	public String toString() {
		return "MosaicJobDto{" +
				"id='" + id + '\'' +
				", algorithmType='" + algorithmType + '\'' +
				", ownerUserId='" + ownerUserId + '\'' +
				", inPath='" + inPath + '\'' +
				", outPath='" + outPath + '\'' +
				", status='" + status + '\'' +
				", jobid='" + jobid + '\'' +
				", operationUserId='" + operationUserId + '\'' +
				", startTime=" + startTime +
				", acceptTime=" + acceptTime +
				", endTime=" + endTime +
				", project='" + project + '\'' +
				", outBand=" + outBand +
				", outImage=" + outImage +
				", progress=" + progress +
				", log='" + log + '\'' +
				", isRestart=" + isRestart +
				", jobName='" + jobName + '\'' +
				", isDel=" + isDel +
				'}';
	}
}