package com.rsclouds.gtparallel.service.impl;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.SysRoleDto;
import com.rscloud.ipc.rpc.api.dto.SysRoleMenuDto;
import com.rscloud.ipc.rpc.api.service.SysRoleService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.gtparallel.dao.SysRoleDao;
import com.rsclouds.gtparallel.dao.SysRoleMenuDao;
import com.rsclouds.gtparallel.entity.SysRole;
import com.rsclouds.gtparallel.entity.SysRoleMenu;
import com.rsclouds.jdbc.repository.JdbcRepository;
import com.rsclouds.jdbc.repository.SearchFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class SysRoleServiceImpl extends JdbcRepository<SysRole, String> implements SysRoleService {

	@Autowired
	public SysRoleDao sysRoleDao;


	@Autowired
	private SysRoleMenuDao sysRoleMenuDao;
	@Override
	public Set<String> findRoles(String SysUsername,Integer userType) {
		List<String> result = sysRoleDao.findRoles(SysUsername, userType);
		Set<String> set = new HashSet<String>();
		set.addAll(result);
		return set;
	}
	@Override
	public SysRoleDto findRoleByName(String roleName){
		Map<String, Object> searchParams = new HashMap<String, Object>();
		searchParams.put("EQ_role", roleName);
		return BeanMapper.map(findOne(SearchFilter.parse(searchParams).values()), SysRoleDto.class) ;
	}
	@Override
	public String editRoleMenu(String sysRoleId, String[] pageMenu) {
		// 先删
		sysRoleMenuDao.deleteByRoleId(sysRoleId);

		if (pageMenu != null) {
			// 再增
			SysRoleMenu sourceBean = new SysRoleMenu();
			sourceBean.setRoleId(sysRoleId);
			sourceBean.setPermissionLevel("*");
			for (String menuId : pageMenu) {
				sourceBean.setMenuId(menuId);
				sysRoleMenuDao.add(sourceBean);
			}
		}
		return "编辑成功";
	}
	@Override
	public List<SysRoleMenuDto> findAllRoleMenuByRoleId(String sysRoleId) {
		return sysRoleMenuDao.findByRoleId(sysRoleId);
	}
	

	@Override
	public Map<String, Object> querySysRoleByPage(Integer rows, Integer page, String keyword, Boolean available) {
		Map<String, Object> map=new HashMap<String, Object>();
		Page<SysRoleDto> pages=(Page<SysRoleDto>)sysRoleDao.querySysRole(keyword, page, rows, available);
		map.put("result", pages);
		map.put("totalPage", pages.getTotal());
		map.put("pages", pages.getPages());
		map.put("pageNum", pages.getPageNum());
		return map;
	}

	@Override
	public SysRoleDto createRole(SysRoleDto role) {

		if (role.getId() == null) {
			role.setId(UUID.randomUUID().toString());
		}
		SysRole ret = save(BeanMapper.map(role, SysRole.class));
		if (ret != null) {
			System.out.println("创建角色成功");
		} else {
			System.out.println("创建角色失败");
		}
		return role;

	}

	@Override
	public String updateRole(SysRoleDto role) {

		if (role == null || role.getId() == null) {
			return "更新失败";
		}
		SysRole roleCk =findOne(role.getId());
		if(!roleCk.getRole().equals(role.getRole())){
			if(findRoleByName(role.getRole())!=null){
				return "更新失败,已存在角色"+role.getRole();
			}
		}
		int ret = sysRoleDao.updateByPrimaryKeySelective(BeanMapper.map(role, SysRole.class));
		
		if (ret == 1) {
			return "更新角色成功";
		} else {
			return "更新角色失败";
		}

	}

}
