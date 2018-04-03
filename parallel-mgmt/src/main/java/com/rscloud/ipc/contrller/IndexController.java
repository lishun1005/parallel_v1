package com.rscloud.ipc.contrller;


import com.rscloud.ipc.rpc.api.dto.SysMenuDto;
import com.rscloud.ipc.rpc.api.service.SysMenuService;
import com.rscloud.ipc.rpc.api.service.SysUserService;
import com.rscloud.ipc.shiro.MyUsernamePasswordToken;
import com.rscloud.ipc.shiro.ShiroKit;
import com.rscloud.ipc.shiro.UserRealm;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


@Controller
public class IndexController extends BaseContrller{
	
	@Autowired
	@Lazy
	public SysUserService sysUserManageService;
	/**
	 * 菜单逻辑控制类
	 */
	@Autowired
	@Lazy
	private SysMenuService sysMenuService;
	
	@Autowired
	@Lazy
	private CacheManager cacheManager;
	/**
	 * 
	 * Description：后台管理 登录成功后，跳转到后台管理中心首页
	 * @param user
	 * @param model
	 * @return       
	 *
	 */
	@RequestMapping(value = "index", method = RequestMethod.GET)
	public String index(Model  model,HttpServletRequest request){
		List<SysMenuDto> menuList = sysMenuService.findAllMenu(2);
		
		// 判断用户是否具备访问该菜单的权限
		ListIterator<SysMenuDto> lit = menuList.listIterator();
		while (lit.hasNext()) {
			SysMenuDto sysMenu = lit.next();
			if("2".equals(sysMenu.getMenuType())){ //页面菜单才需要进行过滤
				String permission = sysMenu.getPermission();
				//permission += ":list";
				if (!ShiroKit.hasPermission(permission)) {
					lit.remove();
				}
			}else if("3".equals(sysMenu.getMenuType())){
				lit.remove();
			}
			
		}
		model.addAttribute("menuList", menuList);
		return "index";
	}
	
	/**
	 * 
	 * Description：***
	 * @param model
	 * @return       
	 *
	 */
	@RequestMapping(value = "unauthorized", method = RequestMethod.GET)
	public String unauthorized(Model  model){
		model.addAttribute("msg", "你没有权限执行该操作，请联系管理员");
		return "unauthorized";
	}
	
	/**
	 * 
	 * Description：***
	 * @param model
	 * @return       
	 *
	 */
	@RequestMapping(value = "logout", method = RequestMethod.GET)
	public String logout(Model  model){		
		Subject subject = SecurityUtils.getSubject();
		if(subject.isAuthenticated()){
			subject.logout();
		}
		return "redirect:login";
	}

	/**
	 * 
	 * Description：后台管理 跳转到用户登录页面
	 * @param model
	 * @return       
	 *
	 */
	@RequestMapping(value = "login", method = RequestMethod.GET)
	public String login(Model model,String iframeUrl){
		return "login";
	}

	/**
	 * 
	 * Description：后台管理 使用输入的用户名，密码登录系统
	 * @param model
	 * @return       
	 *
	 */
	@ResponseBody
	@RequestMapping(value = "login", method = RequestMethod.POST)
	public Map<String,Object> loginSystem(Model model, String username, String password,Integer userType,HttpServletRequest request){
		Map<String,Object> res=new HashMap<String,Object>();
		Subject subject = SecurityUtils.getSubject();
		MyUsernamePasswordToken token = new MyUsernamePasswordToken(username, password,userType);
		//token.setRememberMe(true);
		try {
			//登录前清空用户授权和认证缓存
			/*cacheManager.getCache("authenticationCache").remove(username + "-" +userType);
			cacheManager.getCache("com.rscloud.ipc.shiro.UserRealm.authorizationCache").remove(username + "-" +userType);
			RealmSecurityManager securityManager =
					(RealmSecurityManager) SecurityUtils.getSecurityManager();
			UserRealm userRealm = (UserRealm) securityManager.getRealms().iterator().next();*/

			//getUserRealm().clearAllCachedAuthenticationInfo();
			subject.login(token);
			res.put("code", 2001);
		}catch(ExcessiveAttemptsException eae){
			res.put("code", 2002);
			res.put("errorMessage", "用户名/密码错误超过10次,锁定一小时");
		}catch (LockedAccountException e) {
			res.put("code", 2002);
			res.put("errorMessage", "用户被锁定");
		} catch (IncorrectCredentialsException ice) {
			res.put("code", 2002);
			res.put("errorMessage", "用户名或密码错误");
		}catch (AuthenticationException e) {
			res.put("code", 2002);
			res.put("errorMessage", "用户名或密码错误");
		}catch (Exception e) {
			e.printStackTrace();
			res.put("code", 2002);
			res.put("errorMessage", e.getMessage());
		}
		return res;
	}
	
	@RequestMapping(value = "sysIndex", method = RequestMethod.GET)
	public String sysIndex(){
		return "sysIndex";
	}
	
	
}
