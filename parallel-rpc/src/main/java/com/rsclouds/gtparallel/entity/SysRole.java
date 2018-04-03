package com.rsclouds.gtparallel.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "rscipc_sys_role")
public class SysRole implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3788490571958333197L;
	private String id;
	private String role;
	private String description;
	private Boolean available;
	
	
	@Id
	@Column(name = "id")
	public String getId(){
		return id; 
	}
	public  void setId(String id){
		this.id=id; 
	}

	@Column(name = "role")
	public String getRole(){
		return role; 
	}
	public  void setRole(String role){
		this.role=role; 
	}

	@Column(name = "description")
	public String getDescription(){
		return description; 
	}
	public  void setDescription(String description){
		this.description=description; 
	}

	@Column(name = "available")
	public Boolean getAvailable(){
		return available; 
	}
	public  void setAvailable(Boolean available){
		this.available=available; 
	}
	

}
