package com.rsclouds.gtparallel.core.download.mr;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsclouds.gtparallel.core.common.CoreConfig;

public class DownloadUtils {
	
	private final static Log log = LogFactory.getLog(DownloadUtils.class.getName());
	private static String pushProgressUrl = CoreConfig.URL_PUSH_PROGRESS;
	public static int pushProgress(String jobid,String progress,String state) throws IOException{
		String urlString = pushProgressUrl.replace("{jobid}", jobid).replace("{progress}", progress);
		log.info("发送请求:" + urlString);
		if(state !=null){
			urlString = urlString +"&state="+state;
		}
		URL url = new URL(urlString);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setRequestMethod("PUT");
		int responseCode = httpCon.getResponseCode();
		if (responseCode == 200) {
			// 打印log日志
			log.info("写入进度成功");
		}else{
			log.info("写入进度失败");
		}
		return  responseCode;		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
