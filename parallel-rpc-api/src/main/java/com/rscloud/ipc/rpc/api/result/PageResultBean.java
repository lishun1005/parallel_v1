package com.rscloud.ipc.rpc.api.result;

import java.io.Serializable;
import java.util.List;

/**
 * @author lishun
 * @Description: TODO
 * @date 2017/10/24
 */
public class PageResultBean<T> implements Serializable {
	public Integer code;
	private String message = "";
	private List<T> resultData;
	private long totalPage;
	private int pages;
	private int pageNum;
	public PageResultBean(){}
	public PageResultBean(List<T> resultData, long totalPage, int pages, int pageNum){
		this.resultData = resultData;
		this.totalPage = totalPage;
		this.pages = pages;
		this.pageNum = pageNum;
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
	public void  setResultData(List<T> resultData){
		this.resultData = resultData;
	}
	public  List<T> getResultData(){return this.resultData; }
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

}
