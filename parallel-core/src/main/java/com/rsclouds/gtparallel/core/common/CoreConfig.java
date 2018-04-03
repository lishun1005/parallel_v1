package com.rsclouds.gtparallel.core.common;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;


/**
 * gt-parallel整体配置文件
 * @author wugq
 *
 */
public class CoreConfig {
	public static int ERROR = -1;
	public static int SUCCESS = 1;
	public static String LOCA_LTEMP_DIR = ConfProperty.getInstance().getStringValue("local.temp.dir");
	
	
	
	/*gt-data*/
	public static final String HBASE_SEPARATOR = "/";
	
	//user access
	public static String USERS_TABLE = ConfProperty.getInstance().getStringValue("users.table");
	public static int PERMISSION_READ = ConfProperty.getInstance().getIntValue("permission.read.value");
	public static int PERMISSION_WRITE= ConfProperty.getInstance().getIntValue("permission.write.value");
	public static String MAP_META_TABLE = ConfProperty.getInstance().getStringValue("map.meta.table");
	public static String MAP_RES_TABLE = ConfProperty.getInstance().getStringValue("map.res.table");
	
	public static int INTERVAL_DAY = ConfProperty.getInstance().getIntValue("delete.dir.interval.time.day");
	public static String RABBITMQ_HOST = ConfProperty.getInstance().getStringValue("rabbitmq.host");
	public static String RABBITMQ_VIRTUAL_HOST = ConfProperty.getInstance().getStringValue("rabbitmq.virtual.host");
	public static int RABBITMQ_PORT = ConfProperty.getInstance().getIntValue("rabbitmq.port");
	public static String RABBITMQ_USER = ConfProperty.getInstance().getStringValue("rabbitmq.user");
	public static String RABBITMQ_PWD = ConfProperty.getInstance().getStringValue("rabbitmq.pwd");
	public static String RABBITMQ_QUEUE_NAME = ConfProperty.getInstance().getStringValue("rabbitmq.queue.name");
	public static String RABBITMQ_REALTIMECHINA_PRODUCE_QUEUE_NAME  =  ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.produce.line.queue");
	public static String RABBITMQ_REALTIMECHINA_SEGMENT_QUEUE_NAME  =  ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.segment.queue");
	public static String RABBITMQ_REALTIMECHINA_SEGMENT_FAILED_QUEUE_NAME  =  ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.segment.failed.queue");
	public static String RABBITMQ_REALTIMECHINA_PRODUCE_FAILED_QUEUE_NAME  = ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.produce.line.queue.failed");
	
	
	public static String MODIS_GTDATA_PATH = ConfProperty.getInstance().getStringValue("modis.gtpdata.path");

	
	//实时地球一张图modis生产线
	public static String RABBITMQ_REALTIMECHINA_MAP_MODIS_DOWNLOAD_QUEUE_NAME =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.map.modis.download");
	public static String RABBITMQ_REALTIMECHINA_MAP_MODIS_MOSAIC =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.map.modis.mosaic");
	public static String RABBITMQ_REALTIMECHINA_MAP_MODIS_SEGEMENT_QUEUE_NAME =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.map.modis.segment");
	//实时地球一张图切片生产线
	public static String RABBITMQ_REALTIME_MAP_IMG_SEGMENT_QUEUE =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.map.img.segment");
	public static String RABBITMQ_REALTIMECHINA_MAP_SEGEMENT_FAILED =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.map.segment.failed");
	public static String RABBITMQ_REALTIMECHINA_MAP_INFO_UPDATE_FAILED =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.map.info.update.failed");
	public static String RABBITMQ_REALTIMECHINA_MAP_PUBLISH_FAILED =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.map.publish.failed");
	
	//遥感集市modis 产品生产队列
	public static String RABBITMQ_RSMART_MODIS_PRODUCE =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.rsmart.modis.produce");
	
	//pl 生产线rabbitmq 队列名
	public static String RABBITMQ_REALTIMECHINA_PL_PRODUCE_QUEUE_NAME =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.pl.produce.line");
	public static String RABBITMQ_REALTIMECHINA_PL_SEGMENT_QUEUE_NAME =
			ConfProperty.getInstance().getStringValue("rabbitmq.queue.realtimechina.pl.segment.queue");
	
	//实时地球分块信息rabbitmq队列
	public static String OLEARTH_BOLCKINFO_RABBITMQ_HOST = ConfProperty.getInstance().getStringValue("olearth.blockinfo.rabbitmq.host");
	public static int OLEARTH_BOLCKINFO_RABBITMQ_PORT = ConfProperty.getInstance().getIntValue("olearth.blockinfo.rabbitmq.port");
	public static String OLEARTH_BOLCKINFO_RABBITMQ_VIRTUAL_HOST = ConfProperty.getInstance().getStringValue("olearth.blockinfo.rabbitmq.virtual.host");
	public static String OLEARTH_BOLCKINFO_RABBITMQ_USER = ConfProperty.getInstance().getStringValue("olearth.blockinfo.rabbitmq.user");
	public static String OLEARTH_BOLCKINFO_RABBITMQ_QUEUE = ConfProperty.getInstance().getStringValue("olearth.blockinfo.rabbitmq.queue");
	public static String OLEARTH_BOLCKINFO_END = "block_info_end";
	public static String OLEARTH_BOLCKINFO_UPDATE_HOST = ConfProperty.getInstance().getStringValue("olearth.blockinfo.update.host");
	public static int OLEARTH_BOLCKINFO_UPDATE_PORT = ConfProperty.getInstance().getIntValue("olearth.blockinfo.update.port");
	public static String OLEARTH_BOLCKINFO_UPDATE_USER = ConfProperty.getInstance().getStringValue("olearth.blockinfo.update.user");
	public static String OLEARTH_BOLCKINFO_UPDATE_PWD = ConfProperty.getInstance().getStringValue("olearth.blockinfo.update.passwd");
	public static String OLEARTH_BLOCKINFO_REMOTE_UPDATE_TIME_URL = ConfProperty.getInstance().getStringValue("olearth.blodkinfo.remote.update.time.url");
	public static String OLEARTH_BLOCKINFO_REMOTE_UPDATE_SHPFILE_URL = ConfProperty.getInstance().getStringValue("olearth.blodkinfo.remote.update.shpfile.url");
	public static String OLEARTH_BLOCKINFO_LOCAL_SHP_UPDATE_QUEUE = ConfProperty.getInstance().getStringValue("olearth.blockinfo.local.shpfile.update.queue");
	public static String OLEARTH_BLOCKINFO_SHPUPDATE_LOCAL_PATH = ConfProperty.getInstance().getStringValue("olearth.blockinfo.local.shpfile.update.temp.path");
	
	public static String ERROR_DIR_PATH = ConfProperty.getInstance().getStringValue("error.dir.path");
	public static String ERROR_LOG_PATH = ConfProperty.getInstance().getStringValue("error.log.path");
	public static String ACCESS_LOG_PATH = ConfProperty.getInstance().getStringValue("access.log.path");
	
	
	//realtime China
	public static String URL_REALTIMECHINA_UPDATE = ConfProperty.getInstance().getStringValue("url.realtimechina.update.time");
	public static String URL_REALTIMECHINA_UPDATE_FLAG = ConfProperty.getInstance().getStringValue("url.realtimechina.update.flag");
	public static String URL_REALTIMECHINA_UPDATE_SHPFILE = ConfProperty.getInstance().getStringValue("url.realtimechina.update.shpfile");
	public static String URL_REALTIMECHAINA_LOG_UPDATE = ConfProperty.getInstance().getStringValue("url.realtimechina.log.update");
	public static String UPDATE_SHP_PATH = ConfProperty.getInstance().getStringValue("everydata.update.shp.path");
	
	public static final String USERS_ATTS = "atts";
	public static final String USERS_ATTS_PWD = "pwd";
	public static final String USERS_ATTS_ACCESS = "access";
	
	
	/*cutting*/
	public static final String CUTTING_OUTPUTPATH = "cutting_outputPath";
	public static final String TRASH = "/Trash";
	public static final String JOBID = "jobid";
	public static final String CUTTING_INPUTFILE = "cutting_input_path";
	public static int WIDTH_DEFAULT = ConfProperty.getInstance().getIntValue("cutting.width.rang");
	public static int HEIGHT_DEFAULT = ConfProperty.getInstance().getIntValue("cutting.height.rang");
	public static int IMGBLOCK_WIDTH = ConfProperty.getInstance().getIntValue("cutting.imgblock.rang");
	public static String LODINFO_SCALE = ConfProperty.getInstance().getStringValue("map.lodinfo.scale");
	public static String GEOWEBCACHE_XML_PATH = ConfProperty.getInstance().getStringValue("geowebcache.xml.path");
	//定义切片每个层级的分辨率
	public static final String KEY_DST_RESOLUTION = "dst_resolution";
	public static final String KEY_CURRENT_LAYER = "current_layer";
	public static final String KEY_MIN_LAYER = "min_layer";
	public static final String KEY_TILEORIGIN_X = "TileOrigin_X";
	public static final String KEY_TILEORIGIN_Y = "TileOrigin_Y";
	public static final String KEY_WARTERMARK = "wartermark";
	public static final String KEY_COVERAGE_BOOLEAN = "bCoverage";
	public static final String KEY_MOSAIC_BOOLEAN = "bMosaic";
	public static final String KEY_TIME_STRING = "time";
	public static final String KEY_TRUNC_BOOLEAN = "trunc_floor";
	public static final String KEY_ZERO_PERCENTAGE_INT = "zero_percentage";
	public static final String KEY_UPDATE_MINLAYER_INT = "update_minlayer";
	public static final String KEY_OUTPUT_FORMAT = "output_format";
	//WGS84 切片原点坐标
	public static final double VALUE_TILEORIGIN_X_WGS84 = 180;
	public static final double VALUE_TILEORIGIN_Y_WGS84 = 90;
	
	//墨卡托 切片原点坐标
	public static final double VALUE_TILEORIGIN_X_MERCATOR = 20037508.342787;
	public static final double VALUE_TILEORIGIN_Y_MERCATOR = 20037508.342787;
	
//	public static final String WGS84P_ROJECT = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\"," +
//			"\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433],AUTHORITY[\"EPSG\",\"4326\"]]";
	public static final String WGS84P_ROJECT = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",";
	public static final String MERCATOR_PROJECT = "PROJCS[\"WGS_1984_Web_Mercator_Auxiliary_Sphere\",GEOGCS[\""; //+
//			"SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]," +
//			"PROJECTION[\"Mercator_Auxiliary_Sphere\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0]," +
//			"PARAMETER[\"Central_Meridian\",0.0],PARAMETER[\"Standard_Parallel_1\",0.0],PARAMETER[\"Auxiliary_Sphere_Type\",0.0],UNIT[\"Meter\",1.0]]";
	public static final String MERCATOR_PROJECT1 = "PROJCS[\"WGS_1984_Web_Mercator\",GEOGCS[\"GCS_WGS_1984_Major_Auxiliary_Sphere\"";
	public static final String PSEUDO_MERCATOR_PROJECT = "PROJCS[\"WGS 84 / Pseudo-Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\"";
	public static final String PSEUDO_MECATOR_PROJECT_1 = "PROJCS[\"WGS_84_Pseudo_Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\"";
	
	
	public static final String WGS84_UTMZ_ZONE_49N = "PROJCS[\"WGS 84 / UTM zone 49N\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\"," +
			"SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0]," +
			"UNIT[\"degree\",0.0174532925199433],AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0]," +
			"PARAMETER[\"central_meridian\",111],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0]," +
			"UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AUTHORITY[\"EPSG\",\"32649\"]]";
	
	public static final double[] LAYERS_RESOLUTION = {0.703125, 0.3515625, 0.17578125, 0.087890625, 0.0439453125,
		0.02197265625, 0.010986328125, 0.0054931640625, 0.00274658203125, 0.001373291015625,
		0.0006866455078125, 0.00034332275390625, 0.000171661376953125, 8.58306884765625e-005, 4.291534423828125e-005,
		2.1457672119140625e-005, 1.0728836059570313e-005, 5.3644180297851563e-006, 2.6822090148925781e-006, 1.3411045074462891e-006,
		6.7055225372314453e-007};
	public static final double[] LAYERS_SCALE ={295497593.05875003, 147748796.52937502, 73874398.264687508, 36937199.132343754,
		18468599.566171877, 9234299.7830859385, 4617149.8915429693, 2308574.9457714846, 1154287.4728857423, 577143.73644287116,
		288571.86822143558, 144285.93411071779, 72142.967055358895, 36071.483527679447, 18035.741763839724, 9017.8708819198619,
		4508.9354409599309, 2254.4677204799655, 1127.2338602399827, 563.61693011999137, 281.80846505999568};
	
    public static final double[] MERCATOR_LAYERS_RESOLUTION = {156543.033928, 78271.5169639999, 39135.7584820001, 19567.8792409999,
	   9783.93962049996, 4891.96981024998, 2445.98490512499, 1222.99245256249, 
	   611.49622628138, 305.748113140558, 152.874056570411,  76.4370282850732, 
	   38.2185141425366, 19.1092570712683, 9.55462853563415, 4.77731426794937,
	   2.38865713397468, 1.19432856685505, 0.597164283559817, 0.298582141647617
	   }; 
    public static final double[] MERCATOR_LAYERS_SCALE = {591657527.591555, 295828763.795777, 147914381.897889, 73957190.948944,
    	36978595.474472, 18489297.737236, 9244648.868618, 4622324.434309, 2311162.217155, 1155581.108577, 577790.554289, 
    	288895.277144,   144447.638572,   72223.819286,   36111.909643,   18055.954822,   9027.977411,    4513.988705,
    	2256.994353,     1128.497176};
	 
	public static int PERSPLIT_ROKEYS_NUM = ConfProperty.getInstance().getIntValue("perSplit.rowkeys.number");
	
	public static String URL_DELETE_ACK = ConfProperty.getInstance().getStringValue("url.delete.acknowledgement");
	
	/*job_meta*/
	public static String MANAGER_JOB_TABLE = ConfProperty.getInstance().getStringValue("manager.job.table");
	public static String IMAGE_JOB_TABLE = ConfProperty.getInstance().getStringValue("image.job.table");
	
	
	public enum JOB{
		FAMILY("meta"),
		PID("pid"),
		IN_PATH("in_path"),
		OUT_PATH("out_path"),
		AOI_PATH("aoi_path"),
		EXTEND("extend"),
		NODE("node"),
		STATE("state"),
		STATUS("status"),
		TYPE("type"),
		PROGRESS("progress"),
		PART("part"),
		JID("jid"),
		START_TIME("start_time"),//set by worker
		END_TIME("end_time"),//set by worker
		ACCEPT_TIME("accept_time"),//set by client
		COMPLETE_TIME("complete_time"),//set by client
		LOG("log"),
		QUERY_STR("query_str"),
		CALLBACK_URL("callback_url"),
		MAP_NAME("map_name"),
		GEO_RANGE("geo_range"),
		CURRNT("curent"),
		TOTAL("total")
		;	
		public final String strVal;
		public final byte[] byteVal;
		JOB(String str){
			strVal = str;
			byteVal = Bytes.toBytes(str);
		}
	}
	
	public static String IMAGEJOB_OVERALL_TABLENAME = ConfProperty.getInstance().getStringValue("image.job.overall.table");
	public enum IMAGEJOB_OVERALL{
		FAMILY("meta"),
		PROD_LING("prod_line"),
		SEGEMENT("segment"),
		IMAGESTARTTIME("imageStime"),
		IMAGESENDTIME("imageEtime"),
		IMAGETYPE("image_type"),
		IMAGERESOLUTION("imageResolution"),
		PROVINCECODE("provinceCode"),
		MAPLAYERNAME("layerName"),
		WARTERMARK("wartermark"), 
		SRSEPSG("srsEpsg");
		
		public final String strVal;
		public final byte[] byteVal;
		IMAGEJOB_OVERALL(String str) {
			strVal = str;
			byteVal = Bytes.toBytes(str);
		}
	}
	
	
	public static final String[] LEVEL = {"_allLayers","L00" ,"L01" ,"L02","L03","L04","L05","L06","L07","L08","L09","L0a","L0b","L0c","L0d","L0e","L0f","L10","L11","L12","L13","L14"};
	
	//download 
	public static String DOWNLOAD_TEMP_PATH = ConfProperty.getInstance().getStringValue("download.temp.path");
	public static String DOWNLOAD_HDFS_PATH = new Path(GtDataConfig.HDFS_ROOT_PATH,ConfProperty.getInstance().getStringValue("download.hdfs.path")).toString();
	public static String URL_PUSH_PROGRESS= ConfProperty.getInstance().getStringValue("url.push.progress");
	public static String KEY_GRID_LAYER_INT = "keyGridLayer";
	
	
	//auth
	public static final String AUTH_FIMALY="atts";
	public static final String AUTH_PREFIX="prefix";
	public static final String AUTH_PWD="pwd";
	public static final String AUTH_ROLE="role";
	public static final String AUTH_TOTAL="total";
	public static final String AUTH_USED="used";
	
	
	public enum JOB_STATE{
		ACCEPTED,
		RUNNING,
		FAILED,
		SUCCEEDED,
		PAUSED,
		CANCELLED
	}
	
	public enum DECOMPRESS_TYPE{
		NO,
		ZIP,
		RAR,
		GZ,
	}
	
	public enum JOB_TYPE{
		ONEMAP,REALTIMEUPT,GENERATE_MAP_CONF
	}
	
	public static final String JOB_OP_ONEMAP = "ONEMAP";
	public static final String JOB_OP_REALTIMEUPT = "REALTIMEUPT";
	public static final String JOB_OP_GENERATE_MAP_CONF = "GENERATE_MAP_CONF";
	
	
	
	
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	}

}
