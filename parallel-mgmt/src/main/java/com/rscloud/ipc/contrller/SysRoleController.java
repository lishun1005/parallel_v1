package com.rscloud.ipc.contrller;

import com.rscloud.ipc.dto.RoleMenuVo;
import com.rscloud.ipc.rpc.api.dto.SysMenuDto;
import com.rscloud.ipc.rpc.api.dto.SysRoleDto;
import com.rscloud.ipc.rpc.api.dto.SysRoleMenuDto;
import com.rscloud.ipc.rpc.api.service.SysMenuService;
import com.rscloud.ipc.rpc.api.service.SysRoleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/sysrole")
public class SysRoleController extends BaseContrller {
	
	@Autowired
	@Lazy
	private SysRoleService sysRoleService;

	/*@Autowired
	private CacheManager cacheManager;*/
	/**
	 * 菜单逻辑控制类
	 */
	@Autowired
	@Lazy
	private SysMenuService sysMenuService;
	
	/**
	 * 获取角色的菜单信息
	 * 
	 * @author huangxj 2016年11月15日 
	 *
	 * @version v1.0
	 */
	@RequiresPermissions("systemManager:sysrole:getMyMenu")
	@RequestMapping(value = "getMyMenu", method = RequestMethod.GET)
	@ResponseBody
	public Map<String,Object> getMyMenu(String sysRoleId){
		Map<String,Object> jsonMap = new HashMap<String, Object>();
		
		//1、找出所有没被隐藏的菜单SysMenuList
		List<SysMenuDto> menuList = sysMenuService.findAllMenu(2);
		
		//2、找出该角色拥有的SysMenuId
		List<SysRoleMenuDto> roleMenuList = sysRoleService.findAllRoleMenuByRoleId(sysRoleId);
		
		//3、组装json，返回给前台
		List<RoleMenuVo> roleMenuDtoList = new ArrayList<RoleMenuVo>();
		
		for (SysMenuDto sysMenu : menuList) {
			
			RoleMenuVo roleMenuDto = new RoleMenuVo();
			roleMenuDto.setSysMenuDto(sysMenu);
			String menuId = sysMenu.getId();
			for (SysRoleMenuDto sysRoleMenu : roleMenuList) {
				if(menuId.equals(sysRoleMenu.getMenuId())){
					roleMenuDto.setIsCheck(true);
					roleMenuDto.setPermissionLevel(sysRoleMenu.getPermissionLevel());
					break;
				}
			}
			roleMenuDtoList.add(roleMenuDto);
		}

		jsonMap.put("roleMenuDtoList", roleMenuDtoList);
		
		return jsonMap;
	}
	
	/**
	 * 分配菜单
	 * 
	 * @author huangxj 2016年12月7日 
	 *
	 * @version v1.0
	 */
	@RequiresPermissions("systemManager:sysrole:editMyMenu")
	@RequestMapping(value = "editMyMenu", method = RequestMethod.POST)
	public String editMyMenu(HttpServletRequest request, String sysRoleId, String[] pageMenu, Model model){
		
		String msg = sysRoleService.editRoleMenu(sysRoleId,pageMenu);
		model.addAttribute("msg", msg);
		getUserRealm().clearAllCachedAuthorizationInfo();
		String refer = request.getHeader("REFERER"); 
		int msgindex = refer.indexOf("&msg");
		if(msgindex != -1){
			refer = refer.substring(0,msgindex);
		}
		
		return "redirect:" + refer;	
	}
	@RequiresPermissions("systemManager:sysrole:list")
	@RequestMapping(value = "sysRoleList", method = RequestMethod.GET)
	public String sysRoleList(Integer rows,Integer pageNo,String keyword,Model model){
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		Map<String, Object> map =  sysRoleService.querySysRoleByPage(rows, pageNo, keyword, null);
		model.addAttribute("pageInfo", map);
		
		return "/sysuser/sysRoleList";
	}
	@RequiresPermissions("systemManager:sysrole:add")
	@RequestMapping(value = "sysRoleAdd", method = RequestMethod.POST)
	public String sysRoleAdd(String role,String description,String available,Model model){
		if(sysRoleService.findRoleByName(role)==null){
			SysRoleDto sysRole = new SysRoleDto();
			sysRole.setRole(role);
			sysRole.setDescription(description);
			if("true".equalsIgnoreCase(available))
				sysRole.setAvailable(true);
			else
				sysRole.setAvailable(false);
			sysRole = sysRoleService.createRole(sysRole);
			model.addAttribute("msg", "添加成功");
		}else{
			model.addAttribute("msg", "添加失败,已存在角色"+role);
		}
		return "redirect:/sysrole/sysRoleList";
	}
	@RequiresPermissions("systemManager:sysrole:update")
	@RequestMapping(value = "sysRoleUpdate", method = RequestMethod.POST)
	public String sysRoleUpdate(String id,String role,String description,String available,Model model){
		SysRoleDto sysRole = new SysRoleDto();
		sysRole.setId(id);
		sysRole.setRole(role);
		sysRole.setDescription(description);
		if("true".equalsIgnoreCase(available))
			sysRole.setAvailable(true);
		else
			sysRole.setAvailable(false);
		model.addAttribute("msg",sysRoleService.updateRole(sysRole));
		return "redirect:/sysrole/sysRoleList";
	}
}
