package com.rsclouds.gtparallel.dao;

import com.rsclouds.gtparallel.entity.ProductLine;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductLineDao {

    List<ProductLine> queryAll(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
                                @Param("pageSize") int pageSize);
    int insert(ProductLine record);

    int update(ProductLine record);

}