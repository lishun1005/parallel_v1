package com.rscloud.ipc.aop;

import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lishun
 * @Description: 控制器aop拦截
 * @date 2017/10/27
 */
public class ControllerAop {
	public static Logger logger = LoggerFactory.getLogger(ControllerAop.class);
	public Object handlerControllerMethod(ProceedingJoinPoint pjp) {
		ResultBean<?> result;
		try {
			result = (ResultBean<?>) pjp.proceed();
		} catch (Throwable e) {
			result = new ResultBean();
			result.setMessage(e.toString());
			result.setCode(ResultCode.FAILED);
			e.printStackTrace();
		}
		return result;
	}
}
