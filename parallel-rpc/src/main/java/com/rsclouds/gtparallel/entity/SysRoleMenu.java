package com.rsclouds.gtparallel.entity;


import javax.persistence.Column;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "rscipc_sys_role_menu")
public class SysRoleMenu implements Serializable{

	private static final long serialVersionUID = -841954339475645474L;
	
	private String roleId; //角色id
	private String menuId; //菜单id
	private String permissionLevel; //权限级别
	
	@Column(name = "role_id")
	public String getRoleId() {
		return roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	
	@Column(name = "menu_id")
	public String getMenuId() {
		return menuId;
	}
	public void setMenuId(String menuId) {
		this.menuId = menuId;
	}
	
	@Column(name = "permission_level")
	public String getPermissionLevel() {
		return permissionLevel;
	}
	public void setPermissionLevel(String permissionLevel) {
		this.permissionLevel = permissionLevel;
	}
	
	
}
