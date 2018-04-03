package com.rscloud.ipc.rest;

import com.rabbitmq.client.MessageProperties;
import com.rscloud.ipc.contrller.BaseContrller;
import com.rscloud.ipc.dto.MosaicAddDto;
import com.rscloud.ipc.dto.MosaicQualityAddDto;
import com.rscloud.ipc.rabbitmq.RabbitmqClient;
import com.rscloud.ipc.rpc.api.dic.Constant;
import com.rscloud.ipc.rpc.api.dto.MosaicJobDto;
import com.rscloud.ipc.rpc.api.dto.ProductLineDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.MosaicService;
import com.rscloud.ipc.rpc.api.service.ProductlineService;
import com.rscloud.ipc.utils.gtdata.GtPath;
import com.rscloud.ipc.utils.gtdata.GtdataFile;
import com.rscloud.ipc.utils.gtdata.GtdataFileUtil;
import com.rsclouds.common.utils.BeanMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.rmi.ServerException;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Controller
@Api(value="影像镶嵌api",description="影像镶嵌api")
@RequestMapping(value = "/api/v3/mosaic")
public class MosaicRest extends BaseContrller{
	public static Logger logger = LoggerFactory.getLogger(MosaicRest.class);

	@Autowired
	@Lazy
	private MosaicService mosaicService;
	
	@Value("#{fixparamProperty[queue_mosaic]}")
	protected String queueMosaic;

	@Autowired
	@Lazy
	private ProductlineService productlineService;

	@Value("#{fixparamProperty['auto.proc.modis.map.function']}")
	public String autoProcModisMapFunction;

	@Value("#{fixparamProperty['auto.prod.pl.mosaic.function']}")
	public String autoProdPlMosaicFunction;

	@Value("#{fixparamProperty['auto.prod.quality.evaluate.function']}")
	public String autoProdQualityEvaluateFunction;


	@Value("#{fixparamProperty['auto.prod.gf1pms.mosaic.function']}")
	public String autoProdGf1pmsMosaicFunction;

	@Value("#{fixparamProperty['auto.prod.gf1wfv.mosaic.function']}")
	public String autoProdGf1wfvMosaicFunction;

	@Value("#{fixparamProperty['auto.prod.gf2.mosaic.function']}")
	public String autoProdGf2MosaicFunction;
	/**
	 * @Description: GF1_16m镶嵌
	 * @author lishun
	 * @date 2017年6月26日
	 * @return Map<String,Object>
	 * @throws ServerException
	 */
	@RequestMapping(value="/GF1_16m",method = {RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "GF1_16m镶嵌")
	public ResultBean<String> mosaicGF1_16m(@Valid @RequestBody MosaicAddDto mosaicAddDto, HttpServletRequest request) {
		ResultBean<String> resultBean = new ResultBean<String>();
		MosaicJobDto mosaicJobDto = new MosaicJobDto();
		BeanMapper.copyProperties(mosaicAddDto, mosaicJobDto);
		setAlgorithmType(request, mosaicJobDto);
		if (setModelId(mosaicJobDto)) {
			mosaicJobDto.setGearmanFunc(autoProdGf1wfvMosaicFunction);
			String parms = org.apache.commons.lang.StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
					mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
			mosaicJobDto.setGearmanParms(parms);
			return mosaic(mosaicJobDto, mosaicAddDto.getSign());
		} else {
			resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
			return resultBean;
		}

	}

	/**
	 * @Description: GF1_2m镶嵌
	 * @author lishun
	 * @date 2017年6月26日
	 * @return Map<String,Object>
	 * @throws ServerException
	 */
	@RequestMapping(value="/GF1_2m",method = {RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "GF1_2m镶嵌")
	public ResultBean<String> mosaicGF1_2m(@Valid @RequestBody MosaicAddDto mosaicAddDto, HttpServletRequest request) {
		ResultBean<String> resultBean = new ResultBean<String>();
		MosaicJobDto mosaicJobDto = new MosaicJobDto();
		BeanMapper.copyProperties(mosaicAddDto, mosaicJobDto);
		setAlgorithmType(request, mosaicJobDto);
		if (setModelId(mosaicJobDto)) {
			mosaicJobDto.setGearmanFunc(autoProdGf1pmsMosaicFunction);
			String parms = org.apache.commons.lang.StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
					mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
			mosaicJobDto.setGearmanParms(parms);
			return mosaic(mosaicJobDto, mosaicAddDto.getSign());
		} else {
			resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
			return resultBean;
		}
	}

	/**
	 * @Description: GF2_08m镶嵌
	 * @author lishun
	 * @date 2017年6月26日
	 * @return Map<String,Object>
	 * @throws ServerException
	 */
	@RequestMapping(value="/GF2_08m",method = {RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "GF2-0.8m镶嵌")
	public ResultBean<String> mosaicGF2_08m(@Valid @RequestBody MosaicAddDto mosaicAddDto, HttpServletRequest request) {
		ResultBean<String> resultBean = new ResultBean<String>();
		MosaicJobDto mosaicJobDto = new MosaicJobDto();
		BeanMapper.copyProperties(mosaicAddDto, mosaicJobDto);
		setAlgorithmType(request, mosaicJobDto);
		if (setModelId(mosaicJobDto)) {
			mosaicJobDto.setGearmanFunc(autoProdGf2MosaicFunction);
			String parms = org.apache.commons.lang.StringUtils.join(
					new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
							mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
			mosaicJobDto.setGearmanParms(parms);
			return mosaic(mosaicJobDto, mosaicAddDto.getSign());
		} else {
			resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
			return resultBean;
		}
	}
	/**
	* @Description: pl镶嵌
	* @author lishun 
	* @date 2017年6月26日 
	* @return Map<String,Object>
	 * @throws ServerException 
	 */
	@RequestMapping(value="/pl",method = {RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "PL镶嵌")
	public ResultBean<String> mosaicPL(@Valid @RequestBody MosaicAddDto mosaicAddDto, HttpServletRequest request) {
		ResultBean<String> resultBean = new ResultBean<String>();
		MosaicJobDto mosaicJobDto = new MosaicJobDto();
		BeanMapper.copyProperties(mosaicAddDto, mosaicJobDto);
		setAlgorithmType(request, mosaicJobDto);
		if (setModelId(mosaicJobDto)) {
			mosaicJobDto.setGearmanFunc(autoProdPlMosaicFunction);
			String parms = org.apache.commons.lang.StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
					mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
			mosaicJobDto.setGearmanParms(parms);
			return mosaic(mosaicJobDto, mosaicAddDto.getSign());
		} else {
			resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
			return resultBean;
		}
	}
	/**
	 * 
	* Description: 质量评估
	*  @param mosaicQualityAddDto
	*  @return
	* @author lishun 
	* @date 2017年8月23日 
	* @return Map<String,Object>
	 */
	@RequestMapping(value="/pl_quality",method = {RequestMethod.POST})
	@ResponseBody
	@ApiOperation(value = "PL质量评估")
	public ResultBean<String> mosaicPlQuality(@Valid @RequestBody MosaicQualityAddDto mosaicQualityAddDto, HttpServletRequest request) {
		ResultBean<String> resultBean = new ResultBean<String>();
		MosaicJobDto mosaicJobDto = new MosaicJobDto();
		BeanMapper.copyProperties(mosaicQualityAddDto, mosaicJobDto);
		setAlgorithmType(request, mosaicJobDto);
		if (setModelId(mosaicJobDto)) {
			mosaicJobDto.setGearmanFunc(autoProdQualityEvaluateFunction);
			String parms = org.apache.commons.lang.StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getOutPath()}, ",");
			mosaicJobDto.setGearmanParms(parms);
			return mosaic(mosaicJobDto, mosaicQualityAddDto.getSign());
		} else {
			resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
			return resultBean;
		}
	}
	public ResultBean<String> mosaic(MosaicJobDto mosaicDto,String sign) {
		ResultBean<String> res = new ResultBean<String>();
		try {
			Map<String,Object> checkMap = checkApi(sign);
			if("2001".equals(checkMap.get("code"))){
				String username = checkMap.get("username").toString();
				String userid = sysUserService.findByUsername(username, 2).getId();
				if(Objects.equals("users", username)){
					mosaicDto.setInPath(StringUtils.join(new String[]{username, mosaicDto.getInPath()},"/"));
					mosaicDto.setOutPath(StringUtils.join(new String[]{username, mosaicDto.getOutPath()},"/"));
				}else{
					mosaicDto.setInPath(StringUtils.join(new String[]{"/users" , username, mosaicDto.getInPath()},"/"));
					mosaicDto.setOutPath(StringUtils.join(new String[]{"/users" , username, mosaicDto.getOutPath()},"/"));
				}
				if(!existPath(new GtPath(mosaicDto.getOutPath()).getGtParent())){
					res.setResultBean(ResultCode.FAILED,"输出文件夹不存在");
					return res;
				}
				if(Objects.equals(Constant.MOSAIC_PL_QUALITY,mosaicDto.getAlgorithmType())){
					if(!checkFiles(mosaicDto.getInPath(),1)){//重启无需检查输入路径文件数
						res.setResultBean(ResultCode.FAILED,"数据源文件夹下必须存在(*.tif;*.tiff)文件");
						return res;
					}
				}else{
					if(!checkFiles(mosaicDto.getInPath(),2)){//重启无需检查输入路径文件数
						res.setResultBean(ResultCode.FAILED,"数据源文件夹下必须存在1个以上(*.tif;*.tiff;*.tar.gz)文件");
						return res;
					}
				}
				mosaicDto.setOperationUserId(userid);
				mosaicDto.setOwnerUserId(userid);
				res = mosaicService.mosaicAdd(mosaicDto);
				if(Objects.equals(res.getCode(), ResultCode.OK)){
					RabbitmqClient.getChannel().basicPublish("",
							queueMosaic,MessageProperties.PERSISTENT_TEXT_PLAIN, res.getResultData().getBytes());
				}
			}else {
				res.setResultBean(ResultCode.FAILED,checkMap.get("errorMessage").toString());
				return res;
			}
		}catch (Exception e) {
			res.setResultBean(ResultCode.FAILED,"user authentication failed: system error");
			e.printStackTrace();
		}
		return res;
	}
	/**
	* @Description: 进度查询
	* @author lishun 
	* @date 2017年6月26日 
	* @return Map<String,Object>
	 * @throws ServerException 
	 */
	@RequestMapping(value="/progress",method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation(value = "镶嵌任务进度查询")
	public ResultBean<Map<String, Object>> progress(String jobid,String sign) {
		ResultBean<Map<String, Object>> resultBean = new ResultBean<Map<String, Object>>();
		try {
			if(StringUtils.isNotBlank(jobid)){
				Map<String,Object> checkMap = checkApi(sign);
				if("2001".equals(checkMap.get("code"))){
					resultBean = mosaicService.progressByHbase(jobid);
				}else{
					resultBean.setResultBean(ResultCode.FAILED, checkMap.get("errorMessage").toString());
				}
			}else{
				resultBean.setCode(ResultCode.PARAMS_ERR);
				resultBean.setMessage("jobid is not null");
			}

		}catch (Exception e) {
			resultBean.setResultBean(ResultCode.FAILED, "get job info failed:%s",e.getMessage());
			e.printStackTrace();
		}
		return resultBean;
	}
	private boolean setModelId(MosaicJobDto mosaicJobDto) {
		ResultBean<ProductLineDto> resultBean =
				productlineService.findByIdOrName("", mosaicJobDto.getAlgorithmType());
		if (Objects.equals(resultBean.getCode(), ResultCode.OK) && resultBean.getResultData() != null) {
			mosaicJobDto.setModelId(resultBean.getResultData().getModelId());
			return true;
		} else {
			return false;
		}
	}
	private void setAlgorithmType(HttpServletRequest request, MosaicJobDto mosaicJobDto){
		String servletPath = request.getServletPath();
		servletPath = servletPath.substring(servletPath.lastIndexOf("/") + 1);
		mosaicJobDto.setAlgorithmType(servletPath);
	}
	private boolean existPath(String path){
		path = path.substring(path.indexOf("/users") + 6);
		boolean existPath = false;
		try {
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
	/**
	 *
	 * Description: 文件夹必须存在超过num个(*.tif || *.tiff)文件
	 *  @param outPath
	 *  @return
	 * @author lishun
	 * @date 2017年8月28日
	 * @return boolean
	 */
	private boolean checkFiles(String inPath,int num){// "/user/zmp/pldata"
		boolean isFiles = false;
		String relativePath = inPath.substring(inPath.indexOf("/",1) + 1);// "zmp/pldata"
		String[] paths = relativePath.split("/");
		String username = paths[0];
		try {
			Map<String, Object> map=GtdataFileUtil.getAllFileByPath(relativePath, username);
			List<GtdataFile> list = (List<GtdataFile>) map.get("list");
			if(null != list){
				int i = 0;
				for (GtdataFile gtdataFile : list) {
					String fileName = gtdataFile.getFilename();
					if(fileName.indexOf(".") != -1){
						String fileSuffixName = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
						if(".tif".equals(fileSuffixName) || ".tiff".equals(fileSuffixName) || ".gz".equals(fileSuffixName)){
							i= i + 1;
						}
					}
					if(i >= num){
						return true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isFiles;
	}
}
