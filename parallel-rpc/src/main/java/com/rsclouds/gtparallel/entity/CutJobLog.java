package com.rsclouds.gtparallel.entity;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "rscipc_cut_job_log")
public class CutJobLog {
	private String id;

	private String mapId;

	private String inPath;

	private String maxLayers;

	private String status;

	private String jobid;

	private String operationUserId;

	private Date startTime;

	private Date acceptTime;

	private Date endTime;

	private Boolean isCover;

	private String minLayers;

	private String log;

	private Integer cutType;// 切片类型 0：新增切片 1：更新切片

	private String geowebcacheUrl;
	
	private Date realTime;
	
	private String cutId;
	
	public String jobName;
	
	private Integer progress;
	
	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	public String getCutId() {
		return cutId;
	}

	public void setCutId(String cutId) {
		this.cutId = cutId;
	}

	public Date getRealTime() {
		return realTime;
	}

	public void setRealTime(Date realTime) {
		this.realTime = realTime;
	}

	public Boolean getIsCover() {
		return isCover;
	}

	public void setIsCover(Boolean isCover) {
		this.isCover = isCover;
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

	public String getMapId() {
		return mapId;
	}

	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public String getMaxLayers() {
		return maxLayers;
	}

	public void setMaxLayers(String maxLayers) {
		this.maxLayers = maxLayers;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getMinLayers() {
		return minLayers;
	}

	public void setMinLayers(String minLayers) {
		this.minLayers = minLayers;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public Integer getCutType() {
		return cutType;
	}

	public void setCutType(Integer cutType) {
		this.cutType = cutType;
	}

	public String getGeowebcacheUrl() {
		return geowebcacheUrl;
	}

	public void setGeowebcacheUrl(String geowebcacheUrl) {
		this.geowebcacheUrl = geowebcacheUrl;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}
	
}