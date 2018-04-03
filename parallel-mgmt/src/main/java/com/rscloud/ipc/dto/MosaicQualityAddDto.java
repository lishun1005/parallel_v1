package com.rscloud.ipc.dto;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;

/**
 * @author lishun
 * @Description: pl质量评估，modis镶嵌参数验证
 * @date 2018/1/3
 */
public class MosaicQualityAddDto {
	@NotEmpty(message = "job_name is not empty")
	private String jobName;

	@Pattern(regexp = "^.*?\\.(txt)$",message = "输出文件格式错误(txt)")
	@NotEmpty(message = "out_path is not empty")
	private String outPath;

	@NotEmpty(message = "in_path is not empty")
	private String inPath;

	private String sign;

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

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}
}
