package com.rscloud.ipc.dto;

import com.rscloud.ipc.rpc.api.entity.AiModelParams;

import java.util.List;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/2/2
 */
public class AiModelParamsListVo {
	List<AiModelParams> aiModelParams;

	public List<AiModelParams> getAiModelParams() {
		return aiModelParams;
	}

	public void setAiModelParams(List<AiModelParams> aiModelParams) {
		this.aiModelParams = aiModelParams;
	}
}
