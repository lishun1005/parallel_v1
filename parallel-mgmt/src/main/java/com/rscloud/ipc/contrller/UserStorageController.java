package com.rscloud.ipc.contrller;

import com.google.gson.Gson;
import com.rscloud.ipc.dto.SysUserShiroDto;
import com.rscloud.ipc.shiro.ShiroKit;
import com.rscloud.ipc.utils.gtdata.GtdataFile;
import com.rscloud.ipc.utils.gtdata.GtdataFileUtil;
import com.rscloud.ipc.utils.gtdata.GtdataUtil;
import com.rscloud.ipc.utils.gtdata.ShowFileTree;
import com.rsclouds.common.utils.PubFun;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
* @ClassName: 集市用户存储管理  
* @Description: TODO
* @author lishun 
* @date 2017年7月13日 上午10:29:54  
*
 */
@Controller
public class UserStorageController extends BaseContrller {
	//付费订单数据，不占云盘空间
	@Value("#{fixparamProperty[payMentOrder]}")
	protected String payMentOrder;
	
	//付费订单数据，不占云盘空间
	@Value("#{fixparamProperty[tradingCenter]}")
	protected String tradingCenter;
	
	@Value("#{fixparamProperty[queue_cut]}")
	protected String queueCut;

	
	private static Logger logger = LoggerFactory.getLogger(UserStorageController.class);
	
	@RequiresPermissions("rsUser:userStorage:list")
	@RequestMapping(value = "userStorage/list")
	public String list() throws Exception {
		return "rsUserStorage/list";
	}
	/**
	 * 
	* Description: 检查路径是否存在
	*  @param path
	*  @return 
	* @author lishun 
	* @date 2017年8月28日 
	* @return boolean
	 */
	@RequiresPermissions("rsUser:userStorage:existPath")
	@RequestMapping(value = "userStorage/existPath")
	@ResponseBody
	public boolean existPath(String path){
		SysUserShiroDto sysUserShiroDto = ShiroKit.getSysUser();
		if(2 == sysUserShiroDto.getUserType()){
			path = sysUserShiroDto.getUsername() + "/" + path;
		}
		boolean existPath = false;
		try {
			 if("/".equals(path) || "".equals(path)){//不允许查询全部文件夹
				 return false;
			 }
			 Map<String, Object> map = GtdataFileUtil.getAllFileSizeByPath(path);
			 if(98 == (Integer) map.get("result")){
				 existPath = false;
			 }else if(1 == (Integer) map.get("result")){
				 existPath = true;
			 }else{
				 logger.info(map.toString());
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return existPath;
	}
	
	@RequiresPermissions("rsUser:userStorage:list")
	@RequestMapping(value = "userStorage/AllFile")
	@ResponseBody
	public String getUserFilesByPath(String currentPath, Integer currentPage, String sort) throws Exception {
		if(currentPage==null){
			currentPage=1;
		}
		if(StringUtils.isBlank(currentPath)){
			currentPath="";
		}
		currentPath = PubFun.decode(currentPath);
		//int numInPage = 12;// 每页显示条数
		String username = getUserName();
		
		Map<String, Object> map = new HashMap<String, Object>();
		map = GtdataFileUtil.getAllFileByPath(username + "/", username);// 先检查用户的根文件夹存不存在，如果不存在就先创建
		if (98 == (Integer) (map.get("result"))) {
			GtdataFileUtil.createNewDirectory("", username);
		}
		map = GtdataFileUtil.getAllFileByPath(username + currentPath, username);// 获取文件数据
		List<GtdataFile> list= (List<GtdataFile>) map.get("list");
		map.put("total", list.size());
		/*if (null != list && list.size() > 0) {// 分页
			PageUtils.countCurrPageNearPages(currentPage, numInPage, list.size(), map);
			map.put("total", list.size());

			classify(list);
			if ("name_down".equals(sort)) {
				sortListByName(list,"down");// 按之母排序
			} else if ("name_up".equals(sort)) {
				sortListByName(list,"up");// 按之母排序
			} else if ("size_down".equals(sort)) {
				sortListBySize(list,"down");
			} else if ("size_up".equals(sort)) {
				sortListBySize(list,"up");
			} else if ("time_down".equals(sort)) {
				sortListByTime(list,"down");
			} else if ("time_up".equals(sort)) {
				sortListByTime(list,"up");
			} else {
				sortListByName(list,"down");//默认按字母排序
			}
			list = PageUtils.countFileData(list, currentPage, numInPage, list.size());//开始分页
			map.put("list", list);
			map.put("numInPage", numInPage);
		} else {
			map.put("total", 0);
		}*/
		map.put("list", list);
		map.put("currentPath", currentPath);
		map.put("draw", -1);
		map.put("recordsTotal", list.size());
		map.put("recordsFiltered", list.size());
		Gson gson = new Gson();
		return gson.toJson(map);
	}
	/**
	 * 
	 * Description：list集合根据文件或文件夹的名字字母排序
	 * 
	 * @param list
	 *            集合
	 * 
	 *//*
	private static void sortListByName(List<GtdataFile> list,final String sort) {
		Collections.sort(list, new Comparator<GtdataFile>() {// 在根据字母排序
			public int compare(GtdataFile a, GtdataFile b) {
				if ("-1".equals(a.getSize()) && "-1".equals(b.getSize())) {
					if("up".equals(sort)){//降序
						return a.getFilename().toLowerCase().compareTo(b.getFilename().toLowerCase());
					}else{
						return b.getFilename().toLowerCase().compareTo(a.getFilename().toLowerCase());
					}
				}
				if(!"-1".equals(a.getSize()) && !"-1".equals(b.getSize())){
					if("up".equals(sort)){//降序
						return a.getFilename().toLowerCase().compareTo(b.getFilename().toLowerCase());
					}else{
						return b.getFilename().toLowerCase().compareTo(a.getFilename().toLowerCase());
					}
				}
				return 0;
			}
		});
	}
	*//**
	 * 
	 * Description：list集合根据文件或文件夹的大小排序
	 * 
	 * @param list
	 *            集合
	 * 
	 *//*
	private static void sortListBySize(List<GtdataFile> list,final String sort) {
		Collections.sort(list, new Comparator<GtdataFile>() {// 在根据字母排序
			public int compare(GtdataFile a, GtdataFile b) {
				if ("-1".equals(a.getSize()) && "-1".equals(b.getSize())) {
					if("up".equals(sort)){
						return a.getFilename().toLowerCase().compareTo(b.getFilename().toLowerCase());
					}else{
						return b.getFilename().toLowerCase().compareTo(a.getFilename().toLowerCase());
					}
				}
				if(!"-1".equals(a.getSize()) && !"-1".equals(b.getSize())){
					if("up".equals(sort)){
						return Long.compare(Long.parseLong(a.getSize()), Long.parseLong(b.getSize()));
					}else{
						return Long.compare(Long.parseLong(b.getSize()), Long.parseLong(a.getSize()));
					}
				}
				return 0;
			}
		});
	}
	*//**
	 * 
	 * Description：list集合根据文件或文件夹的创建时间排序
	 * 
	 * @param list
	 *            集合
	 * 
	 *//*
	private void sortListByTime(List<GtdataFile> list,final String sort) {
		Collections.sort(list, new Comparator<GtdataFile>() {// 在根据字母排序
			public int compare(GtdataFile a, GtdataFile b) {
				if ("-1".equals(a.getSize()) && "-1".equals(b.getSize())) {
					if("up".equals(sort)){
						return a.getTime().toLowerCase().compareTo(b.getTime().toLowerCase());
					}else{
						return b.getTime().toLowerCase().compareTo(a.getTime().toLowerCase());
					}
				}
				if(!"-1".equals(a.getSize()) && !"-1".equals(b.getSize())){
					if("up".equals(sort)){
						return a.getTime().toLowerCase().compareTo(b.getTime().toLowerCase());
					}else{
						return b.getTime().toLowerCase().compareTo(a.getTime().toLowerCase());
					}
				}
				return 0;
			}
		});
	}
	*//**
	 * 
	* Description: 根据文件夹和文件分好类 -1 ：文件夹  1:文件
	*  @param list 
	* @author lishun 
	* @date 2017年7月14日 
	* @return void
	 *//*
	private static void classify(List<GtdataFile> list) {
		Collections.sort(list, new Comparator<GtdataFile>() {// 在根据大小排序
			public int compare(GtdataFile a, GtdataFile b) {
				long asize = Long.parseLong(a.getSize());
				long bsize = Long.parseLong(b.getSize());
				return Long.compare(asize, bsize);
			}
		});
	}*/
	/**
	 * 
	 * Description：个人空间重命名文件夹或文件
	 * 
	 * @param currentPath
	 *            当前路径
	 * @param oldName
	 *            旧名字
	 * @param newName
	 *            新名字
	 * @return
	 * @throws Exception
	 * 
	 */
	@RequiresPermissions("rsUser:userStorage:renameDirectoryOrFile")
	@RequestMapping(value = "userStorage/renameDirectoryOrFile")
	@ResponseBody
	public Object renameDirectoryOrFile(String currentPath, String oldName, String newName) throws Exception {
		String username = getUserName();
		currentPath = PubFun.decode(currentPath);
		newName = PubFun.decode(newName);
		oldName = PubFun.decode(oldName);
		if (logger.isDebugEnabled()) {
			logger.debug("个人空间重命名文件夹或文件,用户名：" + username + ",当前目录:" + currentPath + ",旧名字：" + oldName + ", 新名字："
					+ newName);
		}
		String oldPath = username + currentPath + "/" + oldName;
		String newPath = username + currentPath + "/" + newName;
		UriComponents oldPathuriComponents = UriComponentsBuilder.fromUriString(oldPath).build();
		oldPath = oldPathuriComponents.toUri().toString();
		UriComponents newPathuriComponents = UriComponentsBuilder.fromUriString(newPath).build();
		newPath = newPathuriComponents.toUri().toString();
		Map<String, Object> map = new HashMap<String, Object>();
		map = GtdataFileUtil.renameDirectoryOrFile(oldPath, newPath);

		Gson gson = new Gson();
		return gson.toJson(map);
	}
	/**
	 * 
	 * Description：个人中心批量删除文件或文件夹
	 * 
	 * @param currentPath
	 *            当前路径
	 * @param selectFileName
	 *            选中的文件或文件夹名字，json字符串
	 * @return
	 * @throws Exception
	 * 
	 */
	@RequiresPermissions("rsUser:userStorage:deleteSomeDirectoryOrFile")
	@RequestMapping(value = "userStorage/deleteSomeDirectoryOrFile")
	@ResponseBody
	public Object deleteSomeDirectoryOrFile(String currentPath, String selectFileName) throws Exception {
		String username = getUserName();
		currentPath = PubFun.decode(currentPath);
		selectFileName = PubFun.decode(selectFileName);
		if (logger.isDebugEnabled()) {
			logger.debug("个人空间批量删除文件夹或文件,用户名：" + username + ",当前目录：" + currentPath + ", 要删除的文件：" + selectFileName);
		}
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject json = JSONObject.fromObject(selectFileName);
		Object[] objects = json.values().toArray();
		int count = 0;// 记录失败次数
		String failureString = "";
		for (Object obj : objects) // 循环删除
		{
			String path = username + currentPath + "/" + (String) obj;
			map = GtdataFileUtil.deleteDirectoryOrFile(path);
			if (1 != (Integer) map.get("result")) {
				failureString += (String) obj + " ";
				count++;
			}
		}
		map.put("result", 1);
		if (0 == count) {
			map.put("message", "文件或文件夹删除成功！");
		} else {
			map.put("message", "有 " + count + " 个文件或文件夹删除失败了，它们是：" + failureString + "");
		}

		Gson gson = new Gson();
		return gson.toJson(map);
	}
	/**
	 * 
	 * Description：个人空间移动文件夹或文件
	 * 
	 * @param currentPath
	 *            当前路径
	 * @param newName
	 *            新路径
	 * @return
	 * @throws Exception
	 * 
	 */
	@RequiresPermissions("rsUser:userStorage:moveDirectoryOrFile")
	@RequestMapping(value = "userStorage/moveDirectoryOrFile")
	@ResponseBody
	public Object moveDirectoryOrFile(String currentPath, String newPath, String selectFileName,boolean operFlag,boolean deleteFlag) throws Exception {
		String username = getUserName();
		currentPath = PubFun.decode(currentPath);
		selectFileName = PubFun.decode(selectFileName);
		newPath = PubFun.decode(newPath);

		if (logger.isDebugEnabled()) {
			logger.debug("个人空间批量移动文件夹或文件,用户名：" + username + ",当前目录：" + currentPath + ",目标目录:" + newPath + ", 要移动的文件："
					+ selectFileName);
		}
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject json = JSONObject.fromObject(selectFileName);
		StringBuffer sb=new StringBuffer("{");
		Object[] objects = json.values().toArray();
		int count = 0;// 记录失败次数
		int countRepeat = 0;
		String failureString = "";
		for (Object obj : objects) // 循环移动
		{
			String oldPath = username + currentPath + "/" + (String) obj;
			String newPathString = username + newPath + "/" + (String) obj;
			if(deleteFlag){
				GtdataFileUtil.deleteDirectoryOrFile(newPathString);
			}
			if(operFlag){
				map = GtdataFileUtil.moveDirectoryOrFile(oldPath, newPathString);
			}else{
				map = GtdataFileUtil.copyDirectoryOrFile(oldPath, newPathString);
			}
			if((Integer) map.get("result")==97&&!operFlag){
				sb.append("\""+countRepeat+"\":").append("\""+obj+"\"").append(",");
				countRepeat++;
			}
				
			if (1 != (Integer) map.get("result")) {
				failureString += (String) obj + "";
				count++;
			}
		}
		if(!operFlag&& countRepeat!=0){//复制，并且有重复
			String str=sb.substring(0, sb.length()-1)+"}";
			map.put("result", 97);
			map.put("message", str);
		}else{
			if (0 == count) {
				map.put("result", 1);
				map.put("message", "全部文件或文件夹移动成功！");
			} else {
				map.put("result", 2);
				map.put("message", "有 " + count + " 个文件或文件夹移动失败了，它们是：" + failureString + "");
			}
		}

		Gson gson = new Gson();
		return gson.toJson(map);
	}
	/**
	 * 
	 * Description：个人空间新建文件夹
	 * 
	 * @param currentPath
	 *            当前路径
	 * @param newName
	 *            新文件夹名字
	 * @return
	 * @throws Exception
	 * 
	 */
	@RequiresPermissions("rsUser:userStorage:createNewDirectory")
	@RequestMapping(value = "userStorage/createNewDirectory")
	@ResponseBody
	public Object createNewDirectory(String currentPath, String newName) throws Exception {
		String username = getUserName();
		currentPath = PubFun.decode(currentPath);
		newName = PubFun.decode(newName);
		if (logger.isDebugEnabled()) {
			logger.debug("个人空间新建文件夹,用户名：" + username + ",当前目录:" + currentPath + ",新文件夹名字：" + newName);
		}

		String oldPath = username + currentPath;
		Map<String, Object> map = new HashMap<String, Object>();
		if(oldPath.indexOf(payMentOrder)!=-1 ||oldPath.indexOf(tradingCenter)!=-1 ){
			map.put("result", 95);
			map.put("message", oldPath+"目录下不允许创建文件夹");
		}else{
			map = GtdataFileUtil.createNewDirectory(oldPath, newName);
		}
		Gson gson = new Gson();
		return gson.toJson(map);
	}
	/**
	 * 
	 * Description：移动文件或文件夹之前，获取当前目录的所有文件夹
	 * 
	 * @param currentPath
	 *            当前目录
	 * @return
	 * @throws Exception
	 * 
	 */
	@RequiresPermissions("rsUser:userStorage:queryDirectory")
	@RequestMapping(value = "userStorage/queryDirectory")
	@ResponseBody
	public Object queryDirectory(Boolean showFile) throws Exception {
		if(showFile==null){
			showFile = false; 
		}
		String username =getUserName();
		String newPath = username;
		Map<String, Object> map = new HashMap<String, Object>();

		map = GtdataFileUtil.getAllFileByPathWithChildPath(newPath, username);// 获取文件数据
		// 分页
		List<GtdataFile> list = (List<GtdataFile>) map.get("list");
		List<GtdataFile> resultList = new ArrayList<GtdataFile>();
		List<ShowFileTree> result = new ArrayList<ShowFileTree>();
		if (null != list && list.size() > 0) {
			// 去掉list多余的部分，先排序再去掉多余
			// 调用排序通用类
			for (int i = 0; i < list.size(); i++) {
				String  fileName=list.get(i).getFilename();
				if (!fileName.equals(payMentOrder)&& !fileName.equals(tradingCenter)) {
					resultList.add(list.get(i));
				}
			}
			
			//sortListByNameDown(resultList);// 按字母排序
			// 处理list,将list变成tree的list
			// 加上我的文件
			result = getChildTreeNode(resultList, "",showFile);
			ShowFileTree top = new ShowFileTree();
			top.setId("");
			top.setpId("");
			top.setName("我的文件");
			top.setOpen("true");
			top.setClick("selectTheNode('','', '我的文件')");
			result.add(top);
			map.put("total", result.size());
			map.put("list", result);
		} else {
			map.put("total", 0);
			map.put("list", result);// 顶层
		}
		return map;
	}
	
	
	/**
	 * Description：根据GtdataFile的子文件夹们，获取到子文件夹和子文件夹的子文件夹，递归。。。。
	 * 
	 * @param list
	 * @return
	 */
	private List<ShowFileTree> getChildTreeNode(List<GtdataFile> list, String pPath,Boolean showFile) {
		List<ShowFileTree> result = new ArrayList<ShowFileTree>();
		try {
			if (null != list && list.size() > 0) {
				for (int i = 0; i < list.size(); i++) {
					GtdataFile gtdataFile = list.get(i);
					if ("-1".equals(gtdataFile.getSize()) || showFile ){// showFile 是否显示文件夹
						ShowFileTree showFileTree = new ShowFileTree();
						showFileTree.setId(gtdataFile.getPath());
						showFileTree.setpId(pPath);
						showFileTree.setName(gtdataFile.getPath().substring(gtdataFile.getPath().lastIndexOf("/") + 1));
						showFileTree.setOpen("false");
						showFileTree.setClick("selectTheNode('" + showFileTree.getId() + "','" + showFileTree.getpId()
								+ "', '" + showFileTree.getName() + "')");
						showFileTree.setSize(gtdataFile.getSize());
						result.add(showFileTree);
						
					}
					if (null != gtdataFile.getChild() && gtdataFile.getChild().size() > 0) {
						result.addAll(getChildTreeNode(gtdataFile.getChild(), gtdataFile.getPath(),showFile));
					}
				}
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return result;
		}
	}
	/**
	 * 
	* Description: 获取用户存储信息
	*  @return
	*  @throws Exception 
	* @author lishun 
	* @date 2017年7月13日 
	* @return Object
	 */
	@RequiresPermissions("rsUser:userStorage:queryUserSize")
	@RequestMapping(value = "userStorage/queryUserSize")
	@ResponseBody
	public Object queryUserSize() throws Exception {
		String username =getUserName();
		Map<String, Object> map = new HashMap<String, Object>();
		map = GtdataUtil.groupUserGetCommonUserInfo(username, GtdataUtil.getSign(),
				GtdataUtil.getSignTime());
		Gson gson = new Gson();
		return gson.toJson(map);
	}
	public String getUserName(){
		String username = "";//若是系统管理员，则查询gt-data所有文件
		SysUserShiroDto sysuser=ShiroKit.getSysUser();
		if(sysuser.getUserType() == 2){
			username= sysuser.getUsername();
		}
		return username;
	}
}
