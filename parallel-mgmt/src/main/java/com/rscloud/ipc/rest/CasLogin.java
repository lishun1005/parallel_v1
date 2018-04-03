package com.rscloud.ipc.rest;

import com.rsclouds.common.utils.HttpClientUtils;
import com.rsclouds.common.utils.PubFun;
import com.rsclouds.common.utils.XmlUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//@Service
public class CasLogin {
	@Value("#{applicationProperty[casServerUrl]}")
	public String casServerUrl;
	
	public Map<String, Object> login(String contact, String password) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		StringBuilder sb=new StringBuilder(casServerUrl);
		try {
			Map<String, String> map = new HashMap<String, String>();
			if (PubFun.isEmail(contact)) {
				map.put("useremail", contact);
			} else if (PubFun.isPhone(contact)) {
				map.put("userphone", contact);
			} else {
				map.put("username", contact);
			}
			map.put("password", password);
			map.put("noMD5Flag", "true");
			HttpClientUtils httpClientApiTGT = new HttpClientUtils(sb.append("/v1/tickets/").toString());
			HttpResponse httpResponse = httpClientApiTGT.execute( map, HttpClientUtils.methodType.POST);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_CREATED) {
				resultMap.put("code", "2001");
				String tgtKey = httpResponse.getHeaders("Location")[0].getValue().substring(
						httpResponse.getHeaders("Location")[0].getValue().lastIndexOf("/") + 1,
						httpResponse.getHeaders("Location")[0].getValue().length());// 获取TGC
				Map<String, Object> userInfo=getUserInfoBCas(sb.append(tgtKey).toString());
				resultMap.put("userInfo", userInfo);
			} else {
				resultMap.put("code", "2002");
				resultMap.put("errorMessage", "登录失败！密码错误或者手机、邮箱不存在！");
				return resultMap;
			}

		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("errorCode", "2002");
			resultMap.put("errorMessage", "登录失败！");
			return resultMap;
		}
		return resultMap;
	}
	 
	public Map<String, Object> getUserInfoBCas(String uri) throws ParseException, IOException{
		Map<String, String> map = new HashMap<String, String>();
		HttpClientUtils httpClientApi = new HttpClientUtils(uri);
		map.put("service", "http://127.0.0.1/");//service参数必填,任意http地址
		HttpResponse httpResponse = httpClientApi.execute(map, HttpClientUtils.methodType.POST);
		String st=EntityUtils.toString(httpResponse.getEntity());
		uri=new StringBuffer(casServerUrl).append("/serviceValidate?ticket=").append(st)
				.append("&service=http://127.0.0.1/").toString();//这里的sevice必须与上面的service一致
		httpClientApi = new HttpClientUtils(uri);
		httpResponse = httpClientApi.execute(map,HttpClientUtils.methodType.GET);
		String respStr=IOUtils.toString(httpResponse.getEntity().getContent());
		return XmlUtils.extractCustomAttributes(respStr,"cas:attributes");
	}
}
