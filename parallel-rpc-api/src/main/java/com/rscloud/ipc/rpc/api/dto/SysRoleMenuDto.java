package com.rscloud.ipc.rpc.api.dto;



import java.io.Serializable;

public class SysRoleMenuDto implements Serializable{

	private static final long serialVersionUID = -841954339475645474L;
	
	private String roleId; //角色id
	private String menuId; //菜单id
	private String permissionLevel; //权限级别
	
	public String getRoleId() {
		return roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	
	public String getMenuId() {
		return menuId;
	}
	public void setMenuId(String menuId) {
		this.menuId = menuId;
	}
	
	public String getPermissionLevel() {
		return permissionLevel;
	}
	public void setPermissionLevel(String permissionLevel) {
		this.permissionLevel = permissionLevel;
	}
	
	
}
