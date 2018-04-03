package com.rscloud.ipc.dto;


import java.io.Serializable;

/**
 * 区域影像查询dto
 * @author mok
 *
 */
public class AreaImageTotalVo implements Serializable {

	private static final long serialVersionUID = -6231642911224429946L;

	// 分辨率
	String resolution;
	// 光谱
	String spectrum;
	// 开始时间 2017/04/14
	String startDate;
	// 结束时间 2017/07/13
	String endDate;
	// 自定义区域 {"type":"Polygon","coordinates":[[[3,4],[3,2],[1,2],[1,4],[3,4]]]}
	String geom;
	// 行政区域 441900
	String areaNo;
	// 分辨率/卫星
	String resolutionSa;
	// 卫星/传感器 {'GF2':'[PMS]','GF4':'[IRS]','GF1':'[PMS]'}
	String saSensor;
	// 云量 10
	String cloud;
	// 卫星 GF1
	String imageSatelliteType;
	 // 分辨率id
	String resId;
	// 是否免费数据
	Boolean isFree;
	// 分页查询 从第几条开始 0
	Integer startRow;
	// 分页查询 查询多少条 10000
	Integer limit;

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getSpectrum() {
		return spectrum;
	}

	public void setSpectrum(String spectrum) {
		this.spectrum = spectrum;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getGeom() {
		return geom;
	}

	public void setGeom(String geom) {
		this.geom = geom;
	}

	public String getAreaNo() {
		return areaNo;
	}

	public void setAreaNo(String areaNo) {
		this.areaNo = areaNo;
	}

	public String getResolutionSa() {
		return resolutionSa;
	}

	public void setResolutionSa(String resolutionSa) {
		this.resolutionSa = resolutionSa;
	}

	public String getSaSensor() {
		return saSensor;
	}

	public void setSaSensor(String saSensor) {
		this.saSensor = saSensor;
	}

	public String getCloud() {
		return cloud;
	}

	public void setCloud(String cloud) {
		this.cloud = cloud;
	}

	public String getImageSatelliteType() {
		return imageSatelliteType;
	}

	public void setImageSatelliteType(String imageSatelliteType) {
		this.imageSatelliteType = imageSatelliteType;
	}

	public String getResId() {
		return resId;
	}

	public void setResId(String resId) {
		this.resId = resId;
	}

	public Boolean getIsFree() {
		return isFree;
	}

	public void setIsFree(Boolean isFree) {
		this.isFree = isFree;
	}

	public Integer getStartRow() {
		return startRow;
	}

	public void setStartRow(Integer startRow) {
		this.startRow = startRow;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

}
