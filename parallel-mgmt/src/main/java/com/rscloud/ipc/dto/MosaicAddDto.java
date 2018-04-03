package com.rscloud.ipc.dto;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/3
 */
public class MosaicAddDto extends MosaicModisAddDto {



	@NotEmpty(message = "project is not empty")
	@Pattern(regexp = "^3857|^4326",message = "out_band must 3857 or 4326")
	private String project;//投影:WGS84,MECATOR

	@NotNull(message = "out_band is not empty")
	@Pattern(regexp = "^3|^4",message = "out_band must 3 or 4")
	private String outBand;//输出波段:3,4

	@NotNull(message = "out_image is not empty")
	@Pattern(regexp = "^8|^16",message = "out_image must 8 or 16")
	private String outImage;//输出影像字节:8,16

	private String sign;

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getOutBand() {
		return outBand;
	}

	public void setOutBand(String outBand) {
		this.outBand = outBand;
	}

	public String getOutImage() {
		return outImage;
	}

	public void setOutImage(String outImage) {
		this.outImage = outImage;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}
}
