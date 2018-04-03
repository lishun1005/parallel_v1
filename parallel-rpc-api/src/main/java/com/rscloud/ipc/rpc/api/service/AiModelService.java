package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.entity.AiModel;
import com.rscloud.ipc.rpc.api.entity.AiModelFile;
import com.rscloud.ipc.rpc.api.entity.AiModelParams;
import com.rscloud.ipc.rpc.api.result.PageResultBean;

import java.util.List;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/2/1
 */
public interface AiModelService {
	AiModelFile delFileById(String id);

	List<AiModelFile> queryFileByAiModelId(String aiModelId);

	/*
		 * @Description:TODO
		 * @author lishun
		 * @date 2018/2/6
		 * @param [aiModelFile]
		 * @return void
		 */
	void addModelFile(AiModelFile aiModelFile);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6 
	 * @param [keyword, pageNum, pageSize]  
	 * @return com.rscloud.ipc.rpc.api.result.PageResultBean<com.rscloud.ipc.rpc.api.entity.AiModel>  
	 */
	PageResultBean<AiModel> queryAll(String keyword, int pageNum, int pageSize);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6 
	 * @param [aiModelId]  
	 * @return java.util.List<com.rscloud.ipc.rpc.api.entity.AiModelParams>  
	 */
	List<AiModelParams> queryParmasByAiModelId(String aiModelId);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6 
	 * @param [aiModel]  
	 * @return void  
	 */
	void add(AiModel aiModel);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6 
	 * @param [aiModel, params]  
	 * @return void  
	 */
	void addModelAndParams(AiModel aiModel, List<AiModelParams> params);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6 
	 * @param [aiModel, params]  
	 * @return void  
	 */
	void updateModelAndParams(AiModel aiModel, List<AiModelParams> params);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6 
	 * @param [aiModel]  
	 * @return void  
	 */
	void update(AiModel aiModel);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6 
	 * @param [name]  
	 * @return com.rscloud.ipc.rpc.api.entity.AiModel  
	 */
	AiModel findAiModelByName(String name);
	/*
	 * @Description:TODO
	 * @author lishun  
	 * @date 2018/2/6
	 * @param [id]  
	 * @return com.rscloud.ipc.rpc.api.entity.AiModel  
	 */
	AiModel findAiModelById(String id);
}
