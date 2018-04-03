package com.rscloud.ipc.rest;

import com.rscloud.ipc.contrller.BaseContrller;
import com.rscloud.ipc.rpc.api.dto.ChangeDetectionDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.ChangeDetetionService;
import com.rscloud.ipc.utils.gtdata.GtdataFileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.rsclouds.common.utils.StringTool.*;


@Controller
@Api(value="变化检测api",description="变化检测api")
public class ChangeDetectionRest extends BaseContrller{
	public static Logger logger = LoggerFactory.getLogger(ChangeDetectionRest.class);

	@Autowired
	@Lazy
	public ChangeDetetionService changeDetetionService;


	@Value("#{fixparamProperty['change.detection.function']}")
	public String changeDetectionFunction;

	@Value("#{fixparamProperty[change_detection_imagefile_path]}")
	public String imagefilePath;

	/*
	 * @Description:新增 变化检测
	 * @author lishun
	 * @date 2017/11/5
	 * @param [changeDetectionDto]
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>
	 */
	@RequestMapping(value="/api/v3/changeDetetion/addJob",method = {RequestMethod.POST})
	@ResponseBody
	@ApiOperation(value = "添加变化检测任务", notes = "添加变化检测任务")
	public ResultBean<String> addJob(@Valid ChangeDetectionDto changeDetectionDto){
		ResultBean<String> resultBean = new ResultBean<String>();
		try {
			changeDetectionDto.setId(getUUID());
			changeDetectionDto.setStartTime(new Date());
			changeDetectionDto.setStatus("ACCEPTED");
			changeDetectionDto.setImagePath(imagefilePath + getUUID()+ ".png");
			String params = StringUtils.join(new String[] {changeDetectionDto.getCoordinateX1().toString(),
					changeDetectionDto.getCoordinateY1().toString(), changeDetectionDto.getCoordinateX2().toString(),
					changeDetectionDto.getCoordinateY2().toString(),changeDetectionDto.getLayer().toString(),
					changeDetectionDto.getImagePath()} , ",");
			changeDetectionDto.setGearmanParms(params);
			changeDetectionDto.setGearmanFunc(changeDetectionFunction);
			return changeDetetionService.add(changeDetectionDto);
		}catch (Exception ex){
			logger.error(ex.getMessage(),ex);
			resultBean.setResultBean(ResultCode.FAILED, ex.getMessage());
		}
		return resultBean;
	}
	@RequestMapping(value="/api/v3/changeDetetion/queryFile",method = {RequestMethod.GET})
	@ResponseBody
	@ApiOperation(value = "变化检测生成文件", notes = "变化检测生成文件")
	public ResultBean<String> queryFile(String jobid) throws Exception {
		ResultBean<String> resultBean = new ResultBean<String>();
		if(StringUtils.isBlank(jobid)){
			resultBean.setResultBean(ResultCode.PARAMS_ERR, "params is can’t be null ");
			return resultBean;
		}
		ResultBean<ChangeDetectionDto> result = changeDetetionService.queryByJobid(jobid);
		if(Objects.equals(result.getCode(), ResultCode.OK)){
			String path = result.getResultData().getImagePath().replace("/users", "");
			Boolean flag = GtdataFileUtil.showDownloadFile(path, getResponse());
			if(!flag){
				resultBean.setResultBean(ResultCode.FAILED, "文件不存在,%s", path);
			}
		}else{
			resultBean.setResultBean(ResultCode.FAILED, result.getMessage());
		}
		return resultBean;
	}
	@RequestMapping(value="/api/v3/changeDetetion/progress",method = {RequestMethod.POST,RequestMethod.GET})
	@ResponseBody
	@ApiOperation(value = "变化检测进度查询", notes = "变化检测进度查询")
	public ResultBean<Map<String, Object>> progress(String jobid){
		ResultBean<Map<String, Object>> resultBean = new ResultBean<Map<String, Object>>();
		if(!parasCheck(jobid)){
			resultBean.setResultBean(ResultCode.FAILED, "params is can’t be null");
			return resultBean;
		}
		return changeDetetionService.progress(jobid);
	}
}
