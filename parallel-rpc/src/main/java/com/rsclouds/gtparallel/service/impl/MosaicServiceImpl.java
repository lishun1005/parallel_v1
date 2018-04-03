package com.rsclouds.gtparallel.service.impl;


import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.MosaicJobDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBeanList;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.MosaicService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.dao.MosaicJobDao;
import com.rsclouds.gtparallel.dao.OptimalModelDao;
import com.rsclouds.gtparallel.entity.MosaicJob;
import com.rsclouds.gtparallel.entity.MosaicJobLog;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.utils.GearmanUtils;
import com.rsclouds.jdbc.repository.JdbcRepository;
import com.rsclouds.jdbc.repository.SearchFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * @author lishun
 * @ClassName: MosaicServiceImpl
 * @Description: TODO
 * @date 2017年8月11日 下午3:49:14
 */
@Service("MosaicService")
@Transactional
public class MosaicServiceImpl extends JdbcRepository<MosaicJob, String> implements MosaicService {

	private static Logger logger = LoggerFactory.getLogger(MosaicService.class);



	@Autowired
	public MosaicJobDao mosaicJobDao;

	@Value("#{appProperty['complete.url']}")
	public String completeUrl;

	@Autowired
	private OptimalModelDao optimalModelDao;





	@Override
	public ResultBeanList<Map<String, Object>> querylog(String mosaicId) {
		ResultBeanList<Map<String, Object>> resultBeanList = new ResultBeanList<Map<String, Object>>();
		resultBeanList.setResultData(mosaicJobDao.querylog(mosaicId));
		resultBeanList.setCode(ResultCode.OK);
		return resultBeanList;
	}

	@Override
	public void mosaicDel(String id) {
		MosaicJobDto mosaicDto = new MosaicJobDto();
		mosaicDto.setId(id);
		mosaicDto.setIsDel(1);
		mosaicJobDao.updateByJobid(mosaicDto);
	}

	@Override
	public ResultBean<Map<String, Object>> mosaicDetail(String id) {
		ResultBean<Map<String, Object>> resultBean = new ResultBean<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> mosaicJob = mosaicJobDao.queryById(id);
		if (mosaicJob != null) {
			String jobid = mosaicJob.get("jobid").toString();
			map.put("detail", mosaicJob);
			map.put("sub_list", mosaicJobDao.getMosaicSub(jobid, null));
		}
		resultBean.setCode(ResultCode.OK);
		resultBean.setResultData(map);
		return resultBean;
	}

	@Override
	public PageResultBean<Map<String, Object>> queryAll(String keyword, String status, int pageNum, int pageSize, String userName, String algorithmType) {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Map<String, Object>> page = (Page<Map<String, Object>>) mosaicJobDao.
				queryAll(keyword, status, pageNum, pageSize, userName, algorithmType);
		PageResultBean<Map<String, Object>> pageResultBean =
				new PageResultBean<Map<String, Object>>(page, page.getTotal(), page.getPages(), page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}

	@Override
	public MosaicJobDto getJobByName(String jobName) {
		Map<String, Object> serach = new HashMap<String, Object>();
		serach.put("EQ_jobName", jobName);
		serach.put("EQ_isDel", 0);
		MosaicJob mosaicJob = findOne(SearchFilter.parse(serach).values());
		if (mosaicJob != null) {
			return BeanMapper.map(mosaicJob, MosaicJobDto.class);
		} else {
			return null;
		}
	}

	@Override
	public ResultBean<String> mosaicRestart(MosaicJobDto mosaicJobDto) {
		ResultBean<String> resultBean = new ResultBean<String>();
		Map<String, Object> serach = new HashMap<String, Object>();
		serach.put("EQ_jobid", mosaicJobDto.getJobid());
		MosaicJob mosaicJob = findOne(SearchFilter.parse(serach).values());
		if (mosaicJob != null) {
			resultBean = gearmanSubmit(mosaicJobDto);
			if (Objects.equals(resultBean.getCode(), ResultCode.OK)) {
				String jobid = resultBean.getResultData();
				MosaicJobLog mosaicJobLog = BeanMapper.map(mosaicJob, MosaicJobLog.class);

				mosaicJobLog.setId(StringTool.getUUID());
				mosaicJobLog.setMosaicId(mosaicJob.getId());
				mosaicJobDao.insertLog(mosaicJobLog);//添加重启日志

				mosaicJob.setJobid(jobid);
				mosaicJob.setStartTime(null);
				mosaicJob.setEndTime(null);
				mosaicJob.setLog(null);
				mosaicJob.setIsRestart(1);
				save(mosaicJob);//更新记录
				List<Map<String,Object>> AlgorithmList = optimalModelDao.queryOptimalModelByModelId(mosaicJobDto.getModelId());
				List<Map<String,Object>> mosaicJobSubList = new ArrayList<>();
				for (Map<String,Object> map : AlgorithmList) {
					Map<String,Object> newMap = new HashMap<>();
					newMap.put("id", StringTool.getUUID());
					newMap.put("jobid", StringTool.getUUID());
					newMap.put("status", map.get("name"));
					newMap.put("progress", 0);
					newMap.put("sortOrder", map.get("ordering"));
					mosaicJobSubList.add(newMap);
				}
				mosaicJobDao.insertMosaicJobSubBatch(mosaicJobSubList);
			}
			return resultBean;
		} else {
			resultBean.setResultBean(ResultCode.FAILED, "jobid not exits,%s", mosaicJobDto.getJobid());
		}
		return resultBean;
	}

	@Override
	public ResultBean<String> mosaicAdd(MosaicJobDto mosaicJobDto) {
		ResultBean<String> resultBean = gearmanSubmit(mosaicJobDto);
		if (Objects.equals(resultBean.getCode(), ResultCode.OK)) {
			String jobid = resultBean.getResultData();
			mosaicJobDto.setId(StringTool.getUUID());
			mosaicJobDto.setStatus("ACCEPTED");
			mosaicJobDto.setJobid(jobid);
			mosaicJobDto.setIsDel(0);
			mosaicJobDto.setAcceptTime(new Date());
			save(BeanMapper.map(mosaicJobDto, MosaicJob.class));

			List<Map<String,Object>> AlgorithmList = optimalModelDao.queryOptimalModelByModelId(mosaicJobDto.getModelId());
			List<Map<String,Object>> mosaicJobSubList = new ArrayList<>();
			for (Map<String,Object> map : AlgorithmList) {
				Map<String,Object> newMap = new HashMap<>();
				newMap.put("id", StringTool.getUUID());
				newMap.put("jobid", jobid);
				newMap.put("status", map.get("name"));
				newMap.put("progress", 0);
				newMap.put("sortOrder", map.get("ordering"));
				mosaicJobSubList.add(newMap);
			}
			if(mosaicJobSubList.size() > 0){
				mosaicJobDao.insertMosaicJobSubBatch(mosaicJobSubList);
			}
			/*if (Objects.equals(algorithmType, Constant.MOSAIC_PL)) {
				addMosaicJobSub(jobid);
			} else if (Objects.equals(algorithmType, Constant.MOSAIC_GF2_08)
					|| Objects.equals(algorithmType, Constant.MOSAIC_GF1_2)
					|| Objects.equals(algorithmType, Constant.MOSAIC_GF1_16)) {
				addMosaicJobSubGF(jobid);
			} else if (Objects.equals(algorithmType, Constant.MOSAIC_MODIS)) {
				addMosaicJobModis(jobid);
			}*/
		}
		return resultBean;
	}


	@Override
	public ResultBean<Map<String, Object>> progressByHbase(String jobid) {
		ResultBean<Map<String, Object>> resultBean = new ResultBean<Map<String, Object>>();
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			Result rs = HbaseBase.selectRow(CoreConfig.IMAGE_JOB_TABLE, jobid);
			if (rs.isEmpty()) {
				resultBean.setResultBean(ResultCode.FAILED, "jobid: %s,not exist", jobid);
			} else {
				for (Map.Entry<byte[], byte[]> entry : rs.getFamilyMap(Bytes.toBytes(CoreConfig.JOB.FAMILY.strVal)).entrySet()) {
					map.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
				}
				resultBean.setCode(ResultCode.OK);
				resultBean.setResultData(map);
			}
		} catch (IOException ioe) {
			resultBean.setResultBean(ResultCode.FAILED, ioe.getMessage());
			logger.error(ioe.getMessage(), ioe);
			;
		}
		return resultBean;
	}

	@Override
	public void updateByJobid(MosaicJobDto mosaicDto) {
		mosaicJobDao.updateByJobid(mosaicDto);
	}

	@Override
	public void updateMosaicJobSub(String id, Integer progress, Integer sortOrder, String jobid) {
		mosaicJobDao.updateMosaicJobSubByJobid(jobid, sortOrder);//更新小于sortOrder的子进度为100%
		mosaicJobDao.updateMosaicJobSub(id, progress);
	}

	@Override
	public List<Map<String, Object>> getMosaicSub(String jobid, String stete) {
		return mosaicJobDao.getMosaicSub(jobid, stete);
	}


	private ResultBean<String> gearmanSubmit(MosaicJobDto mosaicJobDto){
		ResultBean<String> resultBean = new ResultBean<String>();
		try {
			mosaicJobDto.setJobid(StringTool.getUUID());

			createHbaseJob(mosaicJobDto.getJobid(), mosaicJobDto.getAlgorithmType(),
					mosaicJobDto.getInPath(), mosaicJobDto.getOutPath(), null, null);

			String cUrl = completeUrl.replace("{jobid}",mosaicJobDto.getJobid()).replace("{type}", mosaicJobDto.getAlgorithmType());
			String parms = StringUtils.join(
					new String[]{ mosaicJobDto.getJobid(), mosaicJobDto.getGearmanParms(), cUrl, "end"}, ",");
			logger.info("greaman funcName:{}; params:{}", mosaicJobDto.getGearmanFunc(), parms);
			byte[] data = ByteUtils.toUTF8Bytes(parms);
			GearmanJob job = GearmanJobImpl.createBackgroundJob(mosaicJobDto.getGearmanFunc(), data, null);

			//resultBean.setCode(ResultCode.OK);
			//resultBean.setResultData(mosaicJobDto.getJobid());
			GearmanUtils.submit(job);
			if(job.isDone()){
				resultBean.setCode(ResultCode.OK);
				resultBean.setResultData(mosaicJobDto.getJobid());
			}else{
				resultBean.setResultBean(ResultCode.FAILED,"gearman job fail");
			}
		}catch (Exception ex){
			logger.error(ex.getMessage(),ex);
			resultBean.setResultBean(ResultCode.FAILED, ex.getMessage());
		}
		return resultBean;

	}
	public void createHbaseJob(String jobid, String type, String inpath,
	                           String outpath, String aoipath, Integer extend) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		map.put(CoreConfig.JOB.TYPE.strVal, type);
		map.put(CoreConfig.JOB.STATUS.strVal, CoreConfig.JOB_STATE.ACCEPTED.toString());
		map.put(CoreConfig.JOB.PROGRESS.strVal, "0");
		map.put(CoreConfig.JOB.ACCEPT_TIME.strVal, System.currentTimeMillis() + "");
		map.put(CoreConfig.JOB.IN_PATH.strVal, inpath);
		map.put(CoreConfig.JOB.OUT_PATH.strVal, outpath);
		if (aoipath != null) {
			map.put(CoreConfig.JOB.AOI_PATH.strVal, aoipath);
		}
		if (extend != null) {
			map.put(CoreConfig.JOB.EXTEND.strVal, extend + "");
		}
		HbaseBase.writeRows(CoreConfig.IMAGE_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, map);
	}

	/*@Value("#{applicationProperty['auto.proc.modis.map.function']}")
	public String autoProcModisMapFunction;

	@Value("#{applicationProperty['auto.prod.pl.mosaic.function']}")
	public String autoProdPlMosaicFunction;

	@Value("#{applicationProperty['auto.prod.quality.evaluate.function']}")
	public String autoProdQualityEvaluateFunction;


	@Value("#{applicationProperty['auto.prod.gf1pms.mosaic.function']}")
	public String autoProdGf1pmsMosaicFunction;

	@Value("#{applicationProperty['auto.prod.gf1wfv.mosaic.function']}")
	public String autoProdGf1wfvMosaicFunction;

	@Value("#{applicationProperty['auto.prod.gf2.mosaic.function']}")
	public String autoProdGf2MosaicFunction;*/
	/*
	private void addMosaicJobModis(String jobid) {
		//tiff(rpc校准),bandGroup(波段组合),enhanceColor(色彩增强),dehaze(去雾),outprojects(转投影),mosaic(镶嵌),outprojects(转投影-按需）
		mosaicJobDao.addMosaicJobSub(jobid, "tiff", 0, 1, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "bandGroup", 0, 2, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "enhanceColor", 0, 3, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "dehaze", 0, 4, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "outprojects", 0, 5, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "mosaic", 0, 6, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "outprojects", 0, 7, UUID.randomUUID().toString(), new Date());
	}

	private void addMosaicJobSubGF(String jobid) {
		//Gf镶嵌
		//rpc(rpc校准),fusion(UML锐化),enhanceColor(色彩增强),sift(在线配准),dehaze(去雾),mosaic(镶嵌),outprojects(转投影)
		mosaicJobDao.addMosaicJobSub(jobid, "rpc", 0, 1, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "fusion", 0, 2, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "enhanceColor", 0, 3, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "sift", 0, 4, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "dehaze", 0, 5, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "mosaic", 0, 6, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "outprojects", 0, 9, UUID.randomUUID().toString(), new Date());
	}

	private void addMosaicJobSub(String jobid) {
		//目前模型只有pl镶嵌，下面代码后续修改
		//screen(质量筛选),reprojects(转wgs84),dehaze(去雾),mosaic(镶嵌),outprojects(转投影)
		mosaicJobDao.addMosaicJobSub(jobid, "screen", 0, 1, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "reprojects", 0, 2, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "dehaze", 0, 3, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "mosaic", 0, 4, UUID.randomUUID().toString(), new Date());
		mosaicJobDao.addMosaicJobSub(jobid, "outprojects", 0, 5, UUID.randomUUID().toString(), new Date());
	}

	private ResultBean<String> mosaicJob2Gearman(MosaicJobDto mosaicDto) {
		ResultBean<String> resultBean = new ResultBean<String>();
		try {
			//mosaicDto.setJobid(StringTool.getUUID());
			logger.info("request params:{}", mosaicDto);
			String algorithmType =  mosaicDto.getAlgorithmType();
			switch (algorithmType) {
				case Constant.MOSAIC_PL:
					jobAutoMosaicASync(mosaicDto);
					break;
				case Constant.MOSAIC_GF2_08:
					jobAutoProdGf2MosaicASync(mosaicDto);
					break;
				case Constant.MOSAIC_GF1_2:
					jobAutoProdGf1pmsMosaicASync(mosaicDto);
					break;
				case Constant.MOSAIC_GF1_16:
					jobAutoProdGf1wfvMosaicASync(mosaicDto);
					break;
				case Constant.MOSAIC_MODIS:
					jobAutoModisASync(mosaicDto);
				case Constant.MOSAIC_PL_QUALITY:
					jobAutoProdQualityEvaluateASync(mosaicDto);
				default:
					resultBean.setResultBean(ResultCode.FAILED, "%s,algorithm type not exits", algorithmType);
					return resultBean;
			}
			resultBean.setCode(ResultCode.OK);
			resultBean.setResultData(mosaicDto.getJobid());
		} catch (Exception e) {
			resultBean.setResultBean(ResultCode.FAILED, e.getMessage());
			logger.error(e.getMessage(), e);
			;
		}
		return resultBean;
	}

	*//**
	 * @param mosaic
	 * @return boolean
	 * @Description:modis镶嵌
	 * @author lishun
	 * @date 2017/11/27
	 *//*
	private boolean jobAutoModisASync(MosaicJobDto mosaic) throws Exception {

		createHbaseJob(mosaic.getJobid(), mosaic.getAlgorithmType(),
				mosaic.getInPath(), mosaic.getOutPath(), null, null);
		completeUrl = completeUrl.replace("{jobid}", mosaic.getJobid()).replace("{type}",
				mosaic.getAlgorithmType());
		String dataStr = StringUtils.join(new String[]{mosaic.getJobid(), mosaic.getInPath(), mosaic.getOutPath()
				, mosaic.getProject(), completeUrl, "test"}, ",");
		byte[] data = ByteUtils.toUTF8Bytes(dataStr);
		GearmanJob job = GearmanJobImpl.createBackgroundJob(autoProcModisMapFunction, data, null);
		GearmanUtils.submit(job);
		return job.isDone();
	}

	*//**
	 * Description:GF1-2m镶嵌
	 *
	 * @param mosaic
	 * @return boolean
	 * @throws IOException
	 * @author lishun
	 * @date 2017年9月8日
	 *//*
	private boolean jobAutoProdGf1pmsMosaicASync(MosaicJobDto mosaic) throws IOException {
		createHbaseJob(mosaic.getJobid(),mosaic.getAlgorithmType(),
				mosaic.getInPath(), mosaic.getOutPath(), null, null);

		completeUrl = completeUrl.replace("{jobid}",
				mosaic.getJobid()).replace("{type}", mosaic.getAlgorithmType());

		String dataStr = StringUtils.join(new String[]{mosaic.getJobid(), mosaic.getInPath(), mosaic.getProject(),
				mosaic.getOutBand().toString(), mosaic.getOutImage().toString(), mosaic.getOutPath(), completeUrl, "end"}, ",");//参数待定

		byte[] data = ByteUtils.toUTF8Bytes(dataStr);
		GearmanJob job = GearmanJobImpl.createBackgroundJob(autoProdGf1pmsMosaicFunction, data, null);
		GearmanUtils.submit(job);
		return job.isDone();
	}

	*//**
	 * Description: GF1-16m镶嵌
	 *
	 * @param mosaic
	 * @return boolean
	 * @throws IOException
	 * @author lishun
	 * @date 2017年9月8日
	 *//*
	private boolean jobAutoProdGf1wfvMosaicASync(MosaicJobDto mosaic) throws IOException {
		createHbaseJob(mosaic.getJobid(), mosaic.getAlgorithmType(),
				mosaic.getInPath(), mosaic.getOutPath(), null, null);

		completeUrl = completeUrl.replace("{jobid}",
				mosaic.getJobid()).replace("{type}", mosaic.getAlgorithmType());

		String dataStr = StringUtils.join(new String[]{mosaic.getJobid(), mosaic.getInPath(), mosaic.getProject(),
				mosaic.getOutBand().toString(), mosaic.getOutImage().toString(), mosaic.getOutPath(), completeUrl, "end"}, ",");//参数待定

		byte[] data = ByteUtils.toUTF8Bytes(dataStr);
		GearmanJob job = GearmanJobImpl.createBackgroundJob(autoProdGf1wfvMosaicFunction, data, null);
		GearmanUtils.submit(job);
		return job.isDone();
	}

	*//**
	 * Description: GF2-0.8m镶嵌
	 *
	 * @param mosaic
	 * @return boolean
	 * @throws IOException
	 * @author lishun
	 * @date 2017年9月8日
	 *//*
	private boolean jobAutoProdGf2MosaicASync(MosaicJobDto mosaic) throws IOException {
		createHbaseJob(mosaic.getJobid(), mosaic.getAlgorithmType(),
				mosaic.getInPath(), mosaic.getOutPath(), null, null);

		completeUrl = completeUrl.replace("{jobid}",
				mosaic.getJobid()).replace("{type}", mosaic.getAlgorithmType());

		String dataStr = StringUtils.join(new String[]{mosaic.getJobid(), mosaic.getInPath(), mosaic.getProject(),
				mosaic.getOutBand().toString(), mosaic.getOutImage().toString(), mosaic.getOutPath(), completeUrl, "end"}, ",");//参数待定

		byte[] data = ByteUtils.toUTF8Bytes(dataStr);
		GearmanJob job = GearmanJobImpl.createBackgroundJob(autoProdGf2MosaicFunction, data, null);
		GearmanUtils.submit(job);
		return job.isDone();
	}

	*//**
	 * @param mosaic
	 * @return boolean
	 * @Description:pl 镶嵌
	 * @author lishun
	 * @date 2017/11/29
	 *//*
	private boolean jobAutoMosaicASync(MosaicJobDto mosaic) throws IOException {
		createHbaseJob(mosaic.getJobid(), mosaic.getAlgorithmType(),
				mosaic.getInPath(), mosaic.getOutPath(), null, null);
		completeUrl = completeUrl.replace("{jobid}",
				mosaic.getJobid()).replace("{type}", mosaic.getAlgorithmType());
		String dataStr = StringUtils.join(new String[]{mosaic.getJobid(), mosaic.getInPath(), mosaic.getProject(),
				mosaic.getOutBand().toString(), mosaic.getOutImage().toString(), mosaic.getOutPath(), completeUrl, "end"}, ",");//参数待定

		byte[] data = ByteUtils.toUTF8Bytes(dataStr);
		GearmanJob job = GearmanJobImpl.createBackgroundJob(autoProdPlMosaicFunction, data, null);
		GearmanUtils.submit(job);
		return job.isDone();
	}

	*//*
	 * @Description: 质量评估
	 * @author lishun
	 * @date 2017/11/29
	 * @param [mosaic]
	 * @return boolean
	 *//*
	private boolean jobAutoProdQualityEvaluateASync(MosaicJobDto mosaic) throws IOException {
		createHbaseJob(mosaic.getJobid(), mosaic.getAlgorithmType(),
				mosaic.getInPath(), mosaic.getOutPath(), null, null);

		completeUrl = completeUrl.replace("{jobid}",
				mosaic.getJobid()).replace("{type}", mosaic.getAlgorithmType());

		String dataStr = StringUtils.join(new String[]{mosaic.getJobid(), mosaic.getInPath(), mosaic.getOutPath(), completeUrl, "end"}, ",");//参数待定
		byte[] data = ByteUtils.toUTF8Bytes(dataStr);
		GearmanJob job = GearmanJobImpl.createBackgroundJob(autoProdQualityEvaluateFunction, data, null);
		GearmanUtils.submit(job);
		return job.isDone();
	}*/

}
