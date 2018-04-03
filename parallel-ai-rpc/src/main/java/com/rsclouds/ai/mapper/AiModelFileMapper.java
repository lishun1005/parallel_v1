package com.rsclouds.ai.mapper;

import com.rscloud.ipc.rpc.api.entity.AiModelFile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiModelFileMapper {
    int delete(String id);

    int insert(AiModelFile record);

    AiModelFile selectById(@Param("id") String id);

    List<AiModelFile> selectByAiModelId(@Param("aiModelId") String aiModelId);
}