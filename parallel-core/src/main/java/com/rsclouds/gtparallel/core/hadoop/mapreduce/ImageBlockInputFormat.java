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
import com.rsclouds.gtparallel.core.hadoop.io.ImageInfo;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class ImageBlockInputFormat extends FileInputFormat<ImageInfo, NullWritable> {
	private static final Log LOG = LogFactory
			.getLog(ImageBlockInputFormat.class);
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
//		Dataset hDataset = gdal.Open("D://nanlin//guangzhou_051401.tiff", gdalconstConstants.GA_ReadOnly);
		if (hDataset == null) {
			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
			LOG.info(gdal.GetLastErrorMsg());
			System.exit(1);
		}
		//计算影像文件的偏移量，计算行列号时采用向下取整，所以会造成一定的偏差
		double[] adfGeoTransform = hDataset.GetGeoTransform();
		double dstResolution = conf.getDouble(CoreConfig.KEY_DST_RESOLUTION, adfGeoTransform[1]);
		if(dstResolution < 0.0000000000) {
			dstResolution = -dstResolution;
		}
		for (int k = 0; k < 6; k ++) {
			System.out.println(adfGeoTransform[k]);
		}
		double xCoordinate;
		double yCoordinate;
		long  tilerowTopLeft, tilerowOffset;
		long  tilecolTopLeft, tilecolOffset;
		//将坐标转换成geowebcache地图服务的行列号
//		tilerowTopLeft = (long) Math.floor((90 - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / Math.abs(adfGeoTransform[5]));
//		tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / adfGeoTransform[1]);
		tilerowTopLeft = (long) Math.floor((90 - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
		tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / dstResolution);
		
//		yCoordinate = 90 - tilerowTopLeft * Math.abs(adfGeoTransform[5]) * CoreConfig.HEIGHT_DEFAULT; 
//		xCoordinate = tilecolTopLeft * adfGeoTransform[1] * CoreConfig.WIDTH_DEFAULT - 180;
		yCoordinate = 90 - tilerowTopLeft * dstResolution * CoreConfig.HEIGHT_DEFAULT; 
		xCoordinate = tilecolTopLeft * dstResolution * CoreConfig.WIDTH_DEFAULT - 180;
		
//		tilecolOffset =(long) ((adfGeoTransform[0] - xCoordinate)/adfGeoTransform[1]);
//		tilerowOffset =(long) ((adfGeoTransform[3] - yCoordinate)/adfGeoTransform[5]);
		tilecolOffset =(long) ((adfGeoTransform[0] - xCoordinate)/dstResolution);
		tilerowOffset =(long) ((adfGeoTransform[3] - yCoordinate)/(-dstResolution));
		int width = hDataset.GetRasterXSize();
		int height = hDataset.GetRasterYSize(); 
		int dstWidth =(int) (width * adfGeoTransform[1] / dstResolution);   //计算重采样后的width
		int dstHeight = (int) (height*adfGeoTransform[5] /(-dstResolution));//计算重采样后的height
		System.out.println("dstWidth: " + dstWidth + ",  dstHeight: " + dstHeight);
//		int widthRead = 0;    //原始影像实际已读width长度
//		int heightRead = 0;   //原始影像实际已读height长度
		int dstWidthRead = 0; //重采样后已读width长度
		int dstHeightRead = 0;//重采样后已读height长度
		int multiple = CoreConfig.IMGBLOCK_WIDTH;
		int widthrang = 256 * multiple;  //默认分块 width 的长度
		int heightrang = 256 * multiple; //默认分块 height 的长度
//		int heightLength, widthLength;   //原始影像实际分块 width 和  height 的长度
		int dstHeightLen, dstWidthLen;   //重采样后分块 width 和 height 的长度
//		int maxMapNum = 5;
//		if ((dstWidth / widthrang) > maxMapNum) {
//			int  mutiples = (int)(dstWidth / (widthrang * maxMapNum * 1.0) + 0.5);
//			widthrang = widthrang * mutiples;
//			System.out.println("widthrang: " + mutiples + " " + widthrang);
//		}
//		if ((dstHeight / heightrang) > maxMapNum) {
//			int  mutiples = (int)(dstHeight / (heightrang * maxMapNum * 1.0) + 0.5);
//			heightrang = heightrang * mutiples;
//			System.out.println("heightrang: " + mutiples + " " + heightrang);
//		}
		

		for (; dstWidthRead < dstWidth;) {
			if (dstWidth - dstWidthRead < widthrang) {
				dstWidthLen = dstWidth - dstWidthRead;
			}else if (dstWidthRead == 0) {
				dstWidthLen = (int)(widthrang - tilecolOffset);
			}else {
				dstWidthLen = widthrang;
			}
//			widthLength = (int) (dstWidthLen * dstResolution / adfGeoTransform[1]);
//			heightRead = 0;
			for (dstHeightRead = 0; dstHeightRead < dstHeight;) {
				if (dstHeight - dstHeightRead < heightrang) {
					dstHeightLen = dstHeight - dstHeightRead;
				}else if(dstHeightRead == 0) {
					dstHeightLen = (int)(heightrang - tilerowOffset);
				}else {
					dstHeightLen = heightrang;
				}
//				heightLength = (int) (dstHeightLen * (-dstResolution) / adfGeoTransform[5]);
//				pathList.add(new ImageSplit(filename, widthRead, heightRead, widthLength,
//						heightLength));
				pathList.add(new ImageSplit(filename, dstWidthRead, dstHeightRead, dstWidthLen,
						dstHeightLen));
				dstHeightRead += dstHeightLen;
//				heightRead += heightLength;
			}
			dstWidthRead += dstWidthLen;
//			widthRead += widthLength;
		}
		hDataset.delete();
		List<FileStatus> files = listStatus(job);
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());

		LOG.debug("Total # of splits: " + pathList.size());
		return pathList;
	}

	@Override
	public RecordReader<ImageInfo, NullWritable> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException,
			InterruptedException {
		ImageRecordReader img = new ImageRecordReader();
		return img;
	}
}
