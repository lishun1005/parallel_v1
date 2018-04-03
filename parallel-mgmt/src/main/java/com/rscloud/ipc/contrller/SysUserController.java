package com.rscloud.ipc.contrller;


import com.github.pagehelper.Page;
import com.rscloud.ipc.dto.SysRoleVo;
import com.rscloud.ipc.rpc.api.dto.SysRoleDto;
import com.rscloud.ipc.rpc.api.dto.SysUserDto;
import com.rscloud.ipc.rpc.api.service.SysRoleService;
import com.rscloud.ipc.rpc.api.service.SysUserService;
import org.apache.commons.lang3.ArrayUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class SysUserController extends BaseContrller{
	@Autowired
	@Lazy
	private SysUserService sysUserService;

	/*@Autowired
	private CacheManager cacheManager;*/
	@Autowired
	private SysRoleService sysRoleService;

	@RequiresPermissions("systemManager:sysuser:list")
	@RequestMapping(value = "sysuser/sysUserList", method = RequestMethod.GET)
	public String sysUserList(Integer rows, Integer pageNo, String keyword,
	                          Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}

		Map<String, Object> map = sysUserService.querySysUser(keyword, rows, pageNo);
		model.addAttribute("pageInfo", map);
		return "/sysuser/sysUserList";
	}

	@RequiresPermissions("systemManager:sysuser:add")
	@RequestMapping(value = "sysuser/sysUserAdd", method = RequestMethod.POST)
	public String sysUserAdd(SysUserDto user, Model model) {
		try {
			if (sysUserService.findByUsername(user.getUsername(), 1) == null) {
				user.setUserType(1);//添加影像处理中心用户
				sysUserService.addUser(user);
				model.addAttribute("msg", "添加成功");
			} else {
				model.addAttribute("msg", "添加失败,已经存在用户名" + user.getUsername() + "的用户");
			}
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("msg", "添加失败");
		}
		return "redirect:/sysuser/sysUserList";
	}

	@RequiresPermissions("systemManager:sysuser:delete")
	@RequestMapping(value = "sysuser/sysUserDelete", method = RequestMethod.GET)
	public String sysUserDelete(String sysUserId, Model model) {
		try {
			sysUserService.deleteUser(sysUserId);
			model.addAttribute("msg", "添加成功");
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("msg", "添加失败");
		}
		return "redirect:/sysuser/sysUserList";
	}

	@RequiresPermissions("systemManager:sysuser:update")
	@RequestMapping(value = "sysuser/sysUserUpdate", method = RequestMethod.POST)
	public String sysUserUpdate(SysUserDto user, Model model) {
		try {
			sysUserService.updateUser(user);
			model.addAttribute("msg", "更新成功");
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("msg", "更新失败");
		}
		return "redirect:/sysuser/sysUserList";
	}

	/**
	 * 重置用户密码
	 *
	 * @param user
	 * @param newPassword
	 * @param newPassword2
	 * @param model
	 * @return
	 * @author huangxj 2016年11月7日
	 * @version v1.0
	 */
	@RequiresPermissions("systemManager:sysuser:change_password")
	@RequestMapping(value = "sysuser/change_password", method = RequestMethod.POST)
	public String editUserPassword(HttpServletRequest request, SysUserDto user, String newPassword, Model model) {
		int result = sysUserService.changePassword(user, newPassword);
		if (result == 1) {
			model.addAttribute("msg", "修改成功");
			//Cache<String, AuthenticationInfo> cache=cacheManager.getCache("authenticationCache");
			//cache.remove(user.getUsername());//清除认证缓存

			getUserRealm().clearAllCachedAuthenticationInfo();
		}
		String refer = request.getHeader("REFERER");
		int msgindex = refer.indexOf("&msg");
		if (msgindex != -1) {
			refer = refer.substring(0, msgindex);
		}
		return "redirect:" + refer;
	}

	@RequiresPermissions("systemManager:sysuser:getMyRole")
	@RequestMapping(value = "sysuser/getMyRole", method = RequestMethod.GET)
	@ResponseBody
	public Object getMyRole(String username, Integer userType) {
		Set<String> userRoleSet = sysRoleService.findRoles(username, userType);
		Page<SysRoleDto> allRolePage = (Page<SysRoleDto>) sysRoleService.querySysRoleByPage(1000, 0, null, true).get("result");
		List<SysRoleVo> roleDtoList = new ArrayList<SysRoleVo>();
		for (SysRoleDto role : allRolePage) {
			SysRoleVo roleDto = new SysRoleVo();
			if (userRoleSet.contains(role.getRole())) {
				roleDto.setIsbind(true);
			} else {
				roleDto.setIsbind(false);
			}
			roleDto.setSysRoleDto(role);
			roleDtoList.add(roleDto);
		}

		return roleDtoList;
	}

	@RequiresPermissions("systemManager:sysuser:editMyRole")
	@RequestMapping(value = "sysuser/editMyRole", method = RequestMethod.POST)
	public String editMyRole(HttpServletRequest request, String username, Integer userType, Model model, String... role) {
		Set<String> userRoleIdSet = sysUserService.findRoleIds(username, userType);
		SysUserDto user = sysUserService.findByUsername(username, userType);
		List<String> removeList = new ArrayList<String>();
		List<String> addList = new ArrayList<String>();
		if (userRoleIdSet != null) {
			for (String oldRole : userRoleIdSet) {
				if (!ArrayUtils.contains(role, oldRole)) {
					//新的角色主不包含旧的角色
					//删除该角色
					removeList.add(oldRole);
				}
			}
		}
		if (role != null) {
			for (String newRole : role) {
				if (!userRoleIdSet.contains(newRole)) {
					//旧角色不包含新角色
					//新增该角色
					addList.add(newRole);
				}
			}
		}
		if (removeList.size() > 0)
			sysUserService.uncorrelationRoles(user.getId(), removeList.toArray(new String[removeList.size()]));
		if (addList.size() > 0)
			sysUserService.correlationRoles(user.getId(), addList.toArray(new String[addList.size()]));
		//cacheManager.getCache("com.rscloud.ipc.shiro.UserRealm.authorizationCache").remove(user.getUsername() + "-" + userType);//清除某个用户权限缓存
		getUserRealm().clearAllCachedAuthorizationInfo();
		model.addAttribute("msg", "用户：\"" + username + "\"  修改角色成功");
		String refer = request.getHeader("REFERER");
		int msgindex = refer.indexOf("&msg");
		if (msgindex != -1) {
			refer = refer.substring(0, msgindex);
		}
		return "redirect:" + refer;
	}
}
