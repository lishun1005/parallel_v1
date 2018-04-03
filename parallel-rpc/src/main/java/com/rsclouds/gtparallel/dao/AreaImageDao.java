package com.rsclouds.gtparallel.dao;


import com.rscloud.ipc.rpc.api.dto.AreaImageSeachDto;
import com.rsclouds.gtparallel.entity.AreaImage;

import java.util.List;


public interface AreaImageDao {
	public List<AreaImage> query(AreaImageSeachDto areaImageSeachDto);
}