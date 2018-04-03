package com.rsclouds.gtparallel.dao;

import com.rsclouds.gtparallel.entity.MapManage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MapManageDao {
    int deleteById(String id);

    int insert(MapManage record);

    List<Map<String,Object>> queryAll(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
                                      @Param("pageSize") int pageSize, @Param("userName") String userName);

    int updateById(MapManage record);

    MapManage isExistMapName(String mapName);

    int isExistOutPath(String outPath);

    MapManage queryById(@Param("id") String id);
}