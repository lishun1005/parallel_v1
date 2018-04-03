package com.rscloud.ipc.dto;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;

/**
 * @author lishun
 * @Description: pl质量评估，modis镶嵌参数验证
 * @date 2018/1/3
 */
public class MosaicModisAddDto {
	@NotEmpty(message = "job_name is not empty")
	public String jobName;

	@Pattern(regexp = "^.*?\\.(tif|tiff)$",message = "输出文件格式错误(tif|tiff)")
	@NotEmpty(message = "out_path is not empty")
	public String outPath;

	@NotEmpty(message = "in_path is not empty")
	private String inPath;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getOutPath() {
		return outPath;
	}

	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}

	public String getInPath() {
		return inPath;
	}

	public void setInPath(String inPath) {
		this.inPath = inPath;
	}
}
