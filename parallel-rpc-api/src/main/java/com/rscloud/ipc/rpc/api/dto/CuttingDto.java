package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;

public class CuttingDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8704687550773252048L;
	private String jobid;
	private String mapName;// 图层名
	private String hdfsInPath;
	private String inPath;
	private String outPath;
	private Integer maxLayers;
	private Integer minLayers=0;
	private Double maxResolution = null;
	private Double minResolution = null;
	private String waterMark;// 水印方案 【-1:不使用水印 ，0,1,2：水印方案0,1,2】
	private Float zeroPercentage = 0.01f;
	private Long realTime;
	private Boolean bSaveStorage = true;
	private Boolean bRabbitMQ = null;
	
	private Integer nodataInteger;
	private Boolean isCover; //是否覆盖
	
	private Integer cutType = 0;//切片类型 0:新增图层 1:更新图层
	private Integer isPublish;//是否发布到geowebcache 0:否,1:是 
	
	private Integer bxmlUpdate;//是否更新conf.xml文件   0:否,1:是 

	public Boolean getbRabbitMQ() {
		return bRabbitMQ;
	}

	public void setbRabbitMQ(Boolean bRabbitMQ) {
		this.bRabbitMQ = bRabbitMQ;
	}

	public Boolean getbSaveStorage() {
		return bSaveStorage;
	}

	public void setbSaveStorage(Boolean bSaveStorage) {
		this.bSaveStorage = bSaveStorage;
	}

	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

	public String getInPath() {
		return inPath;
	}

	public void setInPath(String inPath) {
		this.inPath = inPath;
	}

	public String getOutPath() {
		return outPath;
	}

	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}

	public Integer getMaxLayers() {
		return maxLayers;
	}

	public void setMaxLayers(Integer maxLayers) {
		this.maxLayers = maxLayers;
	}

	public Double getMaxResolution() {
		return maxResolution;
	}

	public void setMaxResolution(Double maxResolution) {
		this.maxResolution = maxResolution;
	}

	public Double getMinResolution() {
		return minResolution;
	}

	public void setMinResolution(Double minResolution) {
		this.minResolution = minResolution;
	}

	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public String getWaterMark() {
		return waterMark;
	}

	public void setWaterMark(String waterMark) {
		this.waterMark = waterMark;
	}

	public String getHdfsInPath() {
		return hdfsInPath;
	}

	public void setHdfsInPath(String hdfsInPath) {
		this.hdfsInPath = hdfsInPath;
	}

	public Integer getMinLayers() {
		return minLayers;
	}

	public void setMinLayers(Integer minLayers) {
		this.minLayers = minLayers;
	}

	public Float getZeroPercentage() {
		return zeroPercentage;
	}

	public void setZeroPercentage(Float zeroPercentage) {
		this.zeroPercentage = zeroPercentage;
	}


	public Long getRealTime() {
		return realTime;
	}

	public void setRealTime(Long realTime) {
		this.realTime = realTime;
	}

	public Boolean getIsCover() {
		return isCover;
	}

	public void setIsCover(Boolean isCover) {
		this.isCover = isCover;
	}

	public Integer getNodataInteger() {
		return nodataInteger;
	}

	public void setNodataInteger(Integer nodataInteger) {
		this.nodataInteger = nodataInteger;
	}

	public Integer getCutType() {
		return cutType;
	}

	public void setCutType(Integer cutType) {
		this.cutType = cutType;
	}

	public Integer getIsPublish() {
		return isPublish;
	}

	public void setIsPublish(Integer isPublish) {
		this.isPublish = isPublish;
	}

	public Integer getBxmlUpdate() {
		return bxmlUpdate;
	}

	public void setBxmlUpdate(Integer bxmlUpdate) {
		this.bxmlUpdate = bxmlUpdate;
	}

	@Override
	public String toString() {
		return "CuttingDto{" +
				"jobid='" + jobid + '\'' +
				", mapName='" + mapName + '\'' +
				", hdfsInPath='" + hdfsInPath + '\'' +
				", inPath='" + inPath + '\'' +
				", outPath='" + outPath + '\'' +
				", maxLayers=" + maxLayers +
				", minLayers=" + minLayers +
				", maxResolution=" + maxResolution +
				", minResolution=" + minResolution +
				", waterMark='" + waterMark + '\'' +
				", zeroPercentage=" + zeroPercentage +
				", realTime=" + realTime +
				", bSaveStorage=" + bSaveStorage +
				", bRabbitMQ=" + bRabbitMQ +
				", nodataInteger=" + nodataInteger +
				", isCover=" + isCover +
				", cutType=" + cutType +
				", isPublish=" + isPublish +
				", bxmlUpdate=" + bxmlUpdate +
				'}';
	}
}
