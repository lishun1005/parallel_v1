package com.rsclouds.gtparallel.service.impl;


import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.OptimalModelDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.OptimalModelService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.gtparallel.dao.OptimalModelDao;
import com.rsclouds.gtparallel.entity.OptimalModel;
import com.rsclouds.jdbc.repository.JdbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author lishun
 * @ClassName: MosaicServiceImpl
 * @Description: TODO
 * @date 2017年8月11日 下午3:49:14
 */
@Service
@Transactional
public class OptimalModelServiceImpl extends JdbcRepository<OptimalModel, String> implements OptimalModelService {

	private static Logger logger = LoggerFactory.getLogger(OptimalModelService.class);
	@Autowired
	private OptimalModelDao optimalModelDao;
	@Override
	public PageResultBean<OptimalModelDto> queryAll(String keyword, int pageNum, int pageSize) {
		Page<OptimalModel> page = (Page<OptimalModel>)optimalModelDao.queryAll(keyword, pageNum, pageSize);
		PageResultBean<OptimalModelDto> pageResultBean =
				new PageResultBean<OptimalModelDto>(BeanMapper.mapList(page ,OptimalModelDto.class), page.getTotal(),page.getPages(),page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}
	@Override
	public void insert(OptimalModelDto optimalModelDto){
		optimalModelDao.insert(BeanMapper.map(optimalModelDto,OptimalModel.class));
	}
	@Override
	public void update(OptimalModelDto optimalModelDto){
		optimalModelDao.update(BeanMapper.map(optimalModelDto,OptimalModel.class));
	}

	@Override
	public void insertBatch(List<Map<String,Object>> list){
		optimalModelDao.deletemodelAlgorithmByModelId(list.get(0).get("modelId").toString());
		optimalModelDao.insertBatch(list);
	}

	@Override
	public List<Map<String,Object>> queryOptimalModelByModelId(String modelId){
		return optimalModelDao.queryOptimalModelByModelId(modelId);
	}

}
