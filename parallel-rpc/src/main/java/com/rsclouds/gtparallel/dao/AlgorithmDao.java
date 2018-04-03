package com.rsclouds.gtparallel.dao;

import com.rsclouds.gtparallel.entity.Algorithm;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AlgorithmDao extends Repository<Algorithm, String> {
    List<Algorithm> queryAll(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
                             @Param("pageSize") int pageSize);
    void insert(Algorithm algorithm);
    void update(Algorithm algorithm);
}