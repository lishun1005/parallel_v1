package com.rscloud.ipc.contrller;

import com.rscloud.ipc.dto.*;
import com.rscloud.ipc.rpc.api.dto.CutJobDto;
import com.rscloud.ipc.rpc.api.dto.MapManageDto;
import com.rscloud.ipc.rpc.api.dto.SysUserDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.CutService;
import com.rscloud.ipc.shiro.ShiroKit;
import com.rscloud.ipc.utils.gtdata.GtdataFile;
import com.rscloud.ipc.utils.gtdata.GtdataFileUtil;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.*;

/**
 * @author lishun
 * @Description: TODO
 * @date 2017/11/3
 */
@Controller
public class CutJobController extends BaseContrller {
	@Autowired
	@Lazy
	public CutService cutService;

	@Value("#{fixparamProperty[gtdata_prefix]}")
	protected String gtdataPrefix;

	public Map<String,String> getUserIdAndInPath(String inPath){
		Map<String,String> map = new HashMap<String,String>();
		SysUserShiroDto sysuser = ShiroKit.getSysUser();
		String ownerUser = "";
		if(sysuser.getUserType() == 1){ //管理员进行切片;
			ownerUser = getOwnerUserForMangemant(inPath);
		}else{
			inPath = sysuser.getUsername() + inPath;//集市用户文件路径 “/users/用户名/文件相对路径”
			ownerUser = sysuser.getId();
		}
		map.put("in_path", inPath);
		map.put("owner_user_id", ownerUser);
		map.put("operation_user_id", sysuser.getId());
		return map;
	}
	@RequiresPermissions("image:onemap:cut")
	@RequestMapping(value = "/dataCenter/cutAdd",method={RequestMethod.POST})
	@ResponseBody
	public ResultBean<String> cutAdd(@Valid @RequestBody CutAddDto cutAddDto){
		ResultBean<String> resultBean = new ResultBean<String>();

		Map<String,String> mapUser = getUserIdAndInPath(cutAddDto.getInPath());

		CutJobDto cutJobDto = new CutJobDto();
		MapManageDto mapManageDto = new  MapManageDto();

		BeanMapper.copyProperties(cutAddDto, cutJobDto);
		BeanMapper.copyProperties(cutAddDto, mapManageDto);

		cutJobDto.setInPath(mapUser.get("in_path"));
		cutJobDto.setCutType(0);
		cutJobDto.setOperationUserId(mapUser.get("operation_user_id"));
		mapManageDto.setOwnerUserId(mapUser.get("owner_user_id"));

		if (cutService.isExistJobName(cutJobDto.getJobName()).getResultData()) {
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage("任务名已存在");
			return resultBean;
		}
		Map<String,Object> res = readDirFile(cutJobDto);//通过inPath读取目录文件
		if(Objects.equals("2002",res.get("code"))){
			resultBean.setResultBean(ResultCode.PARAMS_ERR, res.get("errorMessage").toString());
			return resultBean;
		}

		Map<String,Object> resCheckParms = checkAddCutJob(cutJobDto, mapManageDto);

		cutJobDto.setInPath(gtdataPrefix + StringTool.str2repeat("/" + cutJobDto.getInPath(), '/'));

		mapManageDto.setOutPath(StringTool.str2repeat("/" + mapManageDto.getOutPath(), '/'));
		if(Objects.equals(resCheckParms.get("code"), ResultCode.PARAMS_ERR)){
			resultBean.setResultBean(ResultCode.PARAMS_ERR, resCheckParms.get("errorMessage").toString());
			return resultBean;
		}
		return cutService.onemapCutTypeAdd(cutJobDto, mapManageDto);
	}

	@RequiresPermissions("image:onemap:cut")
	@RequestMapping(value = "/dataCenter/cutUpt",method = RequestMethod.POST)
	@ResponseBody
	public ResultBean<String> cutUpt(@Valid @RequestBody  CutUptDto cutUptDto){
		ResultBean<String> resultBean = new ResultBean<String>();

		Map<String,String> mapUser = getUserIdAndInPath(cutUptDto.getInPath());

		CutJobDto cutJobDto = new CutJobDto();
		MapManageDto mapManageDto = new  MapManageDto();

		BeanMapper.copyProperties(cutUptDto, cutJobDto);
		BeanMapper.copyProperties(cutUptDto, mapManageDto);
		cutJobDto.setCutType(1);
		cutJobDto.setInPath(mapUser.get("in_path"));
		cutJobDto.setOperationUserId(mapUser.get("operation_user_id"));
		mapManageDto.setOwnerUserId(mapUser.get("owner_user_id"));

		if (cutService.isExistJobName(cutJobDto.getJobName()).getResultData()) {
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage("任务名已存在");
			return resultBean;
		}
		Map<String,Object> res = readDirFile(cutJobDto);//通过inPath读取目录文件
		if(Objects.equals("2002",res.get("code"))){
			resultBean.setResultBean(ResultCode.PARAMS_ERR, res.get("errorMessage").toString());
			return resultBean;
		}
		cutJobDto.setInPath(gtdataPrefix + StringTool.str2repeat("/" + cutJobDto.getInPath(), '/'));
		mapManageDto.setOutPath(StringTool.str2repeat("/" + mapManageDto.getOutPath(), '/'));

		Map<String,Object> resCheckParms = checkUptCutJob(cutJobDto, mapManageDto);
		if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
			resultBean.setResultBean(ResultCode.PARAMS_ERR, resCheckParms.get("errorMessage").toString());
			return resultBean;
		}
		return cutService.onemapCutTypeUpdate(cutJobDto, mapManageDto);
	}

	@RequiresPermissions("image:onemap:cut")
	@RequestMapping(value = "/dataCenter/cutRestartAdd",method={RequestMethod.POST})
	@ResponseBody
	public ResultBean<String> cutRestartAdd(@Valid @RequestBody  CutRestartAddDto cutRestartAddDto){
		ResultBean<String> resultBean = new ResultBean<String>();
		CutJobDto cutJobDto = new CutJobDto();
		MapManageDto mapManageDto = new  MapManageDto();

		BeanMapper.copyProperties(cutRestartAddDto, cutJobDto);
		BeanMapper.copyProperties(cutRestartAddDto, mapManageDto);

		ResultBean<CutJobDto> resCutJobDto = cutService.queryCutJobById(cutRestartAddDto.getId());
		if(Objects.equals(resCutJobDto.getCode(), ResultCode.OK)){
			CutJobDto oldCutJobDto = resCutJobDto.getResultData();
			//cutJobDto.setInPath(oldCutJobDto.getInPath());
			//cutJobDto.setJobName(oldCutJobDto.getJobName());
			//cutJobDto.setPriority(oldCutJobDto.getPriority());
			//cutJobDto.setCutType(0);
			Map<String,String> mapUser = getUserIdAndInPath(oldCutJobDto.getInPath());

			BeanMapper.copyProperties(oldCutJobDto, cutJobDto);

			BeanMapper.copyProperties(cutRestartAddDto, cutJobDto);
			BeanMapper.copyProperties(cutRestartAddDto, mapManageDto);

			cutJobDto.setJobid("");
			cutJobDto.setOperationUserId(mapUser.get("operation_user_id"));
			mapManageDto.setOwnerUserId(mapUser.get("owner_user_id"));

			Map<String,Object> res = readDirFile(cutJobDto);//通过inPath读取目录文件
			if(Objects.equals(ResultCode.PARAMS_ERR,res.get("code"))){
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						res.get("errorMessage").toString());
				return resultBean;
			}
			Map<String,Object> resCheckParms = checkAddCutJob(cutJobDto, mapManageDto);
			mapManageDto.setOutPath(StringTool.str2repeat("/" + mapManageDto.getOutPath(), '/'));
			if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
				resultBean.setResultBean(ResultCode.PARAMS_ERR, resCheckParms.get("errorMessage").toString());
				return resultBean;
			}
			resultBean = cutService.onemapCutTypeAdd(cutJobDto, mapManageDto);
			if(Objects.equals(ResultCode.OK, resultBean.getCode())){
				cutService.addCutJobLog(resultBean.getResultData(), resCutJobDto.getResultData());
			}
		}else{
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage(resCutJobDto.getMessage());
		}
		return resultBean;
	}

	@RequiresPermissions("image:onemap:cut")
	@RequestMapping(value = "/dataCenter/cutRestartUpt",method={RequestMethod.POST})
	@ResponseBody
	public ResultBean<String> cutRestartUpt(@Valid @RequestBody  CutRestartUptDto cutRestartUptDto){
		ResultBean<String> resultBean = new ResultBean<String>();
		CutJobDto cutJobDto = new CutJobDto();
		MapManageDto mapManageDto = new  MapManageDto();


		ResultBean<CutJobDto> resCutJobDto = cutService.queryCutJobById(cutRestartUptDto.getId());
		if(Objects.equals(resCutJobDto.getCode(), ResultCode.OK)){
			CutJobDto oldCutJobDto = resCutJobDto.getResultData();
			/*cutJobDto.setInPath(oldCutJobDto.getInPath());
			cutJobDto.setJobName(oldCutJobDto.getJobName());
			cutJobDto.setPriority(oldCutJobDto.getPriority());
			cutJobDto.setCutType(1);*/
			BeanMapper.copyProperties(oldCutJobDto, cutJobDto);

			BeanMapper.copyProperties(cutRestartUptDto, cutJobDto);
			BeanMapper.copyProperties(cutRestartUptDto, mapManageDto);

			Map<String,String> mapUser = getUserIdAndInPath(oldCutJobDto.getInPath());

			cutJobDto.setJobid("");
			cutJobDto.setOperationUserId(mapUser.get("operation_user_id"));
			mapManageDto.setOwnerUserId(mapUser.get("owner_user_id"));

			Map<String,Object> res = readDirFile(cutJobDto);//通过inPath读取目录文件
			if(Objects.equals(ResultCode.PARAMS_ERR,res.get("code"))){
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						res.get("errorMessage").toString());
				return resultBean;
			}

			Map<String,Object> resCheckParms = checkUptCutJob(cutJobDto, mapManageDto);
			if(Objects.equals(resCheckParms.get("code"), ResultCode.PARAMS_ERR)){
				resultBean.setResultBean(ResultCode.PARAMS_ERR, resCheckParms.get("errorMessage").toString());
				return resultBean;
			}
			resultBean = cutService.onemapCutTypeUpdate(cutJobDto, mapManageDto);
			if(Objects.equals(ResultCode.OK, resultBean.getCode())){
				cutService.addCutJobLog(resultBean.getResultData(), resCutJobDto.getResultData());
			}
		}else{
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage(resCutJobDto.getMessage());
		}
		return resultBean;
	}

	/**
	 *
	 * Description: 一张图接口
	 *  @return
	 * @author lishun
	 * @date 2017年7月6日
	 * @return String
	 */
	@RequiresPermissions("image:onemap:cut")
	@RequestMapping(value = "/dataCenter/onemapCut",method={RequestMethod.POST,RequestMethod.GET})
	@ResponseBody
	public ResultBean<String> onemapCut(CutJobDto cutJobDto, MapManageDto mapManageDto){
		ResultBean<String> resultBean = new ResultBean<String>();
		/*mapManageDto.setIsPublish(1);//默认发布
		SysUserShiroDto sysuser = ShiroKit.getSysUser();
		String ownerUser = "";
		if(sysuser.getUserType() == 1){//管理员进行切片;
			String inPath = cutJobDto.getInPath();
			if(StringTool.isNoneBlank(inPath)){//inPath 不允许null
				ownerUser = getOwnerUserForMangemant(inPath);
			}
		}else{
			cutJobDto.setInPath(sysuser.getUsername() + cutJobDto.getInPath());//集市用户文件路径 “/users/用户名/文件相对路径”
			ownerUser = sysuser.getId();
		}
		if(0 == cutJobDto.getIsRestart()){
			resultBean = cut(cutJobDto, mapManageDto, ownerUser, sysuser.getId());
		}else{
			resultBean = isRestartCut(cutJobDto, mapManageDto, ownerUser, sysuser.getId());
		}*/
		return resultBean;
	}
	/*
	 * @Description:重启
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [cutJobDto, mapManageDto, ownerUser, operationUser]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>  
	 */
	private ResultBean<String> isRestartCut(CutJobDto cutJobDto,MapManageDto mapManageDto,
	                                       String ownerUser, String operationUser){
		ResultBean<String> resultBean = new ResultBean<String>();
		/*if (StringTool.isEmpty(cutJobDto.getId())) {
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage("jobid is null or empty");
			return resultBean;
		}
		ResultBean<CutJobDto> resCutJobDto = cutService.queryCutJobById(cutJobDto.getId());
		if(Objects.equals(resCutJobDto.getCode(), ResultCode.OK)){
			CutJobDto oldCutJobDto = resCutJobDto.getResultData();
			cutJobDto.setInPath(oldCutJobDto.getInPath());
			cutJobDto.setJobName(oldCutJobDto.getJobName());
			cutJobDto.setPriority(oldCutJobDto.getPriority());

			Map<String,Object> res = readDirFile(cutJobDto);//通过inPath读取目录文件
			if(Objects.equals(ResultCode.PARAMS_ERR,res.get("code"))){
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						res.get("errorMessage").toString());
				return resultBean;
			}
			Map<String,Object> resCheckParms = null;
			if ("SUCCEEDED".equals(oldCutJobDto.getStatus())) {//任务成功：切片类型是更新
				cutJobDto.setCutType(1);
				resCheckParms = checkUptCutJob(cutJobDto, mapManageDto);
				if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
					resultBean.setResultBean(ResultCode.PARAMS_ERR,
							resCheckParms.get("errorMessage").toString());
					return resultBean;
				}
				resultBean = cutService.onemapCutTypeUpdate(cutJobDto, mapManageDto, ownerUser, operationUser);
			} else if ("FAILED".equals(oldCutJobDto.getStatus())) {
				cutJobDto.setCutType(oldCutJobDto.getCutType());
				if (oldCutJobDto.getCutType() == 1) {
					resCheckParms = checkUptCutJob(cutJobDto, mapManageDto);
					if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
						resultBean.setResultBean(ResultCode.PARAMS_ERR,
								resCheckParms.get("errorMessage").toString());
						return resultBean;
					}
					resultBean = cutService.onemapCutTypeUpdate(cutJobDto, mapManageDto, ownerUser, operationUser);
				} else {
					resCheckParms = checkAddCutJob(cutJobDto, mapManageDto);
					mapManageDto.setOutPath(StringTool.str2repeat("/" + mapManageDto.getOutPath(), '/'));
					if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
						resultBean.setResultBean(ResultCode.PARAMS_ERR,
								resCheckParms.get("errorMessage").toString());
						return resultBean;
					}
					resultBean = cutService.onemapCutTypeAdd(cutJobDto, mapManageDto, ownerUser, operationUser);
				}
			}
			if (resultBean.getCode() == ResultCode.OK) {//添加日志。。。。
				cutService.addCutJobLog(resultBean.getResultData(), oldCutJobDto);
			}
		}else{
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage(resCutJobDto.getMessage());
		}*/
		return resultBean;
	}
	/*
	 * @Description:非重启
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [cutJobDto, mapManageDto, ownerUser, operationUser]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>  
	 */
	private ResultBean<String> cut(CutJobDto cutJobDto,MapManageDto mapManageDto,
	                              String ownerUser, String operationUser){
		ResultBean<String> resultBean = new ResultBean<String>();
		/*if (cutJobDto.getCutType() == null) {
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage("切片类型不能为空");
			return resultBean;
		}
		if (StringTool.isEmpty(cutJobDto.getJobName())) {
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage("任务名不能为空");
			return resultBean;
		}
		if (cutService.isExistJobName(cutJobDto.getJobName()).getResultData()) {
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage("任务名已存在");
			return resultBean;
		}
		Map<String,Object> res = readDirFile(cutJobDto);//通过inPath读取目录文件
		if(Objects.equals("2002",res.get("code"))){
			resultBean.setResultBean(ResultCode.PARAMS_ERR,
					res.get("errorMessage").toString());
			return resultBean;
		}
		cutJobDto.setInPath("gtdata:///users/" + StringTool.str2repeat("/" + cutJobDto.getInPath(), '/'));
		Map<String,Object> resCheckParms = null;
		if (cutJobDto.getCutType() == 0) {
			resCheckParms = checkAddCutJob(cutJobDto, mapManageDto);
			mapManageDto.setOutPath(StringTool.str2repeat("/" + mapManageDto.getOutPath(), '/'));
			if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						resCheckParms.get("errorMessage").toString());
				return resultBean;
			}
			return cutService.onemapCutTypeAdd(cutJobDto, mapManageDto, ownerUser, operationUser);
		} else {
			resCheckParms = checkUptCutJob(cutJobDto, mapManageDto);
			if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						resCheckParms.get("errorMessage").toString());
				return resultBean;
			}
			return cutService.onemapCutTypeUpdate(cutJobDto, mapManageDto, ownerUser, operationUser);
		}*/
		return resultBean;
	}
	/**
	 * @Description:获取文件夹下的文件路径
	 * @author lishun
	 * @date 2017/11/3
	 * @param cutJobDto
	 * @return java.util.Map<java.lang.String,java.lang.Object>
	 */
	public Map<String,Object> readDirFile(CutJobDto cutJobDto){
		Map<String,Object> res = new HashMap<String, Object>();
		String inPath = cutJobDto.getInPath().replace("gtdata:///users", "");//重启获取inPath带有'gtdata:///users'
		try {
			Map<String, Object> map = GtdataFileUtil.getAllFileSizeByPath(inPath);
			if(1 != (Integer) map.get("result")){
				res.put("code", ResultCode.PARAMS_ERR);
				res.put("errorMessage", "输入路径不存在：" + cutJobDto.getInPath());
				return res;
			}else{
				if(GtdataFileUtil.isDirectory(inPath) == 0){
					List<GtdataFile> list= (List<GtdataFile>)GtdataFileUtil.getAllFileByPath(inPath,"").get("list");
					List<String> pathList = new ArrayList<String>();
					if(list.size() == 0 ){
						res.put("code", ResultCode.PARAMS_ERR);
						res.put("errorMessage", "该文件夹不存在文件");
						return res;
					}else{
						for (GtdataFile gtdataFile : list) {
							String fileName = gtdataFile.getFilename();
							if(fileName.indexOf(".") != -1){
								String fileSuffixName = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
								if(".tif".equals(fileSuffixName) || ".tiff".equals(fileSuffixName)){
									pathList.add(gtdataFile.getPath());
								}
							}
						}
					}
					if(pathList.size() > 0){
						res.put("code", ResultCode.OK);
						cutJobDto.setInPathList(pathList);
					}else{
						res.put("code", ResultCode.PARAMS_ERR);
						res.put("errorMessage", "文件夹没有[*.tif(*.tiff)文件]");
						return res;
					}
				}
			}
		} catch (Exception e) {
			res.put("code", "2002");
			res.put("errorMessage", "readDirFile failed");
			e.printStackTrace();
		}
		return res;
	}
	/*
	 * @Description:新增切片参数验证
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [cutjobDto, mapManageDto]  
	 * @return java.util.Map<java.lang.String,java.lang.Object>  
	 */
	private Map<String,Object> checkAddCutJob(CutJobDto cutjobDto,MapManageDto mapManageDto){
		Map<String,Object> res = new HashMap<String, Object>();
		/*if (!StringTool.parasCheck(cutjobDto.getInPath(), mapManageDto.getMapName(),
				cutjobDto.getMaxLayers(), mapManageDto.getOutPath(), mapManageDto.getWaterMark())) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage","mapName,inPath,maxLayers,outPath,waterMark must can be not empty");
			return res;
		}*/
		if (cutService.isExistMapName(mapManageDto.getMapName()).getResultData()) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage",mapManageDto.getMapName() + "图层名已存在");
			return res;
		}
		if (cutService.isExistOutPath(mapManageDto.getOutPath()).getResultData()) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage",mapManageDto.getOutPath() + "输出路径已存在");
			return res;
		}
		if (mapManageDto.getOutPath().indexOf("/map/autotest/") == -1) {
			mapManageDto.setOutPath("/map/autotest/" +  mapManageDto.getOutPath());
		}
		if (!mapManageDto.getOutPath().endsWith("/Layers/_alllayers") &&
				!mapManageDto.getOutPath().endsWith("/Layers/_alllayers/")) {
			mapManageDto.setOutPath(mapManageDto.getOutPath() + "/Layers/_alllayers");
		}
		/*if (Integer.valueOf(cutjobDto.getMaxLayers()) > 20) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage","最大层级必须小于20");
			return res;
		}*/
		return res;
	}
	/*
	 * @Description:更新切片参数验证
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [cutjobDto, mapManageDto]  
	 * @return java.util.Map<java.lang.String,java.lang.Object>  
	 */
	private Map<String,Object> checkUptCutJob(CutJobDto cutjobDto,MapManageDto mapManageDto){
		Map<String,Object> res = new HashMap<String, Object>();
		/*if (!StringTool.parasCheck(cutjobDto.getInPath(), mapManageDto.getMapName(), cutjobDto.getIsCover(),
				cutjobDto.getMaxLayers(), cutjobDto.getMinLayers())) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage","mapName,inPath,isCover,maxLayers,minLayers must can be not empty");
			return res;
		}
		if (Integer.valueOf(cutjobDto.getMaxLayers()) > 20) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage","最大层级必须小于20");
			return res;
		}
		if (Integer.valueOf(cutjobDto.getMinLayers()) < 0 ||
				Integer.valueOf(cutjobDto.getMinLayers()) > Integer.valueOf(cutjobDto.getMaxLayers())) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage","最小层级必须大于0且必须小于最大层级");
			return res;
		}*/

		if (Integer.valueOf(cutjobDto.getMinLayers()) > Integer.valueOf(cutjobDto.getMaxLayers())) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage","最小层级必须大于0且必须小于最大层级");
			return res;
		}
		if (!cutService.isExistMapName(mapManageDto.getMapName()).getResultData()) {
			res.put("code", ResultCode.PARAMS_ERR);
			res.put("errorMessage",mapManageDto.getMapName() + "图层名不存在/该图层切图失败");
			return res;
		}
		return res;
	}
	/**
	 *
	 * Description: 管理员操作，通过输入路径获取用户名
	 *  @param inPath
	 *  @return
	 * @author lishun
	 * @date 2017年8月22日
	 * @return String
	 */
	 private String getOwnerUserForMangemant(String inPath){
		String username = inPath.substring(0, inPath.indexOf("/"));//通过输入路径获取用户名
		if("".equals(username)){
			inPath = inPath.substring(1);
			username = inPath.substring(0,inPath.indexOf("/"));
		}
		SysUserDto isExituser = sysUserService.findByUsername(username,2);
		if(isExituser == null){
			isExituser = new SysUserDto();
			isExituser.setId(StringTool.getUUID());
			isExituser.setUsername(username);
			isExituser.setUserType(2);
			sysUserService.addUser(isExituser);
		}
		return isExituser.getId();
	}
}
