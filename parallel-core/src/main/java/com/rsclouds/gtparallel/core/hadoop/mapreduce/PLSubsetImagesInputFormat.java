package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.gdal.gdal.gdal;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.hadoop.io.PLSubsetImageInfo;
import com.rsclouds.gtparallel.core.hadoop.io.PLSubsetMapRecord;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class PLSubsetImagesInputFormat extends FileInputFormat<PLSubsetMapRecord, NullWritable>{
	private static final Log LOG = LogFactory.getLog(PLSubsetImagesInputFormat.class);
	
	/**
	 * 获取目录下的所有文件
	 * @param fs
	 * @param dir    遍历目录
	 * @param paths  存放所有文件路径
	 * @return
	 */
	public boolean getPaths(FileSystem fs, Path dir, List<Path> paths) {
		try {
			FileStatus[] fileStatus = fs.listStatus(dir);
			for ( int i = 0; i < fileStatus.length; i++) {
				Path p = fileStatus[i].getPath();
				if(fs.isFile(p)){
					paths.add(p);
				}
				else {
					if (!getPaths(fs, p, paths)) {
						return false;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 计算分幅影像数据的输出信息（输出行列号、图层、偏移量）
	 * @param plSubSetInfoList
	 * @param outputLayer
	 */
	public void calcutePLSubSetInfoOutput(List<PLSubsetImageInfo> plSubSetInfoList, int outputLayer) {
		for (int i = 0; i < plSubSetInfoList.size(); i ++) {
			PLSubsetImageInfo tmpInfo = plSubSetInfoList.get(i);
			tmpInfo.setLayerOut(outputLayer);
			if (tmpInfo.getLayerOrg() >  outputLayer) {
				int intervalLayer = tmpInfo.getLayerOrg() - outputLayer;
				int times = (int)Math.pow(2,intervalLayer);
				int colOut = tmpInfo.getColOrg() / times;
				int rowOut = tmpInfo.getRowOrg() / times;
				int colRemainder = tmpInfo.getColOrg() % times;
				int rowRemainder = tmpInfo.getRowOrg() % times;
				if (times > 256) {//一个瓦片为256*256，所以最多分为256*256个像素，所以余数不可以超过256
					times = times / 256;
					colRemainder = colRemainder / times;
					rowRemainder = rowRemainder / times;
				}
				tmpInfo.setColOut(colOut);
				tmpInfo.setColRemainder(colRemainder);
				tmpInfo.setRowOut(rowOut);
				tmpInfo.setRowRemainder(rowRemainder);
				
			}
		}
	}
	
	
	/**
	 * the files have copied to every datanode's local filesystem 
	 * Generate the list of files and make them into FileSplits.
	 */
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		Configuration conf = job.getConfiguration();
		String rowkey = conf.get(CoreConfig.JOBID);
		int maxLayer = conf.getInt(CoreConfig.KEY_CURRENT_LAYER, 0);
		int minLayer = conf.getInt(CoreConfig.KEY_MIN_LAYER, 0);
		
//		double dstResolution = conf.getDouble(CoreConfig.KEY_DST_RESOLUTION, 0.0);
//		if (dstResolution == 0.0) {
//			LOG.info("Destination resolution is zero, that is error");
//			System.exit(1);
//		}
		String titleOutputDir = conf.get(CoreConfig.CUTTING_OUTPUTPATH);
		if (!titleOutputDir.endsWith("/")) {
			titleOutputDir += "/";
		}
		
		Map<String,String> map = new HashMap<String,String>();
		map.put(CoreConfig.JOB.JID.strVal, job.getJobID().toString());
		map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.RUNNING.toString());
		HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, rowkey,CoreConfig.JOB.FAMILY.strVal, map);
		
		List<InputSplit> splitList = new ArrayList<InputSplit>();
		
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		String inputpath = conf.get(CoreConfig.CUTTING_INPUTFILE);
		Path inputPath = new Path(inputpath);
		List<Path> pathList = new ArrayList<Path>();
		List<PLSubsetImageInfo> plSubSetInfoList = new ArrayList<PLSubsetImageInfo>();//存放原始分幅影像信息
		FileSystem fs = FileSystem.get(conf);
//		FileSystem fs = FileSystem.getLocal(conf);
		if (getPaths(fs, inputPath, pathList)) {//遍历所有分幅影像数据
			int fileCount = pathList.size();
			//geowebcache格式的瓦片行列号
			int maxRowGeoWebCache = 0;
			int minRowGeoWebCache = Integer.MAX_VALUE;
			//获取影像格网对应的层级
			int gridLayer = conf.getInt(CoreConfig.KEY_GRID_LAYER_INT, 12);
			double OriginX = -20037508.342787, OriginY = -20037508.342787;
			double gridResolution = 156543.033928/Math.pow(2, gridLayer);
			double topLeftLat, topLefLon;
			//计算分幅影像范围
			for (int i = 0; i < fileCount; i ++) {
				String fileName = pathList.get(i).getName();
				int indexofFirst = fileName.indexOf("-");
				int indexofSecond = fileName.lastIndexOf("-");
				int indexofPoint = fileName.lastIndexOf(".");
				String colStr = fileName.substring(indexofFirst + 1, indexofSecond -1);
				String rowStr = fileName.substring(indexofSecond + 1, indexofPoint -1);
				//pl 自定的瓦片行列号
				int colPL = Integer.parseInt(colStr);
				int rowPL = Integer.parseInt(rowStr);
				//计算影像左上角坐标
				//topLeftLat = (rowPL+1) * gridResolution * 256 + OriginY;
				//topLefLon = colPL * gridResolution * 256 + OriginX;
				// 计算geowebcache格式的行列号
				int colGeowebcache = colPL;//(int) Math.floor((20037508.342787 + topLefLon) / 256 / gridResolution); 
				int rowGeowbcache = 4095 - rowPL;//(int)Math.floor((20037508.342787 - topLeftLat) / 256 / gridResolution);
				maxRowGeoWebCache = maxRowGeoWebCache > rowGeowbcache ? maxRowGeoWebCache : rowGeowbcache;
				minRowGeoWebCache = minRowGeoWebCache < rowGeowbcache ? minRowGeoWebCache : rowGeowbcache;
				plSubSetInfoList.add(new PLSubsetImageInfo(rowGeowbcache, colGeowebcache, gridLayer, pathList.get(i).toString()));
			}
			
			if (maxLayer < gridLayer) {
				maxRowGeoWebCache = maxRowGeoWebCache / (int)Math.pow(2, gridLayer-maxLayer);
				minRowGeoWebCache = minRowGeoWebCache / (int)Math.pow(2, gridLayer-maxLayer);
			}
			
			//每个分块需要处理的分幅影像
			List<PLSubsetImageInfo> perSplitPLSubsetInfoList = new ArrayList<PLSubsetImageInfo>();
			for (int layerCurrent = maxLayer; layerCurrent >= minLayer; ) {
				int rowCount = maxRowGeoWebCache - minRowGeoWebCache + 1;
				int intervalRow =10;
				if (intervalRow < rowCount) {
					while (rowCount / intervalRow > 5000) {
						intervalRow ++;
					}
				}
				calcutePLSubSetInfoOutput(plSubSetInfoList, layerCurrent);
				Collections.sort(plSubSetInfoList);
				//分块
				int plInfoListIndex = 0;
				for (int rowCurrent = minRowGeoWebCache; rowCurrent <= maxRowGeoWebCache;) {
					if (gridLayer <= layerCurrent) {
						for (; plInfoListIndex < plSubSetInfoList.size(); ) {
							PLSubsetImageInfo tmpInfo = plSubSetInfoList.get(plInfoListIndex);
							if (tmpInfo.getRowOrg() >= rowCurrent && tmpInfo.getRowOrg() <= rowCurrent + intervalRow - 1) {
								perSplitPLSubsetInfoList.add(tmpInfo);
								plInfoListIndex ++;
							}else {
								break;
							}
						}
						rowCurrent = rowCurrent + intervalRow;
					}else {
						for (; plInfoListIndex < plSubSetInfoList.size(); ) {
							PLSubsetImageInfo tmpInfo = plSubSetInfoList.get(plInfoListIndex);
							if (tmpInfo.getRowOut() >= rowCurrent && tmpInfo.getRowOut() <= rowCurrent + intervalRow - 1) {
								perSplitPLSubsetInfoList.add(tmpInfo);
								plInfoListIndex ++;
							}else {
								break;
							}
						}
						rowCurrent = rowCurrent + intervalRow;
					}
					if (perSplitPLSubsetInfoList.size() > 0)
					splitList.add(new PLSubsetSplit(perSplitPLSubsetInfoList));
					perSplitPLSubsetInfoList.clear();
				}
				layerCurrent --;
				if (layerCurrent < gridLayer) {
					maxRowGeoWebCache = maxRowGeoWebCache / 2;
					minRowGeoWebCache = minRowGeoWebCache / 2;
				}
			}
		}	
		List<FileStatus> files = listStatus(job);
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());
		LOG.debug("Total # of splits: " + splitList.size());
		return splitList;
	}
	
	
	@Override
	public RecordReader<PLSubsetMapRecord, NullWritable> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException,
			InterruptedException {
		PLSubsetRecordreader recordReader = new PLSubsetRecordreader();
		return recordReader;
	}
	
//	public static void main(String[] args) {
//		Configuration conf = new Configuration();
//		try {
//			FileSystem fs = FileSystem.get(conf);
//			Path pathDir = new Path("hdfs://node03.rsclouds.cn:8020/temp");
//			List<Path> paths = new ArrayList<Path>();
//			getPaths(fs, pathDir, paths);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

}
