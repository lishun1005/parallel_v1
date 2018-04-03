package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.dto.OptimalModelDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;

import java.util.List;
import java.util.Map;

/**
 * @Description:算法
 * @author lishun  
 * @date 2018/1/15
 * @param   
 * @return   
 */
public interface OptimalModelService {

	PageResultBean<OptimalModelDto> queryAll(String keyword, int pageNum, int pageSize);

	void insert(OptimalModelDto optimalModelDto);

	void update(OptimalModelDto optimalModelDto);

	void insertBatch(List<Map<String,Object>> list);

	List<Map<String,Object>> queryOptimalModelByModelId(String modelId);
}
