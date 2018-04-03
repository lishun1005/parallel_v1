package com.rsclouds.gtparallel.service.impl;


import com.rscloud.ipc.rpc.api.dto.SysMenuDto;
import com.rscloud.ipc.rpc.api.service.SysMenuService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.gtparallel.dao.SysMenuDao;
import com.rsclouds.gtparallel.dao.SysRoleMenuDao;
import com.rsclouds.gtparallel.entity.SysMenu;
import com.rsclouds.gtparallel.entity.SysRoleMenu;
import com.rsclouds.jdbc.repository.JdbcRepository;
import com.rsclouds.jdbc.repository.SearchFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 后台菜单管理服务类
 * 
 * @author huangxj 2016年11月10日 
 * 
 * @version v1.0
 */
@Service
@Transactional
public class SysMenuServiceImpl extends JdbcRepository<SysMenu, String> implements SysMenuService{

	@Autowired
	private SysMenuDao sysMenuDao;
	
	/**
	 * 菜单管理id
	 */
	@Value("#{applicationProperty[menuManageId]}")
	protected String menuManageId;
	
	/**
	 * 管理员id
	 */
	@Value("#{applicationProperty[adminId]}")
	protected String adminId;
	
	@Value("#{applicationProperty[sysMgmtId]}")
	protected String sysMgmtId;
	
	/**
	 * 角色管理
	 */
	@Autowired
	private SysRoleMenuDao sysRoleMenuDao;
	
	
	@Override
	public List<SysMenuDto> queryByUrl(String url){
		return sysMenuDao.queryByUrl(url);
	}
	/**
	 * 获取所有的菜单
	 */
	@Override
	public List<SysMenuDto> findAllMenu(int type) {
		 
		List<SysMenuDto> list = null;
		Sort sort = new Sort("sequence");  // 按sort字段升序排序
		switch (type) {
		case 1:
			list = BeanMapper.mapList(findAll(sort), SysMenuDto.class) ;
			break;
		case 2:
			Map<String, Object> searchParams = new HashMap<String, Object>();
			searchParams.put("EQ_isShow", true);
			Map<String, SearchFilter> filters = SearchFilter.parse(searchParams);
			list =  BeanMapper.mapList(findAll(filters.values(),sort),SysMenuDto.class);			
			break;
		}
		return list;
	}

	/**
	 * 新增或修改菜单
	 */
	@Override
	public String editMenu(SysMenuDto sysMenu) {
		SysMenu result = null;
		String id = sysMenu.getId();
		if(StringUtils.isBlank(id)){//是add
			sysMenu.setId(UUID.randomUUID().toString());
			SysMenu parent = findOne(sysMenu.getParentId());//isShow字段跟他爸一样
			sysMenu.setIsShow(parent.getIsShow());
			result = save(BeanMapper.map(sysMenu, SysMenu.class));
			if(result != null){
				String type=sysMenu.getMenuType();//页面菜单,接口，的话则需跟管理员角色挂上钩
				if("2".equals(type)||"3".equals(type)||"4".equals(type)){
					SysRoleMenu sourceBean = new SysRoleMenu();
					sourceBean.setRoleId(adminId);
					sourceBean.setPermissionLevel("*");
					sourceBean.setMenuId(sysMenu.getId());
					sysRoleMenuDao.add(sourceBean);
				}
				return "添加成功";
			}
		}else{
			//是update
			if(id.equals(menuManageId)){
				return "菜单管理状态不可修改";
			}
			//如果更新的菜单是文件夹菜单，则还要递归更新其子目录的菜单状态
			if("1".equals(sysMenu.getMenuType())){
				Map<String, Object> searchParams = new HashMap<String, Object>();
				searchParams.put("EQ_parentId", sysMenu.getId());
				Map<String, SearchFilter> filters = SearchFilter.parse(searchParams);
				List<SysMenu> childList = findAll(filters.values());		
				for (SysMenu childSysMenu : childList) {
					if(!childSysMenu.getId().equals(menuManageId)){
						if(!"3".equals(childSysMenu.getMenuType())){//接口不用更新
							updateMenuStatus(childSysMenu.getId(),sysMenu.getIsShow());
						}
						
					}
				}
			}
			if(sysMgmtId.equals(id)||sysMgmtId.equals(sysMenu.getParentId())){
				return "系统管理子菜单不能修改";
			}
			SysMenu menuOld=findOne(sysMenu.getId());
			sysMenu.setPermission(menuOld.getPermission());;
			sysMenu.setUrl(menuOld.getUrl());//权限和url不更新
			result = save(BeanMapper.map(sysMenu, SysMenu.class));
			if(result != null){
				return "修改成功";
			}
		}
		
		return "操作失败";
	}

	/**
	 * 检查权限名在数据库中是否存在
	 */
	@Override
	public boolean checkPermission(String permission, String id) {

		Map<String, Object> searchParams = new HashMap<String, Object>();
		searchParams.put("LIKE_permission", permission.trim());
		
		Map<String, SearchFilter> filters = SearchFilter.parse(searchParams);
		List<SysMenu> list = findAll(filters.values());		
		
		if(list != null && list.size() > 0){
			if(list.get(0).getId().equals(id)){
				return false;
			}else{
				return true;
			}
		}else{
			return false;
		}
	}
	
	@Override
	public String deleteById(String id){
		SysMenu sysMenu = findOne(id);
		if(sysMgmtId.equals(id)||sysMgmtId.equals(sysMenu.getParentId())){
			return "系统管理子菜单不能删除";
		}
		if("1".equals(sysMenu.getMenuType())){//如果更新的菜单是文件夹菜单，先删除子菜单
			Map<String, Object> searchParams = new HashMap<String, Object>();
			searchParams.put("EQ_parentId", sysMenu.getId());
			Map<String, SearchFilter> filters = SearchFilter.parse(searchParams);
			List<SysMenu> childList = findAll(filters.values());		
			for (SysMenu childSysMenu : childList) {
				delete(childSysMenu.getId());
			}
		}
		delete(id);
		return "删除成功";
	}
	/**
	 * 显示菜单或隐藏菜单
	 */
	@Override
	public String updateMenuStatus(String id, boolean isShow) {
		
		//得到菜单实体
		SysMenu sysMenu = findOne(id);
		
		//想要显示菜单，必需其父辈菜单均为显示状态方能将其修改为显示
		if(isShow){
			
			SysMenu sysMenuParent = findOne(sysMenu.getParentId());
			
			if(!sysMenuParent.getIsShow()){
				return "父级菜单处于隐藏状态，子菜单不能显示，请先将父级菜单置为显示！";
			}
		}
		
		//更新状态
		SysMenu record = new SysMenu();
		record.setId(id);
		record.setIsShow(isShow);
		sysMenuDao.updateByPrimaryKeySelective(record);
		
		//如果更新的菜单是文件夹菜单，则还要递归更新其子目录的菜单状态
		if("1".equals(sysMenu.getMenuType())){
			
			
			Map<String, Object> searchParams = new HashMap<String, Object>();
			searchParams.put("EQ_parentId", sysMenu.getId());
			
			Map<String, SearchFilter> filters = SearchFilter.parse(searchParams);
			List<SysMenu> childList = findAll(filters.values());		
			
			for (SysMenu childSysMenu : childList) {
				
				if(!childSysMenu.getId().equals(menuManageId)){
					updateMenuStatus(childSysMenu.getId(),isShow);
				}
			}
		}
			
		return "更新成功";
	}

}
