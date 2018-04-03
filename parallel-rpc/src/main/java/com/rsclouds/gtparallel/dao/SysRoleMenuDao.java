package com.rsclouds.gtparallel.dao;

import com.rscloud.ipc.rpc.api.dto.SysRoleMenuDto;
import com.rsclouds.gtparallel.entity.SysRoleMenu;

import java.util.List;

public interface SysRoleMenuDao {

	public int add(SysRoleMenu sysRoleMenu);
	
	public int deleteByRoleId(String roleId);
	
	public List<SysRoleMenuDto> findByRoleId(String roleId);
}
