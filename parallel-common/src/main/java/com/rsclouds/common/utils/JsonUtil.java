package com.rsclouds.common.utils;


import com.google.gson.Gson;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 2016-03-18
 * @author lishun
 *
 */
public class JsonUtil {
	private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);
	/**
	 * 把json字符串转换Map
	 * @param strJson
	 * @return
	 */
	public static Map<String, Object> json2Map(String strJson){
		try {
			Gson gson=new Gson();
			return gson.fromJson(strJson, Map.class);
		} catch (Exception e) {
			logger.info("json2Map error:{}",strJson);
			logger.error(e.getMessage(), e);;
		}
		return null;
		
	}
	/**
	 * 把json字符串转换ListMap
	 * @param strJson
	 * @return
	 */
	public static List<Map<String, Object>> toListMap(String strJson){
		JSONArray jArray = JSONArray.fromObject(strJson);
		List<Map<String, Object>> list=new ArrayList<Map<String,Object>>();
		for (Object object : jArray) {
			Map<String, Object> map = json2Map(object.toString());
			list.add(map);
		}
		return list;
	}
	public static void main(String[] args){
	}
}


