package com.rscloud.ipc.dto;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotNull;

/**
 * @author lishun
 * @Description: TODO
 * @date 2017/12/28
 */
public class CutRestartAddDto {
	@NotEmpty(message = "id is not empty")
	private String id;
	@NotEmpty(message = "map_name is not empty")
	private String mapName;
	@NotEmpty(message = "out_path is not empty")
	private String outPath;
	@NotNull(message = "water_mark is not empty")
	private Short waterMark;

	@NotNull(message = "max_layers is not empty")
	@DecimalMax(value = "20",message = "最大层级必须小于20")
	private Integer maxLayers;

	private Integer isPublish = 1;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public String getOutPath() {
		return outPath;
	}

	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}

	public Short getWaterMark() {
		return waterMark;
	}

	public void setWaterMark(Short waterMark) {
		this.waterMark = waterMark;
	}
	public Integer getMaxLayers() {
		return maxLayers;
	}

	public void setMaxLayers(Integer maxLayers) {
		this.maxLayers = maxLayers;
	}

	public Integer getIsPublish() {
		return isPublish;
	}

	public void setIsPublish(Integer isPublish) {
		this.isPublish = isPublish;
	}

}
