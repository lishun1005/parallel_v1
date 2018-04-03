package com.rsclouds.gtparallel.dao;

import com.rscloud.ipc.rpc.api.dto.SysUserDto;
import com.rsclouds.gtparallel.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


public interface SysUserDao{
	public void add(@Param("id") String id, @Param("username") String username);
	public List<SysUserDto> querySysUser(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
	                                     @Param("pageSize") int pageSize);
	public void delUserRoleByUserId(String id);
	public void updateUser(SysUser user);


	public List<Map<String, Object>> findPermissionsByUsername(@Param("username") String username, @Param("userType") Integer userType);


	public List<Map<String, Object>> findSysRolesByUsername(@Param("username") String username, @Param("userType") Integer userType);
	
	public int updateByPrimaryKeySelective(SysUser record);
	
}
