package com.rscloud.ipc.rpc.api.service;


import java.util.List;

import com.rscloud.ipc.rpc.api.dto.SysMenuDto;

/**
 * 后台菜单服务类接口
 * 
 * @author huangxj 2016年11月10日 
 * 
 * @version v1.0
 */
public interface SysMenuService {

	public String editMenu(SysMenuDto sysMenu);

	public boolean checkPermission(String permission, String id);

	public String updateMenuStatus(String id, boolean isShow);
	
	
	/**
	 * 查出所有的菜单
	 * 
	 * @param type 查询类型 1：全部查出，2：只查出不隐藏的菜单
	 * @return
	 * 
	 * @author huangxj 2016年11月11日 
	 *
	 * @version v1.0
	 */
	public List<SysMenuDto> findAllMenu(int type);
	/**
	 * 
	* Description: 删除菜单
	*  @param id
	* @author lishun 
	* @date 2017年7月2日 
	* @return void
	 */
	String deleteById(String id);
	/**
	 * 
	* Description: 根据url获取用户权限
	*  @param url 两边必须没有 “/” eg: ipc/api/cut/v3
	*  @return 
	* @author lishun 
	* @date 2017年7月6日 
	* @return List<SysMenu>
	 */
	List<SysMenuDto> queryByUrl(String url);

}
