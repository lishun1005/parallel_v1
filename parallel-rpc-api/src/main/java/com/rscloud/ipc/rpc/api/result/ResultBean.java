package com.rscloud.ipc.rpc.api.result;

import java.io.Serializable;
import java.util.List;

/**
 * @author lishun
 * @Description: TODO
 * @date 2017/10/24
 */
public class ResultBean<T> implements Serializable {

	public String message = "";
	public Integer code;
	private T resultData;

	private long totalPage;
	private int pages;
	private int pageNum;
	private List<T> resultDataList;

	public ResultBean() {
	}

	public ResultBean(List<T> resultData, long totalPage, int pages, int pageNum) {
		this.resultDataList = resultData;
		this.totalPage = totalPage;
		this.pages = pages;
		this.pageNum = pageNum;
	}

	public ResultBean(T resultData) {
		this.resultData = resultData;
	}


	public void setMessage(String message) {
		this.message = message;
	}

	public long getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(long totalPage) {
		this.totalPage = totalPage;
	}

	public int getPages() {
		return pages;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}


	public List<T> getResultDataList() {
		return resultDataList;
	}

	public void setResultDataList(List<T> resultDataList) {
		this.resultDataList = resultDataList;
	}


	public void setMessage(String message, Object... args) {
		this.message = String.format(message, args);
	}

	public String getMessage() {
		return message;
	}

	public void setResultData(T resultData) {
		this.resultData = resultData;
	}

	public T getResultData() {
		return this.resultData;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public <T> void setResultBean(Integer code, String message,
	                              Object... mesaageFormatArgs) {
		setCode(code);
		setMessage(message, mesaageFormatArgs);
	}
}
