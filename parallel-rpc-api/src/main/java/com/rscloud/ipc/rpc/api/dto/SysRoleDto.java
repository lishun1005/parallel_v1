package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;

public class SysRoleDto implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3788490571958333197L;
	private String id;
	private String role;
	private String description;
	private Boolean available;
	
	
	public String getId(){
		return id; 
	}
	public  void setId(String id){
		this.id=id; 
	}

	public String getRole(){
		return role; 
	}
	public  void setRole(String role){
		this.role=role; 
	}

	public String getDescription(){
		return description; 
	}
	public  void setDescription(String description){
		this.description=description; 
	}

	public Boolean getAvailable(){
		return available; 
	}
	public  void setAvailable(Boolean available){
		this.available=available; 
	}
	

}
