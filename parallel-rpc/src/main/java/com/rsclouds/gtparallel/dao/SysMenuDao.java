package com.rsclouds.gtparallel.dao;


import com.rscloud.ipc.rpc.api.dto.SysMenuDto;
import com.rsclouds.gtparallel.entity.SysMenu;

import java.util.List;

public interface SysMenuDao {

	public int updateByPrimaryKeySelective(SysMenu record);
	public List<SysMenuDto> queryByUrl(String url);
}
