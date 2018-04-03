package com.rscloud.ipc.rpc.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.SysUserDto;

public interface SysUserService {

	/**
	 * description:根据用户名查找用户
	 * 
	 * @param SysUsername
	 * @return SysUser
	 */
	public SysUserDto findByUsername(String SysUsername,Integer userType);

	/**
	 * 
	 * @Title: querySysUser @Description: 用户列表 @author lishun @param @param
	 *         rows @param @param pageNo @param @param
	 *         keyword @param @return @return List<SysUser> @throws
	 */
	public Map<String, Object> querySysUser(String keyword, Integer rows, Integer pageNo);

	public void addUser(SysUserDto user);

	public void deleteUser(String id);

	public void updateUser(SysUserDto user);

	Set<String> findPermissionsByUsername(String username,Integer userType);

	/**
	 * 根据用户名查找角色id数组
	 * 
	 * @param username
	 * @return
	 */
	public Set<String> findRoleIds(String username,Integer userType);

	/**
	 * 修改密码
	 * 
	 * @param userId
	 * @param newPassword
	 */
	public int changePassword(SysUserDto user, String newPassword);

	/**
	 * 移除用户-角色关系
	 * 
	 * @param userId
	 * @param roleIds
	 */
	public void uncorrelationRoles(String userId, String... roleIds);

	/**
	 * 添加用户-角色关系
	 * 
	 * @param userId
	 * @param roleIds
	 */
	public void correlationRoles(String userId, String... roleIds);

	SysUserDto getUserById(String id);

}
