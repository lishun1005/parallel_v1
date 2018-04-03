package com.rsclouds.gtparallel.geowebcache;


import com.rsclouds.common.utils.HttpClientUtils;
import com.rsclouds.common.utils.ShellExecutor;
import com.rsclouds.common.utils.StringTool;
import com.rsclouds.gtparallel.core.gtdata.common.Dom4JForMapPublishing;
import com.rsclouds.gtparallel.core.gtdata.common.FileOperate;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


@Service("gwcService")
public class GwcService {
	
	private final static Logger logger = LoggerFactory.getLogger(GwcService.class);

	public final static String TEMP_FILE_PATH = "/home/yarn/temp";

	@Autowired
	@Qualifier("gwcConfig")
	private GwcConfig gwcConfig;
	
	@Autowired
	@Qualifier("Dom4JForMapPublishing")
	Dom4JForMapPublishing dom4JForMapPublishing;
	
	
	public boolean scpRemote2Local(String username,String remoteConfPath,String localConfPath,String... hosts){
		return execScpCommand(gwcConfig.getScpRemote2Local(), username, remoteConfPath, localConfPath, hosts);
	}
	
	public boolean scpLocal2Remote(String username,String remoteConfPath,String localConfPath,String... hosts){
		return execScpCommand(gwcConfig.getScpLocal2Remote(), username, remoteConfPath, localConfPath, hosts);
	}
	
	private boolean execScpCommand(String scpCommand,String username,String remoteConfPath,String localConfPath,String... hosts){
		File tempFile = new File(localConfPath);
		if(!tempFile.getParentFile().exists()){
			tempFile.getParentFile().mkdirs();
		}
		boolean flag = false;
		if(hosts == null){
			return flag;
		}
		try {
			String commandDemo = scpCommand;
			commandDemo = commandDemo.replace("{username}", username)
						.replace("{remoteConfPath}", remoteConfPath)
						.replace("{localConfPath}", localConfPath);
			for(String remoteHost : hosts){
				String command = commandDemo.replace("{host}", remoteHost);
				logger.info(command);
				int status = ShellExecutor.execLocal(command);
				if(status == 0){
					return true;
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);;
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);;
		}finally{
		}
		return flag;
	}
	
	
	
	public boolean sendReloadRequest(String reloadUrl,String username,String password){
		boolean flag = false; 
        try { 
			HttpClientUtils client = new HttpClientUtils(reloadUrl);
			Map<String,String> parms = new HashMap<String, String>();
			parms.put("reload_configuration", "1");
			Map<String,String> result = client.executeAuthSchemes2String(parms, HttpClientUtils.methodType.POST,username,password);
			if(!"200".equals(result.get("HttpStatus"))){
				flag=false;
				logger.info("reload failed {}",result);
			}else{
				String message = result.get("result");
				if(message.indexOf("There was a problem reloading the configuration:")!=-1){//geowebcache返回状态码200，可能也是重载配置文件失败
					logger.info("reload failed {}",result);
					flag = false;
				}else{
					flag = true;
				}
			}
        }catch (Exception e) {
        	logger.error(e.getMessage(), e);;
        } 
		return flag;
	}
	public String getGeoRange(String imagePath, String cdiPath){
		double[] range = new double[4];
		List<String> filePaths = new ArrayList<String>();
		try {
			FileOperate.listFiles(imagePath, filePaths);
			Dom4JForMapPublishing.getImageRange(cdiPath, filePaths, range);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);;
		}
		
		return Arrays.toString(range);
	}
	/**
	 * 发布地图
	 * @param bean geowebcacge集群参数实体
	 * @param mapname 地图名
	 * @param xmlPath 瓦片xml配置文件参数
	 * @param alllayersPath 瓦片存储路径
	 * @param cutType 切片类型 ：更新图层(1)不需要修改geowebcache.xml
	 * @return
	 *  	Map<String,String> = {"result" = "true/false" , "message" = "....."}
	 */
	public Map<String, String> mapPublish(GwcConfigBean bean, String mapname,
			String xmlPath, String alllayersPath,Integer cutType) {
		Map<String, String> map = new HashMap<String, String>();
		String msg = "";
		boolean flag = false;
		File tempFile = new File(TEMP_FILE_PATH, StringTool.getUUID() + "_geowebcache.xml");
		File originalFile = new File(TEMP_FILE_PATH,  StringTool.getUUID() + "_orig_geowebcache.xml");
		if(!tempFile.getParentFile().exists()){
			tempFile.getParentFile().mkdir();
		}
		try {
			if (bean == null) {
				msg = "geowebcache集群参数异常";
				return map;
			}
			String[] hosts = bean.getHosts();// 拷贝远程geowebcache.xml到本地
			if(cutType==0){
				if (hosts == null || hosts.length < 1) {
					msg = "geowebcache集群hosts参数不能为空";
					return map;
				}
				for (String host : hosts) {
					flag = scpRemote2Local(bean.getUsername(), bean.getConfPath(),tempFile.getPath(), host);
					if (flag) {
						scpRemote2Local(bean.getUsername(), bean.getConfPath(),originalFile.getPath(), host);//防止修改geowebcache.xml出错，再把原文件覆盖远程的geowebcache.xml
						break;
					} else {
						logger.error("获取远程机器配置文件失败:" + host);
					}
				}
				if (!flag) {
					msg = "获取远程机器配置文件失败";
					return map;
				}
				if (dom4JForMapPublishing.isMapNameExist(mapname,tempFile.getPath())) {
					msg = "地图名字已存在";
					return map;
				}
				if (!dom4JForMapPublishing.addGeowebcacheXML(mapname, xmlPath,alllayersPath, tempFile.getPath())) {
					msg = "修改geowebcache.xml文件失败";
					return map;
				}
				boolean scpResult = false;
				formatXMLFile(tempFile.getPath());//格式化xml
				for (String host : hosts) {// 拷贝本地修改后的xml文件到远程的多台geowebcache节点
					if (scpLocal2Remote(bean.getUsername(), bean.getConfPath(),tempFile.getPath(), host)) {
						scpResult = true;
					} else {
						logger.error("发送配置文件到远程机器失败:" + host);
					}
				}
				if (!scpResult) {
					msg = "发送配置文件到远程机器失败";// 发送配置信息到各个节点都失败
					flag = false;
					return map;
				}
			}
			String command = bean.getReloadURL().replace("{port}",bean.getPort() + "");// reload各节点的配置文件
			boolean reloadResult = false;
			for (String host : hosts) {
				if (sendReloadRequest(command.replace("{host}", host),bean.getGwcAdmin(), bean.getGwcPassword())) {
					reloadResult = true;
				} else {
					if(cutType==0){
						scpLocal2Remote(bean.getUsername(), bean.getConfPath(),originalFile.getPath(), host);//恢复原先配置文件
					}
					logger.error("重载各节点的配置文件失败:" + host);
				}
			}
			if (!reloadResult) {
				msg = "重载各节点的配置失败";// 重载各节点的配置都失败
				flag = false;
				return map;
			}
			flag = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);;
			logger.error("mapPublish ERROR", e);
			flag = false;
		} finally {
			if (flag) {
				msg = "地图发布成功";
			} else {
				msg = StringUtils.isEmpty(msg) == true ? "地图发布失败" : msg;
			}
			tempFile.delete();
			originalFile.delete();
			map.put("result", flag + "");
			map.put("message", msg);
		}
		return map;
	}
	public  int formatXMLFile(String filename) {  
		int returnValue = 0;  
		try {  
			SAXReader saxReader = new SAXReader();  
			Document document = saxReader.read(new File(filename));  
			XMLWriter writer = null;  
			OutputFormat format = OutputFormat.createPrettyPrint();   /** 格式化输出,类型IE浏览一样 */  
			format.setEncoding("UTF-8");  /** 指定XML编码 */  
			writer = new XMLWriter(new FileWriter(new File(filename)), format);  
			writer.write(document);  
			writer.close();  
			returnValue = 1;    /** 执行成功,需返回1 */  
		} catch (Exception ex) {  
			ex.printStackTrace();  
		}  
		return returnValue;  
	}  


}
