package com.rsclouds.gtparallel.core.entity;

import java.util.HashMap;
import java.util.Map;

import com.rsclouds.gtparallel.core.common.CoreConfig;


public class Job {
	
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

	public String toString(){
		return rowKey + "\n" 
				+ pid + "\n"
				+ inPath + "\n"
				+ outPath + "\n" 
				+ node + "\n" 
				+ state + "\n" 
				+ type + "\n" 
				+ part + "\n" 
				+ progress + "\n" 
				+ jid + "\n" 
				+ startTime + "\n" 
				+ endTime + "\n" 
				+ geoRange + "\n";
	}
	

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		if (pid != null)
			map.put(CoreConfig.JOB.PID.strVal, pid);
		if (type != null)
			map.put(CoreConfig.JOB.TYPE.strVal, type);
		if (node != null)
			map.put(CoreConfig.JOB.NODE.strVal, node);
		if (inPath != null)
			map.put(CoreConfig.JOB.IN_PATH.strVal, inPath);
		if (outPath != null)
			map.put(CoreConfig.JOB.OUT_PATH.strVal, outPath);
		if (progress != null)
			map.put(CoreConfig.JOB.PROGRESS.strVal, progress);
		if (state != null)
			map.put(CoreConfig.JOB.STATE.strVal, state);
		if (jid != null)
			map.put(CoreConfig.JOB.JID.strVal, jid);
		if (startTime != null)
			map.put(CoreConfig.JOB.START_TIME.strVal, startTime);
		if (endTime != null)
			map.put(CoreConfig.JOB.END_TIME.strVal, endTime);
		if (mapName != null){
			map.put(CoreConfig.JOB.MAP_NAME.strVal, mapName);
		}
		if (current != null){
			map.put(CoreConfig.JOB.CURRNT.strVal, current);
		}
		if (total != null){
			map.put(CoreConfig.JOB.TOTAL.strVal, total);
		}
		return map;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}
