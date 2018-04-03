package com.rscloud.ipc.contrller;


import com.rscloud.ipc.redis.RedisClientTemplate;
import com.rscloud.ipc.rpc.api.dto.SysMenuDto;
import com.rscloud.ipc.rpc.api.service.SysMenuService;
import com.rscloud.ipc.rpc.api.service.SysUserService;
import com.rscloud.ipc.shiro.UserRealm;
import com.rsclouds.common.utils.PubFun;
import com.rsclouds.common.utils.StringTool;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.ModelAttribute;
import redis.clients.jedis.exceptions.JedisException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class BaseContrller implements Serializable {
	public static Logger logger = LoggerFactory.getLogger(BaseContrller.class);

	private static final long serialVersionUID = 6688777670610210538L;

	private HttpServletRequest request;
	private HttpServletResponse response;
	private HttpSession session;
	private ServletContext sc;
	private ApplicationContext applicationContext;
	@Autowired
	@Lazy
	public SysMenuService sysMenuService;

	@Autowired
	@Lazy
	public RedisClientTemplate redisClientTemplate;

	@Autowired
	@Lazy
	protected SysUserService sysUserService;

	@ModelAttribute
	public void setReqAndRes(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		if (request != null)
			this.session = request.getSession();
		if (session != null)
			this.sc = session.getServletContext();
	}

	public Map<String, Object> checkApi(String sign) {
		Map<String, Object> res = new HashMap<String, Object>();
		try {
			if (org.apache.commons.lang3.StringUtils.isBlank(sign)) {
				res.put("code", "2002");
				res.put("errorMessage", "请求api失败:sign is null");
				return res;
			}
			Map<String, String> mapRedis = redisClientTemplate.hgetAll(sign);
			if (mapRedis == null || mapRedis.size() < 1) {
				res.put("code", "2002");
				res.put("errorMessage", "请求api失败:sign 无效");
				return res;
			}
			String username = mapRedis.get("username");
			String token = mapRedis.get("token");

			String checkSign = PubFun.MD5(token + request.getRemoteAddr());
			if (sign.equals(checkSign)) {
				if (isPermissions(username)) {//判断一张图切片权限
					res.put("code", "2001");
					res.put("username", username);
				} else {
					res.put("code", "2002");
					res.put("errorMessage", "请求api失败:该用户[" + username + "]没有权限");
					response.setStatus(500);
				}
			} else {
				res.put("code", "2002");
				res.put("errorMessage", "请求api失败:sign 无效");
				response.setStatus(500);
			}
		} catch (JedisException jce) {
			logger.info("redis异常");
			res.put("code", "2002");
			res.put("errorMessage", "请求api失败: 系统错误");
			jce.printStackTrace();
		}
		return res;
	}

	/**
	 * Description: 提供给api权限认证
	 *
	 * @param username 用户名
	 * @param menu：api 对应的权限
	 * @return boolean
	 * @author lishun
	 * @date 2017年7月6日
	 */
	public boolean isPermissions(String username) {
		String uri = org.apache.commons.lang3.StringUtils.strip(request.getServletPath(), "//");
		uri = StringTool.str2repeat(uri, '/');
		List<SysMenuDto> menu = sysMenuService.queryByUrl(uri);//判断当前接口权限
		if (menu.size() < 1) {
			logger.info("{} 系统没有添加该api权限", uri);
		}
		Set<String> permissions = sysUserService.findPermissionsByUsername(username, 2);
		for (String permission : permissions) {
			for (SysMenuDto m : menu) {
				if (m.getPermission().equals(permission)) {
					return true;
				}
			}
		}
		return false;
	}

	public UserRealm getUserRealm() {
		RealmSecurityManager securityManager = (RealmSecurityManager) SecurityUtils.getSecurityManager();
		return (UserRealm) securityManager.getRealms().iterator().next();
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public ServletContext getSc() {
		return sc;
	}

	public void setSc(ServletContext sc) {
		this.sc = sc;
	}

	public HttpSession getSession() {
		return session;
	}

	public void setSession(HttpSession session) {
		this.session = session;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
}
