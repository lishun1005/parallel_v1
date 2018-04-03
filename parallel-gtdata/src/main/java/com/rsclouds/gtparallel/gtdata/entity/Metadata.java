package com.rsclouds.gtparallel.gtdata.entity;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class Metadata {
	
	private String rowKey;
	private String url;
	private String dfs;
	private String size;
	private String time;
	private String capacity;
	private String permisson;
	
	public Metadata(String rowKey,String url,String dfs,String size,String time){
		this.url = url;
		this.dfs = dfs;
		this.size = size;
		this.time = time;
		this.rowKey = rowKey;
		if(size.equals("-1")) {
			capacity = "00";
			permisson = "1110000000110";
		}else {
			capacity = "10";
			permisson = "0110000000110";
		}
	}
	
	public Metadata(Result rs){
		if(rs!= null && !rs.isEmpty()){
			rowKey = Bytes.toString(rs.getRow());
			url = Bytes.toString(rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal));
			dfs = Bytes.toString(rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal));
			size = Bytes.toString(rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
			capacity = Bytes.toString(rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal));
			permisson = Bytes.toString(rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal));
			time = GtDataUtils.timeStrFillZero(Bytes.toString(rs.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal)));
			if("null".equals(url)){
				url = null;
			}
			if("null".equals(dfs)){
				dfs = null;
			}
			if("null".equals(size)){
				size = null;
			}
			if("null".equals(time)){
				time = null;
			}
		}
	}

	public String getRowKey() {
		return rowKey;
	}

	public void setRowKey(String rowKey) {
		this.rowKey = rowKey;
	}

	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public String getDfs() {
		return dfs;
	}


	public void setDfs(String dfs) {
		this.dfs = dfs;
	}


	public String getSize() {
		return size;
	}


	public void setSize(String size) {
		this.size = size;
	}


	public String getTime() {
		return time;
	}


	public void setTime(String time) {
		this.time = time;
	}
	
	public Map<String,String> toStrMap(){
		Map<String, String> map = new HashMap<String, String>();
		map.put(GtDataConfig.META.SIZE.strVal, size);
		map.put(GtDataConfig.META.URL.strVal, url);
		map.put(GtDataConfig.META.DFS.strVal, dfs);
		map.put(GtDataConfig.META.TIME.strVal, time);
		return map;
	}
    

	public String getCapacity() {
		return capacity;
	}

	public void setCapacity(String capacity) {
		this.capacity = capacity;
	}

	public String getPernisson() {
		return permisson;
	}

	public void setPernisson(String pernisson) {
		this.permisson = pernisson;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(Bytes.toString(null));
	}

}
