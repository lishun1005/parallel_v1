package com.rsclouds.gtparallel.service.impl;


import com.rscloud.ipc.rpc.api.dic.Constant;
import com.rscloud.ipc.rpc.api.dto.ChangeDetectionDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.ChangeDetetionService;
import com.rscloud.ipc.rpc.api.service.CutService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.dao.CutJobDao;
import com.rsclouds.gtparallel.dao.MapManageDao;
import com.rsclouds.gtparallel.entity.ChangeDetection;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.utils.GearmanUtils;
import com.rsclouds.jdbc.repository.JdbcRepository;
import com.rsclouds.jdbc.repository.SearchFilter;
import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.Map;

/**
 * @author lishun
 * @ClassName: CutServiceImpl
 * @Description: TODO
 * @date 2017年7月17日 下午2:18:32
 */
@Service("changeDetetionService")
@Transactional
public class ChangeDetetionServiceImpl extends JdbcRepository<ChangeDetection, String>
		implements ChangeDetetionService {
	@Value("#{applicationProperty[parallel_server_change_detection]}")
	public String changeDetectionApi;

	@Value("#{applicationProperty[parallel_server_mosaic_progress]}")
	public String progressApi;


	@Value("#{appProperty['complete.detection.url']}")
	public String completeDetectionUrl;

	@Autowired
	private CutJobDao cutJobDao;

	@Autowired
	private MapManageDao mapManageDao;

	private static Logger logger = LoggerFactory.getLogger(CutService.class);
	@Override
	public ResultBean<String> add(ChangeDetectionDto changeDetectionDto) throws IOException {
		ResultBean<String> resultBean = new ResultBean<String>();
		String jobid = StringTool.getUUID();
		changeDetectionDto.setJobid(jobid);
		Map<String,String> detectionMap = new HashMap<String,String>();
		BeanMapper.copyProperties(changeDetectionDto, detectionMap);
		detectionMap.put(CoreConfig.JOB.STATUS.strVal, CoreConfig.JOB_STATE.ACCEPTED.toString());
		detectionMap.put(CoreConfig.JOB.TYPE.strVal, Constant.CHANGE_DETECTION);
		HbaseBase.writeRows(CoreConfig.IMAGE_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, detectionMap);

		String cUrl = completeDetectionUrl.replace("{jobid}",jobid).replace("{type}", Constant.CHANGE_DETECTION);
		String parms = StringUtils.join(new String[] {jobid, changeDetectionDto.getGearmanParms(), cUrl, "end"} , ",");
		logger.info("params: {}", parms);
		byte[] data = ByteUtils.toUTF8Bytes(parms);
		GearmanJob job = GearmanJobImpl.createBackgroundJob(changeDetectionDto.getGearmanFunc(), data, null);
		GearmanUtils.submit(job);
		if(job.isDone()){
			save(BeanMapper.map(changeDetectionDto, ChangeDetection.class));
			resultBean.setResultData(jobid);
			resultBean.setCode(ResultCode.OK);
		}else{
			resultBean.setResultBean(ResultCode.FAILED,"gearman job fail");
		}
		return resultBean;
	}
	@Override
	public ResultBean<Map<String, Object>> progress(String jobid) {
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
	public ResultBean<ChangeDetectionDto> queryByJobid(String jobid){
		ResultBean<ChangeDetectionDto> resultBean = new ResultBean<ChangeDetectionDto>();
		Map<String, Object> serach = new HashMap<String, Object>();
		serach.put("EQ_jobid", jobid);
		ChangeDetection changeDetection =
				findOne(SearchFilter.parse(serach).values());
		if(changeDetection == null) {
			resultBean.setResultBean(ResultCode.FAILED, "jobid no such record");
		}else{
			resultBean.setCode(ResultCode.OK);
			resultBean.setResultData(BeanMapper.map(changeDetection, ChangeDetectionDto.class));
		}
		return resultBean;
	}
}
