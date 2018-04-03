package com.rscloud.ipc.dto;

import java.io.Serializable;

import com.rscloud.ipc.rpc.api.dto.SysRoleDto;

public class SysRoleVo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SysRoleDto sysRoleDto;
	public SysRoleDto getSysRoleDto() {
		return sysRoleDto;
	}



	public void setSysRoleDto(SysRoleDto sysRoleDto) {
		this.sysRoleDto = sysRoleDto;
	}



	private Boolean isbind;	





	public Boolean getIsbind() {
		return isbind;
	}



	public void setIsbind(Boolean isbind) {
		this.isbind = isbind;
	}



	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
