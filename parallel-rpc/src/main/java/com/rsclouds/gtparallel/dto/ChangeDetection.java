package com.rsclouds.gtparallel.dto;

/**
 * @author lishun
 * @Description: TODO
 * @date 2017/11/5
 */
public class ChangeDetection {
	private String jobid;
	/**
	 * 矩形坐标点
	 */
	private Double coordinateX1;
	private Double coordinateY1;
	private Double coordinateX2;
	private Double coordinateY2;
	private String imagePath; //生成图片路径
	private Integer layer;

	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

	public Double getCoordinateX1() {
		return coordinateX1;
	}

	public void setCoordinateX1(Double coordinateX1) {
		this.coordinateX1 = coordinateX1;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public Double getCoordinateY1() {
		return coordinateY1;
	}

	public void setCoordinateY1(Double coordinateY1) {
		this.coordinateY1 = coordinateY1;
	}

	public Double getCoordinateX2() {
		return coordinateX2;
	}

	public void setCoordinateX2(Double coordinateX2) {
		this.coordinateX2 = coordinateX2;
	}

	public Double getCoordinateY2() {
		return coordinateY2;
	}

	public void setCoordinateY2(Double coordinateY2) {
		this.coordinateY2 = coordinateY2;
	}

	public Integer getLayer() {
		return layer;
	}

	public void setLayer(Integer layer) {
		this.layer = layer;
	}

	@Override
	public String toString() {
		return "ChangeDetection{" +
				"jobid='" + jobid + '\'' +
				", coordinateX1=" + coordinateX1 +
				", coordinateY1=" + coordinateY1 +
				", coordinateX2=" + coordinateX2 +
				", coordinateY2=" + coordinateY2 +
				", imagePath='" + imagePath + '\'' +
				", layer=" + layer +
				'}';
	}
}
