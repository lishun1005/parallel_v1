package com.rsclouds.ai.service.impl;

import com.github.pagehelper.Page;
import com.rscloud.ipc.rpc.api.entity.AiVm;
import com.rscloud.ipc.rpc.api.entity.AiVmInstancePort;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.AiVmService;
import com.rsclouds.ai.mapper.AiVmMapper;
import com.rsclouds.common.utils.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/24
 */
@Service
@Transactional
public class AiVmServiceImpl implements AiVmService {
	@Autowired
	public AiVmMapper aiVmMapper;

	@Override
	public PageResultBean<AiVm> queryAll(String keyword, int pageNum, int pageSize) {
		Page<AiVm> page = (Page<AiVm>)aiVmMapper.queryAll(keyword, pageNum, pageSize);
		PageResultBean<AiVm> pageResultBean =
				new PageResultBean<AiVm>(page, page.getTotal(),page.getPages(),page.getPageNum());
		pageResultBean.setCode(ResultCode.OK);
		return pageResultBean;
	}
	@Override
	public void add(AiVm aiVm){
		aiVm.setId(StringTool.getUUID());
		aiVmMapper.insert(aiVm);
	}
	@Override
	public void update(AiVm aiVm){
		aiVmMapper.update(aiVm);
	}
	@Override
	public void aiVmInstancePortAdd(AiVmInstancePort aiVmInstancePort){
		aiVmMapper.aiVmInstancePortAdd(aiVmInstancePort);
	}
	@Override
	public AiVmInstancePort selectAiVmInstancePortByVmIdAndPort(String vmId, Integer vmPort){
		return aiVmMapper.selectAiVmInstancePortByVmIdAndPort(vmId, vmPort);
	}

	@Override
	public void updateAiVmInstancePort(AiVmInstancePort aiVmInstancePort){
		aiVmMapper.updateAiVmInstancePort(aiVmInstancePort);
	}
}
