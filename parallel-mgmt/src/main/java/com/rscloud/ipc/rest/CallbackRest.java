package com.rscloud.ipc.rest;

import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.HbaseTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/19
 */
@Controller
public class CallbackRest {

	public static Logger logger = LoggerFactory.getLogger(CallbackRest.class);
	@Autowired
	public HbaseTableService hbaseTableService;
	/*
	 * @Description:影像镶嵌回调
	 * @author lishun  
	 * @date 2018/1/19
	 * @param [jobid, type]  
	 * @return void  
	 */
	@RequestMapping(value = "/callback/mosaic/v3/complete")
	public ResultBean<String> mosaicComplete(String jobid, String type, String ststus){
		return  hbaseTableService.updateJobStatusById(jobid, ststus);
	}
	/*
	 * @Description:变化检测回调
	 * @author lishun  
	 * @date 2018/1/19
	 * @param [jobid, type, ststus]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>  
	 */
	@RequestMapping(value = "/callback/detection/v3/complete")
	public ResultBean<String>  detectionComplete(String jobid, String type, String ststus){
		ResultBean<String> resultBean = new ResultBean<>();
		resultBean.setCode(ResultCode.OK);
		resultBean.setResultData("ignore detection callback action");
		logger.info("ignore detection callback action");
		return resultBean;
	}

}
