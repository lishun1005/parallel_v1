package com.rscloud.ipc.rpc.api.result;

import java.io.Serializable;
import java.util.List;

/**
 * @author lishun
 * @Description: 返回list
 * @date 2017/10/30
 */
public class ResultBeanList<T> implements Serializable {
	public String message = "";
	private List<T> resultData;
	public Integer code;

	public ResultBeanList() {
	}

	public ResultBeanList(List<T> resultData) {
		this.resultData = resultData;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public void setMessage(String message, Object... args) {
		this.message = String.format(message, args);
	}

	public String getMessage() {
		return message;
	}

	public void setResultData(List<T> resultData) {
		this.resultData = resultData;
	}

	public List<T> getResultData() {
		return this.resultData;
	}
}
