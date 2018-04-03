package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.dto.AlgorithmDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;

/**
 * @Description:算法
 * @author lishun  
 * @date 2018/1/15
 * @param   
 * @return   
 */
public interface AlgorithmService {
	/*
	 * @Description:算法列表
	 * @author lishun
	 * @date 2018/1/15
	 * @param [keyword, pageNum, pageSize]
	 * @return java.util.List<com.rscloud.ipc.rpc.api.dto.AlgorithmDto>
	 */
	PageResultBean<AlgorithmDto> queryAlgorithmAll(String keyword, int pageNum, int pageSize);

	void insert(AlgorithmDto algorithmDto);

	void update(AlgorithmDto algorithmDto);

	ResultBean<AlgorithmDto> findByIdOrName(String id, String name);

}
