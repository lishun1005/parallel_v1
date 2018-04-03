package com.rscloud.ipc.contrller;



import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rscloud.ipc.rpc.api.dto.SatelliteDto;
import com.rscloud.ipc.rpc.api.service.AreaInfoService;
import com.rscloud.ipc.rpc.api.service.SatelliteService;


@Controller
public class AreaInfoController {
	
	@Autowired
	@Lazy
	public AreaInfoService areaInfoService;
	
	@Autowired
	@Lazy
	public SatelliteService satelliteService;
	/**
	 * Description: 地区选择列表
	 * @param fatherId
	 * @return
	 */
	@RequestMapping(value = "/area/getAreaList")
	@ResponseBody
	public Object getAreasList(String fatherId){
		return areaInfoService.getDirectSubAreas(fatherId);
	}
	
	@RequestMapping(value = "/area/getFullAreasById", method = RequestMethod.GET)
	@ResponseBody
	public String getFullAreasById(String id){
		if(StringUtils.isNotBlank(id)){
			return areaInfoService.getFullAreasById(id);
		}
		return "地区id不能为空";
	}
	@RequestMapping(value = "getProvince")
	@ResponseBody
	public Object getProvince(){
		return areaInfoService.getProvince();
	}

	
	@RequestMapping("/querySatelliteType")
	@ResponseBody
	public List<SatelliteDto> queryAllSatellite() {
		return satelliteService.queryAllSatellite();
	}
	@RequestMapping("/areaInfo/queryGeomByAreaCode")
	@ResponseBody
	public String queryGeomByAreaCode(String admincode) {
		return areaInfoService.queryGeomByAreaCode(admincode);
	}
}
