package com.rsclouds.gtparallel.core.gtdata.cutting;

//import java.awt.Color;
//import java.awt.Font;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//
//import javax.imageio.ImageIO;
//import javax.swing.ImageIcon;
//
//import net.sf.json.JSONObject;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.conf.Configured;
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.hbase.HBaseConfiguration;
//import org.apache.hadoop.hbase.client.Get;
//import org.apache.hadoop.hbase.client.HTable;
//import org.apache.hadoop.hbase.client.Put;
//import org.apache.hadoop.hbase.client.Result;
//import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
//import org.apache.hadoop.hbase.mapreduce.TableReducer;
//import org.apache.hadoop.hbase.util.Bytes;
//import org.apache.hadoop.io.NullWritable;
//import org.apache.hadoop.io.Text;
//import org.apache.hadoop.mapreduce.Job;
//import org.apache.hadoop.mapreduce.Mapper;
//import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
//import org.apache.hadoop.util.Tool;
//import org.apache.hadoop.util.ToolRunner;
//import org.gdal.gdal.Dataset;
//import org.gdal.gdal.gdal;
//import org.gdal.gdalconst.gdalconst;
//import org.gdal.gdalconst.gdalconstConstants;
//import org.gdal.ogr.Geometry;
//import org.gdal.osr.SpatialReference;
//
//import com.rsclouds.gtparallel.core.common.CoreConfig;
//import com.rsclouds.gtparallel.core.gtdata.common.GdalDatasetBase;
//import com.rsclouds.gtparallel.core.gtdata.common.HttpClientBase;
//import com.rsclouds.gtparallel.core.gtdata.common.ImageUtils;
//import com.rsclouds.gtparallel.core.gtdata.common.GeowebcacheTool;
//import com.rsclouds.gtparallel.core.gtdata.operation.Export;
//import com.rsclouds.gtparallel.core.gtdata.operation.Import;
//import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
//import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;
//import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImageMutilLayersInputFormat;
//import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
//import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
//import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
//import com.rsclouds.gtparallel.gtdata.utills.TransCoding;



public class RealChinaSegementBack {//extends Configured implements Tool{
//	private static final Log LOG = LogFactory.getLog(ImageMutilLayersSegement.class);
//	public static final long MAX_TIME = 9999999999999L;
//		
//	
//	static class ImageSegMapper extends Mapper<ImageMutilLayersInfo, NullWritable, Text, FileInfo> {
//		private Text path = new Text();
//		private FileInfo fileInfo = new FileInfo();
//		private HTable resourceTable;
//		private HTable metadataTable;
//		private StringBuilder strBuilderMD5 = new StringBuilder();
//		
//		private String outputpath = null;	//瓦片输出路径
//		private String[] updateLayerPaths;
//		private boolean wartermarkFlag = true;
//		//零值像素最大百分比，超过该阈值，该瓦片不输出
//		private double zeroMaxPercentage = 1.0;
//		private String timeStr = null;
//		private long timeLong;
//		private int updateMinLayer;
//		private boolean bupdataTile = false;
//		private int cuttingMinLayer;
//		private String rowColTimeUpdateUrl = CoreConfig.URL_REALTIMECHINA_UPDATE;
//				
//		//瓦片的默认长宽
//		private int widthRang;
//		private int heightRang;
//		private int width;
//		private int height;
//
//		//瓦片行列号
//		private StringBuilder colStrBuilder = new StringBuilder();
//		private StringBuilder rowStrBuilder = new StringBuilder();
//				
//		private Dataset dataset = null;
//		private Geometry oGeometry = null;
//		private byte[] nodataValues; 
//		private boolean nodataFlag; //获取影像数据的nodata值是否成功， false->失败，  true->成功
//		private int bandCount;
//		private int colorInterp; //影像颜色表类型
//		private byte[] buffer;   //存放影像二进制数据
//		private int[] rgb;       //存放影像数据的rgb数据
//		private boolean bOutput = false;
//		private int[] bands;     //需要读取的波段数组
//		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
//		private BufferedImage bufferImag;
//		private int[] intZeroAreay;
//		private double[] adfGeoTransform = null;
//		private double[] x_coor = new double[4];
//		private double[] y_coor = new double[4];
//		private int[] row_coor = new int[4];
//		private int[] col_coor = new int[4];
//		private GdalDatasetBase gdalDatasetBase = null;
//		
//		/**
//		 * 初始化参数	
//		 */
//		public void setup(Context context) throws IOException, InterruptedException {
//			super.setup(context);
//			Configuration conf = context.getConfiguration();
//			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
//			timeStr = conf.get(CoreConfig.KEY_TIME_STRING, null);
//			cuttingMinLayer = conf.getInt(CoreConfig.KEY_MIN_LAYER, 13);
//			updateMinLayer = conf.getInt(CoreConfig.KEY_UPDATE_MINLAYER_INT, -1);
//			zeroMaxPercentage = conf.getDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, 1.1);
//			
//			if(timeStr != null) {
//				timeLong = Long.parseLong(timeStr);
//				long time = MAX_TIME - timeLong;
//				StringBuilder timeStrBuilder = new StringBuilder(time + "");
//				int length = timeStrBuilder.length();
//				if(length < 13) {
//					for(int i = length; i < 13; i ++) {
//						timeStrBuilder.insert(0, "0");
//					}
//				}
//				timeStr = timeStrBuilder.toString();
//				fileInfo.setTimeText(timeStr);
//				fileInfo.setGeometry(false);
//			}
//			resourceTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
//			metadataTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
//			resourceTable.setAutoFlushTo(false);
//			widthRang = CoreConfig.WIDTH_DEFAULT;
//			heightRang = CoreConfig.HEIGHT_DEFAULT;
//			rgb = new int[widthRang*heightRang];
//			intZeroAreay = new int[widthRang*heightRang];
//			bufferImag = new BufferedImage(widthRang, heightRang, BufferedImage.TYPE_INT_ARGB_PRE);
//			int mutil = CoreConfig.IMGBLOCK_WIDTH;
//			buffer = new byte[heightRang*widthRang*mutil*3];
//			
//			
//		}	
//				
//		
//		/**
//		 * @bref 将列号号转换为十六进制，并格式化为字符串“R00000000。png”， 即R后面跟八位数十六进制，不足补零，然后再加“。png”后缀。
//		 * @param colNum
//		 */
//		public void setCol(long colNum) {
//			colStrBuilder.replace(0, colStrBuilder.length(), Long.toHexString(colNum));
//			int count = 8 - colStrBuilder.length();
//			for (int i = 0; i < count; i++) {
//				colStrBuilder.insert(0, "0");
//			}
//			colStrBuilder.insert(0, "C");
//		}
//		
//		
//		/**
//		 * @bref 将行号转换为十六进制，并格式化为字符串“R00000000”， 即R后面跟八位数十六进制，不足补零
//		 * @param rowNum
//		 */
//		public void setRow(long rowNum) {
//			rowStrBuilder.replace(0, rowStrBuilder.length(), Long.toHexString(rowNum));
//			int count = 8 - rowStrBuilder.length();
//			for (int i = 0; i < count; i++) {
//				rowStrBuilder.insert(0, "0");
//			}
//			rowStrBuilder.insert(0, "R");
//		}	
//		
//		public boolean output(Context context, String outputpath, long row, long col) throws IOException, InterruptedException{
//			fileInfo.setTimeText(timeStr);
//			String param = "row=" + row + "&col=" + col + "&longtime=" + timeStr;
//			boolean bupdateFlag = false;
//			int count = 3;
//			for(int i = 0; i < count; i ++) {
//				String response = HttpClientBase.sendGet(rowColTimeUpdateUrl, param);
//				if(response == null)
//					continue;
//				JSONObject json = JSONObject.fromObject(response);
//				String codeVale = json.optString("code", "-1");
//				if (codeVale.endsWith("1")) {
//					bupdateFlag = true;
//					break;
//				}
//			}
//
//			//只有更新矢量数据成功后才将瓦片写入到数据库中
//			if(bupdateFlag) {
//				path.set(outputpath + rowStrBuilder.toString() + "/" + colStrBuilder.toString());
//				byteArrayOut.reset();
//				if(wartermarkFlag) {
//					ImageUtils.pressText(bufferImag, "ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 15, Color.white, -1, -1, 0.4f);
//				}
//				ImageIO.write(bufferImag, "png", byteArrayOut);		
//				ImageIO.write(bufferImag, "png", new File("E://test_test.png"));
//				strBuilderMD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));
//				fileInfo.setMD5(strBuilderMD5.toString());
//				fileInfo.setLength(byteArrayOut.size());
//				fileInfo.setFilename(colStrBuilder.toString());
//				Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
//				Result result = resourceTable.get(get);
//				if (result == null || result.isEmpty()) {
//					Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
//					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//							GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
//					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//							GtDataConfig.RESOURCE.DATA.byteVal, byteArrayOut.toByteArray());
//					resourceTable.put(put);
//				}
//				context.write(path, fileInfo);
//				strBuilderMD5.delete(0, strBuilderMD5.length());
//				return true;
//			}else {
//				System.out.println(rowColTimeUpdateUrl + param + " update failed");
//				return false;
//			}
//		}
//		
//	
//		public void map(ImageMutilLayersInfo key, NullWritable value, Context context) throws
//			IOException, InterruptedException {
//			if(gdalDatasetBase == null) {
//				adfGeoTransform = new double[6];
//				adfGeoTransform[0] = key.getXreadOrigin();
//				adfGeoTransform[1] = key.getDstResolution();
//				adfGeoTransform[2] = 0.0;
//				adfGeoTransform[3] = key.getYreadOrigin();
//				adfGeoTransform[4] = 0.0;
//				adfGeoTransform[5] = -key.getDstResolution();
//				
//				
////				gdal.AllRegister();
////				dataset = gdal.Open(key.getFilepath().toString(), gdalconstConstants.GA_ReadOnly);				
//				width = dataset.GetRasterXSize();
//				height = dataset.GetRasterYSize();
//				int bandCount = dataset.getRasterCount();
//				nodataValues = new byte[bandCount];
//				Double[] nodata = new Double[1];
//				nodataFlag = true;
//				for(int i = 1; i <= bandCount; i ++) {
//					dataset.GetRasterBand(i).GetNoDataValue(nodata);
//					if(nodata[0] == null)
//						nodataFlag = false;
//					else if(nodata[0].doubleValue() > 256) {
//						nodataFlag = false;
//					} else {
//						nodataValues[i-1] = (byte)(nodata[0].doubleValue());
//					}
//				}
//				bands = key.getBands();			
//				outputpath = key.getGtdataOutputPath().toString();
//				if(!outputpath.endsWith("/")) {
//					outputpath += "/";
//				}
//				if(key.getCurentLayer() == cuttingMinLayer) {
//					bupdataTile = true;
//					int count = cuttingMinLayer - updateMinLayer;
//					if(count > 0) {
//						updateLayerPaths = new String[count];
//						String minLayerName = GeowebcacheTool.getLayerName(cuttingMinLayer);
//						int currentLayer;
//						for(int i = 0; i < count; i ++) {
//							currentLayer = cuttingMinLayer-i-1;
//							updateLayerPaths[i] = outputpath.replace(minLayerName, GeowebcacheTool.getLayerName(currentLayer));
//						}
//					}
//				}
//				System.out.println(outputpath);
//			}
//						
//			
//			long xOrigin = key.getXreadOrigin();//重采样后的x坐标
//			long yOrigin = key.getYreadOrigin();//重采样后的y坐标
//			int xLength = (int)key.getXreadLen();//重采样后的宽度
//			int yLength = (int)key.getYreadLen();//重采样后的高度
//			int tileColOffset = key.getTileColOffset();
//			int tileRowOffset = key.getTileRowOffset();
//			double dstResolution = key.getDstResolution();//重采样后的分辨率
//			double oriResoulution = key.getOriResolution();
//			long colNum = key.getColNum();
//			long rowNum = key.getRowNum();
//			
//			if (dataset != null) {
//				int nbands = dataset.getRasterCount();
//				bandCount = bands.length;
//				if (nbands == 1) {
//					bandCount = 1;
//					bands = new int[1];
//					bands[0] = 1;
//				}
//				colorInterp = dataset.GetRasterBand(1).GetColorInterpretation();
//				
//				//实际需要读取影像的真实起始点坐标（以像素为单位）
//				int xReadCoordinateActual = (int)(xOrigin * dstResolution / oriResoulution);
//				int yReadCoordinateActual = (int)(yOrigin * dstResolution / oriResoulution);
//				
//				//实际需要读取的瓦片数据范围
//				int xreadActual = (int)(xLength * (dstResolution) / oriResoulution);
//				int yreadActual = (int)(yLength * (dstResolution) / oriResoulution);
//				if(xReadCoordinateActual + xreadActual > width) {
//					xreadActual = width - xReadCoordinateActual;
//				}
//				if(yReadCoordinateActual + yreadActual > height) {
//					yreadActual = height - yReadCoordinateActual;
//				}
//	
//				//重采样后需要读取的瓦片数据范围
//				int xread;
//				//重采样后已经读取的影像范围
//				int xreaded = 0;
//				//写入数据的起始点
//				int xWriteOrigin;
//				int yWriteOrigin = tileRowOffset;
//				if (gdalTool.readData(xReadCoordinateActual, yReadCoordinateActual, xreadActual, yreadActual, xLength, yLength,
//						buffer, colorInterp, bandCount, dataset, rgb, bands)) {
//					for (xreaded = 0; xreaded < xLength; ) {
//						if(xreaded == 0) {
//							xread = widthRang - tileColOffset;
//							if(xread > xLength) {
//								xread = xLength;
//							}
//							xWriteOrigin = tileColOffset;
//						}else {
//							if (xLength -xreaded < widthRang) {
//								xread = xLength -xreaded;
//							}else{
//								xread = widthRang;
//							}
//							xWriteOrigin = 0;
//						}
//							
//						//初始bufferImage
//						bufferImag.setRGB(0, 0, widthRang, heightRang, intZeroAreay, 0, widthRang);
//						bOutput = gdalTool.changRGB(buffer, rgb, xread, yLength, xreaded, xLength, bandCount, zeroMaxPercentage,
//								nodataFlag, nodataValues, bands);
//
//						if (bOutput) {
//							bufferImag.setRGB(xWriteOrigin, yWriteOrigin, xread, yLength, rgb, 0, xread);
//							setCol(colNum);
//							setRow(rowNum);
//							fileInfo.setBupdate(false);
//							//bOutput = output(context, outputpath, rowNum, colNum);
//							if(bupdataTile && bOutput){
//								if(!gdalTool.ImageRowCol2Projection(adfGeoTransform, row_coor, col_coor, x_coor, y_coor))
//									return;
//								Geometry geoTemp = gdalTool.createPolygonWGS84(x_coor, y_coor);
//								if(geoTemp == null)
//									return;
//								if(oGeometry == null) {
//									oGeometry = geoTemp;
//								}else {
//									oGeometry = oGeometry.Union(geoTemp);
//								}
//								
//								String prefix = outputpath+rowStrBuilder.toString()+"/" + colStrBuilder.toString();
//								
//								if(GeowebcacheTool.isNewestTime(metadataTable, prefix, timeLong)) {
//									int count = cuttingMinLayer - updateMinLayer;
//									long currentRowNum = rowNum;
//									int currentRowCoor = 0;
//									long currentColNum = colNum; 
//									int currentColCoor = 0;
//									int times = 1;
//									int currentwidth = widthRang;
//									int currentheight = heightRang;
//									int xOriginTemp = (int)((xOrigin+xreaded) * dstResolution / oriResoulution);
//									fileInfo.setxOringin(xOriginTemp);
//									fileInfo.setyOrigin((int)yReadCoordinateActual);
//									int xreadActualDefault = (int)(widthRang * (dstResolution) / oriResoulution);
//									int yreadActualDefault = (int)(heightRang * (dstResolution) / oriResoulution);
//									if(xOriginTemp + xreadActualDefault > width)
//										xreadActualDefault = width - xOriginTemp;
//									if(yReadCoordinateActual + yreadActualDefault > height) {
//										yreadActualDefault = height - yReadCoordinateActual;
//									}
//									fileInfo.setReadwidth(xreadActualDefault);
//									fileInfo.setReadheight(yreadActualDefault);
//									fileInfo.setBupdate(true);
//									
//									
//									for(int i = 0; i < count; i ++) {
//										currentwidth = currentwidth / 2;
//										currentheight = currentheight / 2;
//										times = times * 2;
//										currentRowCoor = (int)rowNum % times;
//										currentRowNum = currentRowNum / 2;
//										currentColCoor = (int)colNum % times;
//										currentColNum = currentColNum / 2;
//										setCol(currentColNum);
//										setRow(currentRowNum);
//										fileInfo.setColCoor(currentColCoor);
//										fileInfo.setRowCoor(currentRowCoor);
//										fileInfo.setHeight(currentheight);
//										fileInfo.setWidth(currentwidth);
//										path.set(updateLayerPaths[i] + rowStrBuilder.toString() + "/" + colStrBuilder.toString());
//										context.write(path, fileInfo);
//									}// for count
//								}// end if  GeowebcacheTool.isNewestTime
//							}// end if bupdataTile
//						}
//						xreaded += xread;
//						colNum++;
//					}
//																	
//				} else{
//					if(dataset != null) {
//						dataset.delete();
//						dataset = null;
//					}
//					return;
//				}
//			}
//		}
//		
//		protected void cleanup(Context context) throws IOException,
//				InterruptedException {
//			super.cleanup(context);
//			resourceTable.flushCommits();
//			resourceTable.close();
//			if(dataset != null) {
//				dataset.delete();
//			}
//			Configuration conf = context.getConfiguration();
//			HTable metaTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
//			metaTable.setAutoFlush(false, true);
//			//insert the Layers information into meta table
//			StringBuilder rowkeyStrBuilder = new StringBuilder(outputpath);
//			rowkeyStrBuilder.insert(rowkeyStrBuilder.lastIndexOf("/L"), "/");
//			rowkeyStrBuilder.deleteCharAt(rowkeyStrBuilder.length()-1);
//			byte[] keys = Bytes.toBytes(TransCoding.decode(rowkeyStrBuilder.toString(), "UTF-8"));
//			Get get = new Get(keys);
//			Result result = metaTable.get(get);
//			if(result.isEmpty()) {
//			Put put = new Put(Bytes.toBytes(TransCoding.decode(rowkeyStrBuilder.toString(), "UTF-8")));
//			put.add(GtDataConfig.META.FAMILY.byteVal,
//					GtDataConfig.META.DFS.byteVal,
//					Bytes.toBytes("0"));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.SIZE.byteVal,
//					Bytes.toBytes("-1"));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.URL.byteVal,
//					Bytes.toBytes(""));
//			put.add(GtDataConfig.META.FAMILY.byteVal,
//					GtDataConfig.META.TIME.byteVal,
//					Bytes.toBytes("" + System.currentTimeMillis()));
//			metaTable.put(put);
//			metaTable.flushCommits();
//			}
//			metaTable.close();
//			metadataTable.close();
//			
//			if(oGeometry != null) {
//				path.set(timeStr);
//				fileInfo.setMD5(oGeometry.ExportToJson());
//				fileInfo.setGeometry(true);
//				context.write(path, fileInfo);
//			}
//		}
//	}
//	
//	/**
//	 * 
//	 * @author shaolin
//	 *insert the file's record into meta table
//	 */
//	static class MetaTableInsertReducer extends
//			TableReducer<Text, FileInfo, NullWritable> {
//		int indexof;
//		int rowkeyPrefixLen;
////		private int updateMinLayers;
////		private int cuttingMinLayers;
//		private HTable resourceTable;
//		private HTable metaTable;
//		
//		private String format;
//		private String imagePath;
//		private Dataset dataset;
//		private int[] rgb;
//		private int[] bands;
//		private byte[] buffer;
//		private int colorInterp;
//		private int bandCount;
//		private double percentage;
//		private byte[] nodataValues;
//		private boolean nodataFlag;
//		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
//		private BufferedImage bufferImag;
//		private int[] intZeroAreay;
//		private StringBuilder strBuilderMD5 = new StringBuilder();
//		private boolean bReaded;
//		private boolean wartermarkFlag;
//		private boolean bGeometry;
//		private Geometry geometry;
//		private String geometryOutputPath;
//		private String geometryLocalDir;
//		private String timeStr;
//		
//		protected void setup(Context context) throws IOException, InterruptedException {
//			Configuration conf = context.getConfiguration();
////			updateMinLayers = conf.getInt(CoreConfig.KEY_UPDATE_MINLAYER_INT, 30);
////			cuttingMinLayers = conf.getInt(CoreConfig.KEY_MIN_LAYER, 0);
//			format = conf.get(CoreConfig.KEY_OUTPUT_FORMAT, ".png");
//			imagePath = conf.get(CoreConfig.CUTTING_INPUTFILE, null);
//			percentage = conf.getDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, 1.1);
//			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
//			timeStr = conf.get(CoreConfig.KEY_TIME_STRING, null);
//			if(timeStr != null) {
//				long timeLong = Long.parseLong(timeStr);
//				timeLong = MAX_TIME - timeLong;
//				timeStr = "" + timeLong;
//				while(timeStr.length() != 13) {
//					timeStr = "0" + timeStr;
//				}
//			}
//			
//			geometryLocalDir = CoreConfig.LOCA_LTEMP_DIR;
//			if(!geometryLocalDir.endsWith("/")) {
//				geometryLocalDir = geometryLocalDir + "/";
//			}
//			geometryLocalDir +=  conf.get(CoreConfig.JOBID, "123456") + "/"; 
//			
//			geometryOutputPath = CoreConfig.UPDATE_SHP_PATH;
//			while(geometryOutputPath.endsWith("/"))
//				geometryOutputPath = geometryOutputPath.substring(0, geometryOutputPath.length()-1);
//			
//			dataset = gdal.Open(imagePath, gdalconst.GA_ReadOnly);
//			if(dataset != null) {
//				colorInterp = dataset.GetRasterBand(1).GetColorInterpretation();
//				bandCount = dataset.getRasterCount();
//			}
//			int bandCount = dataset.getRasterCount();
//			nodataValues = new byte[bandCount];
//			Double[] nodata = new Double[1];
//			nodataFlag = true;
//			for(int i = 1; i <= bandCount; i ++) {
//				dataset.GetRasterBand(i).GetNoDataValue(nodata);
//				if(nodata[0] == null)
//					nodataFlag = false;
//				else if(nodata[0].doubleValue() > 256) {
//					nodataFlag = false;
//				} else {
//					nodataValues[i-1] = (byte)(nodata[0].doubleValue());
//				}
//			}
//			
//			rgb = new int[128*128];
//			buffer = new byte[128*128*3];
//			bands = new int[3];
//			bands[0] = 1;
//			bands[1] = 2;
//			bands[2] = 3;
//			if(!format.startsWith(".")) {
//				format = "." + format;
//			}
//			intZeroAreay = new int[CoreConfig.WIDTH_DEFAULT * CoreConfig.HEIGHT_DEFAULT];
//			bufferImag = new BufferedImage(CoreConfig.WIDTH_DEFAULT, CoreConfig.HEIGHT_DEFAULT, BufferedImage.TYPE_INT_ARGB_PRE);
//			resourceTable = new HTable(conf, GtDataConfig.TABLE_NAME.MAP_RES_TABLE.getStrVal());
//			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.MAP_META_TABLE.getStrVal());
//		}
//		
//		
//		public void reduce(Text key, Iterable<FileInfo> values, Context context)
//				throws IOException, InterruptedException {
//			if(imagePath == null)
//				return;
//			bReaded = false;
//			bGeometry = false;
//			String gtpath = GtDataUtils.format2GtPath(key.toString()) + format;
//			for (FileInfo val : values) {//不含时间的瓦片更新
//				if(val.isGeometry()) {
//					bGeometry = true;
//					Geometry geoTemp = Geometry.CreateFromJson(val.getMD5().toString());
//					SpatialReference reference = new SpatialReference("");
//					reference.SetWellKnownGeogCS("WGS84");
//					geoTemp.AssignSpatialReference(reference);
//					if(geometry == null) {
//						geometry = geoTemp;
//					}else {
//						geometry = geometry.Union(geoTemp);
//					}
//					
//				}else if(val.getTimeText().getLength() == 0) {
//					Put put = new Put(Bytes.toBytes(gtpath));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.DFS.byteVal,
//							Bytes.toBytes("0"));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.SIZE.byteVal,
//							Bytes.toBytes(String.valueOf(val.getLength())));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.URL.byteVal,
//							Bytes.toBytes(val.getMD5().toString()));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.TIME.byteVal,
//							Bytes.toBytes("" + System.currentTimeMillis()));
//					context.write(NullWritable.get(), put);
//				}else if(!val.getBupdate()){//含有时间轴的瓦片更新，瓦片带有时间属性
//					String gtpathTime = GtDataUtils.format2GtPath(key.toString() + "/" + val.getTimeText().toString()) + format;
//					Put put = new Put(Bytes.toBytes(gtpathTime));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.DFS.byteVal,
//							Bytes.toBytes("0"));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.SIZE.byteVal,
//							Bytes.toBytes(String.valueOf(val.getLength())));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.URL.byteVal,
//							Bytes.toBytes(val.getMD5().toString()));
//					put.add(GtDataConfig.META.FAMILY.byteVal, 
//							GtDataConfig.META.TIME.byteVal,
//							Bytes.toBytes("" + System.currentTimeMillis()));
//					context.write(NullWritable.get(), put);
//				}else {//更新最新瓦片，但不需要显示时间
//					if (dataset == null) {
//						return;
//					}
//					if(gdalTool.readData(val.getxOringin(), val.getyOrigin(), val.getReadwidth(), val.getReadheight(), 
//							val.getWidth(), val.getHeight(), buffer, colorInterp, bandCount, dataset, rgb, bands)) {
//						boolean ouput = gdalTool.changRGB(buffer, rgb, val.getWidth(), val.getHeight(), 0, val.getWidth(), bandCount, percentage,
//								nodataFlag, nodataValues, bands);
//						if(ouput) {
//							if(!bReaded) {
//								Get get = new Get(gtpath.getBytes());
//								Result resultTemp = metaTable.get(get);
//								if (!resultTemp.isEmpty()) {
//									System.out.println("has exit ");
//									byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
//									get = new Get(md5Byte);
//									resultTemp = resourceTable.get(get);
//									ImageIcon imageIcon = new ImageIcon(resultTemp.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
//									bufferImag.setRGB(0, 0, CoreConfig.WIDTH_DEFAULT, CoreConfig.HEIGHT_DEFAULT, this.intZeroAreay,
//											0, CoreConfig.WIDTH_DEFAULT);
//									Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
//									gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
//									gs.dispose();
//									//ImageIO.write(bufferImag, "png", new File("E://test_test.png"));
//								}else {
//									bufferImag.setRGB(0, 0, CoreConfig.WIDTH_DEFAULT, CoreConfig.HEIGHT_DEFAULT, this.intZeroAreay,
//											0, CoreConfig.WIDTH_DEFAULT);
//								}
//								bReaded = true;
//							}
//							int x = val.getColCoor() * val.getWidth();
//							int y = val.getRowCoor() * val.getHeight();
//							bufferImag.setRGB(x, y, val.getWidth(), val.getHeight(), rgb, 0, val.getWidth());
//							//ImageIO.write(bufferImag, "png", new File("E://test_test.png"));
//						}
//					}
//				}
//			}// end for
//			if(bReaded) {
//				byteArrayOut.reset();
//				strBuilderMD5.delete(0, strBuilderMD5.length());
//				if(wartermarkFlag) {
//					ImageUtils.pressText(bufferImag, "ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 15, Color.white, -1, -1, 0.4f);
//				}
//				ImageIO.write(bufferImag, "png", byteArrayOut);		
//				//ImageIO.write(bufferImag, "png", new File("E://test_test.png"));
//				
//				strBuilderMD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));
//				Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
//				get.addFamily(GtDataConfig.RESOURCE.FAMILY.byteVal);
//				if(!resourceTable.exists(get)) {
//					Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
//					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//							GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
//					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//							GtDataConfig.RESOURCE.DATA.byteVal, byteArrayOut.toByteArray());
//					resourceTable.put(put);
//				}
//				
//				Put put = new Put(gtpath.getBytes());
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.DFS.byteVal,
//						Bytes.toBytes("0"));
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.SIZE.byteVal,
//						Bytes.toBytes(String.valueOf(byteArrayOut.size())));
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.URL.byteVal,
//						Bytes.toBytes(strBuilderMD5.toString()));
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.TIME.byteVal,
//						Bytes.toBytes("" + System.currentTimeMillis()));
//				context.write(NullWritable.get(), put);
//			}else if(bGeometry) {
//				File dir = new File(geometryLocalDir);
//				dir.mkdirs();
//				String geometryLocalPath = geometryLocalDir + "tmp.geojson";
//				File file = new File(geometryLocalPath);
//				OutputStream out = new FileOutputStream(file);
//				
//				gtpath = geometryOutputPath + "//" + timeStr + ".geojosn";
//				Get get = new Get(gtpath.getBytes());
//				Result result = metaTable.get(get);
//				if(!result.isEmpty()) {				
//					String geometryLocalPathTemp = geometryLocalDir + "tmp1.geojson";
//					Export exportFile = new Export();
//					exportFile.ExportToLocal(metaTable, resourceTable, geometryOutputPath, geometryLocalPathTemp);
//					Geometry geometryOld = gdalTool.loadMutilGeometry(geometryLocalPathTemp);
//					geometry = geometry.Union(geometryOld);	
//					File filetemp = new File(geometryLocalPathTemp);
//					filetemp.delete();
//				}
//				out.write(geometry.ExportToJson().getBytes());
//				out.close();
//				Import importMap = new Import();
//				importMap.ImportLocalFileToMapTable(geometryLocalPath, geometryOutputPath);
//				file.delete();
//				dir.delete();
//			}
//			
//		}
//		
//		protected void cleanup(Context context)throws IOException, InterruptedException {
//			if(resourceTable != null) {
//				resourceTable.flushCommits();
//				resourceTable.close();
//			}
//			if(dataset != null) {
//				dataset.delete();
//			}
//		}
//	}
//	
//	
//	
//	public int cacluteDstResolution(double srcResolution, double zeroLayersResolution) {
//		int maxLayers = 0;
//		double resolutionTmp = zeroLayersResolution;
//		while(true) {
//			if( srcResolution - resolutionTmp > 0.0000000000000) {
//				maxLayers --;
//				break;
//			}else if(srcResolution == resolutionTmp){
//				break;
//			}
//			resolutionTmp = resolutionTmp / 2.0;
//			maxLayers ++;
//		}
//		return maxLayers;
//	}
//	
//	
//	public int run(String[] args)throws Exception {
//		if (args.length < 4 || (args.length % 2 != 0) ) {
//			usage();
//			return 1;
//		}
//		Configuration conf = HBaseConfiguration.create();
//		int layers = Integer.parseInt(args[3]);
//		int minlayers = 0;
//		int updateMinLayer = -1;
//		//cacluete the maxresolution
//		double dstresolution = 0;
//		double maxLayerResolution = 0;
//		double minLayerResolution = 0;
//		String timeStr = null;
//		boolean maxLayerResolutionFlag = false;
//		boolean watermarkFlag = true;
//		boolean minLayerResolutionFlag = false;
//		double zeroPercentage = 1.00;
//		
//		
//		if(args.length > 4) {
//			for(int i = 4; i < args.length; i ++) {
//				if(args[i].equals("-maxLayer_resolution")) {
//					maxLayerResolutionFlag = true;
//					i++;
//					maxLayerResolution = Double.parseDouble(args[i]);
//				}else if (args[i].equals("minLayer_resolution")) {
//					minLayerResolutionFlag = true;
//					i ++;
//					minLayerResolution = Double.parseDouble(args[i]);
//				}else if (args[i].equals("-watermark")) {
//					i ++;
//					if(args[i].equals("false")){
//						watermarkFlag = false;
//					}
//				}else if (args[i].equals("-time")) {
//					i ++;
//					timeStr = args[i];
//					if(timeStr.length() != 13) {
//						usage();
//						return 1;
//					}
//				}else if (args[i].equals("-minLayers")) {
//					i ++;
//					minlayers = Integer.parseInt(args[i]);
//				}else if (args[i].equals("-zero_percentage")) {
//					i ++;
//					zeroPercentage = Double.parseDouble(args[i]);
//				}else if (args[i].equals("-updateMinLayer")) {
//					i ++;
//					updateMinLayer = Integer.parseInt(args[i]);
//				}
//			}
//		}
//		
//		gdal.AllRegister();
//		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
//		gdal.SetConfigOption("SHAPE_ENCODING", "");
//		Dataset dataset = gdal.Open(args[1], gdalconstConstants.GA_ReadOnly);
//		double[] adfGeoTransform = dataset.GetGeoTransform(); 
//		if (dataset.GetProjectionRef().equalsIgnoreCase(CoreConfig.WGS84P_ROJECT)) {
//			if(maxLayerResolutionFlag) {
//				dstresolution = maxLayerResolution;
//			}else if (minLayerResolutionFlag) {
//				dstresolution = minLayerResolution;
//				for(int i = 0; i < layers; i ++) {
//					dstresolution = dstresolution / 2.0;
//				}
//			}else if (layers >= CoreConfig.LAYERS_RESOLUTION.length) {
//				int length = CoreConfig.LAYERS_RESOLUTION.length - 1;
//				int count = layers - length;
//				dstresolution = CoreConfig.LAYERS_RESOLUTION[length];
//				for(int i = 0; i < count; i ++) {
//					dstresolution = dstresolution / 2;
//				}
//			}else if(layers == 0){
//				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.LAYERS_RESOLUTION[0]);
//				if(layers > CoreConfig.LAYERS_RESOLUTION.length) {
//					int time = 1;
//					for(int i = CoreConfig.LAYERS_RESOLUTION.length; i < layers; i ++) {
//						time *= 2;;
//					}
//					dstresolution = CoreConfig.LAYERS_RESOLUTION[layers] / time;
//				}else {
//					dstresolution = CoreConfig.LAYERS_RESOLUTION[layers];
//				}
//			}else {
//				dstresolution = CoreConfig.LAYERS_RESOLUTION[layers];
//				
//			}
//			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_WGS84);
//			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_WGS84);
//		}else if(dataset.GetProjectionRef().equalsIgnoreCase(CoreConfig.MERCATOR_PROJECT)){
//			if(maxLayerResolutionFlag) {
//				dstresolution = maxLayerResolution;
//			}else if (minLayerResolutionFlag) {
//				dstresolution = minLayerResolution;
//				for(int i = 0; i < layers; i ++) {
//					dstresolution = dstresolution / 2.0;
//				}
//			}else if (layers >= CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
//				int length = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length - 1;
//				int count = layers - length;
//				dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[length];
//				for(int i = 0; i < count; i ++) {
//					dstresolution = dstresolution / 2;
//				}
//			}else if (layers == 0){
//				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.MERCATOR_LAYERS_RESOLUTION[0]);
//				if(layers > CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
//					int time = 1;
//					for(int i = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length; i < layers; i ++) {
//						time *= 2;
//					}
//					dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers] / time;
//				}else {
//					dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers];
//				}
//			}else {
//				dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers];
//			}
//			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_MERCATOR);
//			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_MERCATOR);
//		}else {
//			dataset.delete();
//			System.out.println("投影不支持");
//			return 1;
//		}
//		dataset.delete();	
//		System.out.println("======debug analysis parameters");
//		conf.set(CoreConfig.JOBID, args[0]);
//		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
//		conf.setInt(CoreConfig.KEY_CURRENT_LAYER, layers);
//		conf.setInt(CoreConfig.KEY_MIN_LAYER, minlayers);
//		conf.setInt(CoreConfig.KEY_UPDATE_MINLAYER_INT, updateMinLayer);
//		conf.setDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, zeroPercentage);
//		conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, dstresolution);
//		if(timeStr != null) {
//			conf.set(CoreConfig.KEY_TIME_STRING, timeStr);
//		}
//		conf.set(CoreConfig.CUTTING_OUTPUTPATH, args[2]);
//		conf.setBoolean(CoreConfig.KEY_WARTERMARK, watermarkFlag);
//		conf.set(TableOutputFormat.OUTPUT_TABLE, CoreConfig.MAP_META_TABLE);
//		
//		Job job = Job.getInstance(conf);
//		Path path = new Path("hdfs://192.168.2.3:8020/nanlin/tiff/t1_byte.tif"); //args[1]
//		FileInputFormat.addInputPath(job, path);
//		job.setJarByClass(RealChinaSegementBack.class);
//		job.setInputFormatClass(ImageMutilLayersInputFormat.class);
//		job.setMapperClass(ImageSegMapper.class);
//		job.setMapOutputKeyClass(Text.class);
//		job.setMapOutputValueClass(FileInfo.class);
//		job.setReducerClass(MetaTableInsertReducer.class);
//		job.setNumReduceTasks(6);
//		job.setOutputFormatClass(TableOutputFormat.class);
//		job.setJobName("cutting " + path.getName());	
//		return job.waitForCompletion(true) ? 0 : 1;
//	}
//	
//	public static void usage()
//	{
//		LOG.info("usage: <rowkey> <input_path(hdfs)> <gt-data ouput path(../../_alllayers)> <maxLayers>");
//		LOG.info("       [-maxLayer_resolution maxResolution] [-minLayer_resolution minresolution] [-watermark true/false]");
//		LOG.info("       [-time time(ms)] [-minLayers minlayers] [-updateMinLayer int] [-zero_percentage [0.0-1.0]]");
//		LOG.info("       --updateMinLayer 需要合并更新的最小图层");
//		LOG.info("       -zero_percentage 瓦片含有0值像素的百分比，超过该阈值的瓦片将不会输出");
//	}
//	
//	public static void main(String[] args)throws Exception {
//		if (args == null || args.length == 0) {
//			args = new String[12];
//			args[0] = "123456";
//			args[1] = "D://nanlin//image//zhejiang16m//Zhejiang_16m_2015_PS1.tif";
//			args[2] = "/map/auto_proc/img/warter/RealtimeChinaTest2015080701/Layers/_alllayers";
//			args[3] = "0";
//			args[4] = "-minLayers";
//			args[5] = "13";
//			args[6] = "-updateMinLayer";
//			args[7] = "11";
//			args[8] = "-zero_percentage";
//			args[9] = "0.01";
//			args[10] = "-time";
//			args[11] = "";
//
//		}
//		if (args.length < 4) {
//			usage();
//		}else {
//			if(!GtDataUtils.genterGtdataDir(args[2])) {
//				System.exit(1);
//			}
////			PrintStream out = new PrintStream("E://test20150817.log");  
////		    System.setOut(out);
//			File file = new File("D://nanlin//image//MSS-ENHANCE");
//			File[] files = file.listFiles();
//			int status = 0;
//			for(int i = 0; i < files.length; i++) {
//				if(i == 0 || i == 1)
//					continue;
//				File tmpfile = files[i].listFiles()[0];
//				String filename = tmpfile.getName();
//				if(filename.equals("GF1_PMS2_E110.0_N20.1_20140530_L1A0000239875-MSS2.tiff"))
//					continue;
//				args[1] = tmpfile.getPath();
//				args[11] = ParserTime.parserTimeMilliSeconds(filename);
//				System.out.println(args[1] + " " + args[11]);
//				status = ToolRunner.run(new RealChinaSegementBack(), args);
//				if( status != 0 )
//					break;
//			}
////			int status = ToolRunner.run(new ImageMutilLayersSegement(), args);
//			System.exit(status);
//		}
//	}
}
