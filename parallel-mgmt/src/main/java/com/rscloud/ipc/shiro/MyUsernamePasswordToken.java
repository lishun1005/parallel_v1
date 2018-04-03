package com.rscloud.ipc.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * 
* @ClassName: 重写包装用户登录信息(目的是加入userType)  
* @Description: TODO
* @author lishun 
* @date 2017年7月13日 上午10:37:05  
*
 */
public class MyUsernamePasswordToken extends UsernamePasswordToken{
	private Integer userType;

	public Integer getUserType() {
		return userType;
	}

	public void setUserType(Integer userType) {
		this.userType = userType;
	}
	public MyUsernamePasswordToken(final String username,final String password,Integer userType) {
		super(username,password);
		this.userType=userType;
	}
}
