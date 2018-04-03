package com.rscloud.ipc.shiro;


import com.google.common.base.Objects;
import com.rscloud.ipc.dto.SysUserShiroDto;
import com.rscloud.ipc.rpc.api.dto.SysUserDto;
import com.rscloud.ipc.rpc.api.service.SysRoleService;
import com.rscloud.ipc.rpc.api.service.SysUserService;
import com.rscloud.ipc.utils.gtdata.GtdataUtil;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * SysUser: xiangsx
 * <p>
 * Date: 2015-03-17
 * <p>
 * Version: 1.0
 */
public class UserRealm extends AuthorizingRealm {

	@Autowired
	@Lazy
	private SysUserService sysUserService;

	@Autowired
	@Lazy

	private SysRoleService sysRoleService;
	@Autowired
	@Lazy

	private CacheManager cacheManager;

	/*@Autowired
	private CasLogin casLogin;*/

	protected final Logger logger = LoggerFactory.getLogger(UserRealm.class);

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		SysUserShiroDto sysUserShiroDto = ((SysUserShiroDto)principals.getPrimaryPrincipal());

		SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
		Set<String> userRole = sysRoleService.findRoles(sysUserShiroDto.getUsername(),sysUserShiroDto.getUserType());
		authorizationInfo.setRoles(userRole);
		authorizationInfo.setStringPermissions(sysUserService
				.findPermissionsByUsername(sysUserShiroDto.getUsername(),sysUserShiroDto.getUserType()));
		return authorizationInfo;
	}
	@Override
	protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
		SysUserShiroDto sysUserShiroDto=(SysUserShiroDto)principals.getPrimaryPrincipal();
		return sysUserShiroDto.getUsername() + "-" +  sysUserShiroDto.getUserType();//配合redis缓存使用，key值为"缓存类型-用户名-用户类型"
	};

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		SimpleAuthenticationInfo authenticationInfo=null;
		MyUsernamePasswordToken mytoken=(MyUsernamePasswordToken)token;
		String username=mytoken.getUsername();
		Integer userType=mytoken.getUserType();
		String password=new String((char[])mytoken.getCredentials());
		SysUserShiroDto sysUserShiroDto=null;
		if(userType==1){
			SysUserDto user = sysUserService.findByUsername(username,1);
			if(user==null){
				throw new AuthenticationException();
			}else{
				if(user.getLocked() == null || user.getLocked()){
					throw new LockedAccountException();
				}
				sysUserShiroDto= BeanMapper.map(user, SysUserShiroDto.class);
				authenticationInfo=new SimpleAuthenticationInfo(sysUserShiroDto, user.getPassword(), getName());
			}
		}else{//集市用户登录
			Map<String, Object> gtdataRes = GtdataUtil.login(username, password);//gtdata登录
			if(Objects.equal(1, gtdataRes.get("result"))){
				SysUserDto user = sysUserService.findByUsername(username,2);
				if(user == null){//初次登录，先添加用户到影像处理中心系统，然后赋予该用户public_role角色
					user = new SysUserDto();
					user.setId(StringTool.getUUID());
					user.setUsername(username);
					user.setUserType(2);
					sysUserService.addUser(user);
				}else{
					if(user.getLocked()){
						throw new LockedAccountException();
					}
				}
				sysUserShiroDto= BeanMapper.map(user, SysUserShiroDto.class);
				authenticationInfo=new SimpleAuthenticationInfo(sysUserShiroDto, token.getCredentials(), getName());
			}else{
				throw new AuthenticationException();
			}
		}


		return authenticationInfo;
	}

	@Override
	public void clearCachedAuthorizationInfo(PrincipalCollection principals) {
		super.clearCachedAuthorizationInfo(principals);
	}

	@Override
	public void clearCachedAuthenticationInfo(PrincipalCollection principals) {
		super.clearCachedAuthenticationInfo(principals);
	}

	@Override
	public void clearCache(PrincipalCollection principals) {
		super.clearCache(principals);
	}

	public void clearAllCachedAuthorizationInfo() {
		getAuthorizationCache().clear();
	}

	public void clearAllCachedAuthenticationInfo() {
		getAuthenticationCache().clear();
	}

	public void clearAllCache() {
		clearAllCachedAuthenticationInfo();
		clearAllCachedAuthorizationInfo();
	}
	@Override
	protected void assertCredentialsMatch(AuthenticationToken token,
			AuthenticationInfo info) throws AuthenticationException {
		MyUsernamePasswordToken mytoken=(MyUsernamePasswordToken)token;
		SysUserShiroDto sysUserShiroDto=(SysUserShiroDto)info.getPrincipals().getPrimaryPrincipal();
		if(sysUserShiroDto.getUserType().intValue() != mytoken.getUserType().intValue()){
			 throw new IncorrectCredentialsException("用户名或密码错误");
		}
		super.assertCredentialsMatch(token, info);
	}
	@Override
	protected Object getAuthenticationCacheKey(AuthenticationToken token) {
		return token != null ?token.getPrincipal() +"-"+ ((MyUsernamePasswordToken)token).getUserType():null;
	}
	

}
