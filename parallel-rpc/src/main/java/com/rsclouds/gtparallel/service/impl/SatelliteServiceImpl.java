package com.rsclouds.gtparallel.service.impl;

import com.rscloud.ipc.rpc.api.dto.SatelliteDto;
import com.rscloud.ipc.rpc.api.service.SatelliteService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.gtparallel.entity.Satellite;
import com.rsclouds.jdbc.repository.JdbcRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SatelliteServiceImpl extends JdbcRepository<Satellite, String> implements SatelliteService {
	@Override
	public List<SatelliteDto> queryAllSatellite() {
		return BeanMapper.mapList(findAll(), SatelliteDto.class);
	}

}
