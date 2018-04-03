package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.swing.ImageIcon;

import net.sf.json.JSONObject;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.Geometry;
import org.gdal.osr.SpatialReference;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.GdalDatasetBase;
import com.rsclouds.gtparallel.core.gtdata.common.GeometryBase;
import com.rsclouds.gtparallel.core.gtdata.common.HttpClientBase;
import com.rsclouds.gtparallel.core.gtdata.common.GeowebcacheTool;
import com.rsclouds.gtparallel.core.gtdata.common.RemoteShell;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImageMutilLayersInputFormat;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;



public class RealChinaSegement extends Configured implements Tool{
	private static final Log LOG = LogFactory.getLog(RealChinaSegement.class);
	public static final long MAX_TIME = 9999999999999L;
	public static final String SAVE_STORAGE_BOOLEAN = "save_storage";
	public static final String KEY_TILESHPUPDATE_BOOLEAN = "tile_shp_update";
	public static final String KEY_RESOLUTION_CM_STRING = "resolution_cm";
	public static final String KEY_RABBITMQ_BOOLEAN = "brabbitmq";
	public static final String KEY_LOCALSHPUPDATESYNC_BOOLEAN = "blocalShpUpdateSync";
	public static int resolution[] = {250, 16, 8, 4, 2};
		
	
	static class ImageSegMapper extends Mapper<ImageMutilLayersInfo, NullWritable, Text, FileInfo> {
		//configuration init args
		private HTable resourceTable;
		private HTable metadataTable;
		private boolean wartermarkFlag = true;
//		private boolean saveStorageBool;
		private boolean bTileShpUpdate;
		private int nodataInt;	
		private int updateMinLayer;
		private int cuttingMinLayer;
		private long timeLong;
		//零值像素最大百分比，超过该阈值，该瓦片不输出
		private double zeroMaxPercentage = 1.0;
		private double KEY_TILEORIGIN_X;
		private double KEY_TILEORIGIN_Y;
		private Text path = new Text();
		private FileInfo fileInfo = new FileInfo();
		private StringBuilder strBuilderMD5 = new StringBuilder();
		private String outputpath = null;	//瓦片输出路径
		private int pictureFormatInt;
		private String timeStr = null;
		private String strResolutionCM;
		
		
		private boolean bupdataTile = false;
		private int curentLayers;
		private String[] updateLayerPaths;
		private String rowColTimeUpdateUrl = CoreConfig.URL_REALTIMECHINA_UPDATE;
				
		//瓦片的默认长宽
		private int widthRang;
		private int heightRang;
		private int width;
		private int height;

		//瓦片行列号
		private StringBuilder colStrBuilder = new StringBuilder();
		private StringBuilder rowStrBuilder = new StringBuilder();		
		private GeometryBase geometryBase = null;
		private boolean bOutput = false;
		private double[] adfGeoTransform = null;
		private GdalDatasetBase gdalDatasetBase = null;
		private boolean bCuttingMinLayer = false;
		private boolean brabbitmq;
		public static Channel channel = null;
		
		
		/**
		 * 初始化参数	
		 */
		public void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
			timeStr = conf.get(CoreConfig.KEY_TIME_STRING, null);
			cuttingMinLayer = conf.getInt(CoreConfig.KEY_MIN_LAYER, 0);
			updateMinLayer = conf.getInt(CoreConfig.KEY_UPDATE_MINLAYER_INT, -1);
			zeroMaxPercentage = conf.getDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, 1.1);
//			saveStorageBool = conf.getBoolean(SAVE_STORAGE_BOOLEAN, false);
			brabbitmq = conf.getBoolean(KEY_RABBITMQ_BOOLEAN, false);
			if (brabbitmq) {
				try {
					ConnectionFactory factory = new ConnectionFactory();
					factory.setHost(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_HOST);
					factory.setPort(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_PORT);
					factory.setUsername(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_USER);
					factory.setConnectionTimeout(50000);factory.setShutdownTimeout(50000);
					factory.setPassword("rsclouds@012");
					factory.setVirtualHost(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_VIRTUAL_HOST);
					Connection connection;
					connection = factory.newConnection();
					channel = connection.createChannel();
					channel.queueDeclare(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_QUEUE, true, false, false, null);
					LOG.info("connect rabbitqm: queue=" + CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_QUEUE +
							" host=" + CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_HOST);
				} catch (TimeoutException e) {
					LOG.info("connect rabbitqm failed");
					e.printStackTrace();
					channel = null;
				}
			}
			KEY_TILEORIGIN_X = conf.getDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_WGS84);
			KEY_TILEORIGIN_Y = conf.getDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_WGS84);
			strResolutionCM = conf.get(KEY_RESOLUTION_CM_STRING, "");
			bTileShpUpdate = conf.getBoolean(KEY_TILESHPUPDATE_BOOLEAN, false);
			nodataInt = conf.getInt("NO_DATA", -1);
			
			if (timeStr != null) {
				timeLong = Long.parseLong(timeStr);
				long time = MAX_TIME - timeLong;
				StringBuilder timeStrBuilder = new StringBuilder(time + "");
				int length = timeStrBuilder.length();
				if(length < 13) {
					for(int i = length; i < 13; i ++) {
						timeStrBuilder.insert(0, "0");
					}
				}
				timeStr = timeStrBuilder.toString();
				fileInfo.setTimeText(timeStr);
				fileInfo.setGeometry(false);
				pictureFormatInt = GdalDatasetBase.JPEG_FORMAT_INT;
			}else {
				pictureFormatInt = GdalDatasetBase.PNG_FORMAT_INT;
			}
			resourceTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
			metadataTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
			resourceTable.setAutoFlushTo(false);
			widthRang = CoreConfig.WIDTH_DEFAULT;
			heightRang = CoreConfig.HEIGHT_DEFAULT;
					
		}	
				
		
		/**
		 * @bref 将列号号转换为十六进制，并格式化为字符串“R00000000。png”， 即R后面跟八位数十六进制，不足补零，然后再加“。png”后缀。
		 * @param colNum
		 */
		public void setCol(long colNum) {
			colStrBuilder.replace(0, colStrBuilder.length(), Long.toHexString(colNum));
			int count = 8 - colStrBuilder.length();
			for (int i = 0; i < count; i++) {
				colStrBuilder.insert(0, "0");
			}
			colStrBuilder.insert(0, "C");
		}
		
		
		/**
		 * @bref 将行号转换为十六进制，并格式化为字符串“R00000000”， 即R后面跟八位数十六进制，不足补零
		 * @param rowNum
		 */
		public void setRow(long rowNum) {
			rowStrBuilder.replace(0, rowStrBuilder.length(), Long.toHexString(rowNum));
			int count = 8 - rowStrBuilder.length();
			for (int i = 0; i < count; i++) {
				rowStrBuilder.insert(0, "0");
			}
			rowStrBuilder.insert(0, "R");
		}	
		
		public boolean output(Context context, String outputpath, long row, long col) throws IOException, InterruptedException{
			boolean bupdateFlag = false;
			String param = null;
			if (strResolutionCM.equals("25000")) {
				param = "&longtime=" + timeLong + "&res=" + strResolutionCM;
			}else {
				param = "row=" + row + "&col=" + col + "&longtime=" + timeLong + "&res=" + strResolutionCM;
			}
			if (timeStr != null && bTileShpUpdate) { 
				fileInfo.setTimeText(timeStr);
				int count = 3;
				if(curentLayers ==  cuttingMinLayer) {
					if (brabbitmq) {
						if (channel != null) {
							try {
								channel.basicPublish("", CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_QUEUE,
									MessageProperties.PERSISTENT_TEXT_PLAIN, param.getBytes());
								bupdateFlag = true;
							}catch(IOException e) {
								bupdateFlag = false;
							}
						}else {
							bupdateFlag = false;
						}
					}else {
						for(int i = 0; i < count; i ++) {
							String response = HttpClientBase.sendGet(rowColTimeUpdateUrl, param);
							if(response == null)
								continue;
							JSONObject json = JSONObject.fromObject(response);
							String codeVale = json.optString("code", "-5");
							if (codeVale.equals("1")) {
								System.out.println(codeVale + " " + rowColTimeUpdateUrl + ", param=" + param);
								bupdateFlag = true;
								break;
							}else {
								System.out.println(codeVale + " " + rowColTimeUpdateUrl + ", param=" + param);
							}
						}
					}
				}else {
					bupdateFlag = true;
				}
			}else {
				bupdateFlag = true;
			}
//			bupdateFlag = true;
			//只有更新矢量数据成功后才将瓦片写入到数据库中
			if(bupdateFlag) {
				if (StringUtils.isEmpty(strResolutionCM)) {
					path.set(outputpath + rowStrBuilder.toString() + "/" + colStrBuilder.toString());
				}else {
					path.set(outputpath + rowStrBuilder.toString() + "/" + colStrBuilder.toString() + "/" + strResolutionCM + "cm");
				}
				strBuilderMD5.append(MD5Calculate.fileByteMD5(gdalDatasetBase.getBufferImagedata(pictureFormatInt)));
				fileInfo.setMD5(strBuilderMD5.toString());
				fileInfo.setLength(gdalDatasetBase.getBufferImageSize());
				fileInfo.setFilename(colStrBuilder.toString());
//				System.out.println("=====nanlin====== start check md5:" + strBuilderMD5.toString());
				Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
				Result result = resourceTable.get(get);
				if (result == null || result.isEmpty()) {
					Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
							GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
							GtDataConfig.RESOURCE.DATA.byteVal, gdalDatasetBase.getBufferImagedata(pictureFormatInt));
					resourceTable.put(put);
				}
				context.write(path, fileInfo);
//				gdalDatasetBase.write2LocalFile("D://nanlin//pl_test//" + rowStrBuilder.toString() + "_" + colStrBuilder.toString()+".png", pictureFormatInt);
				System.out.println("outWrite path=" + path.toString());
				strBuilderMD5.delete(0, strBuilderMD5.length());
				return true;
			}else {
				System.out.println(rowColTimeUpdateUrl + param + " update failed");
				return false;
			}
		}
		
	
		public void map(ImageMutilLayersInfo key, NullWritable value, Context context) throws
			IOException, InterruptedException {
			System.out.println("map key: " + key.getGtdataOutputPath().toString());
			if (channel == null && brabbitmq) {
				return;
			}
			if(gdalDatasetBase == null) {			
				int mutil = CoreConfig.IMGBLOCK_WIDTH;
				int[] bands = key.getBands();	
				gdalDatasetBase = new GdalDatasetBase(key.getFilepath().toString(), zeroMaxPercentage, widthRang, heightRang, mutil, bands, nodataInt);
				width = gdalDatasetBase.getWidth();
				height = gdalDatasetBase.getHeight();	
				
				adfGeoTransform = gdalDatasetBase.getAdfGeoTransform();
				adfGeoTransform[1] = key.getDstResolution();
				adfGeoTransform[5] = -key.getDstResolution();
								
				outputpath = key.getGtdataOutputPath().toString();
//				System.out.println("=====nanlin debug===== gdalDatasetBase init " + outputpath);
				System.out.println("first inite outputpath " + outputpath);
				if(!outputpath.endsWith("/")) {
					outputpath += "/";
				}
				
				bupdataTile = false;
				curentLayers = key.getCurentLayer();
				if(key.getCurentLayer() == cuttingMinLayer) {
					bCuttingMinLayer = true;
					geometryBase = new GeometryBase();
					if (updateMinLayer >= 0) {
						bupdataTile = true;
						int count = cuttingMinLayer - updateMinLayer;
						if(count > 0) {
							updateLayerPaths = new String[count];
							String minLayerName = GeowebcacheTool.getLayerName(cuttingMinLayer);
							int currentLayer;
							for(int i = 0; i < count; i ++) {
								currentLayer = cuttingMinLayer-i-1;
								updateLayerPaths[i] = outputpath.replace(minLayerName, GeowebcacheTool.getLayerName(currentLayer));
							}
						}
					}
				}
			}
								
			long xOrigin = key.getXreadOrigin();//重采样后的x坐标
			long yOrigin = key.getYreadOrigin();//重采样后的y坐标
			int xLength = (int)key.getXreadLen();//重采样后的宽度
			int yLength = (int)key.getYreadLen();//重采样后的高度
			int tileColOffset = key.getTileColOffset();
			int tileRowOffset = key.getTileRowOffset();
			double dstResolution = key.getDstResolution();//重采样后的分辨率
			double oriResoulution = key.getOriResolution();
			long colNum = key.getColNum();
			long rowNum = key.getRowNum();
			
			if (gdalDatasetBase != null) {

				//实际需要读取影像的真实起始点坐标（以像素为单位）
				int xReadCoordinateActual = (int)(xOrigin * dstResolution / oriResoulution);
				int yReadCoordinateActual = (int)(yOrigin * dstResolution / oriResoulution);
				
				//实际需要读取的瓦片数据范围
				int xreadActual = (int)(xLength * (dstResolution) / oriResoulution);
				int yreadActual = (int)(yLength * (dstResolution) / oriResoulution);
				if(xReadCoordinateActual + xreadActual > width) {
					xreadActual = width - xReadCoordinateActual;
				}
				if(yReadCoordinateActual + yreadActual > height) {
					yreadActual = height - yReadCoordinateActual;
				}
	
				//重采样后需要读取的瓦片数据范围
				int xread;
				//重采样后已经读取的影像范围
				int xreaded = 0;
				//写入数据的起始点
				int xWriteOrigin;
				int yWriteOrigin = tileRowOffset;
				if (gdalDatasetBase.readData(xReadCoordinateActual, yReadCoordinateActual, xreadActual, yreadActual, xLength, yLength)) {
//					System.out.println("=====nanlin===== finish readdata");
					for (xreaded = 0; xreaded < xLength; ) {
//						System.out.println("=====nanlin===== finish readdata" + xreaded);
						if(xreaded == 0) {
							xread = widthRang - tileColOffset;
							if(xread > xLength) {
								xread = xLength;
							}
							xWriteOrigin = tileColOffset;
						}else {
							if (xLength -xreaded < widthRang) {
								xread = xLength -xreaded;
							}else{
								xread = widthRang;
							}
							xWriteOrigin = 0;
						}
							
						//初始bufferImage
						gdalDatasetBase.resetBufferedImage();
//						System.out.println("=====nanlin===== changeRGB" + xread + " " + yLength + " " + xreaded + " " + xLength);
						bOutput = gdalDatasetBase.changRGB(xread, yLength, xreaded, xLength, true);
//						System.out.println("=====nanlin===== changeRGB finish" + bOutput);
						if(bOutput) {
							pictureFormatInt = GdalDatasetBase.JPEG_FORMAT_INT;
						}else {
							pictureFormatInt = GdalDatasetBase.PNG_FORMAT_INT;
						}

						if (bOutput) {
							gdalDatasetBase.setBufferedImage(xWriteOrigin, yWriteOrigin, xread, yLength, pictureFormatInt);
							if(wartermarkFlag) {
								gdalDatasetBase.setWarterMask("ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 13, Color.white, -1, -1, 0.4f, pictureFormatInt);
							}

							setCol(colNum);
							setRow(rowNum);
							fileInfo.setBupdate(false);
							bOutput = output(context, outputpath, rowNum, colNum);							
							
							if(bOutput && bCuttingMinLayer){

								if( !geometryBase.Union(cuttingMinLayer, colNum, rowNum, KEY_TILEORIGIN_X, KEY_TILEORIGIN_Y, GeometryBase.MERCATOR) ) {
									System.out.println("=====nanlin===== Union error");
									return;
								}else if (bupdataTile) {
									String prefix = outputpath+rowStrBuilder.toString()+"/" + colStrBuilder.toString();	
									//更新指定层级范围的瓦片
									if(GeowebcacheTool.isNewestTime(metadataTable, prefix, timeLong)) {
										int count = cuttingMinLayer - updateMinLayer;
										long currentRowNum = rowNum;
										int currentRowCoor = 0;
										long currentColNum = colNum; 
										int currentColCoor = 0;
										int times = 1;
										int currentwidth = widthRang;
										int currentheight = heightRang;
										int xOriginTemp = (int)((xOrigin+xreaded) * dstResolution / oriResoulution);
										fileInfo.setxOringin(xOriginTemp);
										fileInfo.setyOrigin((int)yReadCoordinateActual);
										int xreadActualDefault = (int)(widthRang * (dstResolution) / oriResoulution);
										int yreadActualDefault = (int)(heightRang * (dstResolution) / oriResoulution);
										if(xOriginTemp + xreadActualDefault > width)
											xreadActualDefault = width - xOriginTemp;
										if(yReadCoordinateActual + yreadActualDefault > height) {
											yreadActualDefault = height - yReadCoordinateActual;
										}
										fileInfo.setReadwidth(xreadActualDefault);
										fileInfo.setReadheight(yreadActualDefault);
										fileInfo.setBupdate(true);
									
										for(int i = 0; i < count; i ++) {
											currentwidth = currentwidth / 2;
											currentheight = currentheight / 2;
											times = times * 2;
											currentRowCoor = (int)rowNum % times;
											currentRowNum = currentRowNum / 2;
											currentColCoor = (int)colNum % times;
											currentColNum = currentColNum / 2;
											setCol(currentColNum);
											setRow(currentRowNum);
											fileInfo.setColCoor(currentColCoor);
											fileInfo.setRowCoor(currentRowCoor);
											fileInfo.setHeight(currentheight);
											fileInfo.setWidth(currentwidth);
											path.set(updateLayerPaths[i] + rowStrBuilder.toString() + "/" + colStrBuilder.toString());
											context.write(path, fileInfo);
										}// for count
									}// end if GeowebcacheTool.isNewestTime
								}// end if bupdataTile
							}// end if boutput
						}
						xreaded += xread;
						colNum++;
					}
																	
				} 
			}
		}
		
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			super.cleanup(context);
			if (resourceTable != null){
				resourceTable.flushCommits();
				resourceTable.close();
			}
			if(gdalDatasetBase != null) {
				gdalDatasetBase.close();
			}
			if(outputpath != null) {
				Configuration conf = context.getConfiguration();
				HTable metaTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
				metaTable.setAutoFlush(false, true);
				//insert the Layers information into meta table
				StringBuilder rowkeyStrBuilder = new StringBuilder(outputpath);
				rowkeyStrBuilder.insert(rowkeyStrBuilder.lastIndexOf("/L"), "/");
				rowkeyStrBuilder.deleteCharAt(rowkeyStrBuilder.length()-1);
				byte[] keys = Bytes.toBytes(TransCoding.decode(rowkeyStrBuilder.toString(), "UTF-8"));
				Get get = new Get(keys);
				Result result = metaTable.get(get);
				if(result.isEmpty()) {
					Put put = new Put(Bytes.toBytes(TransCoding.decode(rowkeyStrBuilder.toString(), "UTF-8")));
					put.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.DFS.byteVal,
							Bytes.toBytes("0"));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.SIZE.byteVal,
							Bytes.toBytes("-1"));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.URL.byteVal,
							Bytes.toBytes(""));
					put.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.TIME.byteVal,
							Bytes.toBytes("" + System.currentTimeMillis()));
					metaTable.put(put);
					metaTable.flushCommits();
				}
				metaTable.close();
			}
			metadataTable.close();
			if(geometryBase != null && geometryBase.getJSON() != null) {
				//String path1 = "F://test" + System.currentTimeMillis() + ".geojson";
				//geometryBase.writeToLocalFile(path1);
				path.set(timeStr);
				fileInfo.setMD5(geometryBase.getJSON());
				fileInfo.setGeometry(true);
				context.write(path, fileInfo);
			}
		}
	}
	
	/**
	 * 
	 * @author shaolin
	 *insert the file's record into meta table
	 */
	static class MetaTableInsertReducer extends
			TableReducer<Text, FileInfo, NullWritable> {
		int indexof;
		int rowkeyPrefixLen;
		private HTable resourceTable;
		private HTable metaTable;
		private String strResolutionCM;
		
		private String format;
		private String imagePath;
		private double zeroMaxPercentage;

		private StringBuilder strBuilderMD5 = new StringBuilder();
		private boolean bReaded;
		private boolean wartermarkFlag;
		private String geometryOutputPath;
		private String geometryLocalDir;
		private String timeStr;
		private long timeLong;
		
		private boolean bGeometry;
		private GeometryBase geometry = null;
		private GdalDatasetBase gdalDatasetBase = null;
		private int pictureFormatInt = GdalDatasetBase.PNG_FORMAT_INT;
		private boolean brabbitmq;
		private static Channel channel = null;
//		private boolean blocalShpUpdateSync = true;
		
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			format = conf.get(CoreConfig.KEY_OUTPUT_FORMAT, ".png");
			imagePath = conf.get(CoreConfig.CUTTING_INPUTFILE, null);
			zeroMaxPercentage = conf.getDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, 1.1);
			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
			strResolutionCM = conf.get(KEY_RESOLUTION_CM_STRING, "");
			timeStr = conf.get(CoreConfig.KEY_TIME_STRING, null);
			brabbitmq = conf.getBoolean(KEY_RABBITMQ_BOOLEAN, false);
//			blocalShpUpdateSync = conf.getBoolean(KEY_LOCALSHPUPDATESYNC_BOOLEAN, true);
			if (brabbitmq) {
				try {
					ConnectionFactory factory = new ConnectionFactory();
					factory.setHost(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_HOST);
					factory.setPort(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_PORT);
					factory.setUsername(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_USER);
					factory.setConnectionTimeout(50000);factory.setShutdownTimeout(50000);
					factory.setPassword("rsclouds@012");
					factory.setVirtualHost(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_VIRTUAL_HOST);
					Connection connection;
					connection = factory.newConnection();
					channel = connection.createChannel();
					channel.queueDeclare(CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_QUEUE, true, false, false, null);
					LOG.info("connect rabbitqm: queue=" + CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_QUEUE +
							" host=" + CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_HOST);
				} catch (TimeoutException e) {
					LOG.info("connect rabbitqm failed");
					e.printStackTrace();
					channel = null;
				}
			}
			if(timeStr != null) {
				timeLong = Long.parseLong(timeStr);
				long timeLongTemp = MAX_TIME - timeLong;
				timeStr = "" + timeLongTemp;
				while(timeStr.length() != 13) {
					timeStr = "0" + timeStr;
				}
			}
			
			geometryLocalDir = CoreConfig.LOCA_LTEMP_DIR;
			if(!geometryLocalDir.endsWith("/")) {
				geometryLocalDir = geometryLocalDir + "/";
			}
			geometryLocalDir +=  conf.get(CoreConfig.JOBID, "123456") + "/"; 
			
			geometryOutputPath = CoreConfig.UPDATE_SHP_PATH;
			while(geometryOutputPath.endsWith("/"))
				geometryOutputPath = geometryOutputPath.substring(0, geometryOutputPath.length()-1);
			if (!StringUtils.isEmpty(strResolutionCM)){
				geometryOutputPath += "/" + strResolutionCM + "cm";
			}
			
			if(!format.startsWith(".")) {
				format = "." + format;
			}
			resourceTable = new HTable(conf, GtDataConfig.TABLE_NAME.MAP_RES_TABLE.getStrVal());
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.MAP_META_TABLE.getStrVal());
		}
		
		
		public void reduce(Text key, Iterable<FileInfo> values, Context context)
				throws IOException, InterruptedException {
			if(imagePath == null){
				System.out.println("image inputpath is null");
				return;
			}
			
			bReaded = false;
			bGeometry = false;
			String gtpath = GtDataUtils.format2GtPath(key.toString()) + format;
			for (FileInfo val : values) {
				if(val.isGeometry()) {
					bGeometry = true;
					Geometry geoTemp = Geometry.CreateFromJson(val.getMD5().toString());
					SpatialReference reference = new SpatialReference("");
					reference.SetWellKnownGeogCS("WGS84");
					geoTemp.AssignSpatialReference(reference);
					System.out.println("geometry=========");
					if(geometry == null) {
						geometry = new GeometryBase(geoTemp);
					}else {
						geometry.Union(geoTemp);
					}
					
				}else if(val.getTimeText() == null || val.getTimeText().getLength() == 0) {//不含时间的瓦片更新
					Put put = new Put(Bytes.toBytes(gtpath));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.DFS.byteVal,
							Bytes.toBytes("0"));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.SIZE.byteVal,
							Bytes.toBytes(String.valueOf(val.getLength())));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.URL.byteVal,
							Bytes.toBytes(val.getMD5().toString()));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.TIME.byteVal,
							Bytes.toBytes("" + System.currentTimeMillis()));
					context.write(NullWritable.get(), put);
				}else if(!val.getBupdate()){//含有时间轴的瓦片更新，瓦片带有时间属性
					String gtpathTime = GtDataUtils.format2GtPath(key.toString() + "/" + val.getTimeText().toString()) + format;
					Put put = new Put(Bytes.toBytes(gtpathTime));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.DFS.byteVal,
							Bytes.toBytes("0"));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.SIZE.byteVal,
							Bytes.toBytes(String.valueOf(val.getLength())));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.URL.byteVal,
							Bytes.toBytes(val.getMD5().toString()));
					put.add(GtDataConfig.META.FAMILY.byteVal, 
							GtDataConfig.META.TIME.byteVal,
							Bytes.toBytes("" + System.currentTimeMillis()));
					context.write(NullWritable.get(), put);
				}else {//更新最新瓦片，但不需要显示时间
					if (gdalDatasetBase == null) {
						int width  = CoreConfig.WIDTH_DEFAULT;
						int height = CoreConfig.HEIGHT_DEFAULT;
						int mutil  = CoreConfig.IMGBLOCK_WIDTH;
						int[] bands = {1,2,3};
						gdalDatasetBase = new GdalDatasetBase(imagePath, zeroMaxPercentage, width, height, mutil, bands, -1);
					}
					System.out.println("children update:" + val.getxOringin()+","+
					val.getyOrigin()+","+ val.getReadwidth()+","+val.getReadheight()+","+ val.getWidth()+","+ val.getHeight());
					if(gdalDatasetBase.readData(val.getxOringin(), val.getyOrigin(), val.getReadwidth(), val.getReadheight(), 
							val.getWidth(), val.getHeight())) {
						boolean ouput = gdalDatasetBase.changRGB(val.getWidth(), val.getHeight(), 0, val.getWidth(), false);
						if(ouput) {
							if(!bReaded) {
								Get get = new Get(gtpath.getBytes());
								Result resultTemp = metaTable.get(get);
								if (!resultTemp.isEmpty()) {
									System.out.println("has exit ");
									byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
									get = new Get(md5Byte);
									resultTemp = resourceTable.get(get);
									ImageIcon imageIcon = new ImageIcon(resultTemp.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
									gdalDatasetBase.setBufferedImage(imageIcon, pictureFormatInt);
								}else {
									gdalDatasetBase.resetBufferedImage();
								}
								bReaded = true;
							}
							int x = val.getColCoor() * val.getWidth();
							int y = val.getRowCoor() * val.getHeight();
							gdalDatasetBase.setBufferedImage(x, y, val.getWidth(), val.getHeight(), pictureFormatInt);
						}
					}
				}
			}// end for
			if(bReaded) {
				strBuilderMD5.delete(0, strBuilderMD5.length());
				if(wartermarkFlag) {
					gdalDatasetBase.setWarterMask("ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 13, Color.white, -1, -1, 0.3f, pictureFormatInt);
				}	
				System.out.println("test update:"+gtpath);
				strBuilderMD5.append(MD5Calculate.fileByteMD5(gdalDatasetBase.getBufferImagedata(pictureFormatInt)));
				Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
				get.addFamily(GtDataConfig.RESOURCE.FAMILY.byteVal);
				if(!resourceTable.exists(get)) {
					Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
							GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
							GtDataConfig.RESOURCE.DATA.byteVal, gdalDatasetBase.getBufferImagedata(pictureFormatInt));
					resourceTable.put(put);
				}
				
				Put put = new Put(gtpath.getBytes());
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.DFS.byteVal,
						Bytes.toBytes("0"));
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.SIZE.byteVal,
						Bytes.toBytes(String.valueOf(gdalDatasetBase.getBufferImageSize())));
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.URL.byteVal,
						Bytes.toBytes(strBuilderMD5.toString()));
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.TIME.byteVal,
						Bytes.toBytes("" + System.currentTimeMillis()));
				context.write(NullWritable.get(), put);
			}else if(bGeometry) {
				File dir = new File(geometryLocalDir);
				dir.mkdirs();
				//存放合并后的矢量文件
//				String geometryLocalPath = geometryLocalDir + "tmp.geojson";
				//存放影像更新范围
				String geometryImageLocalPath = geometryLocalDir + "image.geojson"; 
				File fileImageGeojson = new File(geometryImageLocalPath);
				OutputStream outImageGeojson = new FileOutputStream(fileImageGeojson);
				outImageGeojson.write(geometry.getJSON().getBytes());
				outImageGeojson.close();
				
				InetAddress addr = InetAddress.getLocalHost();
				String ip = addr.getHostAddress().toString();
				System.out.println("local ip = " + ip);
				RemoteShell exe = new RemoteShell(ip, CoreConfig.OLEARTH_BOLCKINFO_UPDATE_PORT,
						CoreConfig.OLEARTH_BOLCKINFO_UPDATE_USER, CoreConfig.OLEARTH_BOLCKINFO_UPDATE_PWD);
//				if (blocalShpUpdateSync) {//实时更新本地每日更新覆盖情况
//					File file = new File(geometryLocalPath);
//					OutputStream out = new FileOutputStream(file);
//					gtpath = geometryOutputPath + "//" + timeStr + ".geojson";
//					Get get = new Get(gtpath.getBytes());
//					Result result = metaTable.get(get);
//					if(!result.isEmpty()) {				
//						String geometryLocalPathTemp = geometryLocalDir + "tmp1.geojson";
//						Export exportFile = new Export();
//						exportFile.ExportToLocal(metaTable, resourceTable, gtpath, geometryLocalPathTemp);
//						geometry.Union(geometryLocalPathTemp);	
//						File filetemp = new File(geometryLocalPathTemp);
//						filetemp.delete();
//					}
//					out.write(geometry.getJSON().getBytes());
//					out.close();
//					Import importMap = new Import();
//					importMap.ImportLocalFileToMapTable(geometryLocalPath, gtpath);
//					file.delete();
//				}else {
//					String uuidLocalUpdateShpFile = CoreConfig.OLEARTH_BLOCKINFO_SHPUPDATE_LOCAL_PATH + UUID.randomUUID().toString()  + ".geojson";
//					String commandShp = "scp " + geometryImageLocalPath + " root@" + CoreConfig.OLEARTH_BOLCKINFO_UPDATE_HOST
//							+ ":" + uuidLocalUpdateShpFile;
//					try {
//						if (exe.exec(commandShp) == 0) {
//							String message = timeLong + "," + strResolutionCM + "," + uuidLocalUpdateShpFile;
//							channel.basicPublish("", CoreConfig.OLEARTH_BLOCKINFO_LOCAL_SHP_UPDATE_QUEUE,
//									MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
//						}else {
//							System.out.println("exec error: " + commandShp);
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
				
				if (brabbitmq) {
					String uuidFile = "/home/yarn/" + UUID.randomUUID().toString()  + ".geojson";
					String command = "scp " + geometryImageLocalPath + " root@" + CoreConfig.OLEARTH_BOLCKINFO_UPDATE_HOST
							+ ":" + uuidFile;
					System.out.println(command);
					
					try {
						String message;
						for (int i = 0; i < 5; i ++) {
							int exitStatus = exe.exec(command);
							if (exitStatus == 0) {
								message = CoreConfig.OLEARTH_BOLCKINFO_END + "," + timeLong + "," + strResolutionCM
										+ "," + uuidFile;
								channel.basicPublish("", CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_QUEUE,
										MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
								break;
							}else {
								LOG.info("send file error: " + command);
							}
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}else {
					Map<String, String> param = new HashMap<String, String>();
					param.put("date", ""+timeLong);
					if (strResolutionCM != null && strResolutionCM.length() > 0) {
						param.put("res", strResolutionCM);
					}
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					Date date = new Date(Long.valueOf(timeLong));
					int status = HttpClientBase.uploadFile(fileImageGeojson, CoreConfig.URL_REALTIMECHINA_UPDATE_SHPFILE, param);
					LOG.info(CoreConfig.URL_REALTIMECHINA_UPDATE_SHPFILE + " uploadFile " + format.format(date) + " status= " + status);
				}
				
				
				fileImageGeojson.delete();
				dir.delete();
			}
			
		}
		
		protected void cleanup(Context context)throws IOException, InterruptedException {
			if(resourceTable != null) {
				resourceTable.flushCommits();
				resourceTable.close();
			}
			if(gdalDatasetBase != null) {
				gdalDatasetBase.close();
			}
			if(geometry != null) {
				
			}
		}
	}	
	
	public int cacluteDstResolution(double srcResolution, double zeroLayersResolution) {
		int maxLayers = 0;
		double resolutionTmp = zeroLayersResolution;
		while(true) {
			if( srcResolution - resolutionTmp > 0.0000000000000) {
				maxLayers --;
				break;
			}else if(srcResolution == resolutionTmp){
				break;
			}
			resolutionTmp = resolutionTmp / 2.0;
			maxLayers ++;
		}
		return maxLayers;
	}
	
	public String getResolutionCMByName(String filename) {
		String strResolutionCM = null;
		if (filename.startsWith("GF1_WFV")) {
			strResolutionCM = "1600";
		}else if (filename.startsWith("GF1_PMS")) {
			strResolutionCM = "200";
		}else if (filename.startsWith("GF2")) {
			strResolutionCM = "100";
		}else if (filename.startsWith("pl_")) {
			strResolutionCM = "400";
		}
		return strResolutionCM;
	}
	
	/**
	 * 
	 * @param resolution 分辨率单位为米cm
	 * @return
	 */
	public String getResolutionCM(double resolutionDouble) {
		String strResolutionCM;
		int resolution = (int) resolutionDouble * 100;
		if (resolution > 10000) {
			strResolutionCM = "25000";
		}else if (resolution > 1000) {
			strResolutionCM = "1600";
		}else if (resolution > 500) {
			strResolutionCM = "800";
		}else if (resolution > 300) {
			strResolutionCM = "400";
		}else if (resolution > 100) {
			strResolutionCM = "200";
		}else if (resolution > 60) {
			strResolutionCM = "100";
		}else {
			strResolutionCM = "50";
		}
		return strResolutionCM;
	}
	
	public int run(String[] args)throws Exception {
		System.out.println(System.getProperty("java.library.path"));
		if (args.length < 4 || (args.length % 2 != 0) ) {
			usage();
			return 1;
		}
		Configuration conf = HBaseConfiguration.create();
		int layers = Integer.parseInt(args[3]);
		int minlayers = 0;
		int updateMinLayer = -1;
		int nodataInt = -1;
		//cacluete the maxresolution
		double dstresolution = 0;
		double maxLayerResolution = 0;
		double minLayerResolution = 0;
		double zeroPercentage = 1.00;
		String timeStr = null;
		boolean maxLayerResolutionFlag = false;
		boolean watermarkFlag = true;
		boolean minLayerResolutionFlag = false;
		boolean bTileShpUpdate = false;
		boolean zeroPercentageBool = false;
		boolean saveStorageBoolean = false;
		boolean bResolutionDir = false;
		String strResolutionCM = "";
		boolean brabbitmq = false;
		boolean blocalShpUpdateSync = true;
		
		
//		System.out.println(args[4]);
		if(args.length > 4) {
			for(int i = 4; i < args.length; i ++) {
				if(args[i].equals("-maxLayer_resolution")) {
					maxLayerResolutionFlag = true;
					i++;
					maxLayerResolution = Double.parseDouble(args[i]);
				}else if (args[i].equals("minLayer_resolution")) {
					minLayerResolutionFlag = true;
					i ++;
					minLayerResolution = Double.parseDouble(args[i]);
				}else if (args[i].equals("-watermark")) {
					i ++;
					if(args[i].equals("false")){
						watermarkFlag = false;
					}
				}else if (args[i].equals("-time")) {
					i ++;
					timeStr = args[i];
					if(timeStr.length() != 13) {
						usage();
						return 1;
					}
				}else if (args[i].equals("-minLayers")) {
					i ++;
					minlayers = Integer.parseInt(args[i]);
				}else if (args[i].equals("-zero_percentage")) {
					i ++;
					zeroPercentage = Double.parseDouble(args[i]);
//					zeroPercentage = 0.01;
					zeroPercentageBool = true;
				}else if (args[i].equals("-updateMinLayer")) {
					i ++;
					updateMinLayer = Integer.parseInt(args[i]);
				}else if (args[i].equals("-save_storage")) {
					i++;
					saveStorageBoolean = BooleanUtils.toBoolean(args[i]);
				}else if (args[i].equals("-nodata")) {
					i++;
					nodataInt = Integer.parseInt(args[i]);
				}else if ("-bResolutionDir".equalsIgnoreCase(args[i])) {
					i ++;
					bResolutionDir = BooleanUtils.toBoolean(args[i]);
				}else if ("-bTileShpUpdate".equalsIgnoreCase(args[i])) {
					i ++;
					bTileShpUpdate = BooleanUtils.toBoolean(args[i]);
				}else if ("-bRabbitMQ".equalsIgnoreCase(args[i])) {
					i ++;
					brabbitmq = BooleanUtils.toBoolean(args[i]);
				}else if ("-blocalShpUpdateSync".equalsIgnoreCase(args[i])) {
					i ++;
					blocalShpUpdateSync = BooleanUtils.toBoolean(args[i]);
				}
			}
		}
		if(saveStorageBoolean && !zeroPercentageBool) {
			zeroPercentage = 0.0001;
		}
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		Dataset dataset = gdal.Open(args[1], gdalconstConstants.GA_ReadOnly);
		double[] adfGeoTransform = dataset.GetGeoTransform();
		Path path = new Path(args[1]); 
//		Path path = new Path("hdfs://node03.rsclouds.cn:8020/nanlin/pl_langfang_4_5.log");
		Path pathtemp = new Path(args[1]);
		String filename = pathtemp.getName();
		strResolutionCM = getResolutionCMByName(filename);
		
		if (dataset.GetProjectionRef().startsWith(CoreConfig.WGS84P_ROJECT)) {
			if(maxLayerResolutionFlag) {
				dstresolution = maxLayerResolution;
			}else if (minLayerResolutionFlag) {
				dstresolution = minLayerResolution;
				for(int i = 0; i < layers; i ++) {
					dstresolution = dstresolution / 2.0;
				}
			}else if(layers < 0){
				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.LAYERS_RESOLUTION[0]);
				dstresolution = 0.703125/Math.pow(2, layers);
			}else {
				dstresolution = 0.703125/Math.pow(2, layers);
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_WGS84);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_WGS84);
			if (bResolutionDir) {
				if (strResolutionCM == null)
					strResolutionCM = getResolutionCM(adfGeoTransform[1] * 2* Math.PI * 6371 * 1000  / 360);//
			}
			System.out.println("is wgs84");
			dataset.delete();
			return 0;
		}else if(dataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT1) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MERCATOR_PROJECT) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MECATOR_PROJECT_1)){
			if(maxLayerResolutionFlag) {
				dstresolution = maxLayerResolution;
			}else if (minLayerResolutionFlag) {
				dstresolution = minLayerResolution;
				for(int i = 0; i < layers; i ++) {
					dstresolution = dstresolution / 2.0;
				}
			}else if (layers < 0){
				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.MERCATOR_LAYERS_RESOLUTION[0]);
				dstresolution = 156543.033928/Math.pow(2, layers);
			}else {
				dstresolution = 156543.033928/Math.pow(2, layers);
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_MERCATOR);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_MERCATOR);
			if (bResolutionDir) {
				if (strResolutionCM == null)
					strResolutionCM = getResolutionCM(adfGeoTransform[1]);
			}
		}else {
			System.out.println("投影不支持" + dataset.GetProjection());
			dataset.delete();	
			return 1;
		}
		dataset.delete();	
		System.out.println("======debug analysis parameters");
		conf.set(CoreConfig.JOBID, args[0]);
		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
		conf.set(KEY_RESOLUTION_CM_STRING, strResolutionCM);
		conf.setInt(CoreConfig.KEY_CURRENT_LAYER, layers);
		conf.setInt(CoreConfig.KEY_MIN_LAYER, minlayers);
		conf.setInt(CoreConfig.KEY_UPDATE_MINLAYER_INT, updateMinLayer);
		conf.setDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, zeroPercentage);
		conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, dstresolution);
		conf.setBoolean(SAVE_STORAGE_BOOLEAN, saveStorageBoolean);
		conf.setBoolean(KEY_TILESHPUPDATE_BOOLEAN, bTileShpUpdate);
		conf.setBoolean(KEY_RABBITMQ_BOOLEAN, brabbitmq);
		conf.setBoolean(KEY_LOCALSHPUPDATESYNC_BOOLEAN, blocalShpUpdateSync);
		conf.setInt("NO_DATA", nodataInt);
		if(timeStr != null) {
			conf.set(CoreConfig.KEY_TIME_STRING, timeStr);
		}
		conf.set(CoreConfig.CUTTING_OUTPUTPATH, args[2]);
		conf.setBoolean(CoreConfig.KEY_WARTERMARK, watermarkFlag);
		conf.set(TableOutputFormat.OUTPUT_TABLE, CoreConfig.MAP_META_TABLE);
		
		Job job = Job.getInstance(conf);
		FileInputFormat.addInputPath(job, path);
		job.setJarByClass(RealChinaSegement.class);
		job.setInputFormatClass(ImageMutilLayersInputFormat.class);
		job.setMapperClass(ImageSegMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FileInfo.class);
		job.setReducerClass(MetaTableInsertReducer.class);
		job.setNumReduceTasks(4);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setJobName("cutting " + path.getName());	
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public static void usage()
	{
		LOG.info("usage: <rowkey> <input_path(hdfs)> <gt-data ouput path(../../_alllayers)> <maxLayers>");
		LOG.info("       [-maxLayer_resolution maxResolution] [-minLayer_resolution minresolution] [-watermark true/false]");
		LOG.info("       [-time time(ms)] [-minLayers minlayers] [-updateMinLayer int] [-zero_percentage [0.0-1.0]] [-picture_format]");
		LOG.info("       [-save_storage true/false] [-nodata int] [-bResolutionDir true/false] [-bTileShpUpdate true/false] [-bRabbitMQ true/false]");
		LOG.info("       [-blocalShpUpdateSync true/false]");
		LOG.info("       maxLayers is interger number, if it's a negative, we will caclute a right layers by the image's resolution");
		LOG.info("       -picture_format 瓦片输出格式，可选为png或jpeg");
		LOG.info("       -updateMinLayer 需要合并更新的最小图层");
		LOG.info("       -bResolutionDir 瓦片路径是否增加分辨率目录（如200cm、80cm、1600cm等）");
		LOG.info("       -zero_percentage 瓦片含有0值像素的百分比，超过该阈值的瓦片将不会输出, 如果和-save_storage（=true）一起使用时，则超过阈值的存储格式为png，否则为jpeg");
		LOG.info("       -save_storage 为true且和-zero_percentage一起使用，表示超过阈值的存储格式为png，否则为jpeg， -zero_percentage默认为0。01%，为false时，可以忽略，默认为false");
		LOG.info("       -bRabbitMQ 为true 将分块信息入库到rabbitmq队列，false：直接入库到矢量数据库");
		LOG.info("       -blocalShpUpdateSync 为true 将每景影像矢量更新范围实时更新到本地备份数据库中（hbase），false：异步处理影像矢量更新");
	}
	
	public static void main(String[] args)throws Exception {
		if (args.length < 4) {
			usage();
			args = new String[16];
			args[0] = "123456";
//			args[1] = "D:\\nanlin\\image\\test.tiff";
			args[1] = "D:\\nanlin\\extract.tiff";
			args[2] = "/map/auto_proc/img/mercator/warter/realtimeChina_test_pl/Layers/_alllayers";
			args[3] = "14";
			args[4] = "-minLayers";
			args[5] = "14";
			args[6] = "-watermark";
			args[7] = "true";
			args[8] = "-zero_percentage";
			args[9] = "0.01";
			args[10] = "-save_storage";
			args[11] = "true";
			args[12] = "-bResolutionDir";
			args[13] = "true";
//			args[14] = "-bTileShpUpdate";
//			args[15] = "true";
			args[14] = "-time";
			args[15] = "1451404800000";
//			args[18] = "-bRabbitMQ";
//			args[19] = "true";
			int status = ToolRunner.run(new RealChinaSegement(), args);
			System.exit(status);
		}else {
			int status = ToolRunner.run(new RealChinaSegement(), args);
			System.exit(status);
		}
	}
//	Path path = new Path("hdfs://192.168.2.3:8020/nanlin/pl_langfang_4_5.log");
//	if (args == null || args.length == 0) {
//		args = new String[18];
//		args[0] = "123456";
//		args[1] = "D:\\nanlin\\image\\5dehaze\\GF1_WFV3_E113.6_N23.9_20160725_L1A0001721210\\GF1_WFV3_E113.6_N23.9_20160725_L1A0001721210.tiff";
//		args[2] = "/map/auto_proc/img/mercator/warter/realtimeChina_test_pl/Layers/_alllayers";
//		args[3] = "15";
//		args[4] = "-minLayers";
//		args[5] = "14";
//		args[6] = "-watermark";
//		args[7] = "true";
//		args[8] = "-zero_percentage";
//		args[9] = "0.001";
//		args[10] = "-save_storage";
//		args[11] = "true";
//		args[12] = "-bResolutionDir";
//		args[13] = "true";
//		args[14] = "-bTileShpUpdate";
//		args[15] = "true";
//		args[16] = "-time";
//		args[17] = "1451404800000";
//		args[18] = "-bRabbitMQ";
//		args[19] = "true";
//	}
}
