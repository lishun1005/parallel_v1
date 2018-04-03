package com.rsclouds.gtparallel.dto;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/2
 */
public class CutConfig {
	public String cutHost;
	public Integer cutPort;
	public String cutUserName;
	public String cutPassword;
	public String appJarPath;

	public String getCutHost() {
		return cutHost;
	}

	public void setCutHost(String cutHost) {
		this.cutHost = cutHost;
	}

	public Integer getCutPort() {
		return cutPort;
	}

	public void setCutPort(Integer cutPort) {
		this.cutPort = cutPort;
	}

	public String getCutUserName() {
		return cutUserName;
	}

	public void setCutUserName(String cutUserName) {
		this.cutUserName = cutUserName;
	}

	public String getCutPassword() {
		return cutPassword;
	}

	public void setCutPassword(String cutPassword) {
		this.cutPassword = cutPassword;
	}

	public String getAppJarPath() {
		return appJarPath;
	}

	public void setAppJarPath(String appJarPath) {
		this.appJarPath = appJarPath;
	}
}
