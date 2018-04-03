package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
import com.rsclouds.gtparallel.core.gtdata.operation.Export;
import com.rsclouds.gtparallel.core.gtdata.operation.Import;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImageModisInputFormat;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;



public class RealChinaModisSegement extends Configured implements Tool{
	private static final Log LOG = LogFactory.getLog(RealChinaModisSegement.class);
	public static final long MAX_TIME = 9999999999999L;
	public  static final String SAVE_STORAGE_BOOLEAN = "save_storage";
	public static final String KEY_TILESHPUPDATE_BOOLEAN = "tile_shp_update";
	public static final String KEY_RESOLUTION_CM_STRING = "resolution_cm";
	public static final String KEY_RABBITMQ_BOOLEAN = "brabbitmq";
	public static int resolution[] = {250, 16, 8, 4, 2};
		
	
	static class ImageSegMapper extends Mapper<ImageMutilLayersInfo, NullWritable, Text, FileInfo> {
		//configuration init args
		private HTable resourceTable;
		private HTable metadataTable;
		private boolean wartermarkFlag = true;
		private int nodataInt;	
		private long timeLong;
		private Text path = new Text();
		private FileInfo fileInfo = new FileInfo();
		private StringBuilder strBuilderMD5 = new StringBuilder();
		private String outputpath = null;	//瓦片输出路径
		private int pictureFormatInt;
		private String timeStr = null;
		private String rowColTimeUpdateUrl = CoreConfig.URL_REALTIMECHINA_UPDATE;
				
		//瓦片的默认长宽
		private int widthRang;
		private int heightRang;
		private int width;
		private int height;

		//瓦片行列号
		private StringBuilder colStrBuilder = new StringBuilder();
		private StringBuilder rowStrBuilder = new StringBuilder();		
		private boolean bOutput = false;
		private double[] adfGeoTransform = null;
		private GdalDatasetBase gdalDatasetBase = null;
		private boolean bNeedSendUpdate = false;
		
		public static Channel channel = null;
		private boolean brabbitmq = false;
		
		
		/**
		 * 初始化参数	
		 */
		public void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
			timeStr = conf.get(CoreConfig.KEY_TIME_STRING, null);
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
				fileInfo.setTimeText(timeStr);//timeStr
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
		
		public boolean output(Context context, String outputpath, long row, long col)
				throws IOException, InterruptedException{			
			path.set(outputpath + rowStrBuilder.toString() + "/" + colStrBuilder.toString() + "/25000cm" );//+ "/25000cm"
			strBuilderMD5.append(MD5Calculate.fileByteMD5(gdalDatasetBase.getBufferImagedata(pictureFormatInt)));			
			fileInfo.setMD5(strBuilderMD5.toString());
			fileInfo.setLength(gdalDatasetBase.getBufferImageSize());
			fileInfo.setFilename(colStrBuilder.toString());
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
			strBuilderMD5.delete(0, strBuilderMD5.length());
			return true;		
		}
		
	
		public void map(ImageMutilLayersInfo key, NullWritable value, Context context) throws
			IOException, InterruptedException {
//			System.out.println("map key: " + key.getGtdataOutputPath().toString());
			if(gdalDatasetBase == null) {			
				int mutil = CoreConfig.IMGBLOCK_WIDTH;
				int[] bands = key.getBands();	
				gdalDatasetBase = new GdalDatasetBase(key.getFilepath().toString(), 0.0001, widthRang, heightRang, mutil, bands, nodataInt);
				width = gdalDatasetBase.getWidth();
				height = gdalDatasetBase.getHeight();	
				
				adfGeoTransform = gdalDatasetBase.getAdfGeoTransform();
				adfGeoTransform[1] = key.getDstResolution();
				adfGeoTransform[5] = -key.getDstResolution();
								
				outputpath = key.getGtdataOutputPath().toString();
				System.out.println("=====nanlin debug===== gdalDatasetBase init " + outputpath);
				System.out.println("first inite outputpath " + outputpath);
				if(!outputpath.endsWith("/")) {
					outputpath += "/";
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
					for (xreaded = 0; xreaded < xLength; ) {
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

						bOutput = gdalDatasetBase.changRGB(xread, yLength, xreaded, xLength, true);
						if(bOutput) {
							pictureFormatInt = GdalDatasetBase.JPEG_FORMAT_INT;
						}else {
							pictureFormatInt = GdalDatasetBase.PNG_FORMAT_INT;
						}
						setCol(colNum);
						setRow(rowNum);
						
						
						String filePath = outputpath + rowStrBuilder.toString() + "/" + colStrBuilder.toString() + "/25000cm//"
								+ timeStr + ".png";
						Get getTemp = new Get(Bytes.toBytes(TransCoding.decode(filePath, "UTF-8")));
						Result resultTemp = metadataTable.get(getTemp);
						if (!resultTemp.isEmpty()) {
							byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
							getTemp = new Get(md5Byte);
							Result resultRes = resourceTable.get(getTemp);
							ImageIcon imageIcon = new ImageIcon(resultRes.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
							BufferedImage bufferImag = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB_PRE);
							Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
							gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
							int rgbArray[] = new int[256*256];
							bufferImag.getRGB(0, 0, 256, 256, rgbArray, 0, 256);
							pictureFormatInt = gdalDatasetBase.merge(rgbArray, xWriteOrigin, yWriteOrigin, xread, yLength, pictureFormatInt);
						}else {
							gdalDatasetBase.setBufferedImage(xWriteOrigin, yWriteOrigin, xread, yLength, pictureFormatInt);
						}
						if(wartermarkFlag) {
							gdalDatasetBase.setWarterMask("ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 13, Color.white, -1, -1, 0.4f, pictureFormatInt);
						}
//						gdalDatasetBase.write2LocalFile("D://nanlin//modis.png", pictureFormatInt);
						fileInfo.setBupdate(false);
						bOutput = output(context, outputpath, rowNum, colNum);
						if (bOutput == true) {
							bNeedSendUpdate = true;
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
			if(bNeedSendUpdate) {
				String param = "&longtime=" + timeLong + "&res=25000";
				if (brabbitmq) {
					if (channel != null) {
						try {
							channel.basicPublish("", CoreConfig.OLEARTH_BOLCKINFO_RABBITMQ_QUEUE,
								MessageProperties.PERSISTENT_TEXT_PLAIN, param.getBytes());
						}catch(IOException e) {
							LOG.info("rowColTimeUpdateUrl import rabbitmq failed: " + rowColTimeUpdateUrl + "?" + param);
						}
					}else {
						LOG.info("rowColTimeUpdateUrl import rabbitmq failed: channel is null");
					}
				}else {
					boolean bSendResult = false;
					for(int i = 0; i < 3; i ++) {
						String response = HttpClientBase.sendGet(rowColTimeUpdateUrl, param);
						if(response == null)
							continue;
						JSONObject json = JSONObject.fromObject(response);
						String codeVale = json.optString("code", "-5");
						if (codeVale.endsWith("1")) {
							bSendResult = true;
							break;
						}else {
							System.out.println(codeVale);
						}
					}
					if (!bSendResult) {
						LOG.info("rowColTimeUpdateUrl send failed: " + rowColTimeUpdateUrl + "?" + param);
					}
				}
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
		
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			format = conf.get(CoreConfig.KEY_OUTPUT_FORMAT, ".png");
			imagePath = conf.get(CoreConfig.CUTTING_INPUTFILE, null);
			zeroMaxPercentage = conf.getDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, 1.1);
			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
			strResolutionCM = conf.get(KEY_RESOLUTION_CM_STRING, "");
			timeStr = conf.get(CoreConfig.KEY_TIME_STRING, null);
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
				String geometryLocalPath = geometryLocalDir + "tmp.geojson";
				String geometryImageLocalPath = geometryLocalDir + "image.geojson"; 
		
				
				File file = new File(geometryLocalPath);
				OutputStream out = new FileOutputStream(file);
				File fileImageGeojson = new File(geometryImageLocalPath);
				OutputStream outImageGeojson = new FileOutputStream(fileImageGeojson);
				outImageGeojson.write(geometry.getJSON().getBytes());
				outImageGeojson.close();
				
				System.out.println("write shp file=" + geometryLocalPath);
				gtpath = geometryOutputPath + "//" + timeStr + ".geojson";
				Get get = new Get(gtpath.getBytes());
				Result result = metaTable.get(get);
				if(!result.isEmpty()) {				
					String geometryLocalPathTemp = geometryLocalDir + "tmp1.geojson";
					Export exportFile = new Export();
					exportFile.ExportToLocal(metaTable, resourceTable, gtpath, geometryLocalPathTemp);
					geometry.Union(geometryLocalPathTemp);	
					File filetemp = new File(geometryLocalPathTemp);
					filetemp.delete();
				}
				out.write(geometry.getJSON().getBytes());
				out.close();
				Import importMap = new Import();
				importMap.ImportLocalFileToMapTable(geometryLocalPath, gtpath);
				
				Map<String, String> param = new HashMap<String, String>();
				param.put("date", ""+timeLong);
				if (strResolutionCM != null && strResolutionCM.length() > 0) {
					param.put("res", strResolutionCM);
				}
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				Date date = new Date(Long.valueOf(timeLong));
				int status = HttpClientBase.uploadFile(fileImageGeojson, CoreConfig.URL_REALTIMECHINA_UPDATE_SHPFILE, param);
				LOG.info("uploadFile " + format.format(date) + " status= " + status);
				
				file.delete();
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
		}
		return strResolutionCM;
	}
	
	public int run(String[] args)throws Exception {
		if (args.length < 4 || (args.length % 2 != 0) ) {
			usage();
			return 1;
		}
		Configuration conf = HBaseConfiguration.create();
		int layers = Integer.parseInt(args[3]);

		int nodataInt = -1;
		String timeStr = null;
		boolean watermarkFlag = true;
		boolean brabbitmq = false;
		
		if(args.length > 4) {
			for(int i = 4; i < args.length; i ++) {
				if (args[i].equals("-watermark")) {
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
				}else if (args[i].equals("-nodata")) {
					i++;
					nodataInt = Integer.parseInt(args[i]);
				}else if (args[i].equals("-bRabbitMQ")) {
					i ++;
					brabbitmq = BooleanUtils.toBoolean(args[i]);
				}
			}
		}

		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		Dataset dataset = gdal.Open(args[1], gdalconstConstants.GA_ReadOnly);
		double[] adfGeoTransform = dataset.GetGeoTransform();
//		Path path = new Path("hdfs://192.168.2.3:8020/nanlin/tiff/t1_byte.tif"); //args[1] "hdfs://192.168.2.3:8020/nanlin/tiff/t1_byte.tif"
		Path path = new Path(args[1]); 
		
		double dstresolution;
		if (dataset.GetProjectionRef().startsWith(CoreConfig.WGS84P_ROJECT)) {
			if (layers >= CoreConfig.LAYERS_RESOLUTION.length) {
				int length = CoreConfig.LAYERS_RESOLUTION.length - 1;
				int count = layers - length;
				dstresolution = CoreConfig.LAYERS_RESOLUTION[length];
				for(int i = 0; i < count; i ++) {
					dstresolution = dstresolution / 2;
				}
			}else if(layers < 0){
				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.LAYERS_RESOLUTION[0]);
				if(layers > CoreConfig.LAYERS_RESOLUTION.length) {
					int time = 1;
					for(int i = CoreConfig.LAYERS_RESOLUTION.length; i < layers; i ++) {
						time *= 2;;
					}
					dstresolution = CoreConfig.LAYERS_RESOLUTION[layers] / time;
				}else {
					dstresolution = CoreConfig.LAYERS_RESOLUTION[layers];
				}
			}else {
				dstresolution = CoreConfig.LAYERS_RESOLUTION[layers];
				
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_WGS84);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_WGS84);
			
		}else if(dataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MERCATOR_PROJECT) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MECATOR_PROJECT_1)){
			if (layers >= CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
				int length = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length - 1;
				int count = layers - length;
				dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[length];
				for(int i = 0; i < count; i ++) {
					dstresolution = dstresolution / 2;
				}
			}else if (layers < 0){
				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.MERCATOR_LAYERS_RESOLUTION[0]);
				if(layers > CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
					int time = 1;
					for(int i = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length; i < layers; i ++) {
						time *= 2;
					}
					dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers] / time;
				}else {
					dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers];
				}
			}else {
				dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers];
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_MERCATOR);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_MERCATOR);
		}else {
			System.out.println("投影不支持" + dataset.GetProjection());
			dataset.delete();
			
			return 1;
		}
		dataset.delete();	
		System.out.println("======debug analysis parameters");
		conf.set(CoreConfig.JOBID, args[0]);
		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
		conf.setInt(CoreConfig.KEY_CURRENT_LAYER, layers);
		conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, dstresolution);
		conf.setBoolean(KEY_RABBITMQ_BOOLEAN, brabbitmq);
		conf.setInt("NO_DATA", nodataInt);
		if(timeStr != null) {
			conf.set(CoreConfig.KEY_TIME_STRING, timeStr);
		}
		conf.set(CoreConfig.CUTTING_OUTPUTPATH, args[2]);
		conf.setBoolean(CoreConfig.KEY_WARTERMARK, watermarkFlag);
		conf.set(TableOutputFormat.OUTPUT_TABLE, CoreConfig.MAP_META_TABLE);
		
		Job job = Job.getInstance(conf);
		FileInputFormat.addInputPath(job, path);
		job.setJarByClass(RealChinaModisSegement.class);
		job.setInputFormatClass(ImageModisInputFormat.class);
		job.setMapperClass(ImageSegMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FileInfo.class);
		job.setReducerClass(MetaTableInsertReducer.class);
		job.setNumReduceTasks(3);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setJobName("cutting " + path.getName());	
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public static void usage()
	{
		LOG.info("usage: <rowkey> <input_path(hdfs)> <gt-data ouput path(../../_alllayers)> <maxLayers>");
		LOG.info("       [-watermark true/false] [-time time(ms)] [-bRabbitMQ true/false]");
		LOG.info("       maxLayers is interger number, if it's a negative, we will caclute a right layers by the image's resolution");
		LOG.info("       -bRabbitMQ 为true 将分块信息入库到rabbitmq队列，false：直接入库到矢量数据库");
	}
	
	public static void main(String[] args)throws Exception {
		if (args == null || args.length == 0) {
			args = new String[20];
			args[0] = "123456";
			args[1] = "D://nanlin//image//modis//2016-02-16.tiff";
			args[2] = "/map/auto_proc/img/mercator/warter/modis_test20160330/Layers/_alllayers";
			args[3] = "4";
			args[4] = "-minLayers";
			args[5] = "4";
			args[6] = "-watermark";
			args[7] = "true";
			args[8] = "-zero_percentage";
			args[9] = "0.001";
			args[10] = "-save_storage";
			args[11] = "true";
			args[12] = "-bResolutionDir";
			args[13] = "true";
			args[14] = "-bTileShpUpdate";
			args[15] = "true";
			args[16] = "-time";
			args[17] = "1455552000000";
			args[18] = "-nodata";
			args[19] = "0";

		}
		if (args.length < 4) {
			usage();
		}else {
			int status = ToolRunner.run(new RealChinaModisSegement(), args);
			System.exit(status);
		}
	}
}

