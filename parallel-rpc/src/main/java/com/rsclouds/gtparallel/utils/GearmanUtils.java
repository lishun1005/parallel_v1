package com.rsclouds.gtparallel.utils;

import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class GearmanUtils {

	private final static Logger logger = LoggerFactory.getLogger(GearmanUtils.class);

	public static Object obj= new Object();
	public static String jobServerList;
	@Value("#{appProperty[job_server_list]}")
    public void setGtGroupUser(String str) {
		GearmanUtils.jobServerList = str;
    }
	public static  GearmanClientImpl client = new GearmanClientImpl();
	
	public static void addJobServer(){
		String[] servers =jobServerList.split(",");
		for(String str : servers){
			int index = str.indexOf(":");
			if(index > 0 ){			
				String host = str.substring(0, index);
				int port = Integer.valueOf(str.substring(index+1));
				client.addJobServer(host,port);
			}		
		}	
	}
	public static void submit(GearmanJob job){
		try{
			synchronized (obj) {
				if(0 == client.getSetOfJobServers().size()){
					addJobServer();
				}
			}
			client.submit(job);
		}catch(Exception e){
			logger.error(e.getMessage(), e);;
		}		
	}
}
