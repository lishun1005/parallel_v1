package com.rsclouds.gtparallel.gtdata.entity;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;

import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;


public class GtPath {
	
	private final String[] paths;
	private String gtPath;
	private String displayPath;
	private final static String Encoding = "utf-8";
	
	public GtPath(String path){
		while(path.endsWith("/") && path.length()>1){
			path = path.substring(0, path.length()-1);
		}
		paths = StringUtils.splitByWholeSeparator(path, "/");
	}
	
	
	public String getDisplayFileName(){
		String name = paths[paths.length-1];
		if(name.contains("%")){
			name = TransCoding.decode(name, Encoding);
		}
		return name;
	}
	
	public String getGtPath(){
		if(gtPath == null){
			gtPath = "//";
			for (int i = paths.length-2; i >= 0 ; i--) {
				gtPath = "/" + paths[i] + gtPath;
			}
			gtPath = gtPath + paths[paths.length-1];
			if(!gtPath.contains("%")){
				gtPath = TransCoding.UrlEncode(gtPath, "utf-8");
			}
		}
		return gtPath;
	}
	
	public String getGtParent() {
		String path = "";
		if (paths.length <= 1)
			path =  "//";
		else{
			path = getGtPath();
			path = path.substring(0, path.lastIndexOf("//"));
			path = GtDataUtils.replaceLast(path, "/", "//");
		}		
		return path;
	}
	public String getSuffixName(){
		if(getDisplayFileName().lastIndexOf(".")>0){
			return getDisplayFileName().substring(getDisplayFileName().lastIndexOf("."),getDisplayFileName().length()).toLowerCase();
		}
		return "";
	}
	
	public String getDisplayPath() {
		if(displayPath == null){
			displayPath = "/" + StringUtils.join(paths, "/");
			if(displayPath.contains("%")){
				displayPath = TransCoding.decode(displayPath, Encoding);
			}
		}
		return displayPath;
	}
	
	public String getDisplayParent() {
		String path = "";
		if (paths.length <= 1)
			path =  "/";
		else{
			path = getDisplayPath();
			path = path.substring(0, path.lastIndexOf("/"));
		}		
		return path;
	}

	public static void main(String[] args){
	}

}
