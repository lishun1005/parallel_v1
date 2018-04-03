package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.hadoop.io.ImageBarInfo;

public class ImageBarBlockRecordReader extends RecordReader<ImageBarInfo, NullWritable>{

	private boolean processed = false;
	private ImageBarInfo key = new ImageBarInfo();
	private ImageSplit fileSplit;
	private int tileColOffset;
	private int tileRowOffset;
	private long colNum;
	private long rowNum;
	private int xreadoOrign;
	private int yreadOrigin;
	private int xreadLength;
	private int xreadLenDefault;
	private int yreadLenDefault;
	private int yreadLength;
	private int xLength;
	private int yLength;
	private int xreaded;
	private int yreaded;
	private long dealsize;
	private long totalsize;
	private double dstResolution;
	private double oriResolution;
	private double[] adfGeoTransform = new double[6];

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		Configuration conf = context.getConfiguration();
		dstResolution = conf.getDouble(CoreConfig.KEY_DST_RESOLUTION, 0);
		double tileOriginX = conf.getDouble(CoreConfig.KEY_TILEORIGIN_X, 180);
		double tileOriginY = conf.getDouble(CoreConfig.KEY_TILEORIGIN_Y, 90);
		fileSplit = (ImageSplit)split;
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		String filepath = fileSplit.getPath().toString();
		Dataset hDataset = gdal.Open(filepath, gdalconstConstants.GA_ReadOnly);
		//Dataset hDataset = gdal.Open("D://nanlin//t1_byte.tif", gdalconstConstants.GA_ReadOnly);
		//Dataset hDataset = gdal.Open("F://hunan//hunan_xiangqian_clip.tif", gdalconstConstants.GA_ReadOnly);
		xreaded = 0;
		yreaded = 0;
		xreadoOrign = fileSplit.getxOrigin();
		yreadOrigin = fileSplit.getyOrigin();
		xLength = fileSplit.getxLength();
		yLength = fileSplit.getyLength();
		totalsize = (long)xLength * yLength;
		if (hDataset != null) {
			hDataset.GetGeoTransform(adfGeoTransform);
			oriResolution = adfGeoTransform[1];
			
			
			
			adfGeoTransform[3] = adfGeoTransform[3] + xreadoOrign * adfGeoTransform[4] + yreadOrigin * (-dstResolution);//分块左上角的纬度
			//计算分块的左上角的第一个瓦片的行列号
			rowNum = (int) Math.floor((tileOriginY - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
			colNum = (int) Math.floor((tileOriginX + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / dstResolution);
			double xCoordinate = colNum * dstResolution * CoreConfig.WIDTH_DEFAULT - tileOriginX;
			//计算分块的左上角与实际对应瓦片的左上角的偏移量
			tileColOffset =(int) ((adfGeoTransform[0] - xCoordinate)/dstResolution);
			if (yreadOrigin == 0) {
				double yCoordinate = tileOriginY - rowNum * dstResolution * CoreConfig.HEIGHT_DEFAULT;
				tileRowOffset =(int) ((adfGeoTransform[3] - yCoordinate)/(-dstResolution));
			}else {
				tileRowOffset = 0;
			}
			yreadLenDefault = CoreConfig.HEIGHT_DEFAULT;
			xreadLenDefault = CoreConfig.WIDTH_DEFAULT * CoreConfig.IMGBLOCK_WIDTH;
			if (yreadLenDefault > yLength) {
				yreadLength = yLength;
			}else {
				yreadLength = yreadLenDefault - tileRowOffset;
			}
			key.setDstResolution(dstResolution);
			key.setOriResolution(oriResolution);
			key.setTileRowOffset(tileRowOffset);
			key.setYreadOrigin(yreadOrigin);
			key.setYreadLen(yreadLength);
			key.setFilePath(filepath);
			key.setBands(fileSplit.getBands());
			key.setRowNum(rowNum);
			hDataset.delete();
		}else {
			processed = true;
		}
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if(processed) {
			return false;
		}else {
			for(; yreaded < yLength; ) {
				dealsize = (long)xreaded*yreadLength + (long) xLength * yreaded;
				for (; xreaded < xLength;) {
					int colNumOffset = (xreaded + tileColOffset) / yreadLenDefault;
					long colNumTemp = colNum + colNumOffset;
					if(xLength -xreaded < xreadLenDefault) {
						xreadLength = xLength -xreaded;
					}else if(xreaded == 0){
						xreadLength = xreadLenDefault -tileColOffset;
					}else {
						xreadLength = xreadLenDefault;
					}
					if (xreaded != 0) {
						key.setTileColOffset(0);
					}else {
						key.setTileColOffset(this.tileColOffset);
					}
					key.setColNum(colNumTemp);
					key.setXreadLen(xreadLength);
					key.setXreadOrigin(xreaded);
					
					xreaded += xreadLength;
					return true;
				}
				xreaded = 0;
				yreaded += yreadLength;
				rowNum ++;
				if(yLength - yreaded < yreadLenDefault){
					yreadLength = yLength - yreaded;
				}else {
					yreadLength = yreadLenDefault;
				}
				key.setYreadOrigin(yreaded+yreadOrigin);
				key.setTileRowOffset(0);
				key.setYreadLen(yreadLength);
				key.setRowNum(rowNum);
			}
			processed = true;
			return false;
		}
	}

	@Override
	public ImageBarInfo getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	@Override
	public NullWritable getCurrentValue() throws IOException,
			InterruptedException {
		return NullWritable.get();
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		System.out.println("toatalsize= " + totalsize +" , dealsize= " + dealsize);
		return processed ? 1.0f : (float)(dealsize * 1.0 / totalsize);
	}

	@Override
	public void close() throws IOException {	
	}

}
