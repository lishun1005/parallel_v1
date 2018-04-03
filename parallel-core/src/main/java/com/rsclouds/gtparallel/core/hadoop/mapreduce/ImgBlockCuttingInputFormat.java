package com.rsclouds.gtparallel.core.hadoop.mapreduce;

//import java.io.DataInputStream;
//import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.BytesWritable;
//import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.hadoop.io.AdfGeoTransformArray;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;



public class ImgBlockCuttingInputFormat extends //FileInputFormat<ImageInfo, NullWritable> {
		FileInputFormat<com.rsclouds.gtparallel.core.hadoop.io.AdfGeoTransformArray, BytesWritable> {

	private static final Log LOG = LogFactory
			.getLog(ImgBlockCuttingInputFormat.class);


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
//		Dataset hDataset = gdal.Open(filename, gdalconstConstants.GA_ReadOnly);
		Dataset hDataset = gdal.Open("D://fusion.tiff", gdalconstConstants.GA_ReadOnly);
		if (hDataset == null) {
			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
			LOG.info(gdal.GetLastErrorMsg());
			System.exit(1);
		}
		//计算影像文件的偏移量，计算行列号时采用向下取整，所以会造成一定的偏差
		double[] adfGeoTransform = hDataset.GetGeoTransform();
		for (int k = 0; k < 6; k ++) {
			System.out.println(adfGeoTransform[k]);
		}
		double xCoordinate;
		double yCoordinate;
		long  tilerowTopLeft, tilerowOffset;
		long  tilecolTopLeft, tilecolOffset;
		//将坐标转换成geowebcache地图服务的行列号
		tilerowTopLeft = (long) Math.floor((90 - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / Math.abs(adfGeoTransform[5]));
		tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / adfGeoTransform[1]);
		
		yCoordinate = 90 - tilerowTopLeft * Math.abs(adfGeoTransform[5]) * CoreConfig.HEIGHT_DEFAULT; 
		xCoordinate = tilecolTopLeft * adfGeoTransform[1] * CoreConfig.WIDTH_DEFAULT - 180;
		
		tilecolOffset =(long) ((adfGeoTransform[0] - xCoordinate)/adfGeoTransform[1]);
		tilerowOffset =(long) ((adfGeoTransform[3] - yCoordinate)/adfGeoTransform[5]);
		int width = hDataset.GetRasterXSize();
		int height = hDataset.GetRasterYSize(); 
		int widthRead = 0; //已读width长度
		int heightRead = 0;//已读height长度
		int multiple = CoreConfig.IMGBLOCK_WIDTH;
		int widthrang = 256 * multiple;  //默认分块 width 的长度
		int heightrang = 256 * multiple; //默认分块 height 的长度
		int heightLength, widthLength;   //实际分块 width 和  height 的长度

		for (; widthRead < width;) {
			if (width - widthRead < widthrang) {
				widthLength = width - widthRead;
			}else if (widthRead == 0) {
				widthLength = (int)(widthrang - tilecolOffset);
			}else {
				widthLength = widthrang;
			}
			for (heightRead = 0; heightRead < height;) {
				if (height - heightRead < heightrang) {
					heightLength = height - heightRead;
				}else if(heightRead == 0) {
					heightLength = (int)(heightrang - tilerowOffset);
				}else {
					heightLength = heightrang;
				}
//				pathList.add(new ImageSplit(filename, widthRead,  widthLength, heightRead,
//						heightLength));
				pathList.add(new ImgAreaSplit(filename, widthRead,  widthLength, heightRead,
						heightLength));
				heightRead += heightLength;
			}
			widthRead += widthLength;
		}
		hDataset.delete();
		List<FileStatus> files = listStatus(job);
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());

		LOG.debug("Total # of splits: " + pathList.size());
		return pathList;
	}
	
	
//	/**
//	 * the files have copied to every datanode's local filesystem 
//	 * Generate the list of files and make them into FileSplits.
//	 */
//	public List<InputSplit> getSplits(JobContext job) throws IOException {
//		Configuration conf = job.getConfiguration();
//		Map<String,String> map = new HashMap<String,String>();
//		map.put(CoreConfig.JOB.JID.strVal, job.getJobID().toString());
//		map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.RUNNING.toString());
//		String rowkey = conf.get(CoreConfig.JOBID);
//		HbaseBase.writeRows(CoreConfig.JOB_TABLE, rowkey,CoreConfig.JOB.FAMILY.strVal, map);
//		List<InputSplit> pathList = new ArrayList<InputSplit>();
//		
//		gdal.AllRegister();
//		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
//		gdal.SetConfigOption("SHAPE_ENCODING", "");
//		String filename = conf.get(CoreConfig.CUTTING_INPUTFILE);
//		Dataset hDataset = gdal.Open(filename, gdalconstConstants.GA_ReadOnly);
//		if (hDataset == null) {
//			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
//			LOG.info(gdal.GetLastErrorMsg());
//			System.exit(1);
//		}
//		int width = hDataset.GetRasterXSize(); // ���
//		int height = hDataset.GetRasterYSize(); // �߶�
//		int widthRead = 0;
//		int heightRead = 0;
//		int multiple = CoreConfig.IMGBLOCK_WIDTH;
//		int maxMutiple = multiple + (multiple / 2 < 50 ? multiple / 2 : 50);
//		int widthrang = 256 * multiple;
//		int heightrang = 256 * multiple;
//		if (width / 256 < maxMutiple)
//			widthrang = width;
//		if (height / 256 < maxMutiple)
//			heightrang = height;
//		int heightrangInit = heightrang;
//		for (; widthRead < width;) {
//			if (width - widthRead < widthrang) {
//				widthrang = width - widthRead;
//			}
//
//			for (heightRead = 0; heightRead < height;) {
//				if (height - heightRead < heightrang) {
//					heightrang = height - heightRead;
//				}
//				pathList.add(new ImgAreaSplit(filename, widthRead, widthrang,
//						heightRead, heightrang));
//				heightRead += heightrang;
//			}
//			heightrang = heightrangInit;
//			widthRead += widthrang;
//		}
//		hDataset.delete();
//		List<FileStatus> files = listStatus(job);
//		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());
//
//		LOG.debug("Total # of splits: " + pathList.size());
//		return pathList;
//	}

	@Override
	//public RecordReader<ImageInfo, NullWritable> createRecordReader(
	public RecordReader<AdfGeoTransformArray, BytesWritable> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException,
			InterruptedException {
		ImgAreaRecordReader img = new ImgAreaRecordReader();
		//ImageRecordReader img = new ImageRecordReader();
		return img;
	}

}
