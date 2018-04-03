package com.rscloud.ipc.rpc.api.service;

import com.rscloud.ipc.rpc.api.entity.AiVm;
import com.rscloud.ipc.rpc.api.entity.AiVmInstancePort;
import com.rscloud.ipc.rpc.api.result.PageResultBean;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/30
 */
public interface AiVmService {
	PageResultBean<AiVm> queryAll(String keyword, int pageNum, int pageSize);

	void add(AiVm aiVm);

	void update(AiVm aiVm);

    void aiVmInstancePortAdd(AiVmInstancePort aiVmInstancePort);

	AiVmInstancePort selectAiVmInstancePortByVmIdAndPort(String vmId, Integer vmPort);

	void updateAiVmInstancePort(AiVmInstancePort aiVmInstancePort);
}
