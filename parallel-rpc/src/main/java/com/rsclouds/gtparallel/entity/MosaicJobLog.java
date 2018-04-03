package com.rsclouds.gtparallel.entity;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Table(name = "rscipc_mosaic_job")
public class MosaicJobLog  implements Serializable {
	private String id;
	
	private String algorithmType;//算法类型 {0:PL},{1:GF1-2m},{2:GF1-16m},{3:GF2-0.8m}
	
	private String ownerUserId;
	
	private String inPath;
	
	private String outPath;//输出路径
	
	private String status;

	private String jobid;

	private String operationUserId;

	private Date startTime;

	private Date acceptTime;

	private Date endTime;

	private String project;//投影:WGS84,MECATOR

	private Integer outBand;//输出波段:3,4
	
	private Integer outImage;//输出影像字节:8,16
	
	private Integer progress;

	private String log;
	
	private String mosaicId;
	
	public String jobName;
	
	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	@Id
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
	
	public String getMosaicId() {
		return mosaicId;
	}

	public void setMosaicId(String mosaicId) {
		this.mosaicId = mosaicId;
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
	
}