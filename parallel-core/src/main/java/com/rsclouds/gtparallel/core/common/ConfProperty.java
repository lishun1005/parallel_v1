package com.rsclouds.gtparallel.core.common;

import java.io.InputStream;
import java.util.Properties;

/**
 * 单例，读取config.properties配置文件
 * @author root
 *
 */
public class ConfProperty {
	private Properties properties;
	private static ConfProperty instance = null;
	private static volatile Object obj = new Object();
	private static final String resourceName = "config.properties";

	/**
	 * singleton
	 * 
	 * @return
	 */
	public static ConfProperty getInstance() {
		synchronized (obj) {
			if (instance == null) {
				instance = new ConfProperty();
			}
		}
		return instance;
	}

	public ConfProperty() {
		properties = new Properties();
		try {
			InputStream fis = this.getClass().getClassLoader().getResourceAsStream(resourceName);
			if(fis == null){
				System.out.println(" can not find " + resourceName);
			}
			properties.load(fis);
		} catch (Exception e) {
			System.out.println(" can not find " + resourceName);
			e.printStackTrace();
		}
	}

	/**
	 * 返回 int值
	 * 
	 * @param key
	 * @return
	 */
	public Integer getIntValue(String key) {
		String val = properties.getProperty(key, "0");
		return Integer.valueOf(val);
	}
	
	public Long getLongValue(String key) {
		String val = properties.getProperty(key, "0");
		return Long.valueOf(val);
	}

	/**
	 * 返回字符串
	 * 
	 * @param key
	 * @return
	 */
	public String getStringValue(String key) {
		return properties.getProperty(key, "").trim();
	}

	public static void main(String[] args) {
		System.out.println("redis.host = ["
				+ ConfProperty.getInstance().getStringValue("redis.host")
				+ "]");
	}

}
