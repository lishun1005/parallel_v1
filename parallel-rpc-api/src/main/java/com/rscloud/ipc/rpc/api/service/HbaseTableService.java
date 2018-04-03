package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.result.ResultBean;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/19
 */
public interface HbaseTableService {
	/*
	 * @Description:更新job表状态
	 * @author lishun  
	 * @date 2018/1/19
	 * @param [jobid, status]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>  
	 */
	ResultBean<String> updateJobStatusById(String jobid, String status);
}
