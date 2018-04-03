package com.rscloud.ipc.rpc.api.service;

import java.util.List;

import com.rscloud.ipc.rpc.api.dto.SatelliteDto;



public interface SatelliteService {
	/**
	 * 
	* Description: 查询所有的卫星类型与分辨率
	*  @return 
	* @author lishun 
	* @date 2017年7月3日 
	* @return List<SatelliteApplication>
	 */
	public List<SatelliteDto> queryAllSatellite();

}
