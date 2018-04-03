package com.rscloud.ipc.rest;

import com.rscloud.ipc.contrller.BaseContrller;
import com.rscloud.ipc.dto.CutAddDto;
import com.rscloud.ipc.dto.CutUptDto;
import com.rscloud.ipc.rpc.api.dto.CutJobDto;
import com.rscloud.ipc.rpc.api.dto.MapManageDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.CutService;
import com.rscloud.ipc.utils.gtdata.GtdataFile;
import com.rscloud.ipc.utils.gtdata.GtdataFileUtil;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.rmi.ServerException;
import java.util.*;


@Controller
@Api(value="切片api",description="切片api")
public class CuttingRest extends BaseContrller{
	public static Logger logger = LoggerFactory.getLogger(CuttingRest.class);

	@Autowired
	@Lazy
	public CutService cutService;
	
	@Value("#{fixparamProperty[queue_cut]}")
	protected String queueCut;

	@Value("#{fixparamProperty[gtdata_prefix]}")
	protected String gtdataPrefix;
	
	/**
	* @Description:新增 一张图切片
	* @author lishun 
	* @date 2017年6月26日 
	* @return Map<String,Object>
	 * @throws Exception 
	 * @throws ServerException 
	 */
	@RequestMapping(value="/api/v3/cut/addOnemapcut",method = {RequestMethod.POST})
	@ResponseBody
	@ApiOperation(value = "添加切片任务", notes = "添加切片任务")
	public ResultBean<String>  addcutJob(@Valid @RequestBody CutAddDto cutAddDto) {
		ResultBean<String> resultBean = new ResultBean<String>();
		Map<String,Object> checkMap = checkApi(cutAddDto.getSign());
		if("2001".equals(checkMap.get("code"))){
			if (cutService.isExistJobName(cutAddDto.getJobName()).getResultData()) {
				resultBean.setCode(ResultCode.FAILED);
				resultBean.setMessage("任务名已存在");
				return resultBean;
			}

			CutJobDto cutJobDto = new CutJobDto();
			MapManageDto mapManageDto = new  MapManageDto();
			if(null == mapManageDto.getIsPublish()){
				mapManageDto.setIsPublish(0);//不发布
			}
			if(null == cutJobDto.getPriority()){
				cutJobDto.setPriority(10);
			}
			BeanMapper.copyProperties(cutAddDto, cutJobDto);
			BeanMapper.copyProperties(cutAddDto, mapManageDto);
			cutJobDto.setCutType(0);
			String username = checkMap.get("username").toString();
			String userid = sysUserService.findByUsername(username, 2).getId();
			cutJobDto.setOperationUserId(userid);
			mapManageDto.setOwnerUserId(userid);
			if(!Objects.equals("users", username)){
				cutJobDto.setInPath(username + "/" + cutJobDto.getInPath());//用户云盘文件路径 “/users/用户名/文件相对路径”
			}
			Map<String,Object> res  = readDirFile(cutJobDto);
			if(Objects.equals(ResultCode.PARAMS_ERR,res.get("code"))){
				resultBean.setResultBean(ResultCode.FAILED, res.get("errorMessage").toString());
				return resultBean;
			}
			cutJobDto.setInPath(gtdataPrefix + StringTool.str2repeat("/" + cutJobDto.getInPath(), '/'));

			Map<String,Object> resCheckParms = checkAddCutJob(cutJobDto, mapManageDto);

			mapManageDto.setOutPath(StringTool.str2repeat("/" + mapManageDto.getOutPath(), '/'));

			if(Objects.equals(resCheckParms.get("code"), ResultCode.PARAMS_ERR)){
				resultBean.setResultBean(ResultCode.PARAMS_ERR, resCheckParms.get("errorMessage").toString());
				return resultBean;
			}
			return cutService.onemapCutTypeAdd(cutJobDto, mapManageDto);
		}else{
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage(checkMap.get("errorMessage").toString());
			return resultBean;
		}
	}
	/**
	 * 
	*  @return
	* @author lishun 
	* @date 2017年9月13日 
	* @return Map<String,Object>
	 */
	@RequestMapping(value="/api/v3/cut/uptOnemapcut",method = {RequestMethod.POST})
	@ResponseBody
	@ApiOperation(value = "更新切片任务", notes = "更新切片任务")
	public ResultBean<String> uptOnemapcut(@Valid  @RequestBody CutUptDto cutUptDto) {
		ResultBean<String> resultBean = new ResultBean<String>();


		Map<String,Object> checkMap = checkApi(cutUptDto.getSign());
		if("2001".equals(checkMap.get("code"))){
			if (cutService.isExistJobName(cutUptDto.getJobName()).getResultData()) {
				resultBean.setCode(ResultCode.FAILED);
				resultBean.setMessage("任务名已存在");
				return resultBean;
			}
			CutJobDto cutJobDto = new CutJobDto();
			MapManageDto mapManageDto = new  MapManageDto();

			BeanMapper.copyProperties(cutUptDto, cutJobDto);
			BeanMapper.copyProperties(cutUptDto, mapManageDto);
			cutJobDto.setCutType(1);
			if(null == mapManageDto.getIsPublish()){
				mapManageDto.setIsPublish(0);//不发布
			}
			if(null == cutJobDto.getPriority()){
				cutJobDto.setPriority(10);
			}
			String username = checkMap.get("username").toString();
			String userid = sysUserService.findByUsername(username, 2).getId();
			cutJobDto.setOperationUserId(userid);
			mapManageDto.setOwnerUserId(userid);
			if(!Objects.equals("users", username)){
				cutJobDto.setInPath(username + "/" + cutJobDto.getInPath());//用户云盘文件路径 “/users/用户名/文件相对路径”
			}
			cutJobDto.setInPath(gtdataPrefix + StringTool.str2repeat("/" + cutJobDto.getInPath(), '/'));
			Map<String,Object> resCheckParms = checkUptCutJob(cutJobDto, mapManageDto);
			if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
				resultBean.setResultBean(ResultCode.PARAMS_ERR, resCheckParms.get("errorMessage").toString());
				return resultBean;
			}
			return cutService.onemapCutTypeUpdate(cutJobDto, mapManageDto);
		}else{
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage(checkMap.get("errorMessage").toString());
			return resultBean;
		}
		//return cutting(cutJobDto, mapManage, sign);
	}

	/*private Map<String,Object> cutting(CutJobDto cutJobDto,MapManageDto mapManageDto,String sign) {
		Map<String,Object> res = new HashMap<String, Object>();
		try {
			Map<String,Object> checkMap = checkApi(sign);
			if("2001".equals(checkMap.get("code"))){
				if (StringTool.isEmpty(cutJobDto.getJobName())) {
					res.put("code", ResultCode.PARAMS_ERR);
					res.put("errorMessage","任务名不能为空");
					return res;
				}
				if (cutService.isExistJobName(cutJobDto.getJobName()).getResultData()) {
					res.put("code", ResultCode.PARAMS_ERR);
					res.put("errorMessage","任务名已存在");
					return res;
				}
				if(StringTool.isEmpty(cutJobDto.getInPath())){
					res.put("code", ResultCode.PARAMS_ERR);
					res.put("errorMessage", "参数不能为空:inPath");
					return res;
				}
				String username = checkMap.get("username").toString();
				String userid = sysUserService.findByUsername(username, 2).getId();
				cutJobDto.setOperationUserId(userid);
				mapManageDto.setOwnerUserId(userid);

				if(!Objects.equals("users", username)){
					cutJobDto.setInPath(username + "/" + cutJobDto.getInPath());//用户云盘文件路径 “/users/用户名/文件相对路径”
				}

				res = readDirFile(cutJobDto);
				if(Objects.equals(ResultCode.PARAMS_ERR,res.get("code"))){
					return res;
				}
				cutJobDto.setInPath("gtdata:///users/" + StringTool.str2repeat("/" + cutJobDto.getInPath(), '/'));
				ResultBean<String> resultBean = null;
				Map<String,Object> resCheckParms = null;
				if (cutJobDto.getCutType() == 0) {
					mapManageDto.setOutPath(StringTool.str2repeat("/" + mapManageDto.getOutPath(), '/'));
					resCheckParms = checkAddCutJob(cutJobDto, mapManageDto);
					if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
						return resCheckParms;
					}
					resultBean = cutService.onemapCutTypeAdd(cutJobDto, mapManageDto);
				} else {
					resCheckParms = checkUptCutJob(cutJobDto, mapManageDto);
					if(Objects.equals(resCheckParms.get("code"),ResultCode.PARAMS_ERR)){
						return resCheckParms;
					}
					resultBean = cutService.onemapCutTypeUpdate(cutJobDto, mapManageDto);
				}
				if(Objects.equals(resultBean.getCode(), ResultCode.OK)){
					res.put("jobid",resultBean.getResultData());//返回表id字段(不是jobid字段)
				}else{
					res.put("errorMessage",resultBean.getMessage());
				}
				res.put("code",resultBean.getCode());
			}else{
				res.putAll(checkMap);
			}
		} catch (Exception e) {
			res.put("code", ResultCode.FAILED);
			res.put("errorMessage", "请求api失败: 系统错误");
			e.printStackTrace();
		}
		return res;
	}*/
	/**
	* @Description: 一张图切片进度
	* @author lishun 
	* @date 2017年6月26日 
	* @return Map<String,Object>
	 * @throws ServerException 
	 */
	@RequestMapping(value="/api/v3/cut/progress",method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation(value = "切片进度查询", notes = "切片进度查询")
	public ResultBean<Map<String, Object>> progress(String jobid, String sign) {
		ResultBean<Map<String, Object>> resultBean = new ResultBean<Map<String, Object>>();
		if(org.apache.commons.lang3.StringUtils.isNotBlank(jobid)){
			Map<String,Object> checkMap = checkApi(sign);
			if("2001".equals(checkMap.get("code"))){
				resultBean = cutService.getCutJobInfoById(jobid);
				return resultBean;
			}else{
				resultBean.setCode(ResultCode.PARAMS_ERR);
				resultBean.setMessage(checkMap.get("errorMessage").toString());
				return resultBean;
			}
		}else{
			resultBean.setCode(ResultCode.PARAMS_ERR);
			resultBean.setMessage("jobid is not null");
			return resultBean;
		}
	}
	/**
	 * @Description:获取文件夹下的文件路径
	 * @author lishun
	 * @date 2017/11/3
	 * @param [cutJobDto]
	 * @return java.util.Map<java.lang.String,java.lang.Object>
	 */
	private Map<String,Object> readDirFile(CutJobDto cutJobDto){
		Map<String,Object> res = new HashMap<String, Object>();
		try {
			Map<String, Object> map = GtdataFileUtil.getAllFileSizeByPath(cutJobDto.getInPath());
			if(1 != (Integer) map.get("result")){
				res.put("code", ResultCode.PARAMS_ERR);
				res.put("errorMessage", "输入路径不存在：" + cutJobDto.getInPath());
				return res;
			}else{
				if(GtdataFileUtil.isDirectory(cutJobDto.getInPath()) == 0){
					List<GtdataFile> list= (List<GtdataFile>)GtdataFileUtil.getAllFileByPath(cutJobDto.getInPath(),"").get("list");
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
			res.put("code", ResultCode.FAILED);
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
}
