package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class ImageMutilLayersInputFormat extends FileInputFormat<ImageMutilLayersInfo, NullWritable> {
	private static final Log LOG = LogFactory.getLog(ImageMutilLayersInputFormat.class);
	
	
	/**
	 * the files have copied to every datanode's local filesystem 
	 * Generate the list of files and make them into FileSplits.
	 */
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		Configuration conf = job.getConfiguration();
		String rowkey = conf.get(CoreConfig.JOBID);
		int layers = conf.getInt(CoreConfig.KEY_CURRENT_LAYER, 0);
		int minLayers = conf.getInt(CoreConfig.KEY_MIN_LAYER, 0);
		boolean bfloor = conf.getBoolean(CoreConfig.KEY_TRUNC_BOOLEAN, true);
		
		double dstResolution = conf.getDouble(CoreConfig.KEY_DST_RESOLUTION, 0.0);
		if (dstResolution == 0.0) {
			LOG.info("Destination resolution is zero, that is error");
			System.exit(1);
		}
		String outputpath = conf.get(CoreConfig.CUTTING_OUTPUTPATH);
		if (!outputpath.endsWith("/")) {
			outputpath += "/";
		}
		Map<String,String> map = new HashMap<String,String>();
		map.put(CoreConfig.JOB.JID.strVal, job.getJobID().toString());
		map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.RUNNING.toString());
		HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, rowkey,CoreConfig.JOB.FAMILY.strVal, map);
		
		//hdfs path
		String hdfspath = getInputPaths(job)[0].toString();
		
		List<InputSplit> pathList = new ArrayList<InputSplit>();
		
 		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		String filename = conf.get(CoreConfig.CUTTING_INPUTFILE);
		Dataset dataset = gdal.Open(filename, gdalconstConstants.GA_ReadOnly);
		if (dataset == null) {
			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
			LOG.info(gdal.GetLastErrorMsg());
			System.exit(1);
		}
		
		//计算影像文件的偏移量，计算行列号时采用向下取整，所以会造成一定的偏差
		double[] adfGeoTransform = dataset.GetGeoTransform();
		double tileOriginY = conf.getDouble(CoreConfig.KEY_TILEORIGIN_Y, 90);
		double yCoordinate;
		long  tilerowTopLeft, tilerowOffset;
		
		
		int width = dataset.GetRasterXSize();
		int height = dataset.GetRasterYSize();
		long dstHeightReaded = 0;//重采样后已读height长度
		long heightDefault; //重采样后默认分块 height 的长度
		long dstHeightLen;   //重采样后实际分块  height的长度	
		long dstWidth;     //计算重采样后的width
		long dstHeight;    //计算重采样后的height
		int totalsize =  256* 1024 *1024; //默认256MB一个块
		long mutil;
		StringBuilder layerName = new StringBuilder(Integer.toHexString(layers));
		for(; layers >= minLayers;) {
			//将坐标转换成geowebcache地图服务的行列号
//			if(bfloor) {
				tilerowTopLeft = (long) Math.floor((tileOriginY - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
//			} else {
//				tilerowTopLeft = (long) Math.ceil((tileOriginY - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
//			}
			//根据计算的左上角的行列号，反算该行列号实际对应的坐标
			yCoordinate = tileOriginY - tilerowTopLeft * dstResolution * CoreConfig.HEIGHT_DEFAULT; 
			//计算行列号实际对应的坐标与原始影像左上角的坐标的像素偏移量
			if(!bfloor) {
				tilerowOffset = 0;
			}else {
				tilerowOffset =(long) ((adfGeoTransform[3] - yCoordinate)/(-dstResolution));
				if (tilerowOffset == 256) {
					tilerowOffset = 0;
				}
			}
			
			layerName.replace(0, layerName.length(), Integer.toHexString(layers));
			if (layerName.length() == 1) {
				layerName.insert(0, "L0");
			} else {
				layerName.insert(0, "L");
			}
			layerName.insert(0, outputpath);
			dstWidth =(int) Math.ceil(width * adfGeoTransform[1] / dstResolution);   
			dstHeight = (int) Math.ceil(height*adfGeoTransform[5] /(-dstResolution));
			if(dstWidth == 0) {
				dstWidth = 1;
			}
			if (dstHeight == 0) {
				dstHeight = 1;
			}
			//尽量按照256MB一个block
			mutil = (int)(totalsize *1.0 / CoreConfig.HEIGHT_DEFAULT / dstWidth / 3);
			if (mutil > 1)
				heightDefault = CoreConfig.HEIGHT_DEFAULT * mutil;
			else 
				heightDefault = CoreConfig.HEIGHT_DEFAULT;
			//如果分的块超过100个，扩大block的处理范围，将block的数量降低到100左右
			if (dstHeight / heightDefault > 500) {
				mutil = dstHeight / heightDefault / 500;
				heightDefault *= mutil;
			}
		 
			for (dstHeightReaded = 0; dstHeightReaded < dstHeight;) {
				if (dstHeight - dstHeightReaded < heightDefault) {
					dstHeightLen = dstHeight - dstHeightReaded;
				}else if(dstHeightReaded == 0) {
					dstHeightLen = (int)(heightDefault - tilerowOffset);
				}else {
					dstHeightLen = heightDefault;
				}	
				pathList.add(new ImageMutilLayersSplit(hdfspath,filename, layerName.toString(), 0, dstHeightReaded, dstWidth, dstHeightLen, dstResolution, layers));
				dstHeightReaded += dstHeightLen;
			}
			layers --;
			dstResolution = dstResolution * 2;
		}

		dataset.delete();
		List<FileStatus> files = listStatus(job);
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());

		LOG.debug("Total # of splits: " + pathList.size());
		return pathList;
	}
	
	
	@Override
	public RecordReader<ImageMutilLayersInfo, NullWritable> createRecordReader(
			InputSplit split, TaskAttemptContext context)throws IOException,
			InterruptedException {
		ImageMutilLayersRecordReader img = new ImageMutilLayersRecordReader();
		return img;
	}

}
