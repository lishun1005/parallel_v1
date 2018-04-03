package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;

public class AreaImageDto implements Serializable {
	
	private String id;
	private String areaNo;
	private String imageRowCol;
	private String imageSatelliteType;
	private Double imageStartResolution;
	private String imageSpectrumType;
	private String beginTime;
	private String updateTime;
	private String gemo;
	private String range;
	private Integer num;
	private Integer isCover;
	private String dataId;
	private String imageProductType;
	private String relationNo;
	private String name;
	private Double imageCloudage;
	private String sensorId;
	private String productLevel;
	private String collectStartTime;
	private String filePath;
	private String productId;
	private Boolean iscorrect;
	private String jobid;
	private String areaNoArray;
	private String imgUrl;
	private String imageArea;
	private String recordId;
	private String collectEndTime;
	private Double imageEndResolution;
	private String tip;
	private String srid;
	private String areaDescription;
	private Long imageSize;
	private Integer autoTag;
	/*
	 String geom,
			String productsourceType,String productType,String cloudsrange,String Timerange,String productId,String resolutionrange
	 */
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAreaNo() {
		return areaNo;
	}
	public void setAreaNo(String areaNo) {
		this.areaNo = areaNo;
	}
	public String getImageRowCol() {
		return imageRowCol;
	}
	public void setImageRowCol(String imageRowCol) {
		this.imageRowCol = imageRowCol;
	}
	public String getImageSatelliteType() {
		return imageSatelliteType;
	}
	public void setImageSatelliteType(String imageSatelliteType) {
		this.imageSatelliteType = imageSatelliteType;
	}
	public Double getImageStartResolution() {
		return imageStartResolution;
	}
	public void setImageStartResolution(Double imageStartResolution) {
		this.imageStartResolution = imageStartResolution;
	}
	public String getImageSpectrumType() {
		return imageSpectrumType;
	}
	public void setImageSpectrumType(String imageSpectrumType) {
		this.imageSpectrumType = imageSpectrumType;
	}
	public String getBeginTime() {
		return beginTime;
	}
	public void setBeginTime(String beginTime) {
		this.beginTime = beginTime;
	}
	public String getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	public String getGemo() {
		return gemo;
	}
	public void setGemo(String gemo) {
		this.gemo = gemo;
	}
	public String getRange() {
		return range;
	}
	public void setRange(String range) {
		this.range = range;
	}
	public Integer getNum() {
		return num;
	}
	public void setNum(Integer num) {
		this.num = num;
	}
	public Integer getIsCover() {
		return isCover;
	}
	public void setIsCover(Integer isCover) {
		this.isCover = isCover;
	}
	public String getDataId() {
		return dataId;
	}
	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
	public String getImageProductType() {
		return imageProductType;
	}
	public void setImageProductType(String imageProductType) {
		this.imageProductType = imageProductType;
	}
	public String getRelationNo() {
		return relationNo;
	}
	public void setRelationNo(String relationNo) {
		this.relationNo = relationNo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Double getImageCloudage() {
		return imageCloudage;
	}
	public void setImageCloudage(Double imageCloudage) {
		this.imageCloudage = imageCloudage;
	}
	public String getSensorId() {
		return sensorId;
	}
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}
	public String getProductLevel() {
		return productLevel;
	}
	public void setProductLevel(String productLevel) {
		this.productLevel = productLevel;
	}
	public String getCollectStartTime() {
		return collectStartTime;
	}
	public void setCollectStartTime(String collectStartTime) {
		this.collectStartTime = collectStartTime;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	public Boolean getIscorrect() {
		return iscorrect;
	}
	public void setIscorrect(Boolean iscorrect) {
		this.iscorrect = iscorrect;
	}
	public String getJobid() {
		return jobid;
	}
	public void setJobid(String jobid) {
		this.jobid = jobid;
	}
	public String getAreaNoArray() {
		return areaNoArray;
	}
	public void setAreaNoArray(String areaNoArray) {
		this.areaNoArray = areaNoArray;
	}
	public String getImgUrl() {
		return imgUrl;
	}
	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}
	public String getImageArea() {
		return imageArea;
	}
	public void setImageArea(String imageArea) {
		this.imageArea = imageArea;
	}
	public String getRecordId() {
		return recordId;
	}
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}
	public String getCollectEndTime() {
		return collectEndTime;
	}
	public void setCollectEndTime(String collectEndTime) {
		this.collectEndTime = collectEndTime;
	}
	public Double getImageEndResolution() {
		return imageEndResolution;
	}
	public void setImageEndResolution(Double imageEndResolution) {
		this.imageEndResolution = imageEndResolution;
	}
	public String getTip() {
		return tip;
	}
	public void setTip(String tip) {
		this.tip = tip;
	}
	public String getSrid() {
		return srid;
	}
	public void setSrid(String srid) {
		this.srid = srid;
	}
	public String getAreaDescription() {
		return areaDescription;
	}
	public void setAreaDescription(String areaDescription) {
		this.areaDescription = areaDescription;
	}
	public Long getImageSize() {
		return imageSize;
	}
	public void setImageSize(Long imageSize) {
		this.imageSize = imageSize;
	}
	public Integer getAutoTag() {
		return autoTag;
	}
	public void setAutoTag(Integer autoTag) {
		this.autoTag = autoTag;
	}
	@Override
	public String toString() {
		return "AreaImage [id=" + id + ", areaNo=" + areaNo + ", imageRowCol=" + imageRowCol + ", imageSatelliteType="
				+ imageSatelliteType + ", imageStartResolution=" + imageStartResolution + ", imageSpectrumType="
				+ imageSpectrumType + ", beginTime=" + beginTime + ", updateTime=" + updateTime + ", gemo=" + gemo
				+ ", range=" + range + ", num=" + num + ", isCover=" + isCover + ", dataId=" + dataId
				+ ", imageProductType=" + imageProductType + ", relationNo=" + relationNo + ", name=" + name
				+ ", imageCloudage=" + imageCloudage + ", sensorId=" + sensorId + ", productLevel=" + productLevel
				+ ", collectStartTime=" + collectStartTime + ", filePath=" + filePath + ", productId=" + productId
				+ ", iscorrect=" + iscorrect + ", jobid=" + jobid + ", areaNoArray=" + areaNoArray + ", imgUrl="
				+ imgUrl + ", imageArea=" + imageArea + ", recordId=" + recordId + ", collectEndTime=" + collectEndTime
				+ ", imageEndResolution=" + imageEndResolution + ", tip=" + tip + ", srid=" + srid
				+ ", areaDescription=" + areaDescription + ", imageSize=" + imageSize + ", autoTag=" + autoTag + "]";
	}

}
