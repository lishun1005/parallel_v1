package com.rsclouds.gtparallel.thread;

import com.rscloud.ipc.rpc.api.dto.CutJobDto;
import com.rscloud.ipc.rpc.api.dto.CuttingDto;
import com.rscloud.ipc.rpc.api.dto.HbJobDto;
import com.rscloud.ipc.rpc.api.dto.MapManageDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.CutService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.ShellExecutor;
import com.rsclouds.common.utils.StringTool;
import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.entity.Job;
import com.rsclouds.gtparallel.core.job.JobHbase;
import com.rsclouds.gtparallel.geowebcache.GwcConfig;
import com.rsclouds.gtparallel.geowebcache.GwcConfigBean;
import com.rsclouds.gtparallel.geowebcache.GwcService;
import com.rsclouds.gtparallel.utils.SpringContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class CutThread extends Thread implements Comparable<CutThread>{

	private static Logger logger = LoggerFactory.getLogger(CutThread.class);
	private CutService cutService = (CutService) SpringContextUtils.getBean("cutService");
	private GwcService gwcService = (GwcService) SpringContextUtils.getBean("gwcService");

	public static String cutHost = SpringContextUtils.getProperties("appProperty","master.host");
	public static String cutPort = SpringContextUtils.getProperties("appProperty","master.port");
	public static String cutUserName = SpringContextUtils.getProperties("appProperty","master.username");
	public static String cutPassword = SpringContextUtils.getProperties("appProperty","master.password");
	public static String appJarPath = SpringContextUtils.getProperties("appProperty","app.jar.path");
	private CutJobDto cutjobDto;
	private MapManageDto mapManageDto;
	public CutThread(CutJobDto c, MapManageDto m){
		this.cutjobDto = c;
		this.mapManageDto = m;
	}

	public String getJobid(){
		return cutjobDto.getId();
	}
	@Override
	public int compareTo(CutThread ct) {
		Integer a = ct.cutjobDto.getPriority();
		Integer b = this.cutjobDto.getPriority();
		return (a < b) ? -1 : ((a == b) ? 0 : 1);
	}


	@Override
	public void run() {
		try {
			CuttingDto cuttingDto = new CuttingDto();
			BeanMapper.copy(mapManageDto, cuttingDto);
			BeanMapper.copy(cutjobDto, cuttingDto);
			if(StringUtils.isNotBlank(cutjobDto.getJobid())){//重启后重新查询未完成的切片
				progress(cutjobDto.getJobid(), cuttingDto);
			}else{
				if(1 == cutjobDto.getCutType()){
					if(Integer.parseInt(cutjobDto.getMaxLayers()) <= Integer.parseInt(mapManageDto.getShowMaxLayers())){
						cuttingDto.setBxmlUpdate(0);
					}
					if(null != cutjobDto.getRealTime()){//实时更新参数
						cuttingDto.setRealTime(cutjobDto.getRealTime().getTime());
					}
				}
				ResultBean<String> resultBean = processCutting(cuttingDto);
				if(Objects.equals(resultBean.getCode(), ResultCode.OK)){
					progress(resultBean.getResultData(),cuttingDto);
				}else{
					cutjobDto.setLog(resultBean.getMessage());
					jobFailed();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);;
		}
	}
	/**
	* @Description: 执行切片
	* @param [cuttingDto]
	* @return com.rscloud.ipc.rpc.api.result.ResultBean<java.lang.String>
	* @throws
	* @author lishun
	* @date 2018/3/22 10:51
	*/
	public ResultBean<String> processCutting(CuttingDto cuttingDto) {
		ResultBean<String> resultBean = new  ResultBean<String>();
		Map<String,Object> result = new HashMap<String,Object>();
		try {
			cuttingDto.setJobid(StringTool.getUUID());
			createCutJobToHb(cuttingDto);
			execCutShell(cuttingDto);
			resultBean.setCode(ResultCode.OK);
			resultBean.setResultData(cuttingDto.getJobid());
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			JobHbase.setJobState(cuttingDto.getJobid(), CoreConfig.JOB_STATE.FAILED,
					e.getMessage(), System.currentTimeMillis());
			resultBean.setResultBean(ResultCode.FAILED, "hbase error:%s" ,e.getMessage());
		}
		return resultBean;
	}
	/**
	* @Description: 查询进度
	* @param [jobid, cuttingDto]
	* @return void
	* @throws
	* @author lishun
	* @date 2018/3/22 10:51 
	*/
	public void progress(String jobid, CuttingDto cuttingDto) throws Exception{
		ResultBean<HbJobDto> resultBean = null;
		ResultBean<String> resultString = null;
		while(true){
			Thread.sleep(1000);
			resultBean  = cutService.progress(jobid);//查询job状态
			Double progressRes = -1.0;
			if(resultBean.getCode() == ResultCode.OK){
				HbJobDto hbJobDto = resultBean.getResultData();
				progressRes = Double.valueOf(hbJobDto.getProgress());
				cutjobDto.setJobid(jobid);
				if(CoreConfig.JOB_STATE.ACCEPTED.toString().equals(hbJobDto.getState()) ||
						CoreConfig.JOB_STATE.RUNNING.toString().equals(hbJobDto.getState())){
					if(hbJobDto.getStartTime() != null){
						cutjobDto.setStartTime(new Date(Long.parseLong(hbJobDto.getStartTime())));
					}
					if(Double.compare(cutjobDto.getProgress(), progressRes) != 0){ //状态改变
						cutjobDto.setProgress(progressRes);
						cutjobDto.setStatus(hbJobDto.getState());
						cutService.updateById(cutjobDto);
					}
				}else if(Objects.equals(CoreConfig.JOB_STATE.SUCCEEDED.toString(), hbJobDto.getState())){
					Map<String,String> res = generateCofigAndPublish(cuttingDto);
					long endTime = System.currentTimeMillis();
					if(Objects.equals("1", res.get("code"))){
						JobHbase.setJobState(cuttingDto.getJobid(), CoreConfig.JOB_STATE.SUCCEEDED, "", endTime);
						cutjobDto.setEndTime(new Date(endTime));
						cutjobDto.setGeowebcacheUrl(hbJobDto.getGeowebcacheUrl());
						cutjobDto.setProgress(progressRes);
						cutjobDto.setStatus(CoreConfig.JOB_STATE.SUCCEEDED.toString());
						resultString = cutService.updateById(cutjobDto);
						if(!Objects.equals(resultString.getCode(), ResultCode.OK)){
							logger.error("update cutjob failed");
						}
						mapManageDto.setGeoRange(res.get("geo_range"));
						resultString = cutService.updateMapManageById(mapManageDto);
						if(!Objects.equals(resultString.getCode(), ResultCode.OK)){
							logger.error("update cutjob failed");
						}
						logger.info("cutting job success");
					}else{
						JobHbase.setJobState(cuttingDto.getJobid(), CoreConfig.JOB_STATE.FAILED, res.get("message"), endTime);
						cutjobDto.setLog(res.get("message"));
						jobFailed();
					}
					break;
				}else if(Objects.equals(CoreConfig.JOB_STATE.FAILED.toString(), hbJobDto.getState())){
					cutjobDto.setLog(hbJobDto.getLog());
					jobFailed();
					break;
				}else{
					Thread.sleep(5000);
					logger.info("query progress error result= {}", hbJobDto);
				}
			}else{
				cutjobDto.setLog(resultBean.getMessage());
				jobFailed();
				break;
			}
		}
	}/*
	* @Description: 执行远程切割命令
	* @param [cuttingDto]
	* @return void
	* @throws
	* @author lishun
	* @date 2018/3/22 10:17
	*/
	public void execCutShell(CuttingDto cuttingDto) throws Exception{
		String hdfsPath = "";
		if(cuttingDto.getHdfsInPath() == null){
			hdfsPath = cuttingDto.getInPath();
		}else{
			hdfsPath = cuttingDto.getHdfsInPath();
		}
		ShellExecutor exe = new ShellExecutor(cutHost,  Integer.valueOf(cutPort), cutUserName, cutPassword);
		StringBuilder command = new StringBuilder();
		command.append(appJarPath).append(" ");//水印方案 0 全部瓦片生产水印，1 （ChinaRS,14）,2(ChinaRS)
		if(cuttingDto.getRealTime()!=null){
			command.append(CoreConfig.JOB_OP_REALTIMEUPT).append(" ");
		}else{
			command.append(CoreConfig.JOB_OP_ONEMAP).append(" ");
		}
		command.append(cuttingDto.getJobid()).append(" ");
		command.append(hdfsPath.toString()).append(" ");
		command.append(cuttingDto.getOutPath()).append(" ");
		command.append(cuttingDto.getMaxLayers()).append(" ");

		if(cuttingDto.getMaxResolution() != null){
			command.append(" -maxLayer_resolution ").append(cuttingDto.getMaxResolution());
		}
		if(cuttingDto.getMinResolution() != null){
			command.append(" -minLayer_resolution ").append(cuttingDto.getMinResolution());
		}
		if(cuttingDto.getWaterMark() != null){
			if("-1".equals(cuttingDto.getWaterMark())){
				command.append(" -watermark ").append("false");
			}else{
				command.append(" -watermark ").append("true");
				command.append(" -waterMarkScheme ").append(cuttingDto.getWaterMark());
			}

		}
		if(cuttingDto.getMinLayers() != null){
			command.append(" -minLayers ").append(cuttingDto.getMinLayers());
		}
		if(cuttingDto.getZeroPercentage() != null){
			command.append(" -zero_percentage ").append(cuttingDto.getZeroPercentage());
		}
		if(cuttingDto.getRealTime()!= null){
			command.append(" -time ").append(cuttingDto.getRealTime());
		}
		if(cuttingDto.getbSaveStorage() != null) {
			command.append(" -save_storage ").append(cuttingDto.getbSaveStorage());
		}
		if (cuttingDto.getbRabbitMQ() != null) {
			command.append(" -bRabbitMQ ").append(cuttingDto.getbRabbitMQ());
		}
		if (cuttingDto.getNodataInteger() != null) {
			command.append(" -nodata ").append(cuttingDto.getNodataInteger());
		}
		if (cuttingDto.getIsCover() != null) {
			command.append(" -bcover ").append(cuttingDto.getIsCover());
		}
		logger.info("{}",command);
		exe.execRemoteAsynchronous(command.toString());
		logger.info("end Asynchronous shell");
	}
	/**
	* @Description: 添加切片记录
	* @param [cuttingDto]
	* @return void
	* @throws
	* @author lishun
	* @date 2018/3/22 10:47 
	*/
	private void createCutJobToHb(CuttingDto cuttingDto) throws IOException {
		Job newJob = new Job();
		newJob.setRowKey(cuttingDto.getJobid());
		newJob.setState(CoreConfig.JOB_STATE.ACCEPTED.toString());
		if(null != cuttingDto.getRealTime()){
			newJob.setType(CoreConfig.JOB_TYPE.REALTIMEUPT.toString());
		}else{
			newJob.setType(CoreConfig.JOB_TYPE.ONEMAP.toString());
		}
		newJob.setProgress("0");
		newJob.setInPath(cuttingDto.getInPath());
		newJob.setOutPath(cuttingDto.getOutPath());
		newJob.setStartTime(String.valueOf(System.currentTimeMillis()));
		newJob.setMapName(cuttingDto.getMapName());
		JobHbase.createJob(newJob);
	}
	/*
	 * @Description:切片失败，更新状态
	 * @author lishun  
	 * @date 2018/1/29
	 * @param []  
	 * @return void  
	 */
	private void jobFailed(){
		ResultBean<String> resultString = null;
		cutjobDto.setStatus(CoreConfig.JOB_STATE.FAILED.toString());
		cutjobDto.setEndTime(new Date());
		resultString = cutService.updateById(cutjobDto);
		if(Objects.equals(resultString.getCode(), ResultCode.OK)){
			if (Objects.equals(0, cutjobDto.getCutType())) {
				mapManageDto.setIsDel(1);
				resultString = cutService.updateMapManageById(mapManageDto);
				if(!Objects.equals(resultString.getCode(), ResultCode.OK)){
					logger.error("update mapManage failed");
				}
			}
		}else{
			logger.error("update cutjob failed");
		}
	}
	private Map<String,String> generateCofigAndPublish(CuttingDto cuttingDto) throws Exception {
		Map<String,String> res = new HashMap<String,String>();
		String imagePath = "";
		if(cuttingDto.getHdfsInPath() == null){
			imagePath = cuttingDto.getInPath();
		}else{
			imagePath = cuttingDto.getHdfsInPath();
		}
		String layersPath = cuttingDto.getOutPath().substring(0, cuttingDto.getOutPath().lastIndexOf("/"));// /lishun/cutting/Layers
		if(cuttingDto.getRealTime() == null) {//时间参数，实时更新，无需生成配置文件
			int shellRes = execCreateConfShell(imagePath, layersPath, cuttingDto);
			if (shellRes != 0 && shellRes != -1) {//由于自动生成配置代码出现运行时异常但能正常生成配置文件，返回-1，这里暂时不作为错误处理。
				res.put("code", "0");
				res.put("message", "切图后生成conf配置文件失败");
				return res;
			}
		}

		if(Objects.equals(1, cuttingDto.getIsPublish())){
			for(GwcConfigBean bean : GwcConfig.getInstance().getGwcConfigList()){//修改geowebcache配置文件
				Map<String, String> result = gwcService.mapPublish(bean, cuttingDto.getMapName(), layersPath, cuttingDto.getOutPath(), cuttingDto.getCutType());
				if(Objects.equals(result.get("result"), "false")){
					res.put("code", "0");
					res.put("message", "地图发布失败：" + result.get("message"));
					return res;
				}
			}
		}
		String geoRange = gwcService.getGeoRange(imagePath, layersPath + "/conf.cdi");
		JobHbase.setJobGeoRange(cuttingDto.getJobid(), geoRange);
		res.put("code", "1");
		res.put("geo_range", geoRange);
		return res;
	}
	/**
	 *
	 * Description: 生成配置文件
	 * @param  hdfsPath
	 * @param  layersPath
	 * @author lishun
	 * @date 2017年6月27日
	 * @return Integer
	 * @throws Exception
	 */
	private Integer execCreateConfShell(String hdfsPath, String layersPath, CuttingDto cuttingDto) throws Exception{
		ShellExecutor exe = new ShellExecutor(cutHost, Integer.valueOf(cutPort), cutUserName, cutPassword);

		StringBuilder command = new StringBuilder(appJarPath).append(" ");
		command.append(" ").append(CoreConfig.JOB_OP_GENERATE_MAP_CONF).append(" ");
		command.append(hdfsPath).append(" ");
		command.append(layersPath).append(" ");
		//command.append(alllayersPath).append(" ");
		command.append(cuttingDto.getMaxLayers()).append(" ");
		if(java.util.Objects.equals(0, cuttingDto.getBxmlUpdate())){
			command.append("false").append(" ");
		}else{
			command.append("true").append(" ");
		}
		if(cuttingDto.getMaxResolution() != null){
			command.append(" -maxLayer_resolution ").append(cuttingDto.getMaxResolution()).append(" ");
		}
		if(cuttingDto.getMinResolution() != null){
			command.append(" -maxLayer_resolution ").append(cuttingDto.getMinResolution()).append(" ");
		}
		logger.info("{}",command);
		return exe.execRemoteSynchronization(command.toString());
	}
	@Override
	public boolean equals(Object obj) {
		CutThread temp = (CutThread) obj;
		String a = temp.cutjobDto.getId();
		String b = this.cutjobDto.getId();
		return Objects.equals(a,b);
	}
}
