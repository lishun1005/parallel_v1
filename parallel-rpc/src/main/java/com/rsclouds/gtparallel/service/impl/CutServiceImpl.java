package com.rsclouds.gtparallel.service.impl;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.CutJobDto;
import com.rscloud.ipc.rpc.api.dto.HbJobDto;
import com.rscloud.ipc.rpc.api.dto.MapManageDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBeanList;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.CutService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.common.HttpTookit;
import com.rsclouds.gtparallel.core.entity.Job;
import com.rsclouds.gtparallel.core.job.JobHbase;
import com.rsclouds.gtparallel.dao.CutJobDao;
import com.rsclouds.gtparallel.dao.MapManageDao;
import com.rsclouds.gtparallel.entity.CutJob;
import com.rsclouds.gtparallel.entity.CutJobLog;
import com.rsclouds.gtparallel.entity.MapManage;
import com.rsclouds.gtparallel.geowebcache.GwcConfig;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.thread.CutThreadPool;
import com.rsclouds.gtparallel.utils.GdalUtils;
import com.rsclouds.jdbc.repository.JdbcRepository;
import com.rsclouds.jdbc.repository.SearchFilter;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author lishun
 * @ClassName: CutServiceImpl
 * @Description: TODO
 * @date 2017年7月17日 下午2:18:32
 */
@Service("CutService")
@Transactional
public class CutServiceImpl extends JdbcRepository<CutJob, String> implements CutService {

	private final static Logger logger = LoggerFactory.getLogger(CutService.class);




	private ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 5, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(),
			new ThreadPoolExecutor.CallerRunsPolicy());


	@Autowired
	private CutJobDao cutJobDao;

	@Autowired
	private MapManageDao mapManageDao;

	@Autowired
	@Qualifier("gwcConfig")
	public GwcConfig gwcConfig;

	@Value("${gtdata_prefix}")
	public  String gtdataPrefix;

	@Value("#{appProperty['hadoop.progress.url']}")
	public  String hadoopProgressUrl;

	@Override
	public void initUndoneCutjob() {
		mapManageDao.queryAll(null,1,10,"");
		Map<String, Object> serach = new HashMap<String, Object>();
		serach.put("EQ_isDel", 0);
		serach.put("EQ_status", "ACCEPTED");
		List<CutJob> acceptedList = findAll(SearchFilter.parse(serach).values());
		serach.put("EQ_status", "RUNNING");
		List<CutJob> runningList = findAll(SearchFilter.parse(serach).values());
		serach.put("EQ_status", "INQUEUE");
		List<CutJob> inqueueList = findAll(SearchFilter.parse(serach).values());
		runningList.addAll(acceptedList);//已经执行切片请求先加入队列查询进度(防止未进入队列条目修改高于已经切片优先级，导致已经切片进度查询被加入队列等待)
		for (CutJob cutJob : runningList) {
			MapManage mapManage = mapManageDao.queryById(cutJob.getMapId());
			CutThreadPool.execute(BeanMapper.map(cutJob, CutJobDto.class), BeanMapper.map(mapManage, MapManageDto.class));
		}
		for (CutJob cutJob : inqueueList) {
			MapManage mapManage = mapManageDao.queryById(cutJob.getMapId());
			CutThreadPool.execute(BeanMapper.map(cutJob, CutJobDto.class), BeanMapper.map(mapManage, MapManageDto.class));
		}

	}

	@Override
	public ResultBean<Boolean> cutJobChangePriority(String id, Integer priority) {
		ResultBean<Boolean> resultBean = new ResultBean<Boolean>();
		if (CutThreadPool.remove(id)) {//移除队列元素
			CutJob cutJob = new CutJob();
			cutJob.setId(id);
			cutJob.setPriority(priority);
			cutJobDao.update(cutJob);
			cutJob = findOne(id);
			MapManage mapManage = mapManageDao.queryById(cutJob.getMapId());
			CutThreadPool.execute(BeanMapper.map(cutJob, CutJobDto.class), BeanMapper.map(mapManage, MapManageDto.class));
			resultBean.setResultData(true);
		} else {
			resultBean.setResultData(false);
		}
		return resultBean;
	}

	@Override
	public void cutJobDel(String id) {
		CutJob cutJob = new CutJob();
		cutJob.setId(id);
		cutJob.setIsDel(1);
		cutJobDao.update(cutJob);
	}

	@Override
	public PageResultBean<Map<String, Object>> queryMapManageAll(String keyword, int pageNum, int pageSize, String userName) {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Map<String, Object>> page = (Page<Map<String, Object>>) mapManageDao.queryAll(keyword, pageNum, pageSize, userName);
		PageResultBean<Map<String, Object>> pageResultBean =
				new PageResultBean<Map<String, Object>>(page,page.getTotal(),page.getPages(),page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}


	@Override
	public PageResultBean<Map<String, Object>> queryAll(String keyword, String status, int pageNum, int pageSize, String userName) {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Map<String, Object>> page = (Page<Map<String, Object>>) cutJobDao.queryAll(keyword, status, pageNum, pageSize, userName);
		PageResultBean<Map<String, Object>> pageResultBean =
				new PageResultBean<Map<String, Object>>(page,page.getTotal(),page.getPages(),page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}

	@Override
	public ResultBean<Map<String, Object>> queryById(String id) {
		ResultBean<Map<String, Object>> resultBean = new ResultBean<Map<String, Object>>(cutJobDao.queryById(id));
		resultBean.setCode(ResultCode.OK);
		return resultBean;
	}

	@Override
	public ResultBean<Map<String, Object>> getCutJobInfoById(String id) {
		Map<String, Object> map = new HashMap<String, Object>();
		ResultBean<Map<String, Object>> resultBean = new ResultBean<Map<String, Object>>();
		CutJob cutjob = findOne(id);
		if (null != cutjob) {
			MapManage mapManage = mapManageDao.queryById(cutjob.getMapId());
			if (null != mapManage) {
				map.put("service", mapManage.getServiceType());
				map.put("status", cutjob.getStatus());
				map.put("srs", mapManage.getProject());
				map.put("extent-srs", mapManage.getProject());
				map.put("format", mapManage.getFormat());
				map.put("log", cutjob.getLog());
				map.put("progress", cutjob.getProgress());
				if (Objects.equals("SUCCEEDED", cutjob.getStatus())) {
					map.put("extend", mapManage.getGeoRange());
				}
				resultBean.setCode(ResultCode.OK);
				resultBean.setResultData(map);
			} else {
				resultBean.setCode(ResultCode.FAILED);
				resultBean.setMessage("mapid = %s,图层信息不存在",cutjob.getMapId());
			}
		} else {
			resultBean.setCode(ResultCode.FAILED);
			resultBean.setMessage("id = %s,切片任务不存在",id);
		}
		return resultBean;
	}

	@Override
	public ResultBean<HbJobDto> progress(String jobid) {
		ResultBean<HbJobDto> resultBean = new ResultBean<HbJobDto>();
		try {
			Job job = JobHbase.getJob(jobid);
			if (job == null) {
				resultBean.setResultBean(ResultCode.FAILED, "jobid not exist %s", jobid);
			}else {
				Map<String, String> result = getHadoopProgress(job);
				String newProgress = result.get(CoreConfig.JOB.PROGRESS.strVal);
				if (newProgress != null && !newProgress.equals(job.getProgress())) {
					job.setProgress(newProgress);
					HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, job.getRowKey(), CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.PROGRESS.strVal, job.getProgress());
				}
				String current = job.getCurrent();
				String total = job.getTotal();
				int progress = 0;
				if (current != null && total != null) { //文件夹切片进度
					int currentInt = Integer.parseInt(current) * 100;
					int totalInt = Integer.parseInt(total);
					if (job.getProgress() == null) {
						progress = (currentInt) / (totalInt);
					}else {
						progress = (currentInt + Integer.parseInt(job.getProgress())) / (totalInt);
					}
					//result.put(CoreConfig.JOB.PROGRESS.strVal, progress + "");
					job.setProgress(String.valueOf(progress));
				}else {
					//result.put(CoreConfig.JOB.PROGRESS.strVal, job.getProgress());
				}
				HbJobDto hbJobDto = BeanMapper.map(job, HbJobDto.class);
				String publishUrl = gwcConfig.getGwcConfigList()[0].getPublishUrl();
				hbJobDto.setGeowebcacheUrl(publishUrl.replace("{mapName}", job.getMapName()));
				resultBean.setCode(ResultCode.OK);
				resultBean.setResultData(hbJobDto);
			}
		}catch (IOException ioe){
			resultBean.setResultBean(ResultCode.FAILED, "hbase error:%s" ,ioe.getMessage());
		}
		return resultBean;
	}

	@Override
	public ResultBean<String> updateById(CutJobDto cutjobDto) {
		/*MapManage mapManage = new MapManage();
		mapManage.setId(cutjobDto.getMapId());
		if (Objects.equals(0, cutjobDto.getCutType()) && Objects.equals("FAILED", cutjobDto.getStatus())) {
			mapManage.setIsDel(1);//若是操作类型是添加，且job状态是failed，删除图层信息
			mapManageDao.updateById(mapManage);
		}
		if (StringUtils.isNoneBlank(geoRange)) {
			mapManage.setGeoRange(geoRange);
			mapManageDao.updateById(mapManage);
		}*/

		ResultBean<String> resultBean = new ResultBean<String>();
		if(StringUtils.isBlank(cutjobDto.getId())){
			resultBean.setResultBean(ResultCode.FAILED, "id is not null");
		}else {
			resultBean.setCode(ResultCode.OK);
			cutJobDao.update(BeanMapper.map(cutjobDto, CutJob.class));
		}
		return resultBean;


	}
	@Override
	public ResultBean<String> updateMapManageById(MapManageDto mapManagedto){
		ResultBean<String> resultBean = new ResultBean<String>();
		if(StringUtils.isBlank(mapManagedto.getId())){
			resultBean.setResultBean(ResultCode.FAILED, "id is not null");
		}else {
			resultBean.setCode(ResultCode.OK);
			mapManageDao.updateById(BeanMapper.map(mapManagedto, MapManage.class));
		}
		return resultBean;
	}

	@Override
	public ResultBeanList<Map<String, Object>> querylog(String id) {
		ResultBeanList<Map<String, Object>> resultBeanList = new ResultBeanList<Map<String, Object>>();
		resultBeanList.setResultData(cutJobDao.querylog(id));
		resultBeanList.setCode(ResultCode.OK);
		return resultBeanList;
	}

	@Override
	public ResultBean<CutJobDto> queryCutJobById(String id){
		ResultBean<CutJobDto> resultBean = new ResultBean<CutJobDto>();
		CutJob cutjob = findOne(id);
		if(cutjob == null){
			resultBean.setMessage("id not exits,%s", id);
			resultBean.setCode(ResultCode.FAILED);
		}else{
			resultBean.setCode(ResultCode.OK);
			resultBean.setResultData(BeanMapper.map(cutjob, CutJobDto.class));
		}
		return resultBean;
	}
	@Override
	public void addCutJobLog(String newId, CutJobDto cutJobDto){
		CutJobLog cutjobLog = BeanMapper.map(cutJobDto, CutJobLog.class);
		cutjobLog.setCutId(newId);
		cutJobDao.insertLog(cutjobLog);
		cutJobDao.updateLog(newId, cutJobDto.getId());
		delete(cutJobDto.getId());
	}
	@Override
	public ResultBean<Boolean> isExistMapName(String mapName) {
		ResultBean<Boolean> resultBean = new ResultBean<Boolean>();
		if (mapManageDao.isExistMapName(mapName)!= null) {
			resultBean.setResultData(true);
		} else {
			resultBean.setResultData(false);
		}
		return resultBean;
	}
	@Override
	public ResultBean<Boolean> isExistOutPath(String outPath) {
		ResultBean<Boolean> resultBean = new ResultBean<Boolean>();
		if (mapManageDao.isExistOutPath(outPath) > 0) {
			resultBean.setResultData(true);
		} else {
			resultBean.setResultData(false);
		}
		return resultBean;
	}
	@Override
	public ResultBean<String> onemapCutTypeAdd(CutJobDto cutjobDto, MapManageDto mapManageDto) {//切片类型是新增
		ResultBean<String> resultBean = new ResultBean<String>();
		cutjobDto.setMinLayers("0");//新增时最小图层时必须是0
		Map<String, Object> imageInfo = null;
		if (cutjobDto.getInPathList() == null || cutjobDto.getInPathList().size() == 0) {
			imageInfo = GdalUtils.getImageInfo(cutjobDto.getInPath());
			if (imageInfo.get("project") == null) {
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						imageInfo.get("errorMessage").toString());
				return resultBean;
			}
			if (imageInfo.get("out_byte") == null || !Objects.equals(imageInfo.get("out_byte"), 8)) {
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						"影像输出字节只支持8位");
				return resultBean;
			}
		} else {
			String imageProject = "";
			for (String filePath : cutjobDto.getInPathList()) {
				filePath = gtdataPrefix + filePath;
				imageInfo = GdalUtils.getImageInfo(filePath);
				if (imageInfo.get("project") == null) {
					resultBean.setResultBean(ResultCode.PARAMS_ERR,
							imageInfo.get("errorMessage").toString());
					return resultBean;
				}
				if (Objects.equals("", imageProject)) {
					imageProject = imageInfo.get("project").toString();
				} else {
					if (!Objects.equals(imageProject, imageInfo.get("project").toString())) {
						resultBean.setResultBean(ResultCode.PARAMS_ERR,
								"文件夹文件存在投影类型不一致的影像");
						return resultBean;
					}
				}
				if (imageInfo.get("out_byte") == null || !Objects.equals(imageInfo.get("out_byte"), 8)) {
					resultBean.setResultBean(ResultCode.PARAMS_ERR,
							"影像输出字节只支持8位");
					return resultBean;
				}
			}
		}
		cutjobDto.setId(StringTool.getUUID());
		mapManageDto.setId(StringTool.getUUID());
		cutjobDto.setIsCover(true);//默认是覆盖

		CutThreadPool.execute(cutjobDto, mapManageDto);
		addCutJob(cutjobDto, mapManageDto.getId());
		mapManageDto.setProject(imageInfo.get("project").toString());
		//mapManageDto.setProject("EPSG:3857");
		mapManageDto.setShowMaxLayers(cutjobDto.getMaxLayers());
		mapManageDto.setCtTime(new Date());
		mapManageDto.setServiceType("Hbasecache-Path");
		mapManageDto.setFormat("image/png");
		mapManageDao.insert(BeanMapper.map(mapManageDto, MapManage.class));;
		resultBean.setResultData(cutjobDto.getId());
		resultBean.setCode(ResultCode.OK);
		return resultBean;
	}
	@Override
	public ResultBean<String> onemapCutTypeUpdate(CutJobDto cutjobDto, MapManageDto mapManageDto) {
		ResultBean<String> resultBean = new ResultBean<String>();
		MapManage mapManageOld = mapManageDao.isExistMapName(mapManageDto.getMapName());
		Map<String, Object> imageInfo = null;
		if (cutjobDto.getInPathList() == null || cutjobDto.getInPathList().size() == 0) {
			imageInfo = GdalUtils.getImageInfo(cutjobDto.getInPath());
			if (imageInfo.get("project") == null) {
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						imageInfo.get("errorMessage").toString());
				return resultBean;
			}
			if (imageInfo.get("out_byte") == null || !Objects.equals(imageInfo.get("out_byte"), 8)) {
				resultBean.setResultBean(ResultCode.PARAMS_ERR,
						"影像输出字节只支持8位");
				return resultBean;
			}
		} else {
			String imageProject = mapManageOld.getProject();
			for (String filePath : cutjobDto.getInPathList()) {
				filePath = gtdataPrefix + filePath;
				imageInfo = GdalUtils.getImageInfo(filePath);
				if (imageInfo.get("project") == null) {
					resultBean.setResultBean(ResultCode.PARAMS_ERR,
							imageInfo.get("errorMessage").toString());
					return resultBean;
				}
				if (!Objects.equals(imageProject, imageInfo.get("project").toString())) {
					resultBean.setResultBean(ResultCode.PARAMS_ERR,
							"文件夹文件存在投影类型不是%s的影像", mapManageOld.getProject());
					return resultBean;
				}
				if (imageInfo.get("out_byte") == null || !Objects.equals(imageInfo.get("out_byte"), 8)) {
					resultBean.setResultBean(ResultCode.PARAMS_ERR,
							"影像输出字节只支持8位");
					return resultBean;
				}
			}
		}
		if (Integer.parseInt(cutjobDto.getMaxLayers()) > Integer.parseInt(mapManageOld.getShowMaxLayers())) {//取最大值
			mapManageOld.setShowMaxLayers(cutjobDto.getMaxLayers());
		}

		if (cutjobDto.getMinLayers() == null) {
			cutjobDto.setMinLayers("0");//默认是0
		}
		String cutId = StringTool.getUUID();
		cutjobDto.setId(cutId);
		CutThreadPool.execute(cutjobDto, BeanMapper.map(mapManageOld, MapManageDto.class));
		mapManageOld.setUtTime(new Date());
		mapManageDao.updateById(mapManageOld);
		addCutJob(cutjobDto, mapManageOld.getId());
		resultBean.setResultData(cutId);
		resultBean.setCode(ResultCode.OK);
		return resultBean;
	}
	@Override
	public ResultBean<Boolean> isExistJobName(String jobName) {
		ResultBean<Boolean> resultBean = new ResultBean<Boolean>();
		Map<String, Object> serach = new HashMap<String, Object>();
		serach.put("EQ_jobName", jobName);
		serach.put("EQ_isDel", 0);
		if (findOne(SearchFilter.parse(serach).values()) != null) {
			resultBean.setResultData(true);
		} else {
			resultBean.setResultData(false);
		}
		return resultBean;
	}

	private  Map<String,String> getHadoopProgress(final Job job){
		Map<String,String> map = new HashMap<String,String>();
		String jid = job.getJid();
		int progress = 0;
		String finalStatus;
		if(StringUtils.isNotBlank(jid)){
			String appid = "application" + jid.substring(jid.indexOf("_"));
			String url = hadoopProgressUrl.replace("{appid}", appid);
			String x = HttpTookit.doGet(url,null);
			if(x!= null && !x.isEmpty()){
				JSONObject jsonobj =  JSONObject.fromObject(x);
				progress = jsonobj.getJSONObject("app").getInt("progress");
				map.put(CoreConfig.JOB.PROGRESS.strVal, progress+"");
				finalStatus = jsonobj.getJSONObject("app").getString("finalStatus");
				if("FAILED".equals(finalStatus)){
					map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.FAILED.toString());
				}
			}
		}
		return map;
	}
	private void addCutJob(CutJobDto cutjobDto, String mapManageId) {
		cutjobDto.setStatus("INQUEUE");
		cutjobDto.setIsDel(0);
		cutjobDto.setAcceptTime(new Date());
		cutjobDto.setMapId(mapManageId);
		cutjobDto.setProgress(0.0);
		save(BeanMapper.map(cutjobDto, CutJob.class));
	}
}
