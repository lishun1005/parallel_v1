package com.rscloud.ipc.rpc.api.dto;


import java.util.HashMap;
import java.util.Map;


public class HbJobDto {
	
	private String rowKey;
	private String pid;
	private String inPath;
	private String outPath;
	private String node;
	private String state;
	private String type;
	private String progress;
	private String jid;
	private String startTime;
	private String endTime;
	private String part;
	private String mapName;
	private String geoRange;
	private String log;
	private String current;
	private String total;
	private String geowebcacheUrl;



	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public String getRowKey() {
		return rowKey;
	}

	public void setRowKey(String rowKey) {
		this.rowKey = rowKey;
	}

	public String getPid() {
		return pid;
	}

	public String getInPath() {
		return inPath;
	}

	public void setInPath(String inPath) {
			this.inPath = inPath;
	}

	public void setPid(String pid) {
			this.pid = pid;
	}

	public String getOutPath() {
		return outPath;
	}

	public void setOutPath(String outPath) {
			this.outPath = outPath;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
			this.node = node;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
			this.state = state;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
			this.type = type;
	}

	public String getProgress() {
		return progress;
	}

	public void setProgress(String progress) {
			this.progress = progress;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getPart() {
		return part;
	}

	public void setPart(String part) {
		this.part = part;
	}
	
	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}
	
	public String getGeoRange() {
		return geoRange;
	}

	public void setGeoRange(String geoRange) {
		this.geoRange = geoRange;
	}
	
	public String getCurrent() {
		return current;
	}

	public void setCurrent(String current) {
		this.current = current;
	}

	public String getTotal() {
		return total;
	}

	public void setTotal(String total) {
		this.total = total;
	}

	public String getGeowebcacheUrl() {
		return geowebcacheUrl;
	}

	public void setGeowebcacheUrl(String geowebcacheUrl) {
		this.geowebcacheUrl = geowebcacheUrl;
	}

	@Override
	public String toString() {
		return "HbJobDto{" +
				"rowKey='" + rowKey + '\'' +
				", pid='" + pid + '\'' +
				", inPath='" + inPath + '\'' +
				", outPath='" + outPath + '\'' +
				", node='" + node + '\'' +
				", state='" + state + '\'' +
				", type='" + type + '\'' +
				", progress='" + progress + '\'' +
				", jid='" + jid + '\'' +
				", startTime='" + startTime + '\'' +
				", endTime='" + endTime + '\'' +
				", part='" + part + '\'' +
				", mapName='" + mapName + '\'' +
				", geoRange='" + geoRange + '\'' +
				", log='" + log + '\'' +
				", current='" + current + '\'' +
				", total='" + total + '\'' +
				", geowebcacheUrl='" + geowebcacheUrl + '\'' +
				'}';
	}
}
