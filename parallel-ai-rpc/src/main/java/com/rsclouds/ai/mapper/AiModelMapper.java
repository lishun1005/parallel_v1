package com.rsclouds.ai.mapper;

import com.rscloud.ipc.rpc.api.entity.AiModel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiModelMapper {
    int insert(AiModel record);

    List<AiModel> queryAll(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
                        @Param("pageSize") int pageSize);

    int update(AiModel record);
    AiModel findAiModelByName(@Param("name") String name);
    AiModel findAiModelById(@Param("id") String id);
}