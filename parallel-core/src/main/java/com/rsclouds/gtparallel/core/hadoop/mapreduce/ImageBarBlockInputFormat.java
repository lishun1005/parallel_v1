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
import com.rsclouds.gtparallel.core.hadoop.io.ImageBarInfo;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class ImageBarBlockInputFormat extends FileInputFormat<ImageBarInfo, NullWritable>{

	private static final Log LOG = LogFactory
			.getLog(ImageBarBlockInputFormat.class);
	
	/**
	 * the files have copied to every datanode's local filesystem 
	 * Generate the list of files and make them into FileSplits.
	 */
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		Configuration conf = job.getConfiguration();
		Map<String,String> map = new HashMap<String,String>();
		map.put(CoreConfig.JOB.JID.strVal, job.getJobID().toString());
		map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.RUNNING.toString());
		String rowkey = conf.get(CoreConfig.JOBID);
		HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, rowkey,CoreConfig.JOB.FAMILY.strVal, map);
		List<InputSplit> pathList = new ArrayList<InputSplit>();
		
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		String filename = conf.get(CoreConfig.CUTTING_INPUTFILE);
		Dataset hDataset = gdal.Open(filename, gdalconstConstants.GA_ReadOnly);
		//Dataset hDataset = gdal.Open("D://nanlin//t1_byte1.tif", gdalconstConstants.GA_ReadOnly);
		//Dataset hDataset =  gdal.Open("F://hunan//hunan_xiangqian_clip.tif", gdalconstConstants.GA_ReadOnly);
		if (hDataset == null) {
			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
			LOG.info(gdal.GetLastErrorMsg());
			System.exit(1);
		}
		//计算影像文件的偏移量，计算行列号时采用向下取整，所以会造成一定的偏差
		double[] adfGeoTransform = hDataset.GetGeoTransform();
		double dstResolution = conf.getDouble(CoreConfig.KEY_DST_RESOLUTION, adfGeoTransform[1]);
		double tileOriginY = conf.getDouble(CoreConfig.KEY_TILEORIGIN_Y, 90);
		if(dstResolution < 0.0000000000) {
			dstResolution = -dstResolution;
		}
		for (int k = 0; k < 6; k ++) {
			System.out.println(adfGeoTransform[k]);
		}
//		double xCoordinate;
		double yCoordinate;
		long  tilerowTopLeft, tilerowOffset;
//		long  tilecolTopLeft, tilecolOffset;
		//将坐标转换成geowebcache地图服务的行列号
		tilerowTopLeft = (long) Math.floor((tileOriginY - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
//		tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / dstResolution);
		//根据计算的左上角的行列号，反算该行列号实际对应的坐标
		yCoordinate = tileOriginY - tilerowTopLeft * dstResolution * CoreConfig.HEIGHT_DEFAULT; 
//		xCoordinate = tilecolTopLeft * dstResolution * CoreConfig.WIDTH_DEFAULT - 180;
		//计算行列号实际对应的坐标与原始影像左上角的坐标的像素偏移量
//		tilecolOffset =(long) ((adfGeoTransform[0] - xCoordinate)/dstResolution);
		tilerowOffset =(long) ((adfGeoTransform[3] - yCoordinate)/(-dstResolution));
		
		int width = hDataset.GetRasterXSize();
		int height = hDataset.GetRasterYSize(); 
		int dstWidth =(int) (width * adfGeoTransform[1] / dstResolution);   //计算重采样后的width
		int dstHeight = (int) (height*adfGeoTransform[5] /(-dstResolution));//计算重采样后的height
		if(dstWidth == 0) {
			dstWidth = 1;
		}
		if (dstHeight == 0) {
			dstHeight = 1;
		}
		int dstHeightReaded = 0;//重采样后已读height长度
		int heightDefault; //重采样后默认分块 height 的长度
		int dstHeightLen;   //重采样后实际分块  height的长度	
		
		int totalsize =  256* 1024 *1024;
		int mutil = (int)(totalsize *1.0 / CoreConfig.HEIGHT_DEFAULT / dstWidth / 3);
		if (mutil > 1)
			heightDefault = CoreConfig.HEIGHT_DEFAULT * mutil;
		else 
			heightDefault = CoreConfig.HEIGHT_DEFAULT;
		if (dstHeight / heightDefault > 100) {
			mutil = dstHeight / heightDefault / 100;
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

			pathList.add(new ImageSplit(filename, 0, dstHeightReaded, dstWidth,
						dstHeightLen));
			dstHeightReaded += dstHeightLen;
		}

		hDataset.delete();
		List<FileStatus> files = listStatus(job);
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());

		LOG.debug("Total # of splits: " + pathList.size());
		return pathList;
	}
	@Override
	public RecordReader<ImageBarInfo, NullWritable> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException,
			InterruptedException {
		ImageBarBlockRecordReader img = new ImageBarBlockRecordReader();
		return img;
	}

}
