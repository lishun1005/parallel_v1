package com.rscloud.ipc.dto;


import com.rscloud.ipc.rpc.api.dto.SysMenuDto;

public class RoleMenuVo {

	private SysMenuDto sysMenuDto;
	private Boolean isCheck = false;
	private String permissionLevel;
	
	
	public SysMenuDto getSysMenuDto() {
		return sysMenuDto;
	}
	public void setSysMenuDto(SysMenuDto sysMenuDto) {
		this.sysMenuDto = sysMenuDto;
	}
	public Boolean getIsCheck() {
		return isCheck;
	}
	public void setIsCheck(Boolean isCheck) {
		this.isCheck = isCheck;
	}
	public String getPermissionLevel() {
		return permissionLevel;
	}
	public void setPermissionLevel(String permissionLevel) {
		this.permissionLevel = permissionLevel;
	}
	
	
}
