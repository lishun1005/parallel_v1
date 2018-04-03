package com.rscloud.ipc.dto;

import java.io.Serializable;


public class SysUserShiroDto implements Serializable {
	private String id;
	private String username;
	private String email;
	private Boolean locked;
	private Integer userType;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getLocked() {
		return locked;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	public Integer getUserType() {
		return userType;
	}

	public void setUserType(Integer userType) {
		this.userType = userType;
	}
	@Override
	public String toString() {
		return username;//设置shiro缓存名称
	}
}
