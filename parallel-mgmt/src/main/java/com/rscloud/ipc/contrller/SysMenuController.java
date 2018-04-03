package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rpc.api.dto.SysMenuDto;
import com.rscloud.ipc.rpc.api.service.SysMenuService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * 新的菜单管理控制类，旧的暂时不用
 * 
 * @author huangxj 2016年11月9日 
 * 
 * @version v1.0
 */
@Controller
@RequestMapping(value = "/menuManageNew")
public class SysMenuController{
	

	/**
	 * 菜单逻辑控制类
	 */
	@Autowired
	@Lazy
	private SysMenuService sysMenuService;
	
	/**
	 * 1、获取所有的菜单
	 */
	@RequiresPermissions("systemManager:menu:list")
	@RequestMapping(value = "/findAllMenu")
	public String findAllMenu(Model model){
		
		List<SysMenuDto> menuList = sysMenuService.findAllMenu(1);
		model.addAttribute("menuList",menuList);
			
		return "/sysuser/menuList";
	}
	
	/**
	 * 2、检查数据库中是否存在相同的权限名
	 */
	@RequiresPermissions("systemManager:menu:checkPermission")
	@RequestMapping(value = "/checkPermission")
	@ResponseBody
	public Map<String,Object> checkPermission(String permission, String id){
		
		Map<String,Object> map = new HashMap<String, Object>();
		
		boolean isHere = sysMenuService.checkPermission(permission, id);
		
		map.put("isHere", isHere);
		
		return map;
	}
	
	
	/**
	 * 3、添加或编辑菜单
	 */
	@RequiresPermissions("systemManager:menu:editMenu")
	@RequestMapping(value = "/editMenu")
	public String editMenu(SysMenuDto sysMenu, Model model){
		String result = sysMenuService.editMenu(sysMenu);
		model.addAttribute("msg", result);
		return "redirect:findAllMenu";
	}
	
	@RequiresPermissions("systemManager:menu:deleteMenu")
	@RequestMapping(value = "/deleteMenu")
	public String editMenu(String id, Model model){
	/*	try {
			String msg=sysMenuService.deleteById(id);
			model.addAttribute("msg", msg);
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("msg", e.getMessage());
		}*/
		
		
		return "redirect:findAllMenu";
	}
}
