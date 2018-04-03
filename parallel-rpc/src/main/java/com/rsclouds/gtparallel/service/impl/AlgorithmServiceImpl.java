package com.rsclouds.gtparallel.service.impl;


import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.dto.AlgorithmDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.AlgorithmService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.gtparallel.dao.AlgorithmDao;
import com.rsclouds.gtparallel.entity.Algorithm;
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
@Service("algorithmService")
@Transactional
public class AlgorithmServiceImpl extends JdbcRepository<Algorithm, String> implements AlgorithmService {

	private static Logger logger = LoggerFactory.getLogger(AlgorithmService.class);
	@Autowired
	private AlgorithmDao algorithmDao;
	@Override
	public PageResultBean<AlgorithmDto> queryAlgorithmAll(String keyword, int pageNum, int pageSize) {
		Page<Algorithm> page = (Page<Algorithm>)algorithmDao.queryAll(keyword, pageNum, pageSize);
		PageResultBean<AlgorithmDto> pageResultBean =
				new PageResultBean<AlgorithmDto>(BeanMapper.mapList(page ,AlgorithmDto.class), page.getTotal(),page.getPages(),page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}
	@Override
	public void insert(AlgorithmDto algorithmDto){
		algorithmDao.insert(BeanMapper.map(algorithmDto,Algorithm.class));
	}
	@Override
	public void update(AlgorithmDto algorithmDto){
		algorithmDao.update(BeanMapper.map(algorithmDto,Algorithm.class));
	}
	@Override
	public ResultBean<AlgorithmDto> findByIdOrName(String id, String name){
		ResultBean<AlgorithmDto> resultBean = new ResultBean<>();
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
		Algorithm algorithm = findOne(SearchFilter.parse(parms).values());
		resultBean.setResultData(BeanMapper.map(algorithm ,AlgorithmDto.class));
		resultBean.setCode(ResultCode.OK);
		return resultBean;
	}
}
