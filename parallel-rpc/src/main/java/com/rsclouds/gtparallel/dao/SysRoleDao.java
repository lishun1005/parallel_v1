package com.rsclouds.gtparallel.dao;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.SysRoleDto;
import com.rsclouds.gtparallel.entity.SysRole;
import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleDao {
	public List<String> findRoles(@Param("username") String username, @Param("userType") Integer userType);

	public int updateByPrimaryKeySelective(SysRole record);

	public Page<SysRoleDto> querySysRole(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
	                                     @Param("pageSize") int pageSize, @Param("available") Boolean available);
}
