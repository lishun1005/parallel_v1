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

public class ImageBlockByShpInputFormat extends FileInputFormat<ImageMutilLayersInfo, NullWritable>{
	private static final Log LOG = LogFactory.getLog(ImageBlockByShpInputFormat.class);
	
	
	/**
	 * the files have copied to every datanode's local filesystem 
	 * Generate the list of files and make them into FileSplits.
	 */
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		List<InputSplit> pathList = new ArrayList<InputSplit>();
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
		
		String shpFilePath = conf.get("shpFilePath", null);
		String localShpFilePath = null;
		GeometryBase geoBase = null;
		if (shpFilePath != null) {
			int indexof = shpFilePath.lastIndexOf('/');
			localShpFilePath = "F://home//yarn//cutting_temp//" + job.getJobID().toString() + "//" + shpFilePath.substring(indexof+1);
			File file = new File("F://home//yarn//cutting_temp//");
			if (!file.exists()) {
				file.mkdirs();
			}
			FileSystem fs = FileSystem.get(conf);
			Path inputPath = new Path(shpFilePath);
			fs.copyToLocalFile(inputPath.getParent(), new Path("F://home//yarn//cutting_temp//" + job.getJobID().toString()));
			geoBase = new GeometryBase(localShpFilePath);
		}
		
		
		
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		String filename = conf.get(CoreConfig.CUTTING_INPUTFILE);
		Dataset dataset = gdal.Open(filename, gdalconstConstants.GA_ReadOnly);
		//Dataset dataset = gdal.Open("D://nanlin//guangzhou_051401.tiff", gdalconstConstants.GA_ReadOnly); //guangzhou_051401.tiff //t1_byte.tif
		//Dataset dataset =  gdal.Open("F://hunan//hunan_xiangqian_clip.tif", gdalconstConstants.GA_ReadOnly);
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
		int orgRow = 0;
		int orgCol = 0;
		double oriY = adfGeoTransform[3];
		//
		double[] extend = new double[4];
		if (geoBase != null) {
			GeometryBase imgGeo = new GeometryBase();
			double[] x = new double[5];
			double[] y = new double[5];
			x[0] = adfGeoTransform[0];
			y[0] = adfGeoTransform[3];
			x[1] = adfGeoTransform[0] + width * adfGeoTransform[1];
			y[1] = y[0];
			x[2] = x[1];
			y[2] = adfGeoTransform[3] + height * adfGeoTransform[5];
			x[3] = x[0];
			y[3] = y[2];
			x[4] = adfGeoTransform[0];
			y[4] = adfGeoTransform[3];
			Geometry geo = imgGeo.createPolygon(x, y, 0);
			geo = geoBase.getGeometry().Intersection(geo);
			geoBase.getGeometry().GetEnvelope(extend);
			width = (int)(Math.abs((extend[1] - extend[0])/adfGeoTransform[1]));
			height = (int)(Math.abs((extend[3] - extend[2])/adfGeoTransform[5]));
			oriY = extend[1];
		}
		
		for(; layers >= minLayers;) {
			if (geoBase != null) {
				double dCol = (extend[0] - adfGeoTransform[0])/dstResolution;
		        double dRow = (extend[1] - adfGeoTransform[3])/-dstResolution;
		        if (extend[1] - adfGeoTransform[3] > 0.000000000)
		        	orgRow = 0;	
		        else 
		        	orgRow = (int)(dRow);
		        
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
				pathList.add(new ImageMutilLayersSplit(hdfspath, filename, layerName.toString(), orgCol, orgRow+dstHeightReaded, dstWidth, dstHeightLen, dstResolution, layers));
				dstHeightReaded += dstHeightLen;
			}
			layers --;
			dstResolution = dstResolution * 2;
		}

		dataset.delete();
		List<FileStatus> files = listStatus(job);
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());
		
		if(localShpFilePath != null) {
			File file  = new File(localShpFilePath);
			File dir = file.getParentFile();
			File[] fileList = dir.listFiles();
			for(int i = 0; fileList != null && i < fileList.length; i ++) {
				fileList[i].delete();
			}
			dir.delete();
		}
	
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
