package com.rsclouds.gtparallel.core.gtdata.cutting;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
import com.rsclouds.gtparallel.core.hadoop.io.ImageInfo;
import com.rsclouds.gtparallel.core.hadoop.io.RowColName;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImageBlockInputFormat;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

public class ImageSegement extends Configured implements Tool{
	private static final Log LOG = LogFactory.getLog(ImageSegement.class);

	/**
	 * 
	 * @author shaolin
	 *insert the record into resource table
	 *and output<filePaht, fileInfo> to reduce
	 *
	 */
	
	static class ImageSegMapper extends Mapper<ImageInfo, NullWritable, Text, FileInfo> {

		private Text path = new Text();
		private FileInfo fileInfo = new FileInfo();
		private RowColName rowColName = new RowColName();
		private HTable resourceTable;
		private StringBuilder strBuilderMD5 = new StringBuilder();
		private String outputpath;	

		 //处理影像数据块的起始点
		private int xOrigin; 
		private int yOrigin;
		//写入数据的起始点
		private int xWriteOrigin;
		private int yWriteOrigin;
		//需要处理的影像范围
		private int xLength;
		private int yLength;
		

		//瓦片的默认长宽
		private int widthRang;
		private int heightRang;
		//每个波段统计的最大值和最小值
//		private double[] max;
//		private double[] min;
		//瓦片行列号
		private StringBuilder colStrBuilder = new StringBuilder();
		private StringBuilder rowStrBuilder = new StringBuilder();
		
		
		private Dataset dataset = null;
//		private int dataType;
		private double[] adfGeoTransform; 
		private int bandCount;
		private int colorInterp;
		private byte[] buffer;      //
//		private double[] bufferDou; //存放
//		private byte[] bufferMask;  //存放从掩膜文件读取的数据
		private int[] rgb;          //
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
		//影像数据的左上角与瓦片的左上角的偏移量
		private int tilecolOffset;
		private int tilerowOffset;
		
		private BufferedImage bufferImag;
		private int[] intZeroAreay;
		
		//需要读取的波段数组
		private int[] bands;
		
		public void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			resourceTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
			resourceTable.setAutoFlushTo(false);
			outputpath = conf.get(CoreConfig.CUTTING_OUTPUTPATH);
			if (!outputpath.endsWith("/")) {
				outputpath += "/";
			}
			widthRang = CoreConfig.WIDTH_DEFAULT;
			heightRang = CoreConfig.HEIGHT_DEFAULT;
			rgb = new int[widthRang*heightRang];
			intZeroAreay = new int[widthRang*heightRang];
			bufferImag = new BufferedImage(widthRang, heightRang, BufferedImage.TYPE_INT_ARGB_PRE);
			
		}
		
		//
		public boolean readData(int xOrigin, int yOrigin, int xsize, int ysize, int buf_xsize, int buf_ysize, byte[] buffer) {
			if (colorInterp == 2 && bandCount == 1) {
				int count = buf_xsize*buf_ysize;
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
		
		public void changRGB(byte[]buffer, int[] rgb, int buf_xsize, int buf_ysize, int yreaded, int yLength, boolean bAlpha) {
			if (colorInterp == 2 && bandCount == 1) {
				return ;
			}		
			int count = buf_xsize * buf_ysize;
			int step = buf_xsize * yLength;
			int tripleCount = count *3;
			int readedSize = buf_xsize * yreaded;
			int total = count + readedSize;
			for(int i = readedSize; i < total; i ++) {
				if(bAlpha && buffer[i+tripleCount] == 0 ) {
					rgb[i-readedSize]= (0x00000000);
				} else {
//					rgb[i]= (0xff000000) + ((buffer[i] & 0x000000ff)<< 16) + 
//						(((buffer[i+count]) & 0x000000ff) << 8) + (((int)buffer[i + douCount]) & 0x000000ff);
					rgb[i-readedSize] = (0xff000000);
					for(int j = 0; j < bandCount; j ++) {
						rgb[i-readedSize] += ((buffer[i + step * j] & 0x000000ff) << 8 * (bandCount - j -1));
					}
				}
			}
		}
		
		public void merge(int[] merge, int[] rgb) {
			int length = merge.length;
			for(int i = 0; i < length; i ++) {
				rgb[i] += merge[i];
			}
		}
		//计算瓦片行列号，并将其转为字符串
		public void setRowCol(int xreaded, int yreaded, double resolution) {
			double xCoordinate = adfGeoTransform[0] + xreaded * resolution + yreaded * adfGeoTransform[2];
			double yCoordinate = adfGeoTransform[3] + xreaded * adfGeoTransform[4] + yreaded * (-resolution);
			long tilerowTopLeft = (long) Math.floor((90 - yCoordinate) / CoreConfig.HEIGHT_DEFAULT / resolution);
			long tilecolTopLeft = (long) Math.floor((180 + xCoordinate) / CoreConfig.WIDTH_DEFAULT / resolution);
			colStrBuilder.replace(0, colStrBuilder.length(), Long.toHexString(tilecolTopLeft));
			int count = 8 - colStrBuilder.length();
			for (int i = 0; i < count; i++) {
				colStrBuilder.insert(0, "0");
			}
			colStrBuilder.insert(0, "C");
			colStrBuilder.append(".png");
			
			rowStrBuilder.replace(0, rowStrBuilder.length(), Long.toHexString(tilerowTopLeft));
			count = 8 - rowStrBuilder.length();
			for (int i = 0; i < count; i++) {
				rowStrBuilder.insert(0, "0");
			}
			rowStrBuilder.insert(0, "R");
			rowColName.setRowName(rowStrBuilder.toString());
			rowColName.setColName(colStrBuilder.toString());
		}
		
		

		public void map(ImageInfo key, NullWritable value, Context context) throws
			IOException, InterruptedException {
			gdal.AllRegister();
			dataset = gdal.Open(key.getFilepath().toString(), gdalconstConstants.GA_ReadOnly);
		//	dataset = gdal.Open("D://nanlin//guangzhou_051401.tiff", gdalconstConstants.GA_ReadOnly);
			xOrigin = key.getxOrigin();//重采样后的x坐标
			yOrigin = key.getyOrigin();//重采样后的y坐标
			xLength = key.getxLength();//重采样后的宽度
			yLength = key.getyLength();//重采样后的高度
			double dstResolution = key.getDstResolution();//重采样后的分辨率
			
		//	max = key.getMax();
		//	min = key.getMin();
			bands = key.getBands();
			if (dataset != null) {
				adfGeoTransform = dataset.GetGeoTransform();//获取原始影像的六参数
		//		dataType = dataset.GetRasterBand(1).getDataType();
				int nbands = dataset.getRasterCount();
				bandCount = bands.length;
				if (nbands == 1) {
					bandCount = 1;
					bands = new int[1];
					bands[0] = 1;
				}
				colorInterp = dataset.GetRasterBand(1).GetColorInterpretation();
				if(colorInterp == 2 && nbands == 1)
					buffer = new byte[widthRang*yLength*3];
				else
					buffer = new byte[widthRang*yLength*bandCount];
				
				adfGeoTransform[0] = adfGeoTransform[0] + xOrigin * dstResolution + yOrigin * adfGeoTransform[2]; //分块左上角的经度
				adfGeoTransform[3] = adfGeoTransform[3] + xOrigin * adfGeoTransform[4] + yOrigin * (-dstResolution);//分块左上角的纬度
				//计算分块的左上角的第一个瓦片的行列号
				long tilerowTopLeft = (long) Math.floor((90 - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
				long tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / dstResolution);
				
				//根据计算分块的左上角的第一个瓦片的行列号，计算该行列号实际对应的经纬坐标
				double yCoordinate = 90 - tilerowTopLeft * dstResolution * CoreConfig.HEIGHT_DEFAULT; 
				double xCoordinate = tilecolTopLeft * dstResolution * CoreConfig.WIDTH_DEFAULT - 180;
				
				//计算分块的左上角与实际对应瓦片的左上角的偏移量
				tilecolOffset =(int) ((adfGeoTransform[0] - xCoordinate)/dstResolution);
				tilerowOffset =(int) ((adfGeoTransform[3] - yCoordinate)/(-dstResolution));
				
				//根据重采样后分块左上角的坐标（以像素为单位）反算实际的坐标
				int actualXOrigin = (int)(xOrigin * dstResolution / adfGeoTransform[1]); 
				int actualYOrigin = (int)(yOrigin * (-dstResolution) / adfGeoTransform[5]);
				//实际需要读取影像的真实起始点坐标（以像素为单位）
				int xReadCoordinateActual = actualXOrigin;
				int yReadCoordinateActual = actualYOrigin;
				//实际需要读取的瓦片数据范围
				int xreadActual;
				int yreadActual;
				//重采样后需要读取的瓦片数据范围
				int xread;
				int yread;
				//重采样后已经读取的影像范围
				int xreaded = 0;
				int yreaded = 0;
//				int countNum = 0;
				
				//分块的实际高度
				yreadActual = (int)(yLength * (-dstResolution) / adfGeoTransform[5]);
				int yreadActualTemp = yreadActual;
				int count = 0;
				for(; xreaded < xLength; ) {
					if (xLength - xreaded < widthRang) {
						xread = xLength - xreaded;
					}else if (xreaded == 0) {
						xread = widthRang - tilecolOffset;
					}else {
						xread = widthRang;
					}
					if (xreaded == 0) {
						xWriteOrigin = tilecolOffset;
					}else {
						xWriteOrigin = 0; 
					}
					xreadActual = (int)(xread * dstResolution / adfGeoTransform[1]);
					yReadCoordinateActual = actualYOrigin;
					
					if (readData(xReadCoordinateActual, yReadCoordinateActual, xreadActual, yreadActualTemp, xread, yLength, buffer)) {
						for (yreaded = 0; yreaded < yLength; ) {
							if (yLength - yreaded < heightRang) {
								yread = yLength - yreaded;
							}else if (yreaded == 0) {
								yread = heightRang - tilerowOffset;
							}else {
								yread = heightRang;
							}
							if (yreaded == 0) {
								yWriteOrigin = tilerowOffset;
							}else {
								yWriteOrigin = 0;
							}
							
//							System.err.println(countNum++);
							//初始bufferImage
							bufferImag.setRGB(0, 0, widthRang, heightRang, intZeroAreay, 0, widthRang);
							byteArrayOut.reset();
							
							yreadActual = (int)(yread * (-dstResolution) / adfGeoTransform[5]);		
							changRGB(buffer, rgb, xread, yread, yreaded, yLength, false);
							setRowCol(xreaded, yreaded, dstResolution);
							String filetemp = outputpath + rowColName.getRowName().toString() + "//" + rowColName.getColName().toString();
							Get getTemp = new Get(Bytes.toBytes(TransCoding.decode(filetemp, "UTF-8")));
							Result resultTemp = resourceTable.get(getTemp);
							if (!resultTemp.isEmpty()) {
								byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
								getTemp = new Get(md5Byte);
								resultTemp = resourceTable.get(getTemp);
								ImageIcon imageIcon = new ImageIcon(resultTemp.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
								Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
								gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
								int rgbArray[] = new int[xread*yLength];
								bufferImag.getRGB(xWriteOrigin, yWriteOrigin, xread, yread, rgbArray, 0, xread);
								merge(rgbArray, rgb);
							}
							bufferImag.setRGB(xWriteOrigin, yWriteOrigin, xread, yread, rgb, 0, xread);
							ImageUtils.pressText(bufferImag, "ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 15, Color.BLACK, -1, -1, 0.3f);
							ImageIO.write(bufferImag, "png", byteArrayOut);
							
							path.set(outputpath + rowColName.getRowName().toString());
							strBuilderMD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));
							fileInfo.setMD5(strBuilderMD5.toString());
							fileInfo.setLength(byteArrayOut.size());
							fileInfo.setFilename(rowColName.getColName().toString());
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
							System.out.println(++count);
							context.write(path, fileInfo);
							strBuilderMD5.delete(0, strBuilderMD5.length());
							yreaded += yread;
							yReadCoordinateActual += yreadActual;
						}
																	
					} else{
						if(dataset != null) {
							dataset.delete();
							dataset = null;
						}
						return;
					}
					xreaded += xread;
					xReadCoordinateActual += xreadActual;
				}
				if(dataset != null) {
					dataset.delete();
					dataset = null;
				}
			}
		}
		
		
		
//		public void map(ImageInfo key, NullWritable value, Context context) throws
//				IOException, InterruptedException {
//			gdal.AllRegister();
//			dataset = gdal.Open(key.getFilepath().toString(), gdalconstConstants.GA_ReadOnly);
////			dataset = gdal.Open("D://nanlin//guangzhou_051401.tiff", gdalconstConstants.GA_ReadOnly);
//			xOrigin = key.getxOrigin();
//			yOrigin = key.getyOrigin();
//			xLength = key.getxLength();
//			yLength = key.getyLength();
//			double dstResolution = key.getDstResolution();
//			
////			max = key.getMax();
////			min = key.getMin();
//			bands = key.getBands();
//			if (dataset != null) {
//				adfGeoTransform = dataset.GetGeoTransform();
////				dataType = dataset.GetRasterBand(1).getDataType();
//				int nbands = dataset.getRasterCount();
//				bandCount = bands.length;
//				if (nbands == 1) {
//					bandCount = 1;
//					bands = new int[1];
//					bands[0] = 1;
//				}
//				colorInterp = dataset.GetRasterBand(1).GetColorInterpretation();
//				if(colorInterp == 2 && bandCount == 1)
//					buffer = new byte[widthRang*heightRang*3];
//				else
//					buffer = new byte[widthRang*heightRang*bandCount];
//				
////				adfGeoTransform[0] = adfGeoTransform[0] + xOrigin * adfGeoTransform[1] + yOrigin * adfGeoTransform[2];
////				adfGeoTransform[3] = adfGeoTransform[3] + xOrigin * adfGeoTransform[4] + yOrigin * adfGeoTransform[5];
//				adfGeoTransform[0] = adfGeoTransform[0] + xOrigin * dstResolution + yOrigin * adfGeoTransform[2];
//				adfGeoTransform[3] = adfGeoTransform[3] + xOrigin * adfGeoTransform[4] + yOrigin * (-dstResolution);
//				//calculate the tile of ranks No.
////				long tilerowTopLeft = (long) Math.floor((90 - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / Math.abs(adfGeoTransform[5]));
////				long tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / adfGeoTransform[1]);
//				long tilerowTopLeft = (long) Math.floor((90 - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
//				long tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / dstResolution);
//				
//				//calculate the coordinate of the upper left corner
////				double yCoordinate = 90 - tilerowTopLeft * Math.abs(adfGeoTransform[5]) * CoreConfig.HEIGHT_DEFAULT; 
////				double xCoordinate = tilecolTopLeft * adfGeoTransform[1] * CoreConfig.WIDTH_DEFAULT - 180;
//				double yCoordinate = 90 - tilerowTopLeft * dstResolution * CoreConfig.HEIGHT_DEFAULT; 
//				double xCoordinate = tilecolTopLeft * dstResolution * CoreConfig.WIDTH_DEFAULT - 180;
//				
//				//caclute the offet between image's upper left corner and tile's upper left corner
////				tilecolOffset =(int) ((adfGeoTransform[0] - xCoordinate)/adfGeoTransform[1]);
////				tilerowOffset =(int) ((adfGeoTransform[3] - yCoordinate)/adfGeoTransform[5]);
//				tilecolOffset =(int) ((adfGeoTransform[0] - xCoordinate)/dstResolution);
//				tilerowOffset =(int) ((adfGeoTransform[3] - yCoordinate)/(-dstResolution));
//				
//				int actualXOrigin = (int)(xOrigin * dstResolution / adfGeoTransform[1]);
//				int actualYOrigin = (int)(yOrigin * (-dstResolution) / adfGeoTransform[5]);
//				int xReadCoordinateActual = actualXOrigin;
//				int yReadCoordinateActual = actualYOrigin;
//				int xreadActual;
//				int yreadActual;
//				//需要读取的瓦片数据范围
//				int xread;
//				int yread;
//				//已经读取的影像范围
//				int xreaded = 0;
//				int yreaded = 0;
//				int countNum = 0;
//				for(; xreaded < xLength; ) {
//					if (xLength - xreaded < widthRang) {
//						xread = xLength - xreaded;
//					}else if (xreaded == 0) {
//						xread = widthRang - tilecolOffset;
//					}else {
//						xread = widthRang;
//					}
//					if (xreaded == 0) {
//						xWriteOrigin = tilecolOffset;
//					}else {
//						xWriteOrigin = 0; 
//					}
//					xreadActual = (int)(xread * dstResolution / adfGeoTransform[1]);
//					yReadCoordinateActual = actualYOrigin;
//
//					for (yreaded = 0; yreaded < yLength; ) {
//						if (yLength - yreaded < heightRang) {
//							yread = yLength - yreaded;
//						}else if (yreaded == 0) {
//							yread = heightRang - tilerowOffset;
//						}else {
//							yread = heightRang;
//						}
//						if (yreaded == 0) {
//							yWriteOrigin = tilerowOffset;
//						}else {
//							yWriteOrigin = 0;
//						}
//						System.err.println(countNum++);
//						yreadActual = (int)(yread * (-dstResolution) / adfGeoTransform[5]);					
//						if (readData(xReadCoordinateActual, yReadCoordinateActual, xreadActual, yreadActual, xread, yread, buffer)) {
//							bufferImag.setRGB(0, 0, widthRang, heightRang, intZeroAreay, 0, widthRang);
//							byteArrayOut.reset();
//							changRGB(buffer, rgb, xread, yread, false);
//							String filetemp = outputpath + rowColName.getRowName().toString() + "//" + rowColName.getColName().toString();
//							Get getTemp = new Get(Bytes.toBytes(TransCoding.decode(filetemp, "UTF-8")));
//							Result resultTemp = resourceTable.get(getTemp);
//							if (!resultTemp.isEmpty()) {
//								byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
//								getTemp = new Get(md5Byte);
//								resultTemp = resourceTable.get(getTemp);
//								ImageIcon imageIcon = new ImageIcon(resultTemp.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
//								Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
//								gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
//								int rgbArray[] = new int[xread*yread];
//								bufferImag.getRGB(xWriteOrigin, yWriteOrigin, xread, yread, rgbArray, 0, xread);
//								merge(rgbArray, rgb);
//							}
//							
//							bufferImag.setRGB(xWriteOrigin, yWriteOrigin, xread, yread, rgb, 0, xread);
//							ImageUtils.pressText(bufferImag, "ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 15, Color.BLACK, -1, -1, 0.3f);
//							ImageIO.write(bufferImag, "png", byteArrayOut);
//							setRowCol(xreaded, yreaded, dstResolution);
//							path.set(outputpath + rowColName.getRowName().toString());
//							strBuilderMD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));
//							fileInfo.setMD5(strBuilderMD5.toString());
//							fileInfo.setLength(byteArrayOut.size());
//							fileInfo.setFilename(rowColName.getColName().toString());
//							Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
//							Result result = resourceTable.get(get);
//							if (result == null || result.isEmpty()) {
//								Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
//								put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//										GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
//								put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//										GtDataConfig.RESOURCE.DATA.byteVal, byteArrayOut.toByteArray());
//								resourceTable.put(put);
//							}
////							System.out.println(path.toString() + " " + fileInfo.getFilename().toString());
//							context.write(path, fileInfo);
//							strBuilderMD5.delete(0, strBuilderMD5.length());
//						} else{
//							if(dataset != null) {
//								dataset.delete();
//								dataset = null;
//							}
//							return;
//						}
//						yreaded += yread;
//						yReadCoordinateActual += yreadActual;
//					}
//					xreaded += xread;
//					xReadCoordinateActual += xreadActual;
//				}
//				if(dataset != null) {
//					dataset.delete();
//					dataset = null;
//				}
//			}
//		}
		

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			super.cleanup(context);
			resourceTable.flushCommits();
			resourceTable.close();
			if(dataset != null) {
				dataset.delete();
			}
			Configuration conf = context.getConfiguration();
			HTable metaTable = new HTable(conf,
					CoreConfig.MAP_META_TABLE);
			metaTable.setAutoFlush(false, true);
			//insert the Layers information into meta table
			StringBuilder rowkeyStrBuilder = new StringBuilder(conf.get(CoreConfig.CUTTING_OUTPUTPATH));
			rowkeyStrBuilder.insert(rowkeyStrBuilder.lastIndexOf("/L"), "/");
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
			metaTable.close();
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
				rowkey.replace(rowkeyPrefixLen, rowkey.length(), "//"
						+ val.getFilename().toString());
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

	public int run(String[] args) throws Exception {
		gdal.AllRegister();
		Configuration conf = HBaseConfiguration.create();
		int layersCount = Integer.parseInt(args[3]);
//		Dataset hDataset = gdal.Open(args[1], gdalconstConstants.GA_ReadOnly);
		Dataset hDataset = gdal.Open("D://nanlin//guangzhou_051401.tiff", gdalconstConstants.GA_ReadOnly);
		if (hDataset == null) {
			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
			LOG.info(gdal.GetLastErrorMsg());
			return 1;
		}
		double[] adfGeoTransform = hDataset.GetGeoTransform();
		double dstResolution = adfGeoTransform[1];
		for(int i = 0; i < 21; i++) {
			if (dstResolution - CoreConfig.LAYERS_RESOLUTION[i] == 0.000000000000000000) {
				layersCount = i;
				conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, CoreConfig.LAYERS_RESOLUTION[i]);
				System.out.println(CoreConfig.LAYERS_RESOLUTION[i]);
				break;
			} else if (dstResolution > CoreConfig.LAYERS_RESOLUTION[i]){
				layersCount =  i-1;
				conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, CoreConfig.LAYERS_RESOLUTION[i-1]);
				System.out.println(CoreConfig.LAYERS_RESOLUTION[i-1]);
				break;
			}
		}
		hDataset.delete();

		int totalJobNum = layersCount + 1;
		String urlcode = TransCoding.UrlEncode(args[2], "utf-8");
		String jobCount = "" + totalJobNum;
		StringBuilder layerName = new StringBuilder(
				Integer.toHexString(layersCount));
		if (layerName.length() == 1) {
			layerName.insert(0, "L0");
		} else {
			layerName.insert(0, "L");
		}
		
		
		if ( args[2].endsWith("/")){
			layerName.insert(0, urlcode);
		}else{
			layerName.insert(0, urlcode + "/");
		}
		
		
		conf.set(CoreConfig.JOBID, args[0]);
		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
		conf.set(CoreConfig.CUTTING_OUTPUTPATH, layerName.toString());
		conf.set(TableOutputFormat.OUTPUT_TABLE,
				CoreConfig.MAP_META_TABLE);
		Job job = Job.getInstance(conf);

		FileInputFormat.addInputPath(job, new Path(args[1]));
		job.setMapperClass(ImageSegMapper.class);
		job.setReducerClass(MetaTableInsertReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FileInfo.class);
		job.setInputFormatClass(ImageBlockInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setJarByClass(ImageSegement.class);
		job.setNumReduceTasks(6);
		job.setJobName("1/" + jobCount);

		int status = job.waitForCompletion(true) ? 0 : 1;
		//int status = 0;
		args[2] = jobCount;
		while (status == 0 && layersCount > 0) {

			LOG.info("imageSegementation of the " + layersCount
					+ " is sucessed");
			args[1] = layerName.toString();
			LOG.info("the layer for read: " + args[1]);
			String temp = null;
			
			
			LOG.info("imageSegementation of the " + layersCount
					+ " processing........");
			if (layersCount == 1) {
				layersCount--;
				status = new MergingResamplingMD5().run(args);
			}else {
				layersCount -= 2;
				status = new MergeingResamplingMutil().run(args);
			}
			if (layersCount < 16) {
				temp = "L0" + Integer.toHexString(layersCount);
			} else {
				temp = "L" + Integer.toHexString(layersCount);
			}
			layerName.replace(layerName.length() - 3, layerName.length(), temp);
		}
		if (0 == status) {
			LOG.info("imageSegementation of the " + layersCount
					+ " is sucessed");
		}
		
		return status;
	}

	public static void main(String[] args) throws Exception {
		args = new String[4];
		args[0] = "123456";
		args[1] = "hdfs://192.168.2.3:8020/nanlin/tiff/t1.tiff";
		args[2] = "/2015051502/Layers/_alllayers";
		args[3] = "1";
		if (args.length < 4) {
			LOG.info("usage: <rowkey> <input_path> <gt-data ouput path(../../_alllayers)> <the total number of layers>");
		} else {
			int status = ToolRunner.run(new ImageSegement(), args);
			System.exit(status);
		}
	}

}
