package com.rsclouds.gtparallel.geowebcache;

import com.rsclouds.gtparallel.utils.SpringContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


@Service("gwcConfig")
public class GwcConfig {
	private final static Logger logger = LoggerFactory.getLogger(GwcConfig.class);

	public static GwcConfig instance;
	public static GwcConfig getInstance() {
		return instance;
	}
	private GwcConfig() {
		instance = this;//把instance指向spring初始化的实例中
	}
	
	private  GwcConfigBean[] gwcConfigList = null;
	private  Properties properties;
	public final String scpRemote2Local = "scp {username}@{host}:{remoteConfPath} {localConfPath}";
	public final String scpLocal2Remote = "scp {localConfPath} {username}@{host}:{remoteConfPath}";
	
	public boolean reload(){
		properties = (Properties)SpringContextUtils.getBean("appProperty");
		String cluster = properties.getProperty("cluster.gwc.names", "").trim();
		String[] clusterNames = cluster.split(",");
		List<GwcConfigBean> newConfigList = new ArrayList<GwcConfigBean>();
		if(clusterNames.length <=0){
			throw new IllegalArgumentException("集群名字列表不能为空.");
		}
		for(String name : clusterNames){
			newConfigList.add(properties2Bean(name));
		}
		gwcConfigList = newConfigList.toArray(new GwcConfigBean[newConfigList.size()]);
		return true;
	}
	
	public GwcConfigBean[] getGwcConfigList(){
		if(gwcConfigList == null){
			reload();
		}
		//这里可以考虑只返回副本
		return gwcConfigList;
	}
	
	
	public String getScpRemote2Local() {
		return scpRemote2Local;
	}
	
	public String getScpLocal2Remote() {
		return scpLocal2Remote;
	}

	private  GwcConfigBean properties2Bean(String clusterName){
		String hosts = properties.getProperty(clusterName+".hosts", "").trim();
		String reloadUrl = properties.getProperty(clusterName+".reload.url", "").trim();
		String gwcAdmin = properties.getProperty(clusterName+".gwc.admin", "").trim();
		String gwcPassword = properties.getProperty(clusterName+".gwc.password", "").trim();
		String username = properties.getProperty(clusterName+".username", "").trim();
		String password = properties.getProperty(clusterName+".password", "").trim();
		String confPath = properties.getProperty(clusterName+".conf.path", "").trim();
		String port = properties.getProperty(clusterName+".port", "").trim();
		String publishUrl = properties.getProperty(clusterName+".publish.url", "").trim();
		GwcConfigBean bean = new GwcConfigBean();
		bean.setHosts(hosts.split(","));
		bean.setReloadURL(reloadUrl);
		bean.setGwcAdmin(gwcAdmin);
		bean.setGwcPassword(gwcPassword);
		bean.setUsername(username);
		bean.setPassword(password);
		bean.setConfPath(confPath);
		bean.setPort(Integer.parseInt(port));
		bean.setPublishUrl(publishUrl);
		return bean;
	}
}
