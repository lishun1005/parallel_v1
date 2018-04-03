package com.rsclouds.gtparallel.geowebcache;

import java.io.Serializable;

public class GwcConfigBean implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String[] hosts;
	private String reloadURL;
	private String gwcAdmin;
	private String gwcPassword;
	private String confPath;
	private String username;
	private String password;
	private String publishUrl;
	private Integer port;
	
	public String[] getHosts() {
		return hosts;
	}



	public void setHosts(String[] hosts) {
		this.hosts = hosts;
	}



	public String getReloadURL() {
		return reloadURL;
	}



	public void setReloadURL(String reloadURL) {
		this.reloadURL = reloadURL;
	}



	public String getGwcAdmin() {
		return gwcAdmin;
	}



	public void setGwcAdmin(String gwcAdmin) {
		this.gwcAdmin = gwcAdmin;
	}



	public String getGwcPassword() {
		return gwcPassword;
	}



	public void setGwcPassword(String gwcPassword) {
		this.gwcPassword = gwcPassword;
	}



	public String getConfPath() {
		return confPath;
	}



	public void setConfPath(String confPath) {
		this.confPath = confPath;
	}



	public String getUsername() {
		return username;
	}



	public void setUsername(String username) {
		this.username = username;
	}



	public String getPassword() {
		return password;
	}



	public void setPassword(String password) {
		this.password = password;
	}

	

	public Integer getPort() {
		return port;
	}



	public void setPort(Integer port) {
		this.port = port;
	}
	

	public String getPublishUrl() {
		return publishUrl;
	}



	public void setPublishUrl(String publishUrl) {
		this.publishUrl = publishUrl;
	}




}
