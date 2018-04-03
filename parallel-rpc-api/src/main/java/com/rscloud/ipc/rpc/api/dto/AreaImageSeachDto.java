package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * 
* @ClassName: AreaImageDto  
* @Description: 用于影像检索
* @author lishun 
* @date 2017年7月4日 上午9:38:37  
*
 */
public class AreaImageSeachDto implements Serializable{
	private String areaCode;
	private String imageSatelliteType;
	private String sensorId;
	private Double imageStartResolution;
	private Date startTime;
	private Date endTime;
	private Integer startcloudsRange;
	private Integer endcloudsRange;
	private String gemo;
	public String getAreaCode() {
		return areaCode;
	}
	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}
	public String getImageSatelliteType() {
		return imageSatelliteType;
	}
	public void setImageSatelliteType(String imageSatelliteType) {
		this.imageSatelliteType = imageSatelliteType;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public Integer getStartcloudsRange() {
		return startcloudsRange;
	}
	public void setStartcloudsRange(Integer startcloudsRange) {
		this.startcloudsRange = startcloudsRange;
	}
	public Integer getEndcloudsRange() {
		return endcloudsRange;
	}
	public void setEndcloudsRange(Integer endcloudsRange) {
		this.endcloudsRange = endcloudsRange;
	}
	public String getGemo() {
		return gemo;
	}
	public void setGemo(String gemo) {
		this.gemo = gemo;
	}
	public String getSensorId() {
		return sensorId;
	}
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}
	public Double getImageStartResolution() {
		return imageStartResolution;
	}
	public void setImageStartResolution(Double imageStartResolution) {
		this.imageStartResolution = imageStartResolution;
	}
	
}
