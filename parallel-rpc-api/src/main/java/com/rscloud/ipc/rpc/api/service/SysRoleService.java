package com.rscloud.ipc.rpc.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.SysRoleDto;
import com.rscloud.ipc.rpc.api.dto.SysRoleMenuDto;


public interface SysRoleService {

	Set<String> findRoles(String SysUsername,Integer userType);
	

	/**
	 * 查找出角色和菜单的关系
	 * 
	 * @author huangxj 2016年11月15日 
	 *
	 * @version v1.0
	 */
	public List<SysRoleMenuDto> findAllRoleMenuByRoleId(String sysRoleId);
	public Map<String, Object> querySysRoleByPage(Integer rows, Integer page,String keyword,Boolean available);

	public SysRoleDto createRole(SysRoleDto sysRole);

	public String updateRole(SysRoleDto sysRole);


	String editRoleMenu(String sysRoleId, String[] pageMenu);


	SysRoleDto findRoleByName(String roleName);
}
