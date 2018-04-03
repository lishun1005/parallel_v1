package com.rscloud.ipc.utils.gtdata;


import com.google.common.base.Objects;
import com.rsclouds.common.utils.DateTools;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: 个人中心的我的空间里，对文件的操作就是请求gt的操作，增加安全验证之后的接口
 * 
 * @author ljw 2014-11-03
 * 
 * @version v1.1
 * 
 */
@Component
public class GtdataFileUtil {
	private static Logger logger = LoggerFactory.getLogger(GtdataFileUtil.class);
	
	private static String gtDataHost;
	@Value("#{applicationProperty[gtDataHost]}")
    public void setGtLoginHost(String str) {
		GtdataFileUtil.gtDataHost = str;
    }


	public static boolean showDownloadFile(String filePath, HttpServletResponse response) {
		try {
			String url = gtDataHost + filePath + "?op=OPEN&sign=" +  GtdataUtil.getSign() + "&time="
					+  GtdataUtil.getSignTime();
			InputStream in = doGetFile(url);
			if (null == in) {
				return false;
			}
			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			bis = new BufferedInputStream(in);
			bos = new BufferedOutputStream(response.getOutputStream());
			byte[] buff = new byte[2048];
			int bytesRead;
			while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
				bos.write(buff, 0, bytesRead);
			}
			bis.close();
			bos.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	/**
	 *
	 * Description：get下载文件基础请求
	 *
	 * @param url
	 *            请求地址
	 * @return 请求成功后的结果
	 *
	 */
	public static InputStream doGetFile(String url) {
		InputStream in;
		HttpResponse response;
		try {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(url).build();
			HttpGet delete = new HttpGet(uriComponents.toUri().toString());
			delete.setConfig(RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build());
			response = new DefaultHttpClient().execute(delete);
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				in = response.getEntity().getContent();

				if (200 == response.getStatusLine().getStatusCode()) {
					return in;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
	/**
	 * 
	* Description: 判断文件或文件夹
	*  @param path
	*  @return -1：文件或文件夹目录不存在，0：文件夹，1：文件
	* @author lishun 
	* @date 2017年10月17日 
	* @return int
	 * @throws Exception 
	 */
	public static int isDirectory(String path) throws Exception{
		Map<String, Object> map = getAllFileByPath(path,"");
		if(Objects.equal(map.get("result"), 1)){
			String size = (String)map.get("size");
			if(!Objects.equal("-1", size)){
				return 1;
			}else{
				return 0;
			}
		}else{
			return -1;
		}
		
	}
	/**
	 * 
	 * Description：根据目录查询该目录的所有文件和文件夹，如test文件夹 ，格式：/test
	 * 
	 * @param path
	 *            要查询的目录
	 * @param username
	 *            登录用户名字，用于去掉用户的路径
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> getAllFileByPath(String path,
			String username) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		List<GtdataFile> list = new ArrayList<GtdataFile>();
		String url = gtDataHost + path + "?op=LIST&recursive=false&sign="
				+ GtdataUtil.getSign() + "&time="
				+ GtdataUtil.getSignTime();
		String jsonresult = doGet(url);
		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);// 文件不存在
			map.put("message", "查询错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0) {// 有2种情况：错误GTDataException和files
				if (jsonresult.indexOf("2003") >= 0) {
					logger.info("Gtdate error:{}",jsonObject.toString());
					logger.info("api url:{}",url);
					map.put("result", 98);// 文件不存在
					map.put("message", "文件夹不存在！");
					return map;
				} else {
					map.put("result", 99);
					map.put("message", "查询错误！");
					return map;
				}
			} else if (jsonresult.indexOf("files") >= 0) {
				JSONArray jsonArray = JSONArray.fromObject(JSONObject
						.fromObject(jsonresult).get("files"));
				for (int i = 0; i < jsonArray.size(); i++) {
					GtdataFile gtdataFile = new GtdataFile();
					JSONObject jsonObject2 = (JSONObject) jsonArray.get(i);
					String pathStr = jsonObject2.getString("path").substring(username.length() + 1);
					gtdataFile.setPath(pathStr);
					gtdataFile.setSize(jsonObject2.getString("size"));
					String timeString = jsonObject2.getString("time");
					gtdataFile.setTime(DateTools.timeToDateFormat(Long.parseLong(timeString + "000"),"yyyy-MM-dd HH:mm:ss"));
					gtdataFile.setFilename(gtdataFile.getPath().substring(gtdataFile.getPath().lastIndexOf("/") + 1));
					list.add(gtdataFile);
				}
				map.put("result", 1);// 文件不存在
				map.put("message", "查询成功！");
				map.put("list", list);
				map.put("size", jsonObject.get("size"));
				return map;
			} else{ // 空文件夹
				map.put("result", 1);
				map.put("message", "查询成功！");
				map.put("list", list);
				map.put("size", jsonObject.get("size"));
				return map;
			}
		}
	}
	public static Map<String, Object> createNewDirectory(String beforePath, String newNewDirectoryName)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();

		String url = gtDataHost + beforePath;
		if (newNewDirectoryName != null && !newNewDirectoryName.equals("")) {
			if (url.lastIndexOf("/") != (url.length() - 1))
				url += "/";
			url += newNewDirectoryName;
		}
		url += "?op=MKDIRS&sign=" + GtdataUtil.getSign() + "&time=" + GtdataUtil.getSignTime();
		String jsonresult = doPut(url);
		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);
			map.put("message", "创建错误！系统内部错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0){// 有2种情况：错误GTDataException和files
				logger.info("Gtdate error:{}",jsonObject.toString());
				if (jsonresult.indexOf("2002") >= 0) {
					map.put("result", 97);
					map.put("message", "目录已存在！");
					return map;
				} else if (jsonresult.indexOf("2003") >= 0) {
					map.put("result", 98);
					map.put("message", "父路径不存在！");
					return map;
				} else {
					map.put("result", 99);
					map.put("message", "创建错误！系统内部错误！");
					return map;
				}
			} else if (jsonresult.indexOf("MKDIRS") >= 0) {
				boolean flag = jsonObject.getBoolean("MKDIRS");
				if (flag) {
					map.put("result", 1);
					map.put("message", "创建成功！");
					return map;
				} else {
					map.put("result", 96);
					map.put("message", "创建失败！未知错误！");
					return map;
				}
			} else {
				map.put("result", 99);
				map.put("message", "创建错误！系统内部错误！");
				return map;
			}
		}
	}
	/**
	 * 
	 * Description：重命名文件夹或文件
	 * 
	 * @param oldPath
	 *            旧路径,如:文件夹 "/hello/1" 文件："/hello/1.txt"
	 * @param newPath
	 *            新路径,如:文件夹 "/hello/2" 文件："/hello/2.txt"
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> renameDirectoryOrFile(String oldPath, String newPath) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();

		String url = gtDataHost + URLEncoder.encode(oldPath,"UTF-8")+ "?op=MOVE&destination=/" + newPath + "&sign="
				+ GtdataUtil.getSign() + "&time=" + GtdataUtil.getSignTime();
		String jsonresult = doPut(url);
		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);
			map.put("message", "重命名错误！系统内部错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0){
				logger.info("Gtdate error:{}",jsonObject.toString());
				logger.info("api url:{}",url);
				if (jsonresult.indexOf("2002") >= 0) {
					map.put("result", 97);
					map.put("message", "已存在相同名字的文件夹或文件！！");
					return map;
				} else if (jsonresult.indexOf("2003") >= 0) {
					map.put("result", 98);
					map.put("message", "文件或文件夹不存在！重命名失败！");
					return map;
				} else {
					map.put("result", 99);
					map.put("message", "重命名错误！系统内部错误！");
					return map;
				}
			} else if (jsonresult.indexOf("MOVE") >= 0) {
				boolean flag = jsonObject.getBoolean("MOVE");
				if (flag) {
					map.put("result", 1);
					map.put("message", "重命名成功！");
					return map;
				} else {
					map.put("result", 96);
					map.put("message", "重命名失败！未知错误！");
					return map;
				}
			} else {
				map.put("result", 99);
				map.put("message", "重命名错误！系统内部错误！");
				return map;
			}
		}
	}
	/**
	 * 
	 * Description：删除文件或文件夹
	 * 
	 * @param path
	 *            删除的路径 如文件夹："/hello/1" 文件："/hello/1.txt"
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> deleteDirectoryOrFile(String path) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();

		String url = gtDataHost + path + "?op=DELETE&sign=" + GtdataUtil.getSign() + "&time="
				+ GtdataUtil.getSignTime();
		String jsonresult = doDelete(url);
		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);
			map.put("message", "删除错误！系统内部错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0){
				logger.info("Gtdate error:{}",jsonObject.toString());
				logger.info("api url:{}",url);
				if (jsonresult.indexOf("2003") >= 0) {
					map.put("result", 98);
					map.put("message", "文件或文件夹不存在！删除失败");
					return map;
				} else {
					map.put("result", 99);
					map.put("message", "删除错误！系统内部错误！");
					return map;
				}
			} else if (jsonresult.indexOf("DELETE") >= 0) {
				boolean flag = jsonObject.getBoolean("DELETE");
				if (flag) {
					map.put("result", 1);
					map.put("message", "删除成功！");
					return map;
				} else {
					map.put("result", 96);
					map.put("message", "删除失败！未知错误！");
					return map;
				}
			} else {
				map.put("result", 99);
				map.put("message", "删除错误！系统内部错误！");
				return map;
			}
		}
	}
	
	/**
	 * 
	 * Description：根据目录查询该目录的所有文件的大小，包含子文件夹的文件目录大小，如test/hello 文件夹
	 * ，格式：http://192.168.2.7:8001/gtdata/v1/hello?op=LIST 查询hello文件夹
	 * 
	 * @param path 要查询的文件夹路径 如 test/hello
	 * @param gtdataHost gtdata的ip ，如 http://192.168.2.7:8001/gtdata/v1/
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> getAllFileSizeByPath(String path) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> dMap = new HashMap<String, Object>();
		Long allSize = 0L;
		String firstUrl = gtDataHost + path + "?op=LIST&user=" + GtdataUtil.gtGroupUser + "&recursive=false&sign="
				+ GtdataUtil.getSign() + "&time=" + GtdataUtil.getSignTime();
		dMap = getAllFileByURL(firstUrl);
		if (1 != (Integer) dMap.get("result")) {
			return dMap;
		}
		List<GtdataFile> firstList = (List<GtdataFile>) dMap.get("list");
		if (null == firstList || firstList.size() <= 0) {
			allSize = 0L;
			map.put("result", 1);
			map.put("message", "查询文件夹大小成功！！");
			map.put("size", allSize);
			return map;
		}
		for (int i = 0; i < firstList.size(); i++) {
			if (StringUtils.isBlank(firstList.get(i).getSize())) {
				continue;
			}
			Long childSize = Long.parseLong(firstList.get(i).getSize());
			if (-1 == childSize)// 如果是文件夹继续循环
			{
				String childpath = firstList.get(i).getPath().substring(1);
				Map<String, Object> cMap = getAllFileSizeByPath(childpath);// 递归
				if (1 == (Integer) dMap.get("result")) {
					allSize = allSize + (Long) cMap.get("size");
				}
			} else {
				allSize = allSize + childSize;
			}
		}
		map.put("result", 1);// 文件不存在
		map.put("message", "查询文件夹大小成功！！");
		map.put("size", allSize);
		return map;
	}
	
	/**
	 * 
	 * Description：根据URL查询该目录的所有文件和文件夹，如test文件夹
	 * ，格式：http://192.168.2.7:8001/gtdata/v1/hello?op=LIST 查询hello文件夹
	 * 
	 * @param url
	 *            请求url
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> getAllFileByURL(String url) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		List<GtdataFile> list = new ArrayList<GtdataFile>();
		url = url + "&user=" + GtdataUtil.gtGroupUser + "&recursive=false&sign=" + GtdataUtil.getSign()
				+ "&time=" + GtdataUtil.getSignTime();
		String jsonresult = doGet(url);

		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);// 文件不存在
			map.put("message", "查询错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0){
				logger.info("Gtdate error:{}",jsonObject.toString());
				logger.info("api url:{}",url);
				if (jsonresult.indexOf("2003") >= 0) {
					map.put("result", 98);// 文件不存在
					map.put("message", "文件夹不存在！");
					return map;
				} else {
					map.put("result", 99);// 文件不存在
					map.put("message", "查询错误！");
					return map;
				}
			} else if (jsonresult.indexOf("files") >= 0) {
				JSONArray jsonArray = JSONArray.fromObject(JSONObject.fromObject(jsonresult).get("files"));
				for (int i = 0; i < jsonArray.size(); i++) {
					GtdataFile gtdataFile = new GtdataFile();
					JSONObject jsonObject2 = (JSONObject) jsonArray.get(i);
					String pathStr = jsonObject2.getString("path");
					gtdataFile.setPath(pathStr);
					gtdataFile.setSize(jsonObject2.getString("size"));
					String timeString = jsonObject2.getString("time");
					gtdataFile.setTime(DateTools.timeToDateFormat(Long.parseLong(timeString + "000"),
							"yyyy-MM-dd HH:mm:ss"));
					gtdataFile.setFilename(gtdataFile.getPath().substring(gtdataFile.getPath().lastIndexOf("/") + 1));
					list.add(gtdataFile);
				}

				map.put("result", 1);
				map.put("message", "查询成功！");
				map.put("list", list);
				return map;
			} else // 空文件夹
			{
				map.put("result", 1);
				map.put("message", "查询成功！");
				map.put("list", list);
				return map;
			}
		}
	}
	/**
	 * 
	 * Description：复制文件或文件夹到指定路径，复制会保留原文件，
	 * 
	 * @param oldPath
	 *            旧的文件路径 如文件夹 "/hello/1" 文件 "/hello/1.txt"
	 * @param newPath
	 *            新文件夹路径 如文件夹 "/hello/2/1"(将1复制到2目录下) 文件 "/hello/2/1.txt"
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> copyDirectoryOrFile(String oldPath, String newPath) throws Exception {
		return copyDirectoryOrFile(oldPath, newPath, null, null);
	}
	/**
	 * 
	 * Description：复制文件或文件夹到指定路径，复制会保留原文件(带权限)
	 * 
	 * @param oldPath
	 *            旧的文件路径 如文件夹 "/hello/1" 文件 "/hello/1.txt"
	 * @param newPath
	 *            新文件夹路径 如文件夹 "/hello/2/1"(将1复制到2目录下) 文件 "/hello/2/1.txt"
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> copyDirectoryOrFile(String oldPath, String newPath,  String newcapflag, String newpermisson) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String url = "";
		if(StringUtils.isBlank(newcapflag) || StringUtils.isBlank(newpermisson)) {
			url = gtDataHost + oldPath + "?op=COPY&destination=/" + newPath + "&sign="
					+ GtdataUtil.getSign() + "&time=" + GtdataUtil.getSignTime();
		} else {
			url = gtDataHost + oldPath + "?op=COPY&destination=/" + newPath + "&sign="
					+ GtdataUtil.getSign() + "&time=" + GtdataUtil.getSignTime() + "&newcapflag=" + newcapflag + "&newpermisson=" + newpermisson;
		}
		String jsonresult = doPut(url);
		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);
			map.put("message", "复制错误！系统内部错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0){
				logger.info("Gtdate error:{}",jsonObject.toString());
				logger.info("api url:{}",url);
				if (jsonresult.indexOf("2002") >= 0) {
					map.put("result", 97);
					map.put("message", "目标文件夹已存在同名文件或文件夹！复制失败");
					return map;
				} else if (jsonresult.indexOf("2003") >= 0) {
					map.put("result", 98);
					map.put("message", "文件或文件夹不存在！复制失败");
					return map;
				} else if(jsonresult.indexOf("2006") >= 0){
					map.put("result", 95);
					map.put("message", "用户云盘空间不够！复制失败");
					return map;
				} else{
					map.put("result", 99);
					map.put("message", "复制错误！系统内部错误！");
					return map;
				}
			} else if (jsonresult.indexOf("COPY") >= 0) {
				boolean flag = jsonObject.getBoolean("COPY");
				if (flag) {
					map.put("result", 1);
					map.put("message", "复制成功！");
					return map;
				} else {
					map.put("result", 96);
					map.put("message", "复制失败！未知错误！");
					return map;
				}
			} else {
				map.put("result", 99);
				map.put("message", "复制错误！系统内部错误！");
				return map;
			}
		}
	}

	/**
	 * 
	 * Description：移动文件或文件夹到指定路径，不会保留原文件（先复制再删除）
	 * 
	 * @param oldPath
	 *            旧的文件路径 如文件夹 "/hello/1" 文件 "/hello/1.txt"
	 * @param newPath
	 *            新文件夹路径 如文件夹 "/hello/2/1"(将1复制到2目录下) 文件 "/hello/2/1.txt"
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> moveDirectoryOrFile(String oldPath, String newPath) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();

		String url = gtDataHost + oldPath + "?op=COPY&destination=/" + newPath + "&sign="
				+ GtdataUtil.getSign() + "&time=" + GtdataUtil.getSignTime();
		String jsonresult = doPut(url);
		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);
			map.put("message", "移动失败！系统内部错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0){
				logger.info("Gtdate error:{}",jsonObject.toString());
				logger.info("api url:{}",url);
				if (jsonresult.indexOf("2002") >= 0) {
					map.put("result", 97);
					map.put("message", "目标文件夹已存在同名文件或文件夹！移动失败");
					return map;
				} else if (jsonresult.indexOf("2003") >= 0) {
					map.put("result", 98);
					map.put("message", "文件或文件夹不存在！移动失败");
					return map;
				} else {
					map.put("result", 99);
					map.put("message", "移动失败！系统内部错误！");
					return map;
				}
			} else if (jsonresult.indexOf("COPY") >= 0) {
				boolean flag = jsonObject.getBoolean("COPY");
				if (flag) {
					deleteDirectoryOrFile(oldPath);
					map.put("result", 1);
					map.put("message", "移动成功！");
					return map;
				} else {
					map.put("result", 96);
					map.put("message", "移动失败！未知错误！");
					return map;
				}
			} else {
				map.put("result", 99);
				map.put("message", "移动错误！系统内部错误！");
				return map;
			}
		}
	}
	/**
	 * 
	 * Description：根据目录查询该目录的所有文件和文件夹，包括子文件夹，子文件夹的文件和文件夹都会被查询出来，如test文件夹 ，格式：test 包括子文件夹包括子文件夹包括子文件夹包括子文件夹包括子文件夹
	 * 
	 * @param path 要查询的目录
	 * @param username 登录用户名字，用于去掉用户的路径
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Object> getAllFileByPathWithChildPath(String path, String username) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		List<GtdataFile> list = new ArrayList<GtdataFile>();
		String url = gtDataHost + path + "?op=LIST&recursive=true&sign=" + GtdataUtil.getSign() + "&time="
				+ GtdataUtil.getSignTime();
		String jsonresult = doGet(url);
		if (StringUtils.isBlank(jsonresult)) {
			map.put("result", 99);// 文件不存在
			map.put("message", "查询错误！");
			return map;
		} else {
			JSONObject jsonObject = JSONObject.fromObject(jsonresult);
			if (jsonresult.indexOf("GTDataException") >= 0){
				logger.info("Gtdate error:{}",jsonObject.toString());
				logger.info("api url:{}",url);
				if (jsonresult.indexOf("2003") >= 0) {
					map.put("result", 98);// 文件不存在
					map.put("message", "文件夹不存在！");
					return map;
				} else {
					map.put("result", 99);
					map.put("message", "查询错误！");
					return map;
				}
			} else if (jsonresult.indexOf("files") >= 0) {
				JSONArray jsonArray = JSONArray.fromObject(JSONObject.fromObject(jsonresult).get("files"));
				list = formatJsonToFilesList(jsonArray, username);
				map.put("result", 1);// 文件不存在
				map.put("message", "查询成功！");
				map.put("list", list);
				return map;
			} else // 空文件夹
			{
				map.put("result", 1);
				map.put("message", "查询成功！");
				map.put("list", list);
				return map;
			}
		}
	}
	/**
	 * 
	 * Description：在LIST的包含子文件夹的查询中，要递归获取子文件夹信息
	 * 
	 * @param jsonArray 查询的目录json数组
	 * @return
	 *
	 */
	private static List<GtdataFile> formatJsonToFilesList(JSONArray jsonArray, String username) {
		List<GtdataFile> listAll = new ArrayList<GtdataFile>();
		try {
			for (int i = 0; i < jsonArray.size(); i++) {
				GtdataFile gtdataFile = new GtdataFile();
				JSONObject jsonObject2 = (JSONObject) jsonArray.get(i);
				// 如果是文件夹，就添加到gtdataFile 的child中
				if (null != jsonObject2.get("files"))// 是子目录
				{
					JSONArray jsonArray2 = JSONArray.fromObject(jsonObject2.getJSONArray("files"));
					List<GtdataFile> child = new ArrayList<GtdataFile>();
					child = formatJsonToFilesList(jsonArray2, username);
					gtdataFile.setChild(child);
				}
				String pathStr = jsonObject2.getString("path").substring(username.length() + 1);
				gtdataFile.setPath(pathStr);
				gtdataFile.setSize(jsonObject2.getString("size"));
				String timeString = jsonObject2.getString("time");
				gtdataFile
						.setTime(DateTools.timeToDateFormat(Long.parseLong(timeString + "000"), "yyyy-MM-dd HH:mm:ss"));
				gtdataFile.setFilename(gtdataFile.getPath().substring(gtdataFile.getPath().lastIndexOf("/") + 1));
				listAll.add(gtdataFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return listAll;
		}
		return listAll;
	}

	/**
	 * 
	 * Description：delete基础请求
	 * 
	 * @param url
	 *            请求地址
	 * @return 请求成功后的结果
	 * 
	 */
	public static String doDelete(String url) {
		InputStream in;
		String str = "";
		HttpResponse response;
		try {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(url).build();
			HttpDelete delete = new HttpDelete(uriComponents.toUri().toString());
			
			delete.setConfig(RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build());
			response = HttpClientBuilder.create().build().execute(delete);
			if (response != null) {
				in = response.getEntity().getContent();
				if (in != null) {
					str = inputStream2String(in);
					in.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return str;
		}
		return str;
	}
	/**
	 * 
	 * Description：get基础请求
	 * 
	 * @param url
	 *            请求地址
	 * @return 请求成功后的结果
	 * 
	 */
	public static String doGet(String url) {
		InputStream in;
		String str = "";
		HttpResponse response;
		for (int i = 0; i < 5; i++) {
			try {
				UriComponents uriComponents = UriComponentsBuilder.fromUriString(url).build();
				HttpGet httpGet = new HttpGet(uriComponents.toUri().toString());
				httpGet.setConfig(RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).build());
				response = HttpClientBuilder.create().build().execute(httpGet);
				if (response != null) {
					response.getAllHeaders();
					in = response.getEntity().getContent();
					if (in != null) {
						str = inputStream2String(in);
						in.close();
					}
					i = 5;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return str;
	}
	/**
	 * 
	 * Description：put基础请求，一般命令，查询文件等使用
	 * 
	 * @param url
	 * @return
	 * 
	 */
	public static String doPut(String url) {
		String ret = "";
		for (int i = 0; i < 5; i++) {
			try {
				UriComponents uriComponents = UriComponentsBuilder.fromUriString(url).build();
				HttpPut httpPut = new HttpPut(uriComponents.toUri().toString());
				httpPut.setConfig(RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build());
				HttpResponse response  = HttpClientBuilder.create().build().execute(httpPut);
				if (response != null) {
					StatusLine statusLine = (StatusLine) response.getStatusLine();
					HttpEntity entity = response.getEntity();
					InputStream inputStream = entity.getContent();
					if (inputStream != null) {
						ret = inputStream2String(inputStream);
						inputStream.close();
					}
					i = 5;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ret;
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
	public static String inputStream2String(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"));
		char[] b = new char[4096];
		for (int n; (n = br.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}
	
}
