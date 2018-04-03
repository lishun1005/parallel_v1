package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;

/**
 * Description: 数据中心地区选择列表
 * 
 * @author Huanghs 2015年7月6日
 * 
 * @version 1.0
 */
public class AreasList implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6745027969265628468L;

	private String adminId;
	
	private String name;
	
	private String namePY;

	public String getAdminId() {
		return adminId;
	}
	public void setAdminId(String adminId) {
		this.adminId = adminId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamePY() {
		return namePY;
	}

	public void setNamePY(String namePY) {
		this.namePY = namePY;
	}
	
}
