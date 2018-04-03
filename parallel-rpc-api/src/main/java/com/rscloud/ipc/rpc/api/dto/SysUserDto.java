package com.rscloud.ipc.rpc.api.dto;


import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * User: LISHUN
 * <p>
 * Date: 2015-03-17
 * <p>
 * Version: 1.0
 */
public class SysUserDto implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3834221229440700675L;
	private String id;
	private String username;
	private String password;
	private String email;
	private String salt;
	private Boolean locked;
	private Integer userType;
	private Date createTime;
	
	
	public String getId(){
		return id; 
	}
	public  void setId(String id){
		this.id=id; 
	}

	public String getUsername(){
		return username; 
	}
	public  void setUsername(String username){
		this.username=username; 
	}

	public String getPassword(){
		return password; 
	}
	public  void setPassword(String password){
		this.password=password; 
	}

	public String getEmail(){
		return email; 
	}
	public  void setEmail(String email){
		this.email=email; 
	}

	public String getSalt(){
		return salt; 
	}
	public  void setSalt(String salt){
		this.salt=salt; 
	}

	public Boolean getLocked(){
		return locked; 
	}
	
	public  void setLocked(Boolean locked){
		this.locked=locked; 
	}
	public Integer getUserType() {
		return userType;
	}
	public void setUserType(Integer userType) {
		this.userType = userType;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
