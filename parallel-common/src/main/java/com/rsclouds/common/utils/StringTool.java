package com.rsclouds.common.utils;

import java.util.UUID;


public class StringTool {
	public static boolean parasCheck(Object... par) {
		for(Object obj : par){
			if(obj == null || obj.toString().length() < 1){
				return false;
			}
		}
		return true;
	}
	public static String getUUID() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
	/**
	 * 
	* Description: 去电重复的字符，并保留一个
	* 			   eg "/////ipc////api/v3/cut/progress////" , '/' =>/ipc/api/v3/cut/progress/
	*  @param str
	*  @param c 
	* @author lishun 
	* @date 2017年7月28日 
	* @return void
	 */
	public static String str2repeat(String str,char c){
		char[] cs= str.toCharArray();
		for(int i =0;i < cs.length-1; i++){
			if(cs[i] == c && cs[i]==cs[i+1]){
				cs[i]=' ';
			}
		}
		return new String(cs).replace(" ", "").toString();
	}
}
