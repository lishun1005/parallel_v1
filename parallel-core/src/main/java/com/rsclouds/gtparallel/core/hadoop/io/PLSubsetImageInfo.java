package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.Serializable;

/**
 * 原始分幅影像信息
 * @author root
 *
 */
public class PLSubsetImageInfo implements Comparable<PLSubsetImageInfo>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4022135263827932449L;
	//原始分幅影像对应的层级和geowebcache格式行列号
	private int rowOrg;
	private int colOrg;
	private int layerOrg;
	//原始分幅影像需要输出的层级、行列号和偏移量
	private int rowOut;
	private int colOut;
	private int layerOut;
	private int rowRemainder;
	private int colRemainder;
	private String imagePath;
	
	public PLSubsetImageInfo(int rowOrg, int colOrg, int layerOrg) {
		this.rowOrg = rowOrg;
		this.colOrg = colOrg;
		this.layerOrg = layerOrg;
		imagePath = "";
	}
	
	public PLSubsetImageInfo(int rowOrg, int colOrg, int layerOrg, String filePath) {
		this.rowOrg = rowOrg;
		this.colOrg = colOrg;
		this.layerOrg = layerOrg;
		this.imagePath = filePath;
	}
	
	public PLSubsetImageInfo(int rowOrg, int colOrg, int layerOrg, 
			int rowOut, int colOut, int layerOut, int rowRemainder, int colRemainder) {
		this.rowOrg = rowOrg;
		this.colOrg = colOrg;
		this.layerOrg = layerOrg;
		this.rowOut = rowOut;
		this.colOut = colOut;
		this.layerOut = layerOut;
		this.rowRemainder = rowRemainder;
		this.colRemainder = colRemainder;
	}
	
	public int getRowOrg() {
		return rowOrg;
	}
	public void setRowOrg(int rowOrg) {
		this.rowOrg = rowOrg;
	}
	public int getColOrg() {
		return colOrg;
	}
	public void setColOrg(int colOrg) {
		this.colOrg = colOrg;
	}
	public int getLayerOrg() {
		return layerOrg;
	}
	public void setLayerOrg(int layerOrg) {
		this.layerOrg = layerOrg;
	}
	public int getRowOut() {
		return rowOut;
	}
	public void setRowOut(int rowOut) {
		this.rowOut = rowOut;
	}
	public int getColOut() {
		return colOut;
	}
	public void setColOut(int colOut) {
		this.colOut = colOut;
	}
	public int getLayerOut() {
		return layerOut;
	}
	public void setLayerOut(int layerOut) {
		this.layerOut = layerOut;
	}
	public int getRowRemainder() {
		return rowRemainder;
	}
	public void setRowRemainder(int rowRemainder) {
		this.rowRemainder = rowRemainder;
	}
	public int getColRemainder() {
		return colRemainder;
	}
	public void setColRemainder(int colRemainder) {
		this.colRemainder = colRemainder;
	}
	
	@Override
	public int compareTo(PLSubsetImageInfo tmp) {
		if (this.layerOut != tmp.getLayerOut()) {
			return this.layerOut - tmp.getLayerOut();
		}
		if (this.layerOrg <= this.layerOut) {
			if (this.rowOrg == tmp.getRowOrg()) {
				return this.colOrg - tmp.getColOrg();
			}else {
				return this.rowOrg - tmp.getRowOrg();
			}
		}else {
			if (this.rowOut == tmp.getRowOut()) {
				if (this.colOut == tmp.getColOut()) {
					if (this.rowRemainder == tmp.getRowRemainder()) {
						return this.colRemainder - tmp.getColRemainder();
					}else {
						return this.rowRemainder - tmp.getRowRemainder();
					}
				}else {
					return this.colOut - tmp.getColOut();
				}
			}else {
				return this.rowOut - tmp.getRowOut();
			}
		}
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
}
