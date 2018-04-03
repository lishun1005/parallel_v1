package com.rscloud.ipc.rest;

import com.google.common.base.Objects;
import com.rscloud.ipc.redis.RedisClientTemplate;
import com.rscloud.ipc.rpc.api.dto.SysUserDto;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.SysRoleService;
import com.rscloud.ipc.rpc.api.service.SysUserService;
import com.rscloud.ipc.utils.gtdata.GtdataUtil;
import com.rsclouds.common.utils.PubFun;
import com.rsclouds.common.utils.StringTool;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authc.LockedAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.exceptions.JedisException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@Api(value="登录",description="登录")
public class LoginRest {
	public static Logger logger = LoggerFactory.getLogger(LoginRest.class);
	//@Autowired
	//private CasLogin casLogin;
	@Autowired
	@Lazy
	public RedisClientTemplate  redisClientTemplate;
	
	@Autowired
	@Lazy
	private SysUserService sysUserService;
	
	@Autowired
	@Lazy
	private SysRoleService sysRoleService;
	
	@Value("#{fixparamProperty[apiTimeout]}")
	protected Integer apiTimeout;
	@ApiOperation(value = "更新sign")
	@RequestMapping(value="/api/v3/updateSign",method = {RequestMethod.POST})
	@ResponseBody
	public Map<String,Object> updateSign(String sign,HttpServletRequest request,HttpServletResponse response) {
		Map<String, Object> result=new HashMap<String, Object>();
		try {
			Map<String, String> mapRedis = redisClientTemplate.hgetAll(sign);
			if(mapRedis == null || mapRedis.size() < 1){
				result.put("code", ResultCode.PARAMS_ERR);
				result.put("errorMessage", "请求api失败:sign 无效");
				return result;
			}
			redisClientTemplate.expire(sign, apiTimeout);
			result.put("sign", "更新成功");
			result.put("code", ResultCode.OK);
		}catch(JedisException jce){
			logger.info("redis error: {}",jce.getMessage());
			result.put("code", ResultCode.FAILED);
			result.put("errorMessage", "user authentication failed: system error");
		}catch(Exception e){
			result.put("code", ResultCode.FAILED);
			result.put("errorMessage", "user authentication failed: system error");
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * 
	* Description: restful 登录接口
	*  @param username
	*  @param password
	*  @param request
	*  @return 
	* @author lishun 
	* @date 2017年6月30日 
	* @return Map<String,Object>
	 */
	@RequestMapping(value="/api/v3/login",method = {RequestMethod.POST})
	@ResponseBody
	@ApiOperation(value = "用户登录")
	public Map<String,Object> login(String username,String password,HttpServletRequest request,HttpServletResponse response) {
		Map<String, Object> result=new HashMap<String, Object>();
		try {
			if(!StringTool.parasCheck(username,password)){
				result.put("errorMessage", "用户名或密码不能为空");
				result.put("code", ResultCode.PARAMS_ERR);
				return result;
			}
			Map<String, Object> gtdataRes = GtdataUtil.login(username, password);//gtdata登录
			if(!Objects.equal(1, gtdataRes.get("result"))){
				result.put("errorMessage", gtdataRes.get("message"));
				result.put("code", ResultCode.FAILED);
				return result;
			}
			GtdataUtil.groupUserLogout((String)gtdataRes.get("token"));
			
			String token = PubFun.MD5(UUID.randomUUID().toString());
			String sign = PubFun.MD5(token + request.getRemoteAddr());
			Map<String, String> mapRedis=new HashMap<String, String>();
			mapRedis.put("token", token);
			mapRedis.put("username", username);
			if(redisClientTemplate.exists(sign)){
				result.put("errorMessage", "用户验证失败");
				result.put("code", ResultCode.FAILED);
				return result;
			}
			isExistUser(username);
			redisClientTemplate.hmset(sign,mapRedis);
			redisClientTemplate.expire(sign, apiTimeout);
			result.put("sign", sign);
			result.put("code", ResultCode.OK);
		}catch(JedisException jce){
			logger.info("redis error: {}",jce.getMessage());
			result.put("code", ResultCode.FAILED);
			result.put("errorMessage", "user authentication failed: system error");
		}catch(Exception e){
			result.put("code", ResultCode.FAILED);
			result.put("errorMessage", "user authentication failed: system error");
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * 
	* Description: 判断集市用户是存在影像处理中心
	*  @param username 
	* @author lishun 
	* @date 2017年7月6日 
	* @return void
	 */
	private void isExistUser(String username){
		SysUserDto user = sysUserService.findByUsername(username,2);
		if(user == null){//初次登录，先添加用户到影像处理中心系统
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
	}
}
