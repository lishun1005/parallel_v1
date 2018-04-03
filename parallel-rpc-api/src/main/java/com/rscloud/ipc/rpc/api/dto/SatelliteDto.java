package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Description:卫星申请信息记录表
 */
public class SatelliteDto implements Serializable{
	
	private String id;
	private String satelliteId;		//资源中心卫星命名规范
	private String name;
	private String productLevel;
	private String sensorId;
	private BigDecimal resolution;
	private String cloudage;
	private String areaRange;
	private Integer status;
	private String satellite;  //集市卫星命名规范
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSatelliteId() {
		return satelliteId;
	}
	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProductLevel() {
		return productLevel;
	}
	public void setProductLevel(String productLevel) {
		this.productLevel = productLevel;
	}
	public String getSensorId() {
		return sensorId;
	}
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}
	public BigDecimal getResolution() {
		return resolution;
	}
	public void setResolution(BigDecimal resolution) {
		this.resolution = resolution;
	}
	public String getCloudage() {
		return cloudage;
	}
	public void setCloudage(String cloudage) {
		this.cloudage = cloudage;
	}
	public String getAreaRange() {
		return areaRange;
	}
	public void setAreaRange(String areaRange) {
		this.areaRange = areaRange;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getSatellite() {
		return satellite;
	}
	public void setSatellite(String satellite) {
		this.satellite = satellite;
	}
	
}
