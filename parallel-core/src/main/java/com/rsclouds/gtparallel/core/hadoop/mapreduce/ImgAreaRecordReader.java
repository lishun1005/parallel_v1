package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

import com.rsclouds.gtparallel.core.gtdata.common.ImageUtils;
import com.rsclouds.gtparallel.core.hadoop.io.AdfGeoTransformArray;

public class ImgAreaRecordReader extends
		RecordReader<AdfGeoTransformArray, BytesWritable> {

	private ImgAreaSplit filesplit;
	private boolean processed = false;
	private BytesWritable value = new BytesWritable();
	private AdfGeoTransformArray key = new AdfGeoTransformArray();
	private String filePath;

	@Override
	public void close() throws IOException {
	}

	@Override
	public AdfGeoTransformArray getCurrentKey() throws IOException,
			InterruptedException {
		return this.key;
	}

	@Override
	public BytesWritable getCurrentValue() throws IOException,
			InterruptedException {
		return this.value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return processed ? 0 : 1;
	}

	
	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		filesplit = (ImgAreaSplit) split;
		String inputFilePath = filesplit.getPath().toString();

		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		Dataset hDataset = gdal.Open(inputFilePath, gdalconstConstants.GA_ReadOnly);
		
		

		int rasterCount = hDataset.GetRasterCount(); 
//		if (rasterCount == 4)
//			rasterCount = 3;
		double[] adfGeoTransform = hDataset.GetGeoTransform();
		int xOrigin = filesplit.getxOrigin();
		int yOrigin = filesplit.getyOrigin();
		adfGeoTransform[0] = adfGeoTransform[0] + xOrigin
				 * adfGeoTransform[1] + yOrigin
				 * adfGeoTransform[2];
		 adfGeoTransform[3] = adfGeoTransform[3] + xOrigin
		 * adfGeoTransform[4] + yOrigin
		 * adfGeoTransform[5];
		
		key.setAdfGeoTransform(adfGeoTransform);

		Driver poDriver = gdal.GetDriverByName("PNG");
		Driver driTiff = gdal.GetDriverByName("GTiff");
		Dataset hDatasetPng = poDriver.CreateCopy("/home/yarn/" + xOrigin + "_" + yOrigin + "1.png", hDataset);
		hDatasetPng.delete();
		Dataset tiffDataset = null;
		String tiffTemp = "/home/yarn/" + xOrigin + "_" + yOrigin + ".tiff";

		filePath = "/home/yarn/" + xOrigin + "_" + yOrigin + ".png";
		int widthrang = filesplit.getxLength();
		int heightrang = filesplit.getyLength();
		int num = widthrang * heightrang;
		int readDataType = hDataset.GetRasterBand(1).getDataType();
		int writeDataType = gdalconst.GDT_Byte;
		if (readDataType == 3 || readDataType == 2) {
			writeDataType = gdalconst.GDT_UInt16;
		}

		
		boolean processed_cuting = false;
		if (1 == rasterCount) {
			Band band = hDataset.GetRasterBand(1);
			int colorInterp = band.GetColorInterpretation();
			if (2 == colorInterp) {// colorInterp = Palette
				byte[] b = new byte[num];
				processed_cuting = true;
				byte[] bandbyte = new byte[num];
				band.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
						gdalconst.GDT_Byte, b);
				tiffDataset = driTiff.Create(tiffTemp, widthrang,
						heightrang, 3, gdalconst.GDT_Byte);
				int arraySize = widthrang * heightrang;
				for (int j = 0; j < arraySize; j++) {
					bandbyte[j] = (byte) band.GetColorTable()
							.GetColorEntry((b[j] & 0xff)).getRed();
				}
				tiffDataset.GetRasterBand(1).WriteRaster(0, 0, widthrang,
						heightrang, gdalconst.GDT_Byte, bandbyte);

				for (int j = 0; j < arraySize; j++) {
					bandbyte[j] = (byte) band.GetColorTable()
							.GetColorEntry((b[j] & 0xff)).getGreen();
				}
				tiffDataset.GetRasterBand(2).WriteRaster(0, 0, widthrang,
						heightrang, gdalconst.GDT_Byte, bandbyte);

				for (int j = 0; j < arraySize; j++) {
					bandbyte[j] = (byte) band.GetColorTable()
							.GetColorEntry((b[j] & 0xff)).getBlue();
				}
				tiffDataset.GetRasterBand(3).WriteRaster(0, 0, widthrang,
						heightrang, gdalconst.GDT_Byte, bandbyte);

				Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
						tiffDataset);
				poDataset.FlushCache();
				poDataset.delete();
				tiffDataset.delete();
				

			}
		}
		if (!processed_cuting) {
			tiffDataset = driTiff.Create(tiffTemp, widthrang,
					heightrang, rasterCount, writeDataType);
			if (readDataType == gdalconst.GDT_Byte) {
				byte[] b = new byte[num];
				for (int i = 1; i <= rasterCount; i++) {
					Band band_i = hDataset.GetRasterBand(i);
					band_i.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
							readDataType, b);
					tiffDataset.GetRasterBand(i).WriteRaster(0, 0, widthrang,
							heightrang, writeDataType, b);
				}
			}
			else
			{
				double[] buffer = new double[widthrang];
				byte[] byteBuffer = new byte[widthrang];
				double[] minmax = new double[2];
				for (int i = 1; i <= rasterCount; i++) {
					Band band_i = hDataset.GetRasterBand(i);
					band_i.ComputeRasterMinMax(minmax, 0);
					System.out.println(minmax[0] + " " + minmax[1]);
					for(int yoffset = 0; yoffset < heightrang; yoffset ++) {
						band_i.ReadRaster(xOrigin, yOrigin+yoffset, widthrang, 1,
								gdalconst.GDT_Float64, buffer);
						for(int j = 0; j < widthrang; j ++) {
							byteBuffer[j] = (byte)(( (buffer[j]-minmax[0]) / (minmax[1] - minmax[0] + 1) * 256 ));
						}
						tiffDataset.GetRasterBand(i).WriteRaster(0, yoffset, widthrang,
								1, writeDataType, byteBuffer);
					}
				}
			}
			Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
					tiffDataset);
			poDataset.FlushCache();
			poDataset.delete();
			tiffDataset.delete();
			driTiff.Delete(tiffTemp);
		}
		hDataset.delete();
		gdal.GDALDestroyDriverManager();
		ImageUtils.pressText(filePath, "ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 100, Color.BLACK, 1650, 1200, 0.3f);
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if (!processed) {
			File file = new File(filePath);
			int length = (int)file.length();
			InputStream in = new FileInputStream(file);
			byte[] content = new byte[length];
			int off = 0;
			int readLen;
			while( (readLen = in.read(content, off, length)) != 0){
				off += readLen;
				length -= readLen;
			}
			value.set(content, 0, content.length);
			in.close();
			//file.delete();
			processed = true;
			return true;
		}
		return false;
	}
	
	
	
	
	
	
	
	
	/*
	
	
	@Override
	public void initialize(InputSplit split, TaskAttemptContext context) {
		filesplit = (ImgAreaSplit) split;
		String inputFilePath = filesplit.getPath().toString();

		gdal.AllRegister();
		// support the chinese language
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		Dataset hDataset = gdal.Open(inputFilePath,
				gdalconstConstants.GA_ReadOnly);
		int rasterCount = hDataset.GetRasterCount();
		double[] adfGeoTransform = hDataset.GetGeoTransform();
		int xOrigin = filesplit.getxOrigin();
		int yOrigin = filesplit.getyOrigin();
		adfGeoTransform[0] = adfGeoTransform[0] + xOrigin * adfGeoTransform[1]
				+ yOrigin * adfGeoTransform[2];
		adfGeoTransform[3] = adfGeoTransform[3] + xOrigin * adfGeoTransform[4]
				+ yOrigin * adfGeoTransform[5];

		key.setAdfGeoTransform(adfGeoTransform);

		Driver poDriver = gdal.GetDriverByName("PNG");
		Driver drimen = gdal.GetDriverByName("MEM");
		Dataset menDataset = null;

		filePath = "/home/yarn/" + xOrigin + "_" + yOrigin + ".png";
		int widthrang = filesplit.getxLength();
		int heightrang = filesplit.getyLength();
		int num = widthrang * heightrang;
		int readDataType = hDataset.GetRasterBand(1).getDataType();

		boolean processed_cuting = false;
		if (1 == rasterCount) {
			Band band = hDataset.GetRasterBand(1);
			int colorInterp = band.GetColorInterpretation();
			if (2 == colorInterp) {// colorInterp = Palette
				processed_cuting = true;
				byte[] bandbyte = new byte[num];
				byte[] b = new byte[num];
				band.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
						gdalconst.GDT_Byte, b);
				menDataset = drimen.Create(filePath.toString(), widthrang,
						heightrang, 3, gdalconst.GDT_Byte);
				int arraySize = widthrang * heightrang;
				for (int j = 0; j < arraySize; j++) {
					bandbyte[j] = (byte) band.GetColorTable()
							.GetColorEntry((b[j] & 0xff)).getRed();
				}
				menDataset.GetRasterBand(1).WriteRaster(0, 0, widthrang,
						heightrang, gdalconst.GDT_Byte, bandbyte);

				for (int j = 0; j < arraySize; j++) {
					bandbyte[j] = (byte) band.GetColorTable()
							.GetColorEntry((b[j] & 0xff)).getGreen();
				}
				menDataset.GetRasterBand(2).WriteRaster(0, 0, widthrang,
						heightrang, gdalconst.GDT_Byte, bandbyte);

				for (int j = 0; j < arraySize; j++) {
					bandbyte[j] = (byte) band.GetColorTable()
							.GetColorEntry((b[j] & 0xff)).getBlue();
				}
				menDataset.GetRasterBand(3).WriteRaster(0, 0, widthrang,
						heightrang, gdalconst.GDT_Byte, bandbyte);

				Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
						menDataset);
				poDataset.FlushCache();
				poDataset.delete();
				menDataset.delete();

			}
		}
		if (!processed_cuting) {
			menDataset = drimen.Create(filePath.toString(), widthrang,
					heightrang, rasterCount, gdalconst.GDT_Byte);
			byte[] b = new byte[num];
			for (int i = 1; i <= 3; i++) {
				Band band_i = hDataset.GetRasterBand(i);
					
				band_i.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
							readDataType, b);
				menDataset.GetRasterBand(i).WriteRaster(0, 0, widthrang,
							heightrang, gdalconst.GDT_Byte, b);
			}
			Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
					menDataset);
			poDataset.FlushCache();
			poDataset.delete();
			menDataset.delete();
		}
		hDataset.delete();
		gdal.GDALDestroyDriverManager();
		ImageUtils.pressText(filePath, "ChinaRS中科遥感", "宋体", Font.BOLD
				| Font.ITALIC, 100, Color.BLACK, 0, 0, 0.3f);
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if (!processed) {
			File file = new File(filePath);
			int length = (int) file.length();
			InputStream in = new FileInputStream(file);
			byte[] content = new byte[length];
			int off = 0;
			int readLen;
			while ((readLen = in.read(content, off, length)) != 0) {
				off += readLen;
				length -= readLen;
			}
			value.set(content, 0, content.length);
			in.close();
			// file.delete();
			processed = true;
			return true;
		}
		return false;
	}
*/
	// @Override
	// public void initialize(InputSplit split, TaskAttemptContext context)
	// throws IOException, InterruptedException {
	// filesplit = (ImgAreaSplit) split;
	// String inputFilePath = filesplit.getPath().toString();
	//
	// gdal.AllRegister();
	// gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
	// gdal.SetConfigOption("SHAPE_ENCODING", "");
	// Dataset hDataset = gdal.Open(inputFilePath,
	// gdalconstConstants.GA_ReadOnly);
	//
	// int rasterCount = hDataset.GetRasterCount();
	// double[] adfGeoTransform = hDataset.GetGeoTransform();
	// int xOrigin = filesplit.getxOrigin();
	// int yOrigin = filesplit.getyOrigin();
	// adfGeoTransform[0] = adfGeoTransform[0] + xOrigin * adfGeoTransform[1]
	// + yOrigin * adfGeoTransform[2];
	// adfGeoTransform[3] = adfGeoTransform[3] + xOrigin * adfGeoTransform[4]
	// + yOrigin * adfGeoTransform[5];
	//
	// key.setAdfGeoTransform(adfGeoTransform);
	//
	// Driver poDriver = gdal.GetDriverByName("PNG");
	// Driver drimen = gdal.GetDriverByName("MEM");
	// Dataset menDataset = null;
	//
	// filePath = "/home/yarn/" + xOrigin + "_" + yOrigin + ".png";
	// int widthrang = filesplit.getxLength();
	// int heightrang = filesplit.getyLength();
	// int num = widthrang * heightrang;
	// int readDataType = hDataset.GetRasterBand(1).getDataType();
	// int writeDataType = gdalconst.GDT_Byte;
	// if (readDataType == 3 || readDataType == 2) {
	// writeDataType = gdalconst.GDT_UInt16;
	// }
	// int byte_num = 1;
	// switch (readDataType) {
	// case 2:
	// case 3:
	// case 8:
	// byte_num = 2;
	// break;
	// case 4:
	// case 5:
	// case 6:
	// case 9:
	// case 10:
	// byte_num = 4;
	// break;
	// case 7:
	// case 11:
	// byte_num = 8;
	// default:
	// break;
	// }
	// byte[] b = new byte[num * byte_num];
	// boolean processed_cuting = false;
	// if (1 == rasterCount) {
	// Band band = hDataset.GetRasterBand(1);
	// int colorInterp = band.GetColorInterpretation();
	// if (2 == colorInterp) {// colorInterp = Palette
	// processed_cuting = true;
	// byte[] bandbyte = new byte[num];
	// band.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
	// gdalconst.GDT_Byte, b);
	// menDataset = drimen.Create(filePath.toString(), widthrang,
	// heightrang, 3, gdalconst.GDT_Byte);
	// int arraySize = widthrang * heightrang;
	// for (int j = 0; j < arraySize; j++) {
	// bandbyte[j] = (byte) band.GetColorTable()
	// .GetColorEntry((b[j] & 0xff)).getRed();
	// }
	// menDataset.GetRasterBand(1).WriteRaster(0, 0, widthrang,
	// heightrang, gdalconst.GDT_Byte, bandbyte);
	//
	// for (int j = 0; j < arraySize; j++) {
	// bandbyte[j] = (byte) band.GetColorTable()
	// .GetColorEntry((b[j] & 0xff)).getGreen();
	// }
	// menDataset.GetRasterBand(2).WriteRaster(0, 0, widthrang,
	// heightrang, gdalconst.GDT_Byte, bandbyte);
	//
	// for (int j = 0; j < arraySize; j++) {
	// bandbyte[j] = (byte) band.GetColorTable()
	// .GetColorEntry((b[j] & 0xff)).getBlue();
	// }
	// menDataset.GetRasterBand(3).WriteRaster(0, 0, widthrang,
	// heightrang, gdalconst.GDT_Byte, bandbyte);
	//
	// Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
	// menDataset);
	// poDataset.FlushCache();
	// poDataset.delete();
	// menDataset.delete();
	//
	// }
	// }
	// if (!processed_cuting) {
	// menDataset = drimen.Create(filePath.toString(), widthrang,
	// heightrang, rasterCount, writeDataType);
	// for (int i = 1; i <= rasterCount; i++) {
	// Band band_i = hDataset.GetRasterBand(i);
	// band_i.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
	// readDataType, b);
	// menDataset.GetRasterBand(i).WriteRaster(0, 0, widthrang,
	// heightrang, writeDataType, b);
	// }
	// Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
	// menDataset);
	// poDataset.FlushCache();
	// poDataset.delete();
	// menDataset.delete();
	// }
	// hDataset.delete();
	// gdal.GDALDestroyDriverManager();
	// ImageUtils.pressText(filePath, "ChinaRS中科遥感", "宋体", Font.BOLD
	// | Font.ITALIC, 100, Color.BLACK, 1650, 1200, 0.3f);
	// }

	// @Override
	// public boolean nextKeyValue() throws IOException, InterruptedException {
	// if (!processed) {
	// File file = new File(filePath);
	// int length = (int) file.length();
	// InputStream in = new FileInputStream(file);
	// byte[] content = new byte[length];
	// int off = 0;
	// int readLen;
	// while ((readLen = in.read(content, off, length)) != 0) {
	// off += readLen;
	// length -= readLen;
	// }
	// value.set(content, 0, content.length);
	// in.close();
	// // file.delete();
	// processed = true;
	// return true;
	// }
	// return false;
	// }

}

// public class ImgAreaRecordReader extends
// RecordReader<AdfGeoTransformArray, BytesWritable> {
//
// private ImgAreaSplit filesplit;
// private boolean processed = false;
// private BytesWritable value = new BytesWritable();
// private AdfGeoTransformArray key = new AdfGeoTransformArray();
// private String filePath;
//
// @Override
// public void close() throws IOException {}
//
// @Override
// public AdfGeoTransformArray getCurrentKey() throws IOException,
// InterruptedException {
// return this.key;
// }
//
// @Override
// public BytesWritable getCurrentValue() throws IOException,
// InterruptedException {
// return this.value;
// }
//
// @Override
// public float getProgress() throws IOException, InterruptedException {
// return processed ? 0 : 1;
// }
//
// @Override
// public void initialize(InputSplit split, TaskAttemptContext context)
// throws IOException, InterruptedException {
// filesplit = (ImgAreaSplit) split;
// String inputFilePath = filesplit.getPath().toString();
//
// gdal.AllRegister();
// gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
// gdal.SetConfigOption("SHAPE_ENCODING", "");
// Dataset hDataset = gdal.Open(inputFilePath, gdalconstConstants.GA_ReadOnly);
//
//
// int rasterCount = hDataset.GetRasterCount(); // ������
// // if(rasterCount > 3){
// // rasterCount = 3;
// // }
// double[] adfGeoTransform = hDataset.GetGeoTransform();
// int xOrigin = filesplit.getxOrigin();
// int yOrigin = filesplit.getyOrigin();
// adfGeoTransform[0] = adfGeoTransform[0] + xOrigin
// * adfGeoTransform[1] + yOrigin
// * adfGeoTransform[2];
// adfGeoTransform[3] = adfGeoTransform[3] + xOrigin
// * adfGeoTransform[4] + yOrigin
// * adfGeoTransform[5];
//
// key.setAdfGeoTransform(adfGeoTransform);
//
// Driver poDriver = gdal.GetDriverByName("PNG");
// Driver drimen = gdal.GetDriverByName("MEM");
// Dataset menDataset = null;
//
// filePath = "/home/yarn/" + xOrigin + "_" + yOrigin + ".png";
// int widthrang = filesplit.getxLength();
// int heightrang = filesplit.getyLength();
// int num = widthrang * heightrang;
// int readDataType = hDataset.GetRasterBand(1).getDataType();
// int writeDataType = gdalconst.GDT_Byte;
// if (readDataType == 3 || readDataType == 2) {
// writeDataType = gdalconst.GDT_UInt16;
// }
// int byte_num = 1;
// switch (readDataType) {
// case 2:
// case 3:
// case 8:
// byte_num = 2;
// break;
// case 4:
// case 5:
// case 6:
// case 9:
// case 10:
// byte_num = 4;
// break;
// case 7:
// case 11:
// byte_num = 8;
// default:
// break;
// }
// byte[] b = new byte[num * byte_num];
// boolean processed_cuting = false;
// if (1 == rasterCount) {
// Band band = hDataset.GetRasterBand(1);
// int colorInterp = band.GetColorInterpretation();
// if (2 == colorInterp) {// colorInterp = Palette
// processed_cuting = true;
// byte[] bandbyte = new byte[num];
// band.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
// gdalconst.GDT_Byte, b);
// menDataset = drimen.Create(filePath.toString(), widthrang,
// heightrang, 3, gdalconst.GDT_Byte);
// int arraySize = widthrang * heightrang;
// for (int j = 0; j < arraySize; j++) {
// bandbyte[j] = (byte) band.GetColorTable()
// .GetColorEntry((b[j] & 0xff)).getRed();
// }
// menDataset.GetRasterBand(1).WriteRaster(0, 0, widthrang,
// heightrang, gdalconst.GDT_Byte, bandbyte);
//
// for (int j = 0; j < arraySize; j++) {
// bandbyte[j] = (byte) band.GetColorTable()
// .GetColorEntry((b[j] & 0xff)).getGreen();
// }
// menDataset.GetRasterBand(2).WriteRaster(0, 0, widthrang,
// heightrang, gdalconst.GDT_Byte, bandbyte);
//
// for (int j = 0; j < arraySize; j++) {
// bandbyte[j] = (byte) band.GetColorTable()
// .GetColorEntry((b[j] & 0xff)).getBlue();
// }
// menDataset.GetRasterBand(3).WriteRaster(0, 0, widthrang,
// heightrang, gdalconst.GDT_Byte, bandbyte);
//
// Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
// menDataset);
// poDataset.FlushCache();
// poDataset.delete();
// menDataset.delete();
//
// }
// }
// if (!processed_cuting) {
// menDataset = drimen.Create(filePath.toString(), widthrang,
// heightrang, rasterCount, writeDataType);
// for (int i = 1; i <= rasterCount; i++) {
// Band band_i = hDataset.GetRasterBand(i);
// band_i.ReadRaster(xOrigin, yOrigin, widthrang, heightrang,
// readDataType, b);
// menDataset.GetRasterBand(i).WriteRaster(0, 0, widthrang,
// heightrang, writeDataType, b);
// }
// Dataset poDataset = poDriver.CreateCopy(filePath.toString(),
// menDataset);
// poDataset.FlushCache();
// poDataset.delete();
// menDataset.delete();
// }
// hDataset.delete();
// gdal.GDALDestroyDriverManager();
// }
//
// @Override
// public boolean nextKeyValue() throws IOException, InterruptedException {
// if (!processed) {
// File file = new File(filePath);
// int length = (int)file.length();
// InputStream in = new FileInputStream(file);
// byte[] content = new byte[length];
// int off = 0;
// int readLen;
// while( (readLen = in.read(content, off, length)) != 0){
// off += readLen;
// length -= readLen;
// }
// value.set(content, 0, content.length);
// in.close();
// file.delete();
// processed = true;
// return true;
// }
// return false;
// }
//
// }
