package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.dto.MosaicJobDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBeanList;

import java.util.List;
import java.util.Map;

/**
 * 
 * @ClassName: 镶嵌服务
 * @Description: TODO
 * @author lishun
 * @date 2017年7月17日 下午2:07:25
 *
 */
public interface MosaicService {
	ResultBean<String> mosaicRestart(MosaicJobDto mosaicDto);

	MosaicJobDto getJobByName(String jobName);

	ResultBean<String> mosaicAdd(MosaicJobDto mosaicDto);


	/**
	 * 
	 * Description: TODO
	 * 
	 * @param jobid
	 * @return
	 * @throws Exception
	 * @author lishun
	 * @date 2017年8月11日
	 * @return Map<String,Object>
	 */
	public ResultBean<Map<String, Object>> progressByHbase(String jobid) throws Exception;

	void updateByJobid(MosaicJobDto mosaicDto);

	void updateMosaicJobSub(String id, Integer progress,Integer sortOrder,String jobid);

	List<Map<String, Object>> getMosaicSub(String jobid, String state);
	/**
	 * 
	* Description: TODO
	*  @param keyword
	*  @param status
	*  @param pageNum
	*  @param pageSize
	*  @param userName
	*  @return 
	* @author lishun 
	* @date 2017年8月16日 
	* @return Map<String,Object>
	 */
	PageResultBean<Map<String, Object>> queryAll(String keyword, String status, int pageNum, int pageSize, String userName, String algorithmType);
	/*
	 * @Description:镶嵌任务详细
	 * @author lishun  
	 * @date 2017/10/31
	 * @param [id]  
	 * @return ResultBean<Map<String, Object>>
	 */
	ResultBean<Map<String, Object>> mosaicDetail(String id);
	
	void mosaicDel(String id);

	/*
	 * @Description:获取镶嵌日志
	 * @author lishun  
	 * @date 2017/10/31
	 * @param [mosaicId]  
	 * @return ResultBeanList<Map<String, Object>>
	 */
	ResultBeanList<Map<String, Object>> querylog(String mosaicId);
	
}
