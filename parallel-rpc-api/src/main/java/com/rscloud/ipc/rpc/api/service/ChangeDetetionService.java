package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.dto.ChangeDetectionDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;

import java.io.IOException;
import java.util.Map;

/**
 * @author lishun
 * @Description: TODO
 * @date 2017/11/5
 */
public interface ChangeDetetionService {
	/*
	 * @Description:
	 * @author lishun  
	 * @date 2017/11/5
	 * @param [jobid]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<com.rscloud.ipc.rpc.api.dto.ChangeDetectionDto>  
	 */
	ResultBean<ChangeDetectionDto> queryByJobid(String jobid);
	/*
	 * @Description:添加变化检测任务
	 * @author lishun  
	 * @date 2017/11/5
	 * @param [changeDetectionDto]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>  
	 */
	ResultBean<String> add(ChangeDetectionDto changeDetectionDto) throws IOException;
	/*
	 * @Description:查询进度，(目前查只有查询进度才会更新数据库任务状态)
	 * @author lishun  
	 * @date 2017/11/5
	 * @param [jobid]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.util.Map<java.lang.String,java.lang.Object>>  
	 */
	ResultBean<Map<String, Object>> progress(String jobid);
}
