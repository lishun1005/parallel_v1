package com.rscloud.ipc.rpc.api.service;

import java.util.List;

import com.rscloud.ipc.rpc.api.dto.AreaImageDto;
import com.rscloud.ipc.rpc.api.dto.AreaImageSeachDto;



/**
 * 
* @ClassName: AreaImageService  
* @Description: 影像数据服务
* @author lishun 
* @date 2017年7月4日 上午9:47:09  
*
 */
public interface AreaImageService {
	public List<AreaImageDto> query(AreaImageSeachDto areaImageDto);
}
