package com.rsclouds.gtparallel.service.impl;


import com.rscloud.ipc.rpc.api.service.AreaInfoService;
import com.rsclouds.gtparallel.dao.AreaInfoDao;
import com.rsclouds.gtparallel.entity.AreaInfo;
import com.rsclouds.jdbc.repository.JdbcRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台菜单管理服务类
 * 
 * @author huangxj 2016年11月10日
 * 
 * @version v1.0
 */
@Service
@Transactional
public class AreaInfoServiceImpl extends JdbcRepository<AreaInfo, String> implements AreaInfoService {

	@Autowired
	private AreaInfoDao areaInfoDao;

	@Override
	public Map<String, Object> getDirectSubAreas(String parentId) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		if (StringUtils.isBlank(parentId)) {
			parentId = "000000"; // ，默认查询全国
		}
		resultMap.put("list", areaInfoDao.getDirectSubAreas(parentId));
		resultMap.put("message", "ok");
		resultMap.put("code", "1");
		return resultMap;
	}

	@Override
	public String getFullAreasById(String adminCode) {
		String fullAreasName = "";
		AreaInfo areaInfo = findOne(adminCode);
		if (areaInfo != null) {
			if (areaInfo.getProname() != null) {
				fullAreasName = areaInfo.getProname();
			}
			if (areaInfo.getCityname() != null) {
				fullAreasName += areaInfo.getCityname();
			}
			if (areaInfo.getName() != null) {
				fullAreasName += areaInfo.getName();
			}
		}
		return fullAreasName;
	}
	@Override
	public List<Map<String,Object>>  getProvince(){
		return areaInfoDao.getProvince();
	}

	@Override
	public String queryGeomByAreaCode(String admincode) {
		return areaInfoDao.queryGeomByAreaCode(admincode);
	}
}
