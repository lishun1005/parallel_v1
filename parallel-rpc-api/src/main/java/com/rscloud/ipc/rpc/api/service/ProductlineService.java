package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.dto.ProductLineDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;

/**
 * @Description:算法
 * @author lishun  
 * @date 2018/1/15
 * @param   
 * @return   
 */
public interface ProductlineService {

	PageResultBean<ProductLineDto> queryAll(String keyword, int pageNum, int pageSize);

	void insert(ProductLineDto productLineDto);

	void update(ProductLineDto productLineDto);
	/*
	 * @Description:通过id或name查询数据(二选一;优先是id)
	 * @author lishun  
	 * @date 2018/1/17
	 * @param [name]  
	 * @return com.rscloud.ipc.rpc.api.result.ResultBean<com.rscloud.ipc.rpc.api.dto.ProductLineDto>  
	 */
	ResultBean<ProductLineDto> findByIdOrName(String id, String name);
}
