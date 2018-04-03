package com.rscloud.ipc.contrller;

import com.rabbitmq.client.MessageProperties;
import com.rscloud.ipc.dto.SysUserShiroDto;
import com.rscloud.ipc.rabbitmq.RabbitmqClient;
import com.rscloud.ipc.rpc.api.dic.Constant;
import com.rscloud.ipc.rpc.api.dto.MosaicJobDto;
import com.rscloud.ipc.rpc.api.dto.ProductLineDto;
import com.rscloud.ipc.rpc.api.dto.SysUserDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.MosaicService;
import com.rscloud.ipc.rpc.api.service.ProductlineService;
import com.rscloud.ipc.shiro.ShiroKit;
import com.rscloud.ipc.utils.gtdata.GtPath;
import com.rscloud.ipc.utils.gtdata.GtdataFile;
import com.rscloud.ipc.utils.gtdata.GtdataFileUtil;
import com.rsclouds.common.utils.StringTool;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author lishun
 * @ClassName: 影像镶嵌
 * @Description: TODO
 * @date 2017年8月11日 下午4:24:58
 */
@Api(value = "影像镶嵌", description = "影像镶嵌")
@Controller
public class MosaicController extends BaseContrller {
	private static Logger logger = LoggerFactory.getLogger(MosaicController.class);

	@Autowired
	@Lazy
	private MosaicService mosaicService;

	@Autowired
	@Lazy
	private ProductlineService productlineService;

	@Value("#{fixparamProperty[queue_mosaic]}")
	protected String queueMosaic;

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
	 * Description: GF1-16m镶嵌
	 *
	 * @param mosaicJobDto
	 * @return boolean
	 * @throws IOException
	 * @author lishun
	 * @date 2017年9月8日
	 */
	@RequiresPermissions("image:mosaic:GF1_16m")
	@ResponseBody
	@ApiOperation(value = "GF1_16m镶嵌", notes = "GF1_16m镶嵌")
	@RequestMapping(value = "mosaic/GF1_16m", method = {RequestMethod.POST})
	public ResultBean<String> mosaicGF1_16m(MosaicJobDto mosaicJobDto, HttpServletRequest request) {
		ResultBean<String> resultBean = chkGfPlMosaic(mosaicJobDto);
		if (Objects.equals(resultBean.getCode(), ResultCode.OK)) {
			setAlgorithmType(request, mosaicJobDto);
			if (setModelId(mosaicJobDto)) {
				mosaicJobDto.setGearmanFunc(autoProdGf1wfvMosaicFunction);
				String parms = StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
						mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
				mosaicJobDto.setGearmanParms(parms);
				return mosaic(mosaicJobDto);
			} else {
				resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
				return resultBean;
			}
		} else {
			return resultBean;
		}

	}

	/**
	 * Description:GF1-2m镶嵌
	 *
	 * @param mosaicJobDto
	 * @return boolean
	 * @throws IOException
	 * @author lishun
	 * @date 2017年9月8日
	 */
	@RequiresPermissions("image:mosaic:GF1_2m")
	@ResponseBody
	@ApiOperation(value = "GF1_2m镶嵌", notes = "GF1_2m镶嵌")
	@RequestMapping(value = "mosaic/GF1_2m", method = {RequestMethod.POST})
	public ResultBean<String> mosaicGF1_2m(MosaicJobDto mosaicJobDto, HttpServletRequest request) {
		ResultBean<String> resultBean = chkGfPlMosaic(mosaicJobDto);
		if (Objects.equals(resultBean.getCode(), ResultCode.OK)) {
			setAlgorithmType(request, mosaicJobDto);
			if (setModelId(mosaicJobDto)) {
				mosaicJobDto.setGearmanFunc(autoProdGf1pmsMosaicFunction);
				String parms = StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
						mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
				mosaicJobDto.setGearmanParms(parms);
				return mosaic(mosaicJobDto);
			} else {
				resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
				return resultBean;
			}
		} else {
			return resultBean;
		}
	}

	/**
	 * Description: GF2-0.8m镶嵌
	 *
	 * @param mosaicJobDto
	 * @return boolean
	 * @throws IOException
	 * @author lishun
	 * @date 2017年9月8日
	 */
	@RequiresPermissions("image:mosaic:GF2_08m")
	@ResponseBody
	@RequestMapping(value = "mosaic/GF2_08m", method = {RequestMethod.POST})
	public ResultBean<String> mosaicGf2_08(MosaicJobDto mosaicJobDto, HttpServletRequest request) {
		ResultBean<String> resultBean = chkGfPlMosaic(mosaicJobDto);
		if (Objects.equals(resultBean.getCode(), ResultCode.OK)) {
			setAlgorithmType(request, mosaicJobDto);
			if (setModelId(mosaicJobDto)) {
				mosaicJobDto.setGearmanFunc(autoProdGf2MosaicFunction);
				String parms = StringUtils.join(
						new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
								mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
				mosaicJobDto.setGearmanParms(parms);
				return mosaic(mosaicJobDto);
			} else {
				resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
				return resultBean;
			}
		} else {
			return resultBean;
		}
	}

	/**
	 * pl镶嵌
	 * Description: TODO
	 *
	 * @param mosaicJobDto
	 * @return Map<String,Object>
	 * @author lishun
	 * @date 2017年8月22日
	 */
	@RequiresPermissions("image:mosaic:pl")
	@ResponseBody
	@RequestMapping(value = "mosaic/pl", method = {RequestMethod.POST})
	public ResultBean<String> mosaicPL(MosaicJobDto mosaicJobDto, HttpServletRequest request) {
		ResultBean<String> resultBean = chkGfPlMosaic(mosaicJobDto);
		if (Objects.equals(resultBean.getCode(), ResultCode.OK)) {
			setAlgorithmType(request, mosaicJobDto);
			if (setModelId(mosaicJobDto)) {
				mosaicJobDto.setGearmanFunc(autoProdPlMosaicFunction);
				String parms = StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getProject(),
						mosaicJobDto.getOutBand().toString(), mosaicJobDto.getOutImage().toString(), mosaicJobDto.getOutPath()}, ",");
				mosaicJobDto.setGearmanParms(parms);
				return mosaic(mosaicJobDto);
			} else {
				resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
				return resultBean;
			}
		} else {
			return resultBean;
		}
	}

	/**
	 * pl质量评估
	 * Description: TODO
	 *
	 * @param mosaicJobDto
	 * @return Map<String,Object>
	 * @author lishun
	 * @date 2017年8月22日
	 */
	@RequiresPermissions("image:mosaic:pl_quality")
	@ResponseBody
	@RequestMapping(value = "mosaic/pl_quality", method = {RequestMethod.POST})
	public ResultBean<String> mosaicPlQuality(MosaicJobDto mosaicJobDto, HttpServletRequest request) {
		setAlgorithmType(request, mosaicJobDto);
		if (setModelId(mosaicJobDto)) {
			mosaicJobDto.setGearmanFunc(autoProdQualityEvaluateFunction);
			String parms = StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getOutPath()}, ",");
			mosaicJobDto.setGearmanParms(parms);
			return mosaic(mosaicJobDto);
		} else {
			ResultBean<String> resultBean = new ResultBean<>();
			resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
			return resultBean;
		}
	}

	/**
	 * Description: TODO
	 *
	 * @param mosaicJobDto
	 * @return Map<String,Object>
	 * @author lishun
	 * @date 2017年8月22日
	 */
	@RequiresPermissions("image:mosaic:modis")
	@ResponseBody
	@RequestMapping(value = "mosaic/modis", method = {RequestMethod.POST})
	public ResultBean<String> mosaicModis(MosaicJobDto mosaicJobDto, HttpServletRequest request) {
		setAlgorithmType(request, mosaicJobDto);
		if (setModelId(mosaicJobDto)) {
			mosaicJobDto.setGearmanFunc(autoProcModisMapFunction);
			String parms = StringUtils.join(new String[]{mosaicJobDto.getInPath(), mosaicJobDto.getOutPath()
					, mosaicJobDto.getProject()}, ",");
			mosaicJobDto.setGearmanParms(parms);
			return mosaic(mosaicJobDto);
		} else {
			ResultBean<String> resultBean = new ResultBean<>();
			resultBean.setResultBean(ResultCode.FAILED, "不存在生产线类型:%s,请添加", mosaicJobDto.getAlgorithmType());
			return resultBean;
		}
	}


	private ResultBean<String> mosaic(MosaicJobDto mosaicJobDto) {
		ResultBean<String> result = new ResultBean<String>();
		SysUserShiroDto sysuser = ShiroKit.getSysUser();
		String ownerUser = "";
		if (Objects.equals(mosaicJobDto.getIsRestart(), 0)) {
			if (!StringTool.parasCheck(mosaicJobDto.getOutPath(), mosaicJobDto.getInPath(), mosaicJobDto.getJobName())) {
				result.setResultBean(ResultCode.PARAMS_ERR,
						"invalid value for request parameter:inPath,outPath,jobName");
				return result;
			}
			if (mosaicService.getJobByName(mosaicJobDto.getJobName()) != null) {
				result.setResultBean(ResultCode.PARAMS_ERR, "任务名已存在");
				return result;
			}
			if (Objects.equals(sysuser.getUserType(), Constant.USERTYPE_SYS)) {
				mosaicJobDto.setOutPath(StringUtils.join(new String[]{"/users", mosaicJobDto.getOutPath()}, "/"));
				mosaicJobDto.setInPath(StringUtils.join(new String[]{"/users", mosaicJobDto.getInPath()}, "/"));
				ownerUser = getOwnerUserForMangemant(mosaicJobDto.getInPath());
			} else {
				mosaicJobDto.setOutPath(StringUtils.join(new String[]{"/users", sysuser.getUsername(),
						mosaicJobDto.getOutPath()}, "/"));
				mosaicJobDto.setInPath(StringUtils.join(new String[]{"/users", sysuser.getUsername(),
						mosaicJobDto.getInPath()}, "/"));
				ownerUser = sysuser.getId();
			}
			mosaicJobDto.setOutPath(StringTool.str2repeat(mosaicJobDto.getOutPath(), '/'));
			mosaicJobDto.setInPath(StringTool.str2repeat(mosaicJobDto.getInPath(), '/'));
			result = chkOutPath(mosaicJobDto);
			if (!Objects.equals(result.getCode(), ResultCode.OK)) {
				return result;
			}
			result = chkInPath(mosaicJobDto);
			if (!Objects.equals(result.getCode(), ResultCode.OK)) {
				return result;
			}
			mosaicJobDto.setOwnerUserId(ownerUser);
			mosaicJobDto.setOperationUserId(sysuser.getId());
			result = mosaicService.mosaicAdd(mosaicJobDto);
		} else {
			if (!StringTool.parasCheck(mosaicJobDto.getOutPath(), mosaicJobDto.getJobid())) {
				result.setResultBean(ResultCode.PARAMS_ERR,
						"invalid value for request parameter:outPath,jobId");
				return result;
			}
			if (Objects.equals(sysuser.getUserType(), Constant.USERTYPE_SYS)) {
				mosaicJobDto.setOutPath(StringUtils.join(new String[]{"/users", mosaicJobDto.getOutPath()}, "/"));
				ownerUser = getOwnerUserForMangemant(mosaicJobDto.getInPath());
			} else {
				mosaicJobDto.setOutPath(StringUtils.join(new String[]{"/users", sysuser.getUsername(),
						mosaicJobDto.getOutPath()}, "/"));
				ownerUser = sysuser.getId();
			}
			mosaicJobDto.setOutPath(StringTool.str2repeat(mosaicJobDto.getOutPath(), '/'));
			result = chkOutPath(mosaicJobDto);
			if (!Objects.equals(result.getCode(), ResultCode.OK)) {
				return result;
			}
			mosaicJobDto.setOwnerUserId(ownerUser);
			mosaicJobDto.setOperationUserId(sysuser.getId());
			result = mosaicService.mosaicRestart(mosaicJobDto);
		}
		if(Objects.equals(result.getCode(), ResultCode.OK)){//加入队列
			try {
				RabbitmqClient.getChannel().basicPublish("",
						queueMosaic, MessageProperties.PERSISTENT_TEXT_PLAIN, result.getResultData().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
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

	private ResultBean<String> chkGfPlMosaic(MosaicJobDto mosaicJobDto) {
		ResultBean<String> result = new ResultBean<String>();
		if (!Objects.equals("3857", mosaicJobDto.getProject()) && !Objects.equals("4326", mosaicJobDto.getProject())) {
			result.setResultBean(ResultCode.PARAMS_ERR, "%s,Invalid value for projects", mosaicJobDto.getProject());
			return result;
		}
		if (!Objects.equals(8, mosaicJobDto.getOutImage()) && !Objects.equals(16, mosaicJobDto.getOutImage())) {
			result.setResultBean(ResultCode.PARAMS_ERR, "%s,Invalid value for outImage", mosaicJobDto.getOutImage());
			return result;
		}
		if (!Objects.equals(3, mosaicJobDto.getOutBand()) && !Objects.equals(4, mosaicJobDto.getOutBand())) {
			result.setResultBean(ResultCode.PARAMS_ERR, "%s,Invalid value for outBand", mosaicJobDto.getOutBand());
			return result;
		}
		result.setCode(ResultCode.OK);
		return result;
	}

	private ResultBean<String> chkOutPath(MosaicJobDto mosaicJobDto) {
		ResultBean<String> result = new ResultBean<String>();
		if (!checkSuffixName(mosaicJobDto.getOutPath(), mosaicJobDto.getAlgorithmType())) {
			result.setResultBean(ResultCode.PARAMS_ERR, "输出文件名格式错误");
			return result;
		}
		if (!existPath(new GtPath(mosaicJobDto.getOutPath()).getGtParent())) {
			result.setResultBean(ResultCode.PARAMS_ERR, "输出文件夹不存在");
			return result;
		}
		result.setCode(ResultCode.OK);
		return result;
	}

	/**
	 * Description: 检查文件名后缀
	 *
	 * @param path
	 * @param algorithmType
	 * @return boolean
	 * @author lishun
	 * @date 2017年8月28日
	 */
	private boolean checkSuffixName(String path, String algorithmType) {
		boolean validateName = false;
		GtPath gtPath = new GtPath(path);
		String fileSuffixName = gtPath.getSuffixName();
		if (Objects.equals(Constant.MOSAIC_PL_QUALITY, algorithmType)) {//质量评估删除*.txt文件
			if (".txt".equals(fileSuffixName)) {
				validateName = true;
			}
		} else {//PL GF modis镶嵌输出tif文件
			if (".tiff".equals(fileSuffixName) || ".tif".equals(fileSuffixName)) {
				validateName = true;
			}
		}
		return validateName;
	}

	private ResultBean<String> chkInPath(MosaicJobDto mosaicJobDto) {
		ResultBean<String> result = new ResultBean<String>();
		if (!existPath(mosaicJobDto.getInPath())) {
			result.setResultBean(ResultCode.PARAMS_ERR, "输入源路径不存在");
			return result;
		}
		if (Objects.equals(Constant.MOSAIC_PL_QUALITY, mosaicJobDto.getAlgorithmType())) {
			if (!checkFiles(mosaicJobDto.getInPath(), 1)) {
				result.setResultBean(ResultCode.PARAMS_ERR, "数据源文件夹下必须存在(*.tif;*.tiff)文件");
				return result;
			}
		} else {
			if (!checkFiles(mosaicJobDto.getInPath(), 2)) {//重启无需检查输入路径文件数
				result.setResultBean(ResultCode.PARAMS_ERR, "数据源文件夹下必须存在1个以上(*.tif;*.tiff;*.tar.gz)文件");
				return result;
			}
		}
		result.setCode(ResultCode.OK);
		return result;
	}

	/*
	 * @Description:
	 * @author lishun
	 * @date 2017/11/29
	 * @param [path]  
	 * @return boolean  
	 */
	private boolean existPath(String path) {
		if (path.indexOf("/users") != -1) {
			path = path.substring(6);
		}
		boolean existPath = false;
		try {
			Map<String, Object> map = GtdataFileUtil.getAllFileSizeByPath(path);
			if (98 == (Integer) map.get("result")) {
				existPath = false;
			} else if (1 == (Integer) map.get("result")) {
				existPath = true;
			} else {
				logger.info(map.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return existPath;
	}

	/**
	 * Description: 管理员操作，通过输入路径获取用户名
	 *
	 * @param inPath
	 * @return String
	 * @author lishun
	 * @date 2017年8月22日
	 */
	private String getOwnerUserForMangemant(String inPath) {
		String username = "";
		String path[] = StringUtils.splitByWholeSeparator(inPath, "/");
		if (inPath.indexOf("/users") != -1) {
			if (path.length > 1) {
				username = path[1];
			}
		} else {
			if (path.length > 0) {
				username = path[0];
			}
		}
		if ("".equals(username)) {
			logger.info("获取用户失败,{}", inPath);
			return "";
		} else {
			SysUserDto isExituser = sysUserService.findByUsername(username, 2);
			if (isExituser == null) {
				isExituser = new SysUserDto();
				isExituser.setId(StringTool.getUUID());
				isExituser.setUsername(username);
				isExituser.setUserType(2);
				sysUserService.addUser(isExituser);
			}
			return isExituser.getId();
		}
	}

	/**
	 * Description: 文件夹必须存在超过num个(*.tif || *.tiff)文件
	 *
	 * @param inPath
	 * @return boolean
	 * @author lishun
	 * @date 2017年8月28日
	 */
	private boolean checkFiles(String inPath, int num) {// "/user/zmp/pldata"
		boolean isFiles = false;
		String relativePath = inPath.substring(inPath.indexOf("/", 1) + 1);// "zmp/pldata"
		String[] paths = relativePath.split("/");
		String username = paths[0];
		try {
			Map<String, Object> map = GtdataFileUtil.getAllFileByPath(relativePath, username);
			List<GtdataFile> list = (List<GtdataFile>) map.get("list");
			if (null != list) {
				int i = 0;
				for (GtdataFile gtdataFile : list) {
					String fileName = gtdataFile.getFilename();
					if (fileName.indexOf(".") != -1) {
						String fileSuffixName = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
						if (".tif".equals(fileSuffixName) || ".tiff".equals(fileSuffixName) || ".gz".equals(fileSuffixName)) {
							i = i + 1;
						}
					}
					if (i >= num) {
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
