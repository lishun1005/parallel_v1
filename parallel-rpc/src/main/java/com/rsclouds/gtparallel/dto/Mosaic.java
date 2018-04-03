package com.rsclouds.gtparallel.dto;

public class Mosaic {
	public String algorithmType;//算法类型 PL(pl镶嵌),GF1-2m,GF1-16m,GF2-0.8m,pl-quality(质量评估)
	
	public String jobid;
	
	public String inPath;//输入路径
	
	public String outPath;//输出路径
	
	public String project;//投影:(4326)WGS84,(3857)MECATOR


	public Integer outBand;//输出波段:3,4
	
	public Integer outImage;//输出影像字节:8,16

	public String getAlgorithmType() {
		return algorithmType;
	}

	public void setAlgorithmType(String algorithmType) {
		this.algorithmType = algorithmType;
	}
	
	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}
	
	

	public String getInPath() {
		return inPath;
	}

	public void setInPath(String inPath) {
		this.inPath = inPath;
	}

	public String getOutPath() {
		return outPath;
	}

	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public Integer getOutBand() {
		return outBand;
	}

	public void setOutBand(Integer outBand) {
		this.outBand = outBand;
	}



	public Integer getOutImage() {
		return outImage;
	}

	public void setOutImage(Integer outImage) {
		this.outImage = outImage;
	}
	@Override
	public String toString() {
		return "Mosaci [algorithmType=" + algorithmType + ", jobid=" + jobid
				+ ", inPath=" + inPath + ", outPath=" + outPath + ", project="
				+ project + ", outBand=" + outBand + ", outImage=" + outImage
				+ "]";
	}
	
}
