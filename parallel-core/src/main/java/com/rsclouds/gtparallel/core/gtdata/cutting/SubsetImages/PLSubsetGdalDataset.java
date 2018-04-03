package com.rsclouds.gtparallel.core.gtdata.cutting.SubsetImages;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.ImageUtils;

public class PLSubsetGdalDataset {
	public static int PNG_FORMAT_INT = 0;
	public static int JPEG_FORMAT_INT = 1;
	public static String PNG_FORMAT_NAME = "png";
	public static String JPEG_FORMAT_NAME = "jpeg";

	private byte[]  buffer;       //存放影像数据
	private int[]   rgb;          //存放像素的值
	private int[]   bands;        //读取影像的顺序
	private boolean nodataFlag;   //是否有设置nodata
	private byte[]  nodataValues; //nodata的值
	private double  percentage;   //0值百分比，超过该值的瓦片将被抛弃
	private int     bandCount;
	private int[]   intZeroAreay;

	private BufferedImage bufferImagePNG;
	private BufferedImage bufferImageJPEG;
	private int bufferImageWidth;
	private int bufferImageHeight;
	private ByteArrayOutputStream byteArrayOut;
	private double noneZeroPercentage;
	
	private Dataset dataset;
	
	public PLSubsetGdalDataset() {
		int imageWidth = CoreConfig.WIDTH_DEFAULT;
		int imageHeight = CoreConfig.HEIGHT_DEFAULT;
		bufferImagePNG = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB_PRE);
		bufferImageJPEG = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
	}
	
	public PLSubsetGdalDataset(double percentage, int imageWidth, int imageHeight, int[] bands, int nodata) {
		this.percentage = percentage;
		this.bufferImageWidth = imageWidth;
		this.bufferImageHeight = imageHeight;
		this.bands = bands;
		bandCount = bands.length;
		
		rgb = new int[ imageWidth * imageHeight];
		intZeroAreay = new int[imageWidth * imageHeight];
		for(int i = 0; i < intZeroAreay.length; i ++)
			intZeroAreay[i] = 0;
		
		buffer = new byte[imageWidth * imageHeight * bands.length];
		if (nodata != -1) {
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
	
	public boolean merge(int[] merge, int widthInt, int heightInt) {
		bufferImagePNG.getRGB(0, 0, bufferImageWidth, bufferImageHeight, rgb, 0, bufferImageWidth);
		int length = bufferImageWidth * bufferImageHeight;
		int count = 0;
		for(int i = 0; i < length; i ++) {
			if (rgb[i] == 0x00000000 || rgb[i] == 0xff000000) {
				rgb[i] = merge[i];
				if (rgb[i] == 0x00000000 || rgb[i] == 0xff000000) {
					count ++;
				}
			}
		}
		double zeroPercentage = (count * 1.0) /(bufferImageHeight * bufferImageWidth);
		if (zeroPercentage - percentage < 0.000) {
			return true;
		}
		return false;
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
	 * @breif 将按波段读取的二进制数据 转为 rgb格式， buffer存储的数据为一个瓦片256*256的（1/n），n为1，2，4，8，。。。。（2的n次方）
	 * @param 将buffer转换成像素格式的数据存放在以（xOrg， yOrg）为起始位置，宽为	width，高为 height的范围内
	 * @param xOrg  
	 * @param yOrg
	 * @param width     
	 * @param height    
	 * @param zeroFlag   是否做零值检查
	 * @return           如果0值像素所占比值小于预定阀值percentage， 返回成功，否则返回失败
	 */
	public boolean changRGB(int xOrg, int yOrg, int width, int height, boolean zeroFlag ) {
		int noZeroCount = 0;
		boolean alphaPix = false;
		
		int rgbOrgIndex = xOrg + yOrg * this.bufferImageWidth;
		int rgbIndex;
		int band_step = width * height;
		int buffer_step = 0;
		for (int i = 0; i < height; i ++) {
			buffer_step = i * width;
			for (int j = 0; j < width; j ++) {
				rgbIndex = rgbOrgIndex + i * this.bufferImageWidth + j;
				rgb[rgbIndex] = (0x00000000);//像素设置为透明
				alphaPix = true;
				//如果成功获取影像数据的nodata值，并且该像素对应的所有波段的值都为nodata，则需要将该像素置为透明
				if(nodataFlag) {
					for(int band = 0; band < bandCount; band ++) {
						if(buffer[buffer_step + band_step * band] != nodataValues[bands[band]-1]) {
							alphaPix = false;
							rgb[rgbIndex]= (0xff000000);
							break;
						}
					}
				}else {
					rgb[rgbIndex]= (0xff000000);//像素设置为不透明
					alphaPix = false;
				}
				for(int band = 0; band < bandCount && !alphaPix; band ++) {
					rgb[rgbIndex] += ((buffer[buffer_step + band_step * band] & 0x000000ff) << 8 * (bandCount - band -1));
				}
				if(rgb[rgbIndex] == 0x00000000 || rgb[rgbIndex] == 0xff000000 || alphaPix) {
					
				}else {
					noZeroCount ++;
				}
				buffer_step ++;	
			}
		}
		
		if(zeroFlag) {		
			noneZeroPercentage += ((noZeroCount * 1.0) / (bufferImageWidth * bufferImageHeight));
			double zeroPercentage = 1 - noneZeroPercentage;
			if (zeroPercentage - percentage <= 0.000) {
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
	 * @breif 将按波段读取的二进制数据 转为 rgb格式， buffer存储的数据为一个瓦片256*256的（1/n），n为1，2，4，8，。。。。（2的n次方）
	 * @param 将buffer转换成像素格式的数据存放在以（xOrg， yOrg）为起始位置，宽为	width，高为 height的范围内
	 * @param xOrg  
	 * @param yOrg
	 * @param width     
	 * @param height    
	 * @return           如果0值像素所占比值小于预定阀值percentage， 返回成功，否则返回失败
	 */
	public void changRGB(int xOrg, int yOrg, int width, int height) {
		boolean alphaPix = false;		
		int rgbOrgIndex = xOrg + yOrg * this.bufferImageWidth;
		int rgbIndex;
		int band_step = width * height;
		int buffer_step = 0;
		for (int i = 0; i < height; i ++) {
			buffer_step = i * width;
			for (int j = 0; j < width; j ++) {
				rgbIndex = rgbOrgIndex + i * this.bufferImageWidth + j;
				rgb[rgbIndex] = (0x00000000);//像素设置为透明
				alphaPix = true;
				//如果成功获取影像数据的nodata值，并且该像素对应的所有波段的值都为nodata，则需要将该像素置为透明
				if(nodataFlag) {
					for(int band = 0; band < bandCount; band ++) {
						if(buffer[buffer_step + band_step * band] != nodataValues[bands[band]-1]) {
							alphaPix = false;
							rgb[rgbIndex]= (0xff000000);
							break;
						}
					}
				}else {
					rgb[rgbIndex]= (0xff000000);//像素设置为不透明
					alphaPix = false;
				}
				for(int band = 0; band < bandCount && !alphaPix; band ++) {
					rgb[rgbIndex] += ((buffer[buffer_step + band_step * band] & 0x000000ff) << 8 * (bandCount - band -1));
				}
				buffer_step ++;	
			}
		}
	}
	
	public boolean compareZeroPercentage() {
		int rgbIndex = 0;
		int zeroCount = 0;
		for (int i = 0; i < bufferImageWidth; i ++) {
			for (int j = 0; j < bufferImageHeight; j ++) {
				if(rgb[rgbIndex] == 0x00000000 || rgb[rgbIndex] == 0xff000000) {
					zeroCount ++;
				}
				rgbIndex++;
			}
		}
		double zeroPercentage = (zeroCount * 1.0) /(bufferImageHeight * bufferImageWidth);
		if (zeroPercentage - percentage < 0.000) {
			return true;
		}
		return false;
	}
	
	public void  resetNoneZeroPercentage() {
		noneZeroPercentage = 0.000000000000000000000000;
	}
		
//	private void restBuffer() {
//		for (int i = 0; i < buffer.length; i ++) {
//			buffer[i] = 0;
//		}
//	}
//	private void restBuffer(int size) {
//		for (int i = 0; i < size; i ++) {
//			buffer[i] = 0;
//		}
//	}
	/**
	 * @breif 将影像重采样为 xsize * ysize大小的影像
	 * @return
	 */
	public boolean readData(String filePath, int xsize, int ysize) {
		boolean resultFlag = false;
		if (dataset == null) {
			gdal.AllRegister();
			filePath = filePath.replace("file:/", "");
			this.dataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);
			if (dataset == null) {
				System.out.println(filePath);
				return resultFlag;
			}
		}
		int width = dataset.GetRasterXSize();
		int height = dataset.GetRasterYSize();
		//System.out.println("[readData1] " + xsize + " " + ysize);
		if (dataset.ReadRaster(0, 0, width, height, xsize, ysize, 
				gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
			resultFlag = true;
		}else {
			System.out.println(filePath);
		}
		return resultFlag;
	}
	
	public boolean readData(String filePath, int displacementCol, int displacementRow, int times, int widthRang, int heightRang) {
		boolean resultFlag = false;
		if (dataset == null) {
			gdal.AllRegister();
			filePath = filePath.replace("file:/", "");
			this.dataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);
			if (dataset == null) {
				System.out.println("open error" + filePath);
				return false;
			}
		}
		int wigth = dataset.GetRasterXSize();
		int heigth = dataset.getRasterYSize();
		int titleWigth = wigth / times;
		int titleHeight = heigth / times;
		int xOrigin = displacementCol * titleWigth;
		int yOrigin = displacementRow * titleHeight;
		if (displacementCol == times -1) {
			titleWigth = wigth - xOrigin;
		}
		if (displacementRow == times - 1) {
			titleHeight = heigth - yOrigin;
		}
		//System.out.println("[readdata2] yOrigin=" + xOrigin + " yOrigin=" + yOrigin + " titleWigth=" + titleWigth + "titleHeight=" + titleHeight);
		if (dataset.ReadRaster(xOrigin, yOrigin, titleWigth, titleHeight, widthRang, heightRang, 
				gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
			resultFlag = true;
		}else {
			System.out.println("read error" + filePath);
		}
		return resultFlag;
	}
	
	public void closeDataset() {
		if (dataset != null) {
			dataset.delete();
			dataset = null;
		}
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
	
	public void resetRGB() {
		for (int i = 0; i < rgb.length; i ++) {
			rgb[i] = 0;
		}
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

	public int getBufferImageSize() {
		return byteArrayOut.size();
	}

	
}
