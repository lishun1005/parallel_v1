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
import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;

public class ImageMutilLayersRecordReader extends RecordReader<ImageMutilLayersInfo, NullWritable>{
	private boolean processed = false;
	private ImageMutilLayersInfo key = new ImageMutilLayersInfo();
	private ImageMutilLayersSplit fileSplit;
	private int tileColOffset;
	private int tileRowOffset;
	private long colNum;
	private long rowNum;
	private long xreadOrigin;
	private long yreadOrigin;
	private long xreadLength;
	private long xreadLenDefault;
	private long yreadLenDefault;
	private long yreadLength;
	private long xLength;
	private long yLength;
	private long xreaded;
	private long yreaded;
	private long dealsize;
	private long totalsize;
	private double dstResolution;
	private double oriResolution;
	private double[] adfGeoTransform = new double[6];
	
	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		
		Configuration conf = context.getConfiguration();
		double tileOriginX = conf.getDouble(CoreConfig.KEY_TILEORIGIN_X, 180);
		double tileOriginY = conf.getDouble(CoreConfig.KEY_TILEORIGIN_Y, 90);
		boolean bfloor = conf.getBoolean(CoreConfig.KEY_TRUNC_BOOLEAN, true);
		
		fileSplit = (ImageMutilLayersSplit)split;
		xreaded = 0;
		yreaded = 0;
		xreadOrigin = fileSplit.getxOrigin();
		yreadOrigin = fileSplit.getyOrigin();
		xLength = fileSplit.getxLength();
		yLength = fileSplit.getyLength();
		dstResolution = fileSplit.getDstresolution();
		String gtdataOutputPath = fileSplit.getGtdataOutputPath().toString();
		
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		String filepath = fileSplit.getFileName().toString();
		Dataset hDataset = gdal.Open(filepath, gdalconstConstants.GA_ReadOnly);
		
		
		totalsize = (long)xLength * yLength;
		if (hDataset != null) {
			hDataset.GetGeoTransform(adfGeoTransform);
			oriResolution = adfGeoTransform[1];
			
			adfGeoTransform[3] = adfGeoTransform[3] + xreadOrigin * adfGeoTransform[4] + yreadOrigin * (-dstResolution);//分块左上角的纬度
			adfGeoTransform[0] = adfGeoTransform[0] + yreadOrigin * adfGeoTransform[2] + xreadOrigin * (dstResolution);//分块左上角的经度

			rowNum = (int) Math.floor((tileOriginY - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
			colNum = (int) Math.floor((tileOriginX + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / dstResolution);

			double xCoordinate = colNum * dstResolution * CoreConfig.WIDTH_DEFAULT - tileOriginX;
			//计算分块的左上角与实际对应瓦片的左上角的偏移量
			if(bfloor) {
				tileColOffset = (int) ((adfGeoTransform[0] - xCoordinate)/dstResolution);
			}else {
				tileColOffset = 0;
			}
			if (yreadOrigin == 0) {
				double yCoordinate = tileOriginY - rowNum * dstResolution * CoreConfig.HEIGHT_DEFAULT;
				tileRowOffset =(int) ((adfGeoTransform[3] - yCoordinate)/(-dstResolution));
			}else {
				tileRowOffset = 0;
			}
			yreadLenDefault = CoreConfig.HEIGHT_DEFAULT;
			xreadLenDefault = CoreConfig.WIDTH_DEFAULT * CoreConfig.IMGBLOCK_WIDTH;
			
			yreadLength = yreadLenDefault - tileRowOffset;
			if(yreadLength > yLength)
				yreadLength = yLength;
	
			key.setDstResolution(dstResolution);
			key.setOriResolution(oriResolution);
			key.setTileRowOffset(tileRowOffset);
			key.setYreadOrigin(yreadOrigin);
			key.setYreadLen(yreadLength);
			key.setFilePath(filepath);
			key.setBands(fileSplit.getBands());
			key.setRowNum(rowNum);
			key.setGtDataOutputPath(gtdataOutputPath);
			key.setCurentLayer(fileSplit.getCurrentLayers());
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
					int colNumOffset = (int)((xreaded + tileColOffset) / yreadLenDefault);
					long colNumTemp = colNum + colNumOffset;
					if(xreaded == 0) {
						xreadLength = xreadLenDefault - tileColOffset;
						if(xreadLength > xLength) {
							xreadLength = xLength;
						}
						key.setTileColOffset(this.tileColOffset);
					}else {
						if (xLength -xreaded < xreadLenDefault) {
							xreadLength = xLength -xreaded;
						}else{
							xreadLength = xreadLenDefault;
						}
						key.setTileColOffset(0);
					}
					
					key.setColNum(colNumTemp);
					key.setXreadLen(xreadLength);
					key.setXreadOrigin(xreaded + xreadOrigin);
					
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
	public ImageMutilLayersInfo getCurrentKey() throws IOException,
			InterruptedException {
		return key;
	}

	@Override
	public NullWritable getCurrentValue() throws IOException,
			InterruptedException {
		return NullWritable.get();
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return processed ? 1.0f : (float)(dealsize * 1.0 / totalsize);
	}

	@Override
	public void close() throws IOException {	
	}
	
}
