package com.rsclouds.gtparallel.core.gtdata.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.Geometry;

import com.rsclouds.gtparallel.core.common.CoreConfig;

public class GdalDatasetBase {
	public static int PNG_FORMAT_INT = 0;
	public static int JPEG_FORMAT_INT = 1;
	public static String PNG_FORMAT_NAME = "png";
	public static String JPEG_FORMAT_NAME = "jpeg";

	private Dataset dataset;      //影像数据集对象
	private byte[]  buffer;       //存放影像数据
	private int[]   rgb;          //存放像素的值
	private int[]   bands;        //读取影像的顺序
	private boolean nodataFlag;   //是否有设置nodata
	private byte[]  nodataValues; //nodata的值
	private double  percentage;   //0值百分比，超过该值的瓦片将被抛弃
	private int     bandCount;
	private int     colorInterp;
	private int[]   intZeroAreay;
	private int     width;
	private int     height;
	private double[] adfGeoTransform;

	private BufferedImage bufferImagePNG;
	private BufferedImage bufferImageJPEG;
	private int bufferImageWidth;
	private int bufferImageHeight;
	private ByteArrayOutputStream byteArrayOut;
	
	public GdalDatasetBase() {
		int imageWidth = CoreConfig.WIDTH_DEFAULT;
		int imageHeight = CoreConfig.HEIGHT_DEFAULT;
		bufferImagePNG = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB_PRE);
		bufferImageJPEG = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
	}
	
	public GdalDatasetBase(String imgaPath, double percentage, int imageWidth, int imageHeight, int mutil, int[] bands, int nodata) {
		gdal.AllRegister();
		this.dataset = gdal.Open(imgaPath, gdalconst.GA_ReadOnly);
		this.percentage = percentage;
		this.bufferImageWidth = imageWidth;
		this.bufferImageHeight = imageHeight;
		this.bands = bands;
		bandCount = bands.length;
		if(dataset != null) {
			colorInterp = dataset.GetRasterBand(1).GetColorInterpretation();
			width = dataset.GetRasterXSize();
			height = dataset.GetRasterYSize();
			adfGeoTransform = dataset.GetGeoTransform();
			if(dataset.getRasterCount() == 1) {
				this.bands = new int[1];
				this.bands[0] = 1;
				bandCount = 1;
			}
		}
		
		rgb = new int[ imageWidth * imageHeight];
		intZeroAreay = new int[imageWidth * imageHeight];
		for(int i = 0; i < intZeroAreay.length; i ++)
			intZeroAreay[i] = 0;
		
		buffer = new byte[imageWidth * imageHeight * bands.length * mutil];
		setNodata();
		if (nodata != -1 && nodataFlag == false) {
			nodataFlag = true;
			nodataValues = new byte[bandCount];
			for(int i = 0; i < bandCount; i ++) {
				nodataValues[i] = (byte)nodata;
			}
		}
		byteArrayOut = new ByteArrayOutputStream();
		bufferImagePNG = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB_PRE);
		bufferImageJPEG = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
	}
	
	public void setNodata() {
		nodataFlag = true;
		if(dataset != null || this.bands != null) {
			nodataValues = new byte[bandCount];
			Double[] nodata = new Double[1];
			for(int i = 1; i <= bandCount; i ++) {
				dataset.GetRasterBand(bands[i-1]).GetNoDataValue(nodata);
				if(nodata[0] == null){
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
		}
	}
	
	
	public void close() {
		if(dataset != null)
			dataset.delete();
		dataset = null;
	}
	
	public boolean merge(int[] merge, int widthInt, int heightInt) {
		bufferImagePNG.getRGB(0, 0, bufferImageWidth, bufferImageHeight, rgb, 0, bufferImageWidth);
		int length = bufferImageWidth * bufferImageHeight;
		boolean zeroFlag = false;
		for(int i = 0; i < length; i ++) {
			if ((rgb[i] & 0xff000000) == 0) {
				rgb[i] = merge[i];
				if ((rgb[i] & 0xff000000) == 0) {
					zeroFlag = true;
				}
			}
		}
		return zeroFlag;
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
	public int merge(int[] merge, int xorigin, int yorigin, int widthInt, int heightInt, int format) {
		bufferImagePNG.setRGB(xorigin, yorigin, widthInt, heightInt, rgb, 0, widthInt);
		bufferImagePNG.getRGB(0, 0, bufferImageWidth, bufferImageHeight, rgb, 0, bufferImageWidth);
		int length = bufferImageWidth * bufferImageHeight;
		boolean zeroFlag = false;
		for(int i = 0; i < length; i ++) {
			if ((rgb[i] & 0xff000000) == 0) {
				rgb[i] = merge[i];
				if ((rgb[i] & 0xff000000) == 0) {
					zeroFlag = true;
				}
			}
		}
		bufferImagePNG.setRGB(0, 0, bufferImageWidth, bufferImageHeight, rgb, 0, bufferImageWidth);
		if (!zeroFlag || format == JPEG_FORMAT_INT) {
			bufferImageJPEG.createGraphics().drawImage(bufferImagePNG, 0, 0, Color.WHITE, null);
			format = JPEG_FORMAT_INT;
		}
		return format;
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
	public boolean changRGB(int rgb_xsize, int rgb_ysize, int xreaded, int xLength, boolean zeroFlag ) {
		double zeroPercentage = 0.0;
		int zeroCount = 0;
		boolean alphaPix = false;
		if (colorInterp != 2) {
			int band_step = rgb_ysize * xLength;
			int rgb_step = 0;
			int buffer_step = 0;
			for (int i = 0; i < rgb_ysize; i ++) {
				buffer_step = i * xLength + xreaded;
				for (int j = 0; j < rgb_xsize; j ++) {
					rgb[rgb_step] = (0x00000000);//像素设置为透明
					alphaPix = true;
					//如果成功获取影像数据的nodata值，并且该像素对应的所有波段的值都为nodata，则需要将该像素置为透明
					if(nodataFlag) {
						for(int band = 0; band < bandCount; band ++) {
							if(buffer[buffer_step + band_step * band] != nodataValues[bands[band]-1]) {
								alphaPix = false;
								rgb[rgb_step]= (0xff000000);
								break;
							}
						}
					}else {
						rgb[rgb_step]= (0xff000000);//像素设置为不透明
						alphaPix = false;
					}
					for(int band = 0; band < bandCount && !alphaPix; band ++) {
						rgb[rgb_step] += ((buffer[buffer_step + band_step * band] & 0x000000ff) << 8 * (bandCount - band -1));
					}
					if(rgb[rgb_step] == 0x00000000 || rgb[rgb_step] == 0xff000000 || alphaPix) {
						zeroCount ++;
					}
					rgb_step ++;
					buffer_step ++;	
				}
			}
		}else {
			int count = rgb_xsize * rgb_ysize;
			for(int i = 0; i < count; i++ ) {
				if((rgb[i] & 0xff000000) == 0) {
					zeroCount ++;
				}
			}
		}
		if(zeroFlag) {		
			zeroCount += bufferImageWidth * bufferImageHeight - rgb_xsize * rgb_ysize;
			zeroPercentage = zeroCount * 1.0 / (bufferImageWidth * bufferImageHeight * 1.0);
			if(zeroPercentage - percentage <= 0.000){
				return true;
			}else {
				return false;
			}
		}
		else
		{
			return true;
		}
	}
	
	
	/**
	 * @breif 将按波段读取的二进制数据 转为 rgb格式， 每次只转一部分。比如buffer存储的是256 * 5120范围的数据，那么第一次将一个256*256范围的对应的buffer转为rgb
	 * 第二次将第二个256*256的对应的buffer转为rgb，依次类推，直至将整个buffer转为256*256的rgb，同时统计0值得比值
	 * @param x			   瓦片左上角经度
	 * @param y          瓦片左上角纬度
	 * @param resolution 分辨率
	 * @param buffer	 
	 * @param rgb        存储 宽为rgb_xsize， 长为rgb_ysize的影像数据
	 * @param rgb_xsize  
	 * @param rgb_ysize
	 * @param xreaded    buffer所存储的影像数据在X方向上已读的长度
	 * @param xLength    buffer所存储的影像数据在X方向上的长度
	 * @param Geometry   读取矢量范围，矢量范围内的值保留不变，矢量范围外的设置为透明
	 * @return           
	 */
	public boolean changRGB(double x, double y, double resolution, int rgb_xsize, int rgb_ysize, int xreaded, int xLength, Geometry geo ) {
		double x_coor1, x_coor3;
		double y_coor1, y_coor3;
		Geometry geoClone = geo.Clone();
		GeometryBase geoBase = new GeometryBase();
		if (colorInterp != 2) {
			int band_step = rgb_ysize * xLength;
			int rgb_step = 0;
			int buffer_step = 0;
//			double[] xArray = new double[4];
//			double[] yArray = new double[4];
			for (int i = 0; i < rgb_ysize; i ++) {
				buffer_step = i * xLength + xreaded;
				y_coor1 = y - i * resolution;
				y_coor3 = y - (i+1) * resolution;
				
				for (int j = 0; j < rgb_xsize; j ++) {
					x_coor1 = x + j * resolution;
					x_coor3 = x + (j+1) * resolution;
//					xArray[0] =  x_coor1;
//					xArray[1] =  x_coor3;
//					xArray[2] =  x_coor3;
//					xArray[3] =  x_coor1;
//					yArray[0] = y_coor1;
//					yArray[1] = y_coor1;
//					yArray[2] = y_coor3;
//					yArray[3] = y_coor3;
					Geometry point1 = new Geometry(1);
					point1.AddPoint(x_coor1, y_coor1);
					Geometry point2 = new Geometry(1);
					point2.AddPoint(x_coor1, y_coor3);
					Geometry point3 = new Geometry(1);
					point3.AddPoint(x_coor3, y_coor3);
					Geometry point4 = new Geometry(1);
					point4.AddPoint(x_coor3, y_coor1);

					rgb[rgb_step] = (0x00000000);//像素设置为透明
					if (geoClone.Intersects(point1) || geoClone.Intersects(point2)
							|| geoClone.Intersects(point3) || geoClone.Intersects(point4) ) {
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
					}
					point1.delete();
					point2.delete();
					point3.delete();
					point4.delete();
					rgb_step ++;
					buffer_step ++;	
				}
			}
		}else {
			return false;
		}
		return true;
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
	public boolean readData(int xOrigin, int yOrigin, int xsize, int ysize, int buf_xsize, int buf_ysize) {
		if(dataset == null)
			return false;
		boolean resultFlag = false;
		if (colorInterp == 2) {
			int count = buf_xsize*buf_ysize;
			if (dataset.ReadRaster(xOrigin, yOrigin, xsize, ysize, buf_xsize, buf_ysize, gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
				Band band = dataset.GetRasterBand(1);
				for (int i = 0; i < count; i ++ ) {
					rgb[i] =  band.GetColorTable().GetColorEntry((buffer[i] & 0xff)).getRGB();
				}
				resultFlag = true;
			}
		}
		else {				
			if (dataset.ReadRaster(xOrigin, yOrigin, xsize, ysize, buf_xsize, buf_ysize, 
					gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
				resultFlag = true;
			}
		}
		return resultFlag;
	}
	
	
	public void setWarterMask(String pressText,String fontName, int fontStyle, int fontSize,
			Color color, int x, int y, float alpha, int format) {
		if (format == JPEG_FORMAT_INT) {
			ImageUtils.pressText(bufferImageJPEG, pressText, fontName, fontStyle, fontSize, color, x, y, alpha);
		}else {
			ImageUtils.pressText(bufferImagePNG, pressText, fontName, fontStyle, fontSize, color, x, y, alpha);
		}
	}
	
	public void resetBufferedImage() {
		bufferImagePNG.setRGB(0, 0, bufferImageWidth, bufferImageHeight, intZeroAreay, 0, bufferImageWidth);
		bufferImageJPEG.setRGB(0, 0, bufferImageWidth, bufferImageHeight, intZeroAreay, 0, bufferImageWidth);
	}
	

	public void setBufferedImage(int xorigin, int yorigin, int widthInt, int heightInt, int format) {
		bufferImagePNG.setRGB(xorigin, yorigin, widthInt, heightInt, rgb, 0, widthInt);
		if (format == JPEG_FORMAT_INT) {
			bufferImageJPEG.createGraphics().drawImage(bufferImagePNG, 0, 0, Color.WHITE, null);
		}
	}
	
	public void setBufferedImage(ImageIcon imageIcon, int format) {
		this.resetBufferedImage();
		if (format == JPEG_FORMAT_INT) {
			bufferImageJPEG.createGraphics().drawImage(imageIcon.getImage(), 0, 0, Color.WHITE, null);
		}else {
			Graphics2D gs = (Graphics2D) this.bufferImagePNG.getGraphics();
			gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
			gs.dispose();
		}
	}
	
	public byte[] getBufferImagedata(int format) {
		try {
			byteArrayOut.reset();
			if (format == JPEG_FORMAT_INT) {
				ImageIO.write(bufferImageJPEG, JPEG_FORMAT_NAME, byteArrayOut);
			}else {
				ImageIO.write(bufferImagePNG, PNG_FORMAT_NAME, byteArrayOut);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return byteArrayOut.toByteArray();
	}
	
	public ByteArrayOutputStream getBufferImageByteStream(int format) {
		try {
			byteArrayOut.reset();
			if (format == JPEG_FORMAT_INT)
				ImageIO.write(bufferImageJPEG, JPEG_FORMAT_NAME, byteArrayOut);
			else {
				ImageIO.write(bufferImagePNG, PNG_FORMAT_NAME, byteArrayOut);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return byteArrayOut;
	}
	

	public boolean write2LocalFile(String outputPath, int format) {
		try {
			File file = new File(outputPath);
			if (format == JPEG_FORMAT_INT)
				ImageIO.write(bufferImageJPEG, JPEG_FORMAT_NAME, file);
			else {
				ImageIO.write(bufferImagePNG, PNG_FORMAT_NAME, file);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
	public double[] getAdfGeoTransform() {
		return adfGeoTransform;
	}

	public int getBufferImageSize() {
		return byteArrayOut.size();
	}
}
