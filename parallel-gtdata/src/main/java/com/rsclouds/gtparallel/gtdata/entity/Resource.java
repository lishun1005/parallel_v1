package com.rsclouds.gtparallel.gtdata.entity;

public class Resource {
	
	private byte[] data;
	private long size;
	private String links;
	
	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}	

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String getLinks() {
		return links;
	}

	public void setLinks(String links) {
		this.links = links;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
