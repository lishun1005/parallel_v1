package com.rsclouds.gtparallel.core.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class HttpTookit {
	private static final Log log = LogFactory.getLog(HttpTookit.class);
    /** 
     * 执行一个HTTP GET请求，返回请求响应的HTML 
     * 
     * @param url                 请求的URL地址 
     * @param queryString 请求的查询参数,可以为null 
     * @return 返回请求响应的HTML 
     */ 
	public static String doGet(String url, String queryString) {
		String response = null;
		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);
		try {
			if (StringUtils.isNotBlank(queryString))
				method.setQueryString(URIUtil.encodeQuery(queryString));
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				//response = method.getResponseBodyAsString();
				InputStream inputStream = method.getResponseBodyAsStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						inputStream));
				StringBuffer stringBuffer = new StringBuffer();
				String str = "";
				while ((str = br.readLine()) != null) {
					stringBuffer.append(str);
				}
				response = stringBuffer.toString();
			}
		} catch (URIException e) {
			log.error("执行HTTP Get请求时，编码查询字符串“" + queryString + "”发生异常！", e);
		} catch (IOException e) {
			log.error("执行HTTP Get请求" + url + "时，发生异常！", e);
		} finally {
			method.releaseConnection();
		}
		return response;
	}
	

	/**
	 * 
	 * @param url
	 * @param queryString
	 * @return
	 *    Map<String,Object>={"HttpStatus":200,"response":"xxxxx"}
	 */
	public static Map<String,Object> doGet2Map(String url, String queryString) {
		Map<String,Object> response = new HashMap<String,Object>();
		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);
		try {
			if (StringUtils.isNotBlank(queryString))
				method.setQueryString(URIUtil.encodeQuery(queryString));
			client.executeMethod(method);
			response.put("HttpStatus", method.getStatusCode());
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				//response = method.getResponseBodyAsString();
				InputStream inputStream = method.getResponseBodyAsStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						inputStream));
				StringBuffer stringBuffer = new StringBuffer();
				String str = "";
				while ((str = br.readLine()) != null) {
					stringBuffer.append(str);
				}
				response.put("response", stringBuffer.toString());
				inputStream.close();
			}
		} catch (URIException e) {
			log.error("执行HTTP Get请求时，编码查询字符串“" + queryString + "”发生异常！", e);
		} catch (IOException e) {
			log.error("执行HTTP Get请求" + url + "时，发生异常！", e);
		} finally {
			method.releaseConnection();
		}
		return response;
	}

    

    public static void main(String[] args) {
            String x = doGet("http://192.168.101.51:8080/DatamanagementSys/api/v1/cover//85020d72-cef1-4e5b-96ad-01af46e31454/success","coverage="+0.8);
            System.out.println(x); 
    } 
}


/*
*//** 
 * 执行一个HTTP POST请求，返回请求响应的HTML 
 * 
 * @param url        请求的URL地址 
 * @param params 请求的查询参数,可以为null 
 * @return 返回请求响应的HTML 
 *//* 
public static String doPost(String url, Map<String, String> params) {
        String response = null; 
        HttpClient client = new HttpClient(); 
        PostMethod method = new PostMethod(url);
        //设置Http Post数据 
        if (params != null) { 
                HttpMethodParams p = new HttpMethodParams(); 
                for (Map.Entry<String, String> entry : params.entrySet()) { 
                        p.setParameter(entry.getKey(), entry.getValue()); 
                } 
                method.setParams(p); 
        }                     
        method.setRequestHeader("Cookie", "JSESSIONID=E9F335710ACCCFD326C163ADDE570A53.app161; cScheme=http%3A; JSESSIONID2=b7167064-5cf0-416e-8218-aa948b9ee17b; cid=180302; counter=%5B2460907%2C1%5D; lang=zh_CN; TOURL=http%3A%2F%2Fweb.jingoal.com%2Fmodule%2Fweb%2Fmail%2FmgtMailIndex.do; route=bee2816bf82214e8c30b6a80aeed9b78");
//        method.setRequestHeader("Host", "web.jingoal.com");
//        method.setRequestHeader("Origin", "http://web.jingoal.com");
//        method.setRequestHeader("Referer", "http://web.jingoal.com/Apps/Attendance.jsp?locale=zh_CN&place=MyAttendance");
//        method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.");
//        method.setRequestHeader("X-GWT-Module-Base","http://web.jingoal.com/Apps/Attend/");
        method.setRequestHeader("X-GWT-Permutation","7EA0EF7014ADD1369DF5C52B955BF86A");
//        method.setRequestHeader("Accept-Language","zh-CN,zh;q=0.8");
//        method.setRequestHeader("Connection","keep-alive");
        method.setRequestHeader("Content-Type","text/x-gwt-rpc; charset=UTF-8");
        method.setRequestBody("7|0|6|http://web.jingoal.com/Apps/Attend/|C8A0B08F4BD54548BC3DFFBC7E7DD1F2|com.jingoal.mgt3.attendance.client.remote.AttendanceService|getUserAttendance|I|J|1|2|3|4|3|5|5|6|-1|-1|A|");
        try { 
                client.executeMethod(method); 
//                if (method.getStatusCode() == HttpStatus.SC_OK) { 
//                	response = method.getResponseBodyAsString();
    				InputStream inputStream = method.getResponseBodyAsStream();
    				BufferedReader br = new BufferedReader(new InputStreamReader(
    						inputStream));
    				StringBuffer stringBuffer = new StringBuffer();
    				String str = "";
    				while ((str = br.readLine()) != null) {
    					stringBuffer.append(str);
    				}
    				response = stringBuffer.toString();
    				System.out.println(response);
//                } else{
//                	System.out.println("code : " + method.getStatusCode()); 
//                }
        } catch (IOException e) { 
                log.error("执行HTTP Post请求" + url + "时，发生异常！", e); 
        } finally { 
                method.releaseConnection(); 
        } 

        return response; 
} 

*//** 
 * 执行一个HTTP Put请求，返回请求响应的HTML 
 * 
 * @param url        请求的URL地址 
 * @param params 请求的查询参数,可以为null 
 * @return 返回请求响应的HTML 
 *//* 
public static String doPut(String url, Map<String, String> params) {
        String response = null; 
        HttpClient client = new HttpClient(); 
        PutMethod method = new PutMethod(url); 
        //设置Http put数据 
        if (params != null) { 
                HttpMethodParams p = new HttpMethodParams(); 
                for (Map.Entry<String, String> entry : params.entrySet()) { 
                        p.setParameter(entry.getKey(), entry.getValue()); 
                } 
                method.setParams(p); 
        } 
        try { 
     
                client.executeMethod(method); 
//                if (method.getStatusCode() == HttpStatus.SC_OK) { 
                	//response = method.getResponseBodyAsString();
    				InputStream inputStream = method.getResponseBodyAsStream();
    				BufferedReader br = new BufferedReader(new InputStreamReader(
    						inputStream));
    				StringBuffer stringBuffer = new StringBuffer();
    				String str = "";
    				while ((str = br.readLine()) != null) {
    					stringBuffer.append(str);
    				}
    				response = stringBuffer.toString();
//                } 
        } catch (IOException e) { 
                log.error("执行HTTP put请求" + url + "时，发生异常！", e); 
        } finally { 
                method.releaseConnection(); 
        } 

        return response; 
} */