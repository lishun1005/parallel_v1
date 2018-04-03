package com.rscloud.ipc.rpc.api.service;


import java.util.List;
import java.util.Map;


/**
 * 
* @ClassName: AreaService  
* @Description: TODO
* @author lishun 
* @date 2017年6月14日 下午3:16:34  
*
 */
public interface AreaInfoService {
	Map<String, Object>  getDirectSubAreas(String parentId);
	/**
	 * 
	* @Description: 根据Id查询指定的地区获取完整的地址
	* @param @param adminCode
	* @param @return 
	* @author lishun 
	* @date 2017年6月16日 
	* @return String
	 */
	String getFullAreasById(String adminCode);
	/**
	 * 
	* @Description: 获取所有省份
	* @param @return 
	* @author lishun 
	* @date 2017年6月18日 
	* @return List<Map<String,Object>>
	 */
	List<Map<String, Object>> getProvince();
	/**
	 * 
	* Description: 根据地区id获取该地区的范围
	*  @param admincode
	*  @return 
	* @author lishun 
	* @date 2017年7月4日 
	* @return String
	 */
	String queryGeomByAreaCode(String admincode);
}
