package com.rsclouds.gtparallel.service.impl;


import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.ProductLineDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.OptimalModelService;
import com.rscloud.ipc.rpc.api.service.ProductlineService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.gtparallel.dao.ProductLineDao;
import com.rsclouds.gtparallel.entity.ProductLine;
import com.rsclouds.jdbc.repository.JdbcRepository;
import com.rsclouds.jdbc.repository.SearchFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lishun
 * @ClassName: MosaicServiceImpl
 * @Description: TODO
 * @date 2017年8月11日 下午3:49:14
 */
@Service
@Transactional
public class ProductLineServiceImpl extends JdbcRepository<ProductLine, String> implements ProductlineService {

	private static Logger logger = LoggerFactory.getLogger(OptimalModelService.class);
	@Autowired
	private ProductLineDao productLineDao;

	@Override
	public PageResultBean<ProductLineDto> queryAll(String keyword, int pageNum, int pageSize) {
		Page<ProductLine> page = (Page<ProductLine>)productLineDao.queryAll(keyword, pageNum, pageSize);
		PageResultBean<ProductLineDto> pageResultBean =
				new PageResultBean<ProductLineDto>(BeanMapper.mapList(page ,ProductLineDto.class), page.getTotal(),page.getPages(),page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}
	@Override
	public void insert(ProductLineDto productLineDto){
		productLineDao.insert(BeanMapper.map(productLineDto,ProductLine.class));
	}
	@Override
	public void update(ProductLineDto productLineDto){
		productLineDao.update(BeanMapper.map(productLineDto,ProductLine.class));
	}

	@Override
	public ResultBean<ProductLineDto> findByIdOrName(String id, String name){
		ResultBean<ProductLineDto> resultBean = new ResultBean<>();
		Map<String,Object> parms = new HashMap<String,Object>();
		if(StringUtils.isNotBlank(id)){
			parms.put("EQ_id", id);
		}else if(StringUtils.isNotBlank(name)){
			parms.put("EQ_name", name);
		}else{
			resultBean.setResultBean(ResultCode.FAILED, "is or name 不能同时为空");
			return resultBean;
		}
		parms.put("EQ_isDel", 0);
		ProductLine productLine = findOne(SearchFilter.parse(parms).values());
		resultBean.setResultData(BeanMapper.map(productLine ,ProductLineDto.class));
		resultBean.setCode(ResultCode.OK);
		return resultBean;
	}
}
