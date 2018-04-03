package com.rsclouds.gtparallel.service.impl;


import com.rscloud.ipc.rpc.api.dto.AreaImageDto;
import com.rscloud.ipc.rpc.api.dto.AreaImageSeachDto;
import com.rscloud.ipc.rpc.api.service.AreaImageService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.gtparallel.dao.AreaImageDao;
import com.rsclouds.gtparallel.entity.AreaImage;
import com.rsclouds.jdbc.repository.JdbcRepository;
import org.apache.commons.lang3.StringUtils;
import org.gdal.ogr.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 后台菜单管理服务类
 * 
 * @author huangxj 2016年11月10日
 * 
 * @version v1.0
 */
@Service
@Transactional
public class AreaImageServiceImpl extends JdbcRepository<AreaImage, String> implements AreaImageService {

	@Autowired
	private AreaImageDao areaImageDao;

	@Override
	public List<AreaImageDto> query(AreaImageSeachDto areaImageSeachDto) {
		
		String gemo = areaImageSeachDto.getGemo();
		if(StringUtils.isNotBlank(gemo)){
			Geometry multiPolygon = Geometry.CreateFromJson(gemo.toLowerCase());
			gemo = multiPolygon.ExportToWkt();
			areaImageSeachDto.setGemo(gemo);
		}
		return BeanMapper.mapList(areaImageDao.query(areaImageSeachDto), AreaImageDto.class);
	}


	
}
