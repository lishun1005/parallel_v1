package com.rsclouds.gtparallel.service.impl;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.SysUserDto;
import com.rscloud.ipc.rpc.api.service.SysUserService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import com.rsclouds.gtparallel.dao.SysUserDao;
import com.rsclouds.gtparallel.dao.SysUserRoleDao;
import com.rsclouds.gtparallel.entity.SysUser;
import com.rsclouds.gtparallel.entity.SysUserRole;
import com.rsclouds.jdbc.repository.JdbcRepository;
import com.rsclouds.jdbc.repository.SearchFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class SysUserServiceImpl extends JdbcRepository<SysUser, String> implements SysUserService {
	
	@Autowired
	public SysUserDao sysUserDao;
	
	@Autowired
	public SysUserRoleDao sysUserRoleDao;

	@Override
	public Map<String, Object> querySysUser(String keyword, Integer rows, Integer pageNo) {
		Map<String, Object> map=new HashMap<String, Object>();
		List<SysUserDto> list = sysUserDao.querySysUser(keyword, pageNo, rows);
		Page<SysUserDto> page = (Page<SysUserDto>)sysUserDao.querySysUser(keyword, pageNo, rows);
		map.put("result", page);
		map.put("totalPage", page.getTotal());
		map.put("pages", page.getPages());
		map.put("pageNum", page.getPageNum());
		return map;
	}

	@Override
	public void updateUser(SysUserDto user) {
		sysUserDao.updateUser(BeanMapper.map(user, SysUser.class));
	}
	@Override
	public SysUserDto getUserById(String id) {
		SysUser sysUser=findOne(id);
		if(sysUser != null){
			return BeanMapper.map(sysUser, SysUserDto.class);
		}else{
			return null;
		}
	}
	@Override
	public void addUser(SysUserDto user) {
		if (StringUtils.isBlank(user.getId())) {
			user.setId(StringTool.getUUID());
		}
		user.setCreateTime(new Date());
		save(BeanMapper.map(user, SysUser.class));
	}

	@Override
	public void deleteUser(String id) {
		sysUserDao.delUserRoleByUserId(id);// 先删除角色关联表
		delete(id);
	}

	@Override
	public SysUserDto findByUsername(String sysUsername,Integer userType) {
		Map<String, Object> searchParams = new HashMap<String, Object>();
		searchParams.put("EQ_username", sysUsername);
		searchParams.put("EQ_userType", userType);
		return BeanMapper.map(findOne(SearchFilter.parse(searchParams).values()),SysUserDto.class);
	}

	@Override
	public Set<String> findPermissionsByUsername(String username,Integer userType) {

		List<Map<String, Object>> result = sysUserDao.findPermissionsByUsername(username,userType);

		Set<String> set = new HashSet<String>();
		for (Map<String, Object> row : result) {
			String permission = row.get("permission").toString();
			set.add(permission);
			/*String permissionLevel = row.get("permission_level").toString();
			String[] levelArr = permissionLevel.split(",");
			for (String level : levelArr) {
				set.add(permission + ":" + level);
			}*/
		}

		return set;
	}

	@Override
	public Set<String> findRoleIds(String username,Integer userType) {
		Set<String> roleIds = new HashSet<String>();
		for (Map<String, Object> roleMap : sysUserDao.findSysRolesByUsername(username,userType)) {
			roleIds.add((String) roleMap.get("id"));
		}
		return roleIds;
	}

	@Override
	public int changePassword(SysUserDto user, String newPassword) {
		SysUser u = findOne(user.getId());
		if (u != null) {
			u.setPassword(newPassword);
			SysUser ret = save(u);
			if (ret == null) {
				System.out.println("修改密码成功");
			} else {
				System.out.println("修改密码失败");
			}
		} else {
			System.out.println("用户不存在");
		}
		return 1;
	}

	@Override
	public void uncorrelationRoles(String userId, String... roleIds) {

		if (roleIds == null || roleIds.length == 0) {
			return;
		}
		for (String roleId : roleIds) {
			SysUserRole sysUserRole = new SysUserRole();
			sysUserRole.setRoleId(roleId);
			sysUserRole.setUserId(userId);
			sysUserRoleDao.delete(sysUserRole);
					
		}
	}

	@Override
	public void correlationRoles(String userId, String... roleIds) {
		if (roleIds == null || roleIds.length == 0) {
			return;
		}
		for (String roleId : roleIds) {

			SysUserRole sysUserRole = new SysUserRole();
			sysUserRole.setRoleId(roleId);
			sysUserRole.setUserId(userId);
			sysUserRoleDao.add(sysUserRole);
		}

	}

}
