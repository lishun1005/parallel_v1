package com.rsclouds.common.utils;


import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class HttpClientUtils {
	private String apiURL = "";
	private Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);
	private  AbstractHttpClient httpClient = null;

	public static enum methodType {
		DELETE,POST,GET}
	;

	public HttpClientUtils(String uri) {
		if (uri != null && !"".equals(uri)) {
			httpClient = new DefaultHttpClient();
			this.apiURL=uri;
		}else{
			logger.info("uri is empty");
		}
	}
	public HttpResponse execute(Map<String,String> parms, methodType methodType){
		HttpResponse response=null;
		if(parms!=null){
			try {

				if(methodType==methodType.DELETE){
					response=httpClient.execute(new HttpDelete(apiURL));
				}else if(methodType==methodType.POST){
					List<NameValuePair> params=new ArrayList<NameValuePair>();
					for(String key : parms.keySet()){
		            	params.add(new BasicNameValuePair(key,parms.get(key)));
		            }
					HttpPost httpPost = new HttpPost(apiURL);
					httpPost.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
					response=httpClient.execute(httpPost);
				}else{
					response=httpClient.execute(new HttpGet(apiURL));
				}

			} catch (Exception e) {
				logger.error(e.getMessage(), e);;
			}
		}else{
			logger.info("parms is null ");
		}
		return response;
	}
	public Map<String,String> execute2String(Map<String,Object> parms, methodType methodType){
		Map<String,String> result=new HashMap<String, String>();
		try {
			HttpResponse response=null;

			if(methodType == methodType.DELETE){
				response=httpClient.execute(new HttpDelete(apiURL));
			}else if(methodType == methodType.POST){
				HttpPost httpPost = new HttpPost(apiURL);
				List<NameValuePair> params=new ArrayList<NameValuePair>();
				for(String key : parms.keySet()){
					if(parms.get(key)!=null){
						params.add(new BasicNameValuePair(key,String.valueOf(parms.get(key))));
					}
	            }
				httpPost.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
				response=httpClient.execute(httpPost);
			}else{
				response=httpClient.execute(new HttpGet(apiURL));
			}
			result.put("HttpStatus", String.valueOf(response.getStatusLine().getStatusCode()));
			result.put("result", inputStream2String(response.getEntity().getContent()));
		} catch (Exception e) {
			result.put("HttpStatus", "500");
			result.put("result", e.getMessage());
			logger.error(e.getMessage(), e);;
		}
		return result;
	}
	public Map<String,String> executeAuthSchemes2String(Map<String,String> parms,methodType methodType,
			String userName,String password){
		Map<String,String> result=new HashMap<String, String>();
		try {
			URL urls=new URL(apiURL);
			HttpResponse response=null;
			Credentials creds = new UsernamePasswordCredentials(userName,password);

			httpClient.getCredentialsProvider().setCredentials(new AuthScope(urls.getHost(), urls.getPort()), (Credentials) creds);

			httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, Collections.singleton(AuthPolicy.DIGEST));
			httpClient.getAuthSchemes().register(AuthPolicy.DIGEST, new DigestSchemeFactory());

			if(methodType==methodType.DELETE){
				response=httpClient.execute(new HttpDelete(apiURL));
			}else if(methodType==methodType.POST){
				HttpPost httpPost = new HttpPost(apiURL);
				List<NameValuePair> params=new ArrayList<NameValuePair>();
				for(String key : parms.keySet()){
					if(parms.get(key)!=null){
						params.add(new BasicNameValuePair(key,String.valueOf(parms.get(key))));
					}
	            }
				httpPost.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
				response=httpClient.execute(httpPost);
			}else{
				response=httpClient.execute(new HttpGet(apiURL));
			}
			result.put("HttpStatus", String.valueOf(response.getStatusLine().getStatusCode()));
			result.put("result", inputStream2String(response.getEntity().getContent()));
		} catch (Exception e) {
			result.put("HttpStatus", "500");
			result.put("result", e.getMessage());
			logger.error(e.getMessage(), e);;
		}
		return result;
	}
	/**
	 *
	 * Description:将输入流转为字符串
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 *
	 */
	public  String inputStream2String(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		BufferedReader br = new BufferedReader(new InputStreamReader(in,"utf-8"));
		char[] b = new char[4096];
		for (int n; (n = br.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}

}
