package com.rscloud.ipc.rpc.api.service;

import java.util.List;
import java.util.Map;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.CutJobDto;
import com.rscloud.ipc.rpc.api.dto.CuttingDto;
import com.rscloud.ipc.rpc.api.dto.HbJobDto;
import com.rscloud.ipc.rpc.api.dto.MapManageDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBeanList;


/**
 * 
* @ClassName: 切片服务  
* @Description: TODO
* @author lishun 
* @date 2017年7月17日 下午2:07:25  
*
 */
public interface CutService {


	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [mapName]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.Boolean>  
	 */
	ResultBean<Boolean> isExistMapName(String mapName);
	/**
	 * @Description:TODO
	 * @author lishun  
	 * @date 2017/11/3 
	 * @param [outPath]
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.Boolean>
	 */
	ResultBean<Boolean> isExistOutPath(String outPath);
	/*
	 * @Description:新增切片
	 * @author lishun  
	 * @date 2017/11/3 
	 * @param [cutjobDto, mapManageDto, ownerUser, operationUser]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>  
	 */
	ResultBean<String> onemapCutTypeAdd(CutJobDto cutjobDto, MapManageDto mapManageDto);
	/*
	 * @Description:更新切片
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [cutjobDto, mapManageDto, ownerUser, operationUser]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>  
	 */
	ResultBean<String> onemapCutTypeUpdate(CutJobDto cutjobDto, MapManageDto mapManageDto);
	/**
	 * @Description:TODO
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [newId, oldId, cutJobDto]  
	 * @return void  
	 */
	public void addCutJobLog(String newId,  CutJobDto cutJobDto);
	/**
	 * @Description:TODO
	 * @author lishun  
	 * @date 2017/11/3 
	 * @param [id]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<com.rscloud.ipc.rpc.api.dto.CutJobDto>  
	 */
	public ResultBean<CutJobDto> queryCutJobById(String id);
	/**
	 * @Description:判断任务名
	 * @author lishun  
	 * @date 2017/11/3
	 * @param [jobName]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.Boolean>  
	 */
	ResultBean<Boolean> isExistJobName(String jobName);
	/**
	 * 
	* Description: 通过jobid返回切片状态
	*  @param jobid
	*  @return
	*  @throws Exception 
	* @author lishun 
	* @date 2017年7月17日 
	* @return Map<String,Object>
	 */
	ResultBean<HbJobDto> progress(String jobid);
	/**
	 * 
	* Description: 列表
	*  @param keyword
	*  @param pageNum
	*  @param pageSize
	*  @return 
	* @author lishun 
	* @date 2017年7月21日 
	* @return List<Map<String,Object>>
	 */
	PageResultBean<Map<String, Object>> queryAll(String keyword,String status, int pageNum, int pageSize,String userName);
	/**
	 * 
	* Description: 获取job
	*  @param id
	*  @return 
	* @author lishun 
	* @date 2017年8月9日 
	* @return Map<String,Object>
	 */
	ResultBean<Map<String, Object>> queryById(String id);
	/**
	 *
	* Description: 更新图层
	*  @param record
	* @author lishun
	* @date 2017年8月9日
	* @return void
	 */
	ResultBean<String> updateMapManageById(MapManageDto record);
	/**
	 * 
	* Description: 获取图层列表
	*  @param keyword	
	*  @param pageNum
	*  @param pageSize
	*  @param userName
	*  @return 
	* @author lishun 
	* @date 2017年8月9日 
	* @return List<Map<String,Object>>
	 */
	PageResultBean<Map<String, Object>> queryMapManageAll(String keyword, int pageNum, int pageSize, String userName);


	/**
	 * 
	* Description: 获取日志信息
	*  @param subJobid
	*  @return 
	* @author lishun 
	* @date 2017年8月23日 
	* @return List<Map<String,Object>>
	 */
	ResultBeanList<Map<String, Object>> querylog(String subJobid);
	/**
	 * 
	* Description: 逻辑删除任务信息
	*  @param id 
	* @author lishun 
	* @date 2017年8月24日 
	* @return void
	 */
	void cutJobDel(String id);
	/**
	 * 
	* Description: 提供给api查询切片信息
	*  @param id
	*  @return 
	* @author lishun 
	* @date 2017年9月13日 
	* @return Map<String,Object>
	 */
	ResultBean<Map<String, Object>> getCutJobInfoById(String id);

	/**
	 *
	 * @param cutjobDto
	 */
	ResultBean<String> updateById(CutJobDto cutjobDto);
	/**
	 * 
	* Description: 修改优先级
	*  @param id
	*  @param priority 
	* @author lishun 
	* @date 2017年10月11日 
	* @return void
	 */
	ResultBean<Boolean> cutJobChangePriority(String id, Integer priority);
	/**
	 * 
	* Description: 重启后加入切片任务 
	* @author lishun 
	* @date 2017年10月13日 
	* @return void
	 */
	void initUndoneCutjob();
}
