package com.rsclouds.ai.service.impl;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.entity.AiModel;
import com.rscloud.ipc.rpc.api.entity.AiModelFile;
import com.rscloud.ipc.rpc.api.entity.AiModelParams;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.AiModelService;
import com.rsclouds.ai.mapper.AiModelFileMapper;
import com.rsclouds.ai.mapper.AiModelMapper;
import com.rsclouds.ai.mapper.AiModelParamsMapper;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.StringTool;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/24
 */
@Service
@Transactional
public class AiModelServiceImpl implements AiModelService {
	@Autowired
	public AiModelMapper aiModelMapper;
	@Autowired
	public AiModelParamsMapper aiModelParamsMapper;
	@Autowired
	public AiModelFileMapper aiModelFileMapper;


	@Override
	public AiModelFile delFileById(String id){
		AiModelFile aiModelFile = aiModelFileMapper.selectById(id);
		aiModelFileMapper.delete(id);
		return aiModelFile;
	}

	@Override
	public List<AiModelFile> queryFileByAiModelId(String aiModelId){
		return aiModelFileMapper.selectByAiModelId(aiModelId);
	}

	@Override
	public void addModelFile(AiModelFile aiModelFile) {
		if(StringUtils.isBlank(aiModelFile.getId())){
			aiModelFile.setId(StringTool.getUUID());
		}
		aiModelFileMapper.insert(aiModelFile);
	}


	@Override
	public PageResultBean<AiModel> queryAll(String keyword, int pageNum, int pageSize) {
		Page<AiModel> page = (Page<AiModel>)aiModelMapper.queryAll(keyword, pageNum, pageSize);
		PageResultBean<AiModel> pageResultBean =
				new PageResultBean<AiModel>(BeanMapper.mapList(page ,AiModel.class), page.getTotal(),page.getPages(),page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}

	@Override
	public List<AiModelParams> queryParmasByAiModelId(String aiModelId) {
		return aiModelParamsMapper.queryByAiModelId(aiModelId);
	}

	@Override
	public void add(AiModel aiModel){
		if(StringUtils.isBlank(aiModel.getId())){
			aiModel.setId(StringTool.getUUID());
		}
		aiModelMapper.insert(aiModel);
	}
	@Override
	public void addModelAndParams(AiModel aiModel, List<AiModelParams> params) {
		if(StringUtils.isBlank(aiModel.getId())){
			aiModel.setId(StringTool.getUUID());
		}
		aiModelMapper.insert(aiModel);
		aiModelParamsMapper.insertBatch(params);
	}
	@Override
	public void updateModelAndParams(AiModel aiModel, List<AiModelParams> params) {
		if(StringUtils.isBlank(aiModel.getId())){
			aiModel.setId(StringTool.getUUID());
		}
		aiModel.setUtTime(new Date());
		aiModelMapper.update(aiModel);
		aiModelParamsMapper.delByAiModelId(aiModel.getId());
		aiModelParamsMapper.insertBatch(params);
	}

	@Override
	public void update(AiModel aiModel){
		aiModelMapper.update(aiModel);
	}

	@Override
	public AiModel findAiModelByName(String name){
		return aiModelMapper.findAiModelByName(name);
	}
	@Override
	public AiModel findAiModelById(String id){
		return aiModelMapper.findAiModelById(id);
	}
}
