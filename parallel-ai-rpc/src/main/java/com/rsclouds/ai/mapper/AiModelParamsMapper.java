package com.rsclouds.ai.mapper;

import com.rscloud.ipc.rpc.api.entity.AiModelParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiModelParamsMapper {

    int insertBatch(@Param("list") List<AiModelParams> list);


    int update(AiModelParams record);

    List<AiModelParams> queryByAiModelId(@Param("aiModelId")String aiModelId);

    void delByAiModelId(@Param("aiModelId")String aiModelId);

}