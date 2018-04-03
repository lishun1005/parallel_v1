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
public class CutUptDto {
	@NotEmpty(message = "job_name is not empty")
	private String jobName;
	@NotEmpty(message = "in_path is not empty")
	private String inPath;
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

	@DecimalMin(value = "0",message = "优先级大于0")
	private Integer priority = 1;

	private String sign;


	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getInPath() {
		return inPath;
	}

	public void setInPath(String inPath) {
		this.inPath = inPath;
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

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}
}
