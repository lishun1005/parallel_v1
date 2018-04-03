package com.rsclouds.gtparallel.service.impl;

import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.HbaseTableService;
import com.rsclouds.gtparallel.core.job.ImageJobServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/19
 */
@Service
public class HbaseTableServiceImpl implements HbaseTableService {
	private static final Logger logger = LoggerFactory.getLogger(HbaseTableServiceImpl.class);
	@Autowired
	public ImageJobServiceImpl imageJobServiceImpl;
	public ResultBean<String> updateJobStatusById(String jobid,String status){
		ResultBean<String> resultBean = new ResultBean<>();
		try {
			imageJobServiceImpl.updateJobState(jobid, status);
			resultBean.setCode(ResultCode.OK);
		} catch (IOException e) {
			resultBean.setResultBean(ResultCode.FAILED, e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return resultBean;
	}
}
