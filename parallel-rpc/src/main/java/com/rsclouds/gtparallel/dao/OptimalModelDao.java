package com.rsclouds.gtparallel.dao;

import com.rsclouds.gtparallel.entity.OptimalModel;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface OptimalModelDao {
    List<OptimalModel> queryAll(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
                                @Param("pageSize") int pageSize);
    void insert(OptimalModel OptimalModel);
    void update(OptimalModel OptimalModel);

    void insertBatch(List<Map<String,Object>> list);
    void deletemodelAlgorithmByModelId(@Param("modelId") String modelId);
    List<Map<String,Object>> queryOptimalModelByModelId(@Param("id") String id);
    OptimalModel queryById(@Param("id") String id);
}