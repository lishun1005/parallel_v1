package com.rscloud.ipc.utils.gtdata;


import org.apache.commons.lang.StringUtils;


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
		return name;
	}
	
	public String getGtPath(){
		if(gtPath == null){
			gtPath = "//";
			for (int i = paths.length-2; i >= 0 ; i--) {
				gtPath = "/" + paths[i] + gtPath;
			}
			gtPath = gtPath + paths[paths.length-1];
		}
		return gtPath;
	}
	public  String replaceLast(String string, String toReplace,
	                                 String replacement) {
		int pos = string.lastIndexOf(toReplace);
		if (pos > -1) {
			return string.substring(0, pos)
					+ replacement
					+ string.substring(pos + toReplace.length(),
					string.length());
		} else {
			return string;
		}
	}
	public String getGtParent() {
		String path = "";
		if (paths.length <= 1)
			path =  "//";
		else{
			path = getGtPath();
			path = path.substring(0, path.lastIndexOf("//"));
			path = replaceLast(path, "/", "//");
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

	public static void main(String[] args) {
		GtPath inPathObj = new GtPath("DDDD/D///DDDDD//DD水电费.kk");
		System.out.println(inPathObj.getDisplayParent());

		String paths = org.apache.commons.lang3.StringUtils.join(new String[]{"/users" , null},"/");
		System.out.println(paths);
	}

}
