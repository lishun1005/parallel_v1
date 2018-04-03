package com.rscloud.ipc.rpc.api.dic;

/**
 * 系统常用常量参数，本类存放静态不可修改的常量；如果是随环境影响经常更改的常量，请放在system.propertis中
 * 命名规范：全部大写，单词间用下划线隔开。
 * 
 * @author wangyq
 * 
 */
public final class Constant {
	/** 镶嵌类型 **/
	//public static final String MOSAIC_MODIS = "modis";
	//public static final String MOSAIC_PL = "PL";
	//public static final String MOSAIC_GF2_08 = "GF2-0.8m";
	//public static final String MOSAIC_GF1_2 = "GF1-2m";
	//public static final String MOSAIC_GF1_16 = "GF1-16m";
	public static final String MOSAIC_PL_QUALITY = "pl_quality";
	public static final String CHANGE_DETECTION = "change_detection";

	/** user_information 用户类型: 1 系统用户，0 集市用户 **/
	public static final int USERTYPE_SYS = 1;
	public static final int USERTYPE_RS = 0;

	/** gdal **/
	public static final String WGS84P_ROJECT = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",";
	public static final String MERCATOR_PROJECT = "PROJCS[\"WGS_1984_Web_Mercator_Auxiliary_Sphere\",GEOGCS[\"";
	public static final String MERCATOR_PROJECT1 = "PROJCS[\"WGS_1984_Web_Mercator\",GEOGCS[\"GCS_WGS_1984_Major_Auxiliary_Sphere\"";
	public static final String PSEUDO_MERCATOR_PROJECT = "PROJCS[\"WGS 84 / Pseudo-Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\"";
	public static final String PSEUDO_MECATOR_PROJECT_1 = "PROJCS[\"WGS_84_Pseudo_Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\"";

}
