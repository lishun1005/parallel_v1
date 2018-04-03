package com.rscloud.ipc.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ExceptionHandler implements  HandlerExceptionResolver {

	@Override
	public  ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("exception", e);
		//这里可根据不同异常引起类做不同处理方式，本例做不同返回页面。
		//String viewName = ClassUtils.getShortName(e.getClass());
		model.put("msg", "系统错误,请联系管理员");
		return new ModelAndView("unauthorized", model);
	}
}
