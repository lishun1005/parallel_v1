package com.rsclouds.gtparallel.utils;

import com.rscloud.ipc.rpc.api.dic.Constant;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GdalUtils {
	private static Logger logger = LoggerFactory.getLogger(GdalUtils.class);
	/**
	 * 
	* Description: 获取影像信息
	*  @return 
	* @author lishun 
	* @date 2017年9月9日 
	* @return Map<String,Object>
	* 		  out_byte：输出字节数（8|16）
	* 		  project：投影 （4326|3857）
	* 		
	 */
	public static Map<String,Object> getImageInfo(String gtdataPath){
		Map<String,Object> map = new HashMap<String,Object>();
		try {
	        gdal.AllRegister();    
	        Dataset dataset = gdal.Open(gtdataPath, gdalconstConstants.GA_ReadOnly);//读取tiff文件
	        if (dataset == null)  {
	        	map.put("errorMessage", "gdal读取影像信息错误" + gdal.GetLastErrorMsg());
	        }else{
	        	if(dataset.GetRasterBand(1).getDataType() == gdalconstConstants.GDT_Byte){//判断位数
		        	map.put("out_byte", 8);
		        }else if(dataset.GetRasterBand(1).getDataType() == gdalconstConstants.GDT_UInt16){
		        	map.put("out_byte", 16);
		        }else{
		        	map.put("out_byte", null);
		        }
		        
		        if (dataset.GetProjectionRef().startsWith(Constant.WGS84P_ROJECT)) {//投影判断
		        	map.put("project", "EPSG:4326");//wgs84
				}else if(dataset.GetProjectionRef().startsWith(Constant.MERCATOR_PROJECT) ||
						dataset.GetProjectionRef().startsWith(Constant.MERCATOR_PROJECT1) ||
						dataset.GetProjectionRef().startsWith(Constant.PSEUDO_MERCATOR_PROJECT) ||
						dataset.GetProjectionRef().startsWith(Constant.PSEUDO_MECATOR_PROJECT_1)){
					map.put("project", "EPSG:3857");//mercator
				}else {
					map.put("errorMessage", "unkonwm project");//mercator
					logger.info("投影不支持");
				}
		        dataset.delete();  
	        }
		} catch (Exception e) {
			map.put("errorMessage", "gdal读取影像信息错误" + e.getMessage());
			logger.error(e.getMessage(), e);;
		}finally{
			 gdal.GDALDestroyDriverManager(); 
		}
		return map;
	}
}