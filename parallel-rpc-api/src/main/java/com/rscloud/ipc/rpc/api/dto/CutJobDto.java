package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class CutJobDto  implements Serializable {
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

	public Integer isRestart = 0;//是否重启 0：否 ，1：是   （默认是0）

	public String jobName;

	private Integer isDel;

	private Double progress;

	private Integer priority;

	private List<String> inPathList;

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

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

	public String getId() {
		return id;
	}

	public Integer getIsRestart() {
		return isRestart;
	}

	public void setIsRestart(Integer isRestart) {
		this.isRestart = isRestart;
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

	public Double getProgress() {
		return progress;
	}

	public void setProgress(Double progress) {
		this.progress = progress;
	}

	public List<String> getInPathList() {
		return inPathList;
	}

	public void setInPathList(List<String> inPathList) {
		this.inPathList = inPathList;
	}




}