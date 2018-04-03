package com.rsclouds.gtparallel.dao;


import com.rscloud.ipc.rpc.api.dto.AreasList;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


public interface AreaInfoDao {
	List<AreasList> getDirectSubAreas(@Param("parentId") String parentId);
	List<Map<String,Object>> getProvince();
	String queryGeomByAreaCode(String admincode);
}
