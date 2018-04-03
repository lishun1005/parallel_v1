
package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

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
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.ImageUtils;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImageMutilLayersInputFormat;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

public class ImageMutilLayersSegement extends Configured implements Tool {

	private static final Log LOG = LogFactory.getLog(ImageMutilLayersSegement.class);
	public static final long MAX_TIME = 9999999999999L;
	
	/**
	 * 
	 * @author shaolin
	 *insert the record into resource table
	 *and output<filePaht, fileInfo> to reduce
	 *
	 */
	static class ImageSegMapper extends Mapper<ImageMutilLayersInfo, NullWritable, Text, FileInfo> {
		private Text path = new Text();
		private FileInfo fileInfo = new FileInfo();
		private HTable resourceTable;
		private HTable metadataTable;
		private StringBuilder strBuilderMD5 = new StringBuilder();
		private String outputpath = null;	//瓦片输出路径
		private boolean wartermarkFlag = true;
		//瓦片的默认长宽
		private int widthRang;
		private int heightRang;
		private int width;
		private int height;

		//瓦片行列号
		private StringBuilder colStrBuilder = new StringBuilder();
		private StringBuilder rowStrBuilder = new StringBuilder();
		
		
		
		private Dataset dataset = null;
		private byte[] nodataValues; 
		private boolean nodataFlag; //获取影像数据的nodata值是否成功， false->失败，  true->成功
		private int bandCount;
		private int colorInterp; //影像颜色表类型
		private byte[] buffer;   //存放影像二进制数据
		private int[] rgb;       //存放影像数据的rgb数据
		private int[] rgbArrayTemp;
		private String timeStr = null;
		private long timeLong;
		private boolean bCoverage = false;
		private boolean bMosaic = true;
		private boolean bOutput = false;

		private int[] bands;     //需要读取的波段数组
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();

		private BufferedImage bufferImag;
		private int[] intZeroAreay;
		//是否统计零值像素
		private boolean bZeroCount = false;
		//零值像素最大百分比，超过该阈值，该瓦片不输出
		private double zeroMaxPercentage = 1.1;
//		private String rowColTimeUpdateUrl = CoreConfig.URL_REALTIMECHINA_UPDATE;
		
		
		public void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
			bCoverage = conf.getBoolean(CoreConfig.KEY_COVERAGE_BOOLEAN, false);
			bMosaic = conf.getBoolean(CoreConfig.KEY_MOSAIC_BOOLEAN, true);
			timeStr = conf.get(CoreConfig.KEY_TIME_STRING, null);
			zeroMaxPercentage = conf.getDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, 1.1);
			if(zeroMaxPercentage - 1.0 < 0){
				bZeroCount = true;
			}
			
			if(timeStr != null) {
				timeLong = Long.parseLong(timeStr);
				long time = Long.parseLong(timeStr);
				time = MAX_TIME - time;
				StringBuilder timeStrBuilder = new StringBuilder(time + "");
				int length = timeStrBuilder.length();
				if(length < 13) {
					for(int i = length; i < 13; i ++) {
						timeStrBuilder.insert(0, "0");
					}
				}
				timeStr = timeStrBuilder.toString();
				fileInfo.setTimeText(timeStr);
			}
			resourceTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
			metadataTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
			resourceTable.setAutoFlushTo(false);
			widthRang = CoreConfig.WIDTH_DEFAULT;
			heightRang = CoreConfig.HEIGHT_DEFAULT;
			rgb = new int[widthRang*heightRang];
			rgbArrayTemp = new int[widthRang*heightRang];
			intZeroAreay = new int[widthRang*heightRang];
			bufferImag = new BufferedImage(widthRang, heightRang, BufferedImage.TYPE_INT_ARGB_PRE);
			int mutil = CoreConfig.IMGBLOCK_WIDTH;
			buffer = new byte[heightRang*widthRang*mutil*3];
		}
		
		/**
		 * @breif 读取影像数据（bands全局变量     需要读取的波段）
		 * @param xOrigin    数据读取范围的左上角的x坐标（像素为单位）
		 * @param yOrigin    数据读取范围的左上角的y坐标（像素为单位）
		 * @param xsize      X方向读取的长度（像素为单位）
		 * @param ysize      Y方向读取的长度（像素为单位）
		 * @param buf_xsize  将X方向的读取的长度xsize重采样成新的长度buf_xsize
		 * @param buf_ysize  将Y方向的读取的长度ysize重采样成新的长度buf_ysize
		 * @param buffer     存放读取的数据
		 * @return
		 */
		public boolean readData(int xOrigin, int yOrigin, int xsize, int ysize, int buf_xsize, int buf_ysize, byte[] buffer) {
			if (colorInterp == 2 && bandCount == 1) {
				int count = buf_xsize*buf_ysize;
//				dataset.GetRasterBand(1).GetHistogram(buckets)
				if (dataset.ReadRaster(xOrigin, yOrigin, xsize, ysize, buf_xsize, buf_ysize, gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
					Band band = dataset.GetRasterBand(1);
					for (int i = 0; i < count; i ++ ) {
						rgb[i] =  band.GetColorTable().GetColorEntry((buffer[i] & 0xff)).getRGB();
					}
					return true;
				}
				else {
					return false;
				}
				
			}
			else {				
				if (dataset.ReadRaster(xOrigin, yOrigin, xsize, ysize, buf_xsize, buf_ysize, gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
					return true;
				}
				else {
					return false;
				}
			}
		}
		

		public void changRGBbyMask(byte[] buffer, byte[] mask, int[] rgb, int buf_xsize, int buf_ysize) {
			if (colorInterp == 2 && bandCount == 1) {
				return ;
			}		
			int count = buf_xsize * buf_ysize;
			for(int i = 0; i < count; i ++) {
				if(mask[i] == 0 ) {
					rgb[i]= (0x00000000);
				} else {
					rgb[i] = (0xff000000);
					for(int j = 0; j < bandCount; j ++) {
						rgb[i] += ((buffer[i + count * j] & 0x000000ff) << 8 * (bandCount - j -1));
					}
				}
			}
		}
		
		
		/**
		 * @breif 将按波段读取的二进制数据 转为 rgb格式， 每次只转一部分。比如buffer存储的是256 * 5120范围的数据，那么第一次将一个256*256范围的对应的buffer转为rgb
		 * 第二次将第二个256*256的对应的buffer转为rgb，依次类推，直至将整个buffer转为256*256的rgb
		 * @param buffer	 
		 * @param rgb        存储 宽为rgb_xsize， 长为rgb_ysize的影像数据
		 * @param rgb_xsize  
		 * @param rgb_ysize
		 * @param xreaded    buffer所存储的影像数据在X方向上已读的长度
		 * @param xLength    buffer所存储的影像数据在X方向上的长度
		 */
		public void changRGB(byte[]buffer, int[] rgb, int rgb_xsize, int rgb_ysize, int xreaded, int xLength) {
			if (colorInterp == 2 && bandCount == 1) {
				return ;
			}		
			int band_step = rgb_ysize * xLength;
			int rgb_step = 0;
			int buffer_step = 0;
			for (int i = 0; i < rgb_ysize; i ++) {
				buffer_step = i * xLength + xreaded;
				for (int j = 0; j < rgb_xsize; j ++) {
					rgb[rgb_step] = (0x00000000);//像素设置为透明
					//如果成功获取影像数据的nodata值，并且该像素对应的所有波段的值都为nodata，则需要将该像素置为透明
					if(nodataFlag) {
						for(int band = 0; band < bandCount; band ++) {
							if(buffer[buffer_step + band_step * band] != nodataValues[bands[band]-1]) {
								rgb[rgb_step]= (0xff000000);
								break;
							}
						}
					}else {
						rgb[rgb_step]= (0xff000000);//像素设置为不透明
					}
					for(int band = 0; band < bandCount; band ++) {
						rgb[rgb_step] += ((buffer[buffer_step + band_step * band] & 0x000000ff) << 8 * (bandCount - band -1));
					}
					rgb_step ++;
					buffer_step ++;
				}
			}
		}
		
		
		/**
		 * @breif 将按波段读取的二进制数据 转为 rgb格式， 每次只转一部分。比如buffer存储的是256 * 5120范围的数据，那么第一次将一个256*256范围的对应的buffer转为rgb
		 * 第二次将第二个256*256的对应的buffer转为rgb，依次类推，直至将整个buffer转为256*256的rgb，同时统计0值得比值
		 * @param buffer	 
		 * @param rgb        存储 宽为rgb_xsize， 长为rgb_ysize的影像数据
		 * @param rgb_xsize  
		 * @param rgb_ysize
		 * @param xreaded    buffer所存储的影像数据在X方向上已读的长度
		 * @param xLength    buffer所存储的影像数据在X方向上的长度
		 * @param percentage 0值像素最大比值
		 * @return           如果0值像素所占比值小于预定阀值percentage， 返回成功，否则返回失败
		 */
		
		public boolean changRGB(byte[]buffer, int[] rgb, int rgb_xsize, int rgb_ysize, int xreaded, int xLength, double percentage) {
			if (colorInterp == 2 && bandCount == 1 || rgb_xsize != 256 || rgb_ysize != 256) {
				return false;
			}
			double zeroPercentage = 0.0;
			int zeroCount = 0;
			int band_step = rgb_ysize * xLength;
			int rgb_step = 0;
			int buffer_step = 0;
			for (int i = 0; i < rgb_ysize; i ++) {
				buffer_step = i * xLength + xreaded;
				for (int j = 0; j < rgb_xsize; j ++) {
					rgb[rgb_step] = (0x00000000);//像素设置为透明
					//如果成功获取影像数据的nodata值，并且该像素对应的所有波段的值都为nodata，则需要将该像素置为透明
					if(nodataFlag) {
						for(int band = 0; band < bandCount; band ++) {
							if(buffer[buffer_step + band_step * band] != nodataValues[bands[band]-1]) {
								rgb[rgb_step]= (0xff000000);
								break;
							}
						}
					}else {
						rgb[rgb_step]= (0xff000000);//像素设置为不透明
					}
					for(int band = 0; band < bandCount; band ++) {
						rgb[rgb_step] += ((buffer[buffer_step + band_step * band] & 0x000000ff) << 8 * (bandCount - band -1));
					}
					if(rgb[rgb_step] == 0x00000000 || rgb[rgb_step] == 0xff000000) {
						zeroCount ++;
					}
					rgb_step ++;
					buffer_step ++;
					
				}
			}
			zeroPercentage = zeroCount * 1.0 / (rgb_xsize * rgb_xsize * 1.0);
			if(zeroPercentage - percentage <= 0.000){
				return true;
			}else {
				return false;
			}
			
		}
		
		/**
		 * 将原有的发片和新的瓦片进行合并，重叠区域，以新的瓦片为准		
		 * @param merge
		 * @param rgb
		 * @param xWriteOrigin
		 * @param yWriteOrigin
		 * @param xread
		 * @param yLength
		 */
		public void merge(int[] merge, int[] rgb, int xWriteOrigin, int yWriteOrigin, int xread, int yLength) {
			int length = xread * yLength;
			for(int i = 0; i < length; i ++) {
				if (rgb[i] == 0) {
					rgb[i] = merge[i];
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
		
	
		public void map(ImageMutilLayersInfo key, NullWritable value, Context context) throws
			IOException, InterruptedException {
			if(dataset == null) {
				gdal.AllRegister();
				dataset = gdal.Open(key.getFilepath().toString(), gdalconstConstants.GA_ReadOnly);				
				//dataset = gdal.Open("D://nanlin//guangzhou_051401.tiff", gdalconstConstants.GA_ReadOnly); //guangzhou_051401.tiff //t1_byte.tif
				//dataset = gdal.Open("F://hunan//hunan_xiangqian_clip.tif", gdalconstConstants.GA_ReadOnly);
				width = dataset.GetRasterXSize();
				height = dataset.GetRasterYSize();
				int bandCount = dataset.getRasterCount();
				nodataValues = new byte[bandCount];
				Double[] nodata = new Double[1];
				nodataFlag = true;
				for(int i = 1; i <= bandCount; i ++) {
					dataset.GetRasterBand(i).GetNoDataValue(nodata);
					if(nodata[0] == null) {
						nodataFlag = false;
						break;
					}
					else if(nodata[0].doubleValue() >= 256) {
						nodataFlag = false;
						break;
					} else {
						nodataValues[i-1] = (byte)(nodata[0].doubleValue());
					}
				}
				bands = key.getBands();			
				outputpath = key.getGtdataOutputPath().toString();
				if(!outputpath.endsWith("/")) {
					outputpath += "/";
				}
				System.out.println(outputpath);
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
			setRow(rowNum);
			System.out.println("tileColOffset= " + tileColOffset + " ,tileRowOffset=" + tileRowOffset);
			path.set(outputpath + rowStrBuilder.toString());
			
			if (dataset != null) {
				int nbands = dataset.getRasterCount();
				bandCount = bands.length;
				if (nbands == 1) {
					bandCount = 1;
					bands = new int[1];
					bands[0] = 1;
				}
				colorInterp = dataset.GetRasterBand(1).GetColorInterpretation();
				
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
			
				if (readData(xReadCoordinateActual, yReadCoordinateActual, xreadActual, yreadActual, xLength, yLength, buffer)) {
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
						bufferImag.setRGB(0, 0, widthRang, heightRang, intZeroAreay, 0, widthRang);
						byteArrayOut.reset();
						if( bZeroCount ) {
							bOutput = changRGB(buffer, rgb, xread, yLength, xreaded, xLength, zeroMaxPercentage);
						}else {
							changRGB(buffer, rgb, xread, yLength, xreaded, xLength);
							bOutput = true;
						}
						
						setCol(colNum);
						
						if (bOutput) {
							if(bCoverage){
								bOutput = true;
								bufferImag.setRGB(xWriteOrigin, yWriteOrigin, xread, yLength, rgb, 0, xread);
							}else if(bMosaic){
								String filetemp;
								if(timeStr != null) {
									filetemp = outputpath + rowStrBuilder.toString() + "/" + colStrBuilder.toString()+ "//" + timeStr + ".png";
								}else {
									filetemp = outputpath + rowStrBuilder.toString() + "//" + colStrBuilder.toString() + ".png";
								}
								Get getTemp = new Get(Bytes.toBytes(TransCoding.decode(filetemp, "UTF-8")));
								Result resultTemp = metadataTable.get(getTemp);
								if (!resultTemp.isEmpty()) {
									System.out.println("has exit ");
									byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
									getTemp = new Get(md5Byte);
									resultTemp = resourceTable.get(getTemp);
									ImageIcon imageIcon = new ImageIcon(resultTemp.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
									Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
									gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
									gs.dispose();
									bufferImag.getRGB(xWriteOrigin, yWriteOrigin, xread, yLength, rgbArrayTemp, 0, xread);
									merge(rgbArrayTemp, rgb, xWriteOrigin, yWriteOrigin, xread, yLength);
								}
								bufferImag.setRGB(xWriteOrigin, yWriteOrigin, xread, yLength, rgb, 0, xread);
								bOutput = true;
							}else{
								String filetemp;
								if(timeStr != null) {
									filetemp = outputpath + rowStrBuilder.toString() + "/" + colStrBuilder.toString()+ "//" + timeStr + ".png";
								}else {
									filetemp = outputpath + rowStrBuilder.toString() + "//" + colStrBuilder.toString() + ".png";
								}
								Get getTemp = new Get(Bytes.toBytes(TransCoding.decode(filetemp, "UTF-8")));
								Result resultTemp = metadataTable.get(getTemp);
								if (resultTemp.isEmpty()) {
									bOutput = true;
								}else {
									bOutput = false;
								}
							}
							if(bOutput){
								if(wartermarkFlag) {
									ImageUtils.pressText(bufferImag, "ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 15, Color.white, -1, -1, 0.4f);
								}
								ImageIO.write(bufferImag, "png", byteArrayOut);								
								//ImageIO.write(bufferImag, "png", new File("E://test2017077.png"));
																				
								strBuilderMD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));
								fileInfo.setMD5(strBuilderMD5.toString());
								fileInfo.setLength(byteArrayOut.size());
								fileInfo.setFilename(colStrBuilder.toString());
								Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
								Result result = resourceTable.get(get);
								if (result == null || result.isEmpty()) {
									Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
									put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
											GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
									put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
											GtDataConfig.RESOURCE.DATA.byteVal, byteArrayOut.toByteArray());
									resourceTable.put(put);
								}
								context.write(path, fileInfo);
								strBuilderMD5.delete(0, strBuilderMD5.length());
							}// end if boutput
						}
						xreaded += xread;
						colNum++;
					}//end for 
																	
				} else{// end if read, read data error
					if(dataset != null) {
						dataset.delete();
						dataset = null;
					}
					return;
				}
			}
		}
		
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			super.cleanup(context);
			resourceTable.flushCommits();
			resourceTable.close();
			if(dataset != null) {
				dataset.delete();
			}
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
			metadataTable.close();
		}
	}
	
	/**
	 * 
	 * @author shaolin
	 *insert the file's record into meta table
	 */
	static class MetaTableInsertReducer extends
			TableReducer<Text, FileInfo, NullWritable> {
		private StringBuilder rowkey = new StringBuilder();
		int indexof;
		int rowkeyPrefixLen;
	
		public void reduce(Text key, Iterable<FileInfo> values, Context context)
				throws IOException, InterruptedException {
			rowkey.replace(0, rowkey.length(), key.toString());
			rowkeyPrefixLen = rowkey.length();
			indexof = rowkey.lastIndexOf("/");
			rowkey.insert(indexof, "/");
			//插入行号目录
			Put put = new Put(Bytes.toBytes(TransCoding.decode(rowkey.toString(), "UTF-8")));
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
			context.write(NullWritable.get(), put);
			rowkey.replace(0, rowkey.length(), key.toString());
			//插入列号文件名
			for (FileInfo val : values) {
				if(val.getTimeText().toString().length() != 0) {
					rowkey.replace(rowkeyPrefixLen, rowkey.length(), "//"
							+ val.getFilename().toString());
					put = new Put(Bytes.toBytes(TransCoding.decode(rowkey.toString(), "UTF-8")));
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
					context.write(NullWritable.get(), put);
					
					rowkey.replace(rowkeyPrefixLen, rowkey.length(), "/" + val.getFilename().toString() + "//" + val.getTimeText().toString() + ".png");
					put = new Put(Bytes.toBytes(TransCoding.decode(rowkey.toString(), "UTF-8")));
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
				}else {
					rowkey.replace(rowkeyPrefixLen, rowkey.length(), "//"
							+ val.getFilename().toString() + ".png");
					put = new Put(Bytes.toBytes(TransCoding.decode(rowkey.toString(), "UTF-8")));
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
				}
				
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
	
	
	public int run(String[] args)throws Exception {
		if (args.length < 4 || (args.length % 2 != 0) ) {
			usage();
			return 1;
		}
		Configuration conf = HBaseConfiguration.create();
		int layers = Integer.parseInt(args[3]);
		int minlayers = 0;
		int pcsInt = 0;
		//cacluete the maxresolution
		double dstresolution = 0;
		double maxLayerResolution = 0;
		double  minLayerResolution = 0;
		String timeStr = null;
		boolean maxLayerResolutionFlag = false;
		boolean watermarkFlag = true;
		boolean bCoverage = false;
		boolean bMosaic = true;
		boolean minLayerResolutionFlag = false;
		boolean bfloor = true;
		double zeroPercentage = 1.1;
		
		
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
				}else if (args[i].equals("-coverage")) {
					i ++;
					if(args[i].equals("true")){
						bCoverage = true;
					}else if(args[i].equals("false")) {
						bCoverage = false;
					}
				}else if (args[i].equals("-mosaic")) {
					i ++;
					if(args[i].equals("false")){
						bMosaic = false;
					}else if (args[i].equals("true")){
						bMosaic = true;
					}
				}else if (args[i].equals("-pcs")) {
					i ++;
					pcsInt = Integer.parseInt(args[i]);
				}else if (args[i].equals("-minLayers")) {
					i ++;
					minlayers = Integer.parseInt(args[i]);
				}else if (args[i].equals("-trunc")) {
					i ++;
					if(args[i].equals("ceil")) {
						bfloor = false;
					}
				}else if (args[i].equals("-zero_percentage")) {
					i ++;
					zeroPercentage = Double.parseDouble(args[i]);
				}
			}
		}
		if(bCoverage == true) {
			bMosaic = false;
		}
		
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		Dataset dataset = gdal.Open(args[1], gdalconstConstants.GA_ReadOnly);
		double[] adfGeoTransform = dataset.GetGeoTransform(); 
		System.out.println(dataset.GetProjectionRef());
		if (pcsInt == 0 && dataset.GetProjectionRef().startsWith(CoreConfig.WGS84P_ROJECT)) {
			if(maxLayerResolutionFlag) {
				dstresolution = maxLayerResolution;
			}else if (minLayerResolutionFlag) {
				dstresolution = minLayerResolution;
				for(int i = 0; i < layers; i ++) {
					dstresolution = dstresolution / 2.0;
				}
			}else if (layers >= CoreConfig.LAYERS_RESOLUTION.length) {
				int length = CoreConfig.LAYERS_RESOLUTION.length - 1;
				int count = layers - length;
				dstresolution = CoreConfig.LAYERS_RESOLUTION[length];
				for(int i = 0; i < count; i ++) {
					dstresolution = dstresolution / 2;
				}
			}else if(layers == 0){
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
		}else if(dataset.GetProjectionRef().equalsIgnoreCase(CoreConfig.MERCATOR_PROJECT)){
			if(maxLayerResolutionFlag) {
				dstresolution = maxLayerResolution;
			}else if (minLayerResolutionFlag) {
				dstresolution = minLayerResolution;
				for(int i = 0; i < layers; i ++) {
					dstresolution = dstresolution / 2.0;
				}
			}else if (layers >= CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
				int length = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length - 1;
				int count = layers - length;
				dstresolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[length];
				for(int i = 0; i < count; i ++) {
					dstresolution = dstresolution / 2;
				}
			}else if (layers == 0){
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
			dataset.delete();
			System.out.println("投影不支持");
			return 1;
		}
		dataset.delete();	
		System.out.println("======debug analysis parameters");
		conf.set(CoreConfig.JOBID, args[0]);
		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
		conf.setInt(CoreConfig.KEY_CURRENT_LAYER, layers);
		conf.setInt(CoreConfig.KEY_MIN_LAYER, minlayers);
		conf.setBoolean(CoreConfig.KEY_COVERAGE_BOOLEAN, bCoverage);
		conf.setBoolean(CoreConfig.KEY_MOSAIC_BOOLEAN, bMosaic);
		conf.setBoolean(CoreConfig.KEY_TRUNC_BOOLEAN, bfloor);
		conf.setDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, zeroPercentage);
		if(timeStr != null) {
			conf.set(CoreConfig.KEY_TIME_STRING, timeStr);
		}
		conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, dstresolution);
		conf.set(CoreConfig.CUTTING_OUTPUTPATH, args[2]);
		conf.setBoolean(CoreConfig.KEY_WARTERMARK, watermarkFlag);
		conf.set(TableOutputFormat.OUTPUT_TABLE,
				CoreConfig.MAP_META_TABLE);
		System.out.println("ouput meta table: " + CoreConfig.MAP_META_TABLE);
		Job job = Job.getInstance(conf);
		Path path = new Path(args[1]); //args[1]//"hdfs://192.168.2.3:8020/nanlin/tiff/t1_byte.tif"
		FileInputFormat.addInputPath(job, path);
		job.setJarByClass(ImageMutilLayersSegement.class);
		job.setInputFormatClass(ImageMutilLayersInputFormat.class);
		job.setMapperClass(ImageSegMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FileInfo.class);
		job.setReducerClass(MetaTableInsertReducer.class);
		job.setNumReduceTasks(6);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setJobName("cutting " + path.getName());	
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public static void usage()
	{
		LOG.info("usage: <rowkey> <input_path(hdfs)> <gt-data ouput path(../../_alllayers)> <the total number of layers> <maxLayers>");
		LOG.info("       [-maxLayer_resolution maxResolution] [-minLayer_resolution minresolution] [-watermark true/false]");
		LOG.info("       [-time time(ms)] [-coverage true/false] [-mosaic true/false]");
		LOG.info("       [-minLayers minlayers] [-trunc floor/ceil] [-zero_percentage [0.0-1.0]]");
		LOG.info("       -zero_percentage 瓦片含有0值像素的百分比，超过该阈值的瓦片将不会输出");
	}
	
	public static void main(String[] args)throws Exception {
		if (args == null || args.length == 0) {
			args = new String[16];
			args[0] = "123456";
			args[1] = "H://baoding//baoding_clip.tif";
//			args[1] = "E://yangli_new//hunan_heyang.img";
			args[2] = "/map/auto_proc/img/warter/test/Layers/_alllayers";
			args[3] = "13";
			args[4] = "-coverage";
			args[5] = "true";
			args[6] = "-mosaic";
			args[7] = "false";
			args[8] = "-trunc";
			args[9] = "floor";
			args[10] = "-minLayers";
			args[11] = "13";
			args[12] = "-time";
			args[13] = "1412784000000";
			args[14] = "-zero_percentage";
			args[15] = "0.01";
		}
		if (args.length < 4) {
			usage();
		}else {
//			PrintStream out = new PrintStream("E://test1.log");  
//		    System.setOut(out);
//			File file = new File("D://nanlin//image//MSS-ENHANCE");
//			File[] files = file.listFiles();
//			int status = 0;
//			for(int i = 0; i < files.length; i++) {
//				File tmpfile = files[i].listFiles()[0];
//				String filename = tmpfile.getName();
//				args[1] = tmpfile.getPath();
//				args[13] = ParserTime.parserTimeMilliSeconds(filename);//filename.substring(0, filename.length()-4) + "000";
//				System.out.println(args[1] + " " + args[13]);
//				status = ToolRunner.run(new ImageMutilLayersSegement(), args);
//				if( status != 0 )
//					break;
//			}
			int status = ToolRunner.run(new ImageMutilLayersSegement(), args);
			System.exit(status);
		}
	}
}
