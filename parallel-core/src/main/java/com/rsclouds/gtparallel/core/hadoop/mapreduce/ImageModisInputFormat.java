package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.Geometry;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.GeometryBase;
import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class ImageModisInputFormat extends FileInputFormat<ImageMutilLayersInfo, NullWritable>  {
	private static final Log LOG = LogFactory.getLog(ImageModisInputFormat.class);
	
	
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
		String hdfspath = getInputPaths(job)[0].toString();
		
		String filename = conf.get(CoreConfig.CUTTING_INPUTFILE);
		Path imgagePath = new Path(filename);
//		//"hdfs://node03.rsclouds.cn:8020/temp/modsi_cutting/62f98e42-eb4e-484c-a9a9-02ab137c1135/2016-02-16.tiff"
		FileSystem fs = FileSystem.get(conf);
		FileStatus[] fileStatus = fs.listStatus(imgagePath.getParent());
		Path geojsonPath = null;
		for (int i = 0; i < fileStatus.length; i ++) {
			if (fileStatus[i].getPath().toString().endsWith("geojson")) {
				geojsonPath = fileStatus[i].getPath();
				break;
			}
		}
		double[] extend = new double[4];
		if (geojsonPath != null) {
			Path localGeoJsonPath = new Path("/home/yarn/cutting_temp/" + job.getJobID().toString(), "temp.geojson");
			File file = new File("/home/yarn/cutting_temp/" + job.getJobID().toString());
//			fs.mkdirs(localGeoJsonPath.getParent());
			fs.copyToLocalFile(geojsonPath, localGeoJsonPath);
			GeometryBase geoBase = new GeometryBase(localGeoJsonPath.toString());
			Geometry geo = geoBase.getGeometry().GetBoundary();
			
			double minX = geo.GetX(0);
			double maxY = geo.GetY(0);
			double maxX = geo.GetX(2);
			double minY = geo.GetY(2);
//			System.out.println(geo.GetBoundary().GetPoints());
			extend[0] = minX < maxX ? minX : maxX;
			extend[2] = maxX > minX ? maxX : minX;
			
			extend[1] = maxY > minY ? maxY : minY;
			extend[3] = minY < maxY ? minY : maxY;
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i ++) {
				files[i].delete();
			}
			file.delete();
		}
		
		List<InputSplit> pathList = new ArrayList<InputSplit>();
		
 		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
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
		
		int orgRowl = 0;
		int orgCol = 0;
		double oriY = adfGeoTransform[3];
		if (geojsonPath != null) {
//			double dTemp = adfGeoTransform[1]*adfGeoTransform[5] - adfGeoTransform[2]*adfGeoTransform[4];
//	        double dCol = 0.0, dRow = 0.0;
//	        dCol = (-dstResolution*(extend[0] - adfGeoTransform[0]) - adfGeoTransform[2]*(extend[1] - adfGeoTransform[3])) / dTemp + 0.5;
//	        dRow = (dstResolution*(extend[1] - adfGeoTransform[3]) - adfGeoTransform[4]*(extend[0] - adfGeoTransform[0])) / dTemp + 0.5;
//			orgRowl = (int)(dRow);
//			orgCol = (int)dCol;
//			adfGeoTransform[0] = extend[0];
//			adfGeoTransform[3] = extend[1];
			oriY = extend[1];
			width = (int)((extend[2] - extend[0])/adfGeoTransform[1]);
			height = (int)((extend[1] - extend[3])/Math.abs(adfGeoTransform[5]));
		}
		
		for(; layers >= minLayers;) {
			if (geojsonPath != null) {
		        double dCol = (extend[0] - adfGeoTransform[0])/dstResolution;
		        double dRow = (extend[1] - adfGeoTransform[3])/-dstResolution;
//		        adfGeoTransform[0] = extend[0];
		        if (extend[1] - adfGeoTransform[3] > 0.000000000)
		        	orgRowl = 0;	
		        else 
		        	orgRowl = (int)(dRow);
		        
		        if (extend[0] - adfGeoTransform[0] > 0.000000000)
		        	orgCol = (int)dCol;
		        else
		        	orgCol = 0;
			}
			//将坐标转换成geowebcache地图服务的行列号
			tilerowTopLeft = (long) Math.floor((tileOriginY - oriY) / CoreConfig.HEIGHT_DEFAULT / dstResolution);

			//根据计算的左上角的行列号，反算该行列号实际对应的坐标
			yCoordinate = tileOriginY - tilerowTopLeft * dstResolution * CoreConfig.HEIGHT_DEFAULT; 
			//计算行列号实际对应的坐标与原始影像左上角的坐标的像素偏移量
			if(!bfloor) {
				tilerowOffset = 0;
			}else {
				tilerowOffset =(long) ((oriY - yCoordinate)/(-dstResolution));
			}
			
			layerName.replace(0, layerName.length(), Integer.toHexString(layers));
			if (layerName.length() == 1) {
				layerName.insert(0, "L0");
			} else {
				layerName.insert(0, "L");
			}
			layerName.insert(0, outputpath);
			dstWidth =(int) (width * adfGeoTransform[1] / dstResolution);   
			dstHeight = (int) (height*adfGeoTransform[5] /(-dstResolution));
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
				pathList.add(new ImageMutilLayersSplit(hdfspath, filename, layerName.toString(), orgCol, orgRowl + dstHeightReaded, dstWidth, dstHeightLen, dstResolution, layers));
				dstHeightReaded += dstHeightLen;
			}
			layers --;
			dstResolution = dstResolution * 2;
		}

		dataset.delete();
		List<FileStatus> files = listStatus(job);
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());

		LOG.debug("Total # of splits: " + pathList.size());
//		fs.close();
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
