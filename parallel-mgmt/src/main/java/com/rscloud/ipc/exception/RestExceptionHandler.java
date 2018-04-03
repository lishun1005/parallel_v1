package com.rscloud.ipc.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler{

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		ResultBean<?> result = new ResultBean();
		BindingResult bindResult = null;
		if(ex instanceof BindException){
			bindResult = ((BindException) ex).getBindingResult();
		}
		if(ex instanceof MethodArgumentNotValidException){
			bindResult = ((MethodArgumentNotValidException) ex).getBindingResult();
		}
		if(bindResult != null){
			List<ObjectError> listError = bindResult.getAllErrors();
			for (ObjectError oe : listError) {
				result.setMessage(oe.getDefaultMessage());
				result.setCode(ResultCode.PARAMS_ERR);
				return new ResponseEntity<Object>(result, headers, HttpStatus.OK);//状态码 200
			}
		}
		result.setMessage(ex.getMessage());
		result.setCode(ResultCode.FAILED);
		return new ResponseEntity<Object>(result, headers, status);

	}
}
