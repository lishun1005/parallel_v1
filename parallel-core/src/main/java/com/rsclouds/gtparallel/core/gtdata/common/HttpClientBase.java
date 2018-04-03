package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class HttpClientBase {
	private static Logger logger = LoggerFactory.getLogger(HttpClientBase.class);
	
	public static boolean httpGetStatus(String url, String param){
        //post请求返回结果
        HttpClient httpClient = new DefaultHttpClient();
        url = url + "?" + param;
        HttpGet method = new HttpGet(url);
        try {
            HttpResponse result = httpClient.execute(method);
            /**请求发送成功**/
            if (result.getStatusLine().getStatusCode() == 200) {
                return true;
            }else {
            	return false;
            }
        } catch (IOException e) {
            logger.error("get请求提交失败:" + url, e);
        }
        return false;
    }
	
	public static int uploadFile(File file, String urlStr, Map<String, String>mapAtts) {
		HttpParams my_httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(my_httpParams, 600000);
	    HttpConnectionParams.setSoTimeout(my_httpParams, 600000);
		HttpClient httpclient = new DefaultHttpClient(my_httpParams);  
//		HttpClient httpclient = new DefaultHttpClient(); 
//		httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 600); 
//        httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
        try {     
            HttpPost httppost = new HttpPost(urlStr); 
            FileBody bin = new FileBody(file);    
            StringBody comment = new StringBody(file.getName());
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  

            reqEntity.addPart("geojson", bin);//file1为请求后台的File upload;属性      
            reqEntity.addPart("geojson", comment);//filename1为请求后台的普通参数;属性     
            Iterator<Entry<String, String>> iter = mapAtts.entrySet().iterator();
            while (iter.hasNext()){
            	Entry<String, String> e = iter.next();
            	String key = (String)e.getKey();
            	String value = (String)e.getValue();
            	System.out.println("[uploadFile]mapAtts key=" + key);
            	System.out.println("[uploadFile]mapAtts value=" + value);
            	StringBody commentDate = new StringBody(value, "text/plain", Charset.forName("UTF-8"));
            	reqEntity.addPart(key, commentDate);//filename1为请求后台的普通参数;属性
            }
            httppost.setEntity(reqEntity);
            System.out.println(System.currentTimeMillis());
            HttpResponse response = httpclient.execute(httppost);  
            System.out.println(System.currentTimeMillis());
            int statusCode = response.getStatusLine().getStatusCode();     
            if(statusCode == HttpStatus.SC_OK){              
                HttpEntity resEntity = response.getEntity();  
                EntityUtils.consume(resEntity);  
            }
            return statusCode;
        } catch (ParseException e) { 
        	System.out.println(System.currentTimeMillis());
        	e.printStackTrace();
        	return 0;
        } catch (IOException e) { 
        	System.out.println(System.currentTimeMillis());
        	e.printStackTrace(); 
        	return 0;
        } finally {   
        	try {       
        		httpclient.getConnectionManager().shutdown();   
        	} catch (Exception ignore) {          
            }  
        } 
	}
	
	
	
	
	
	public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
//            Map<String, List<String>> map = connection.getHeaderFields();
//            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            result = null;
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }
	
	public static void main(String[] args) {
//		String path = "/users/rscloudmart/data/PL/20150520/20150520_031057_0822_planet_ortho_29.57N_90.03E//20150520_031057_0822_planet_ortho_29.57N_90.03E.tar.gz";
//		if (path.endsWith(".tar.gz") && path.startsWith("/users/rscloudmart/data/PL")) {
//			System.out.println("true");
//		}
//		Map<String, String> param = new HashMap<String, String>();
//		param.put("date", "1466006400000");
//		param.put("res", "400");
//		File file = new File("C://Users//Administrator//Downloads//b436f1f4-f5bb-43d1-9542-69e8cb41e38f.geojson");
//		System.out.println(HttpClientBase.uploadFile(file, CoreConfig.OLEARTH_BLOCKINFO_REMOTE_UPDATE_SHPFILE_URL, param));
	}
}
