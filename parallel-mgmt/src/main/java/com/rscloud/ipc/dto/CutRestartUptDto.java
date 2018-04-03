package com.rscloud.ipc.dto;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

/**
 * @author lishun
 * @Description: TODO
 * @date 2017/12/28
 */
public class CutRestartUptDto {

	@NotEmpty(message = "id is not empty")
	private String id;

	@NotEmpty(message = "map_name is not empty")
	private String mapName;

	@NotNull(message = "is_cover is not empty")
	private Boolean isCover;

	@NotNull(message = "max_layers is not empty")
	@DecimalMax(value = "20",message = "最大层级必须小于20")
	private Integer maxLayers;

	@NotNull(message = "min_layers is not empty")
	@DecimalMin(value = "1",message = "最小层级必须大于1")
	private Integer minLayers;

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

	public Boolean getIsCover() {
		return isCover;
	}

	public void setIsCover(Boolean isCover) {
		this.isCover = isCover;
	}

	public Integer getMinLayers() {
		return minLayers;
	}

	public void setMinLayers(Integer minLayers) {
		this.minLayers = minLayers;
	}

}
