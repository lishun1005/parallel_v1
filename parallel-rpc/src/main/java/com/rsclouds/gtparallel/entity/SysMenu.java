package com.rsclouds.gtparallel.entity;


import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * <p>
 * User: xiangsx
 * <p>
 * Date: 2015-03-17
 * <p>
 * Version: 2.0
 */
@Table(name = "rscipc_sys_menu")
public class SysMenu implements Serializable {
	private static final long serialVersionUID = -4633766023365047248L;

	private String id; // ID
	private Integer zIndex; // 索引（已废弃，保留，但不用）
	private String name; // 菜单名
	private String url; // 菜单url
	private String parentId; // 父级菜单
	//private Boolean isFolder; // 是否文件夹菜单
	private Integer sequence; // 菜单排序
	private Boolean isShow; // 是否显示
	private String menuType;//类型 1:文件夹 2:页面 3:接口


	/**
	 * 2016-11-10新加 huangxj
	 */
	private String icon; // 板块图标
	private String permission; // 权限名称

	@Id
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "z_index")
	public Integer getZIndex() {
		return zIndex;
	}
	public void setZIndex(Integer zIndex) {
		this.zIndex = zIndex;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "url")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	@Column(name = "parent_id")
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/*@Column(name = "is_folder")
	public Boolean getIsFolder() {
		return isFolder;
	}
	public void setIsFolder(Boolean isFolder) {
		this.isFolder = isFolder;
	}*/

	@Column(name = "sequence")
	public Integer getSequence() {
		return sequence;
	}
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	/**
	 * 2016-11-10新加 huangxj
	 */
	@Column(name = "icon")
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Column(name = "permission")
	public String getPermission() {
		return permission;
	}
	public void setPermission(String permission) {
		this.permission = permission;
	}

	@Column(name = "is_show")
	public Boolean getIsShow() {
		return isShow;
	}
	public void setIsShow(Boolean isShow) {
		this.isShow = isShow;
	}
	public String getMenuType() {
		return menuType;
	}
	public void setMenuType(String menuType) {
		this.menuType = menuType;
	}

}
