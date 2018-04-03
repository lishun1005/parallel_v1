package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.gdal.gdal.gdal;

import com.rsclouds.gtparallel.core.hadoop.io.PLSubsetImageInfo;
import com.rsclouds.gtparallel.core.hadoop.io.PLSubsetMapRecord;

public class PLSubsetRecordreader extends RecordReader<PLSubsetMapRecord, NullWritable>{
	private boolean processed = false;
	private int totalImage;
	private int dealImage;
	
	private PLSubsetMapRecord key = new PLSubsetMapRecord();
	private PLSubsetSplit plSplit;
	private int layerOrg;
	private int layerOut;
	private List<PLSubsetImageInfo> persplitPLSubsetInfoList;  //每个分片包含的所有影像信息
	private List<PLSubsetImageInfo> perRecordPLSubsetInfoList; //每个key-value需要处理的所有影像的信息
	
	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {		
		plSplit = (PLSubsetSplit)split;
		persplitPLSubsetInfoList = plSplit.getPerSplitPLSubsetInfoList();
		perRecordPLSubsetInfoList = new ArrayList<PLSubsetImageInfo>();
		totalImage = persplitPLSubsetInfoList.size();
		dealImage = 0;
		if (totalImage > 0) {
			PLSubsetImageInfo tmp = persplitPLSubsetInfoList.get(0);
			layerOrg = tmp.getLayerOrg();
			layerOut = tmp.getLayerOut();
		}
		gdal.AllRegister();
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if (dealImage < totalImage) {
			if (layerOrg > layerOut) {
				PLSubsetImageInfo tmp = null;
				perRecordPLSubsetInfoList.clear();
				boolean sameRowCol = true;
				while (sameRowCol) {
					tmp = persplitPLSubsetInfoList.get(dealImage);
					perRecordPLSubsetInfoList.add(tmp);
					dealImage ++;
					if (dealImage < totalImage) {
						PLSubsetImageInfo tmp1 = persplitPLSubsetInfoList.get(dealImage);
						if (tmp1.getColOut() != tmp.getColOut() || tmp1.getRowOut() != tmp.getRowOut()) {
							sameRowCol = false;
						}
					}else {
						sameRowCol = false;
					}
				}
				key.setPlSubsetImagePathList(perRecordPLSubsetInfoList);
			}else {
				PLSubsetImageInfo tmp = persplitPLSubsetInfoList.get(dealImage);
				perRecordPLSubsetInfoList.clear();
				perRecordPLSubsetInfoList.add(tmp);
				key.setPlSubsetImagePathList(perRecordPLSubsetInfoList);
				dealImage ++;
			}
			return true;
		}
		processed = true;
		return false;
	}

	@Override
	public PLSubsetMapRecord getCurrentKey() throws IOException,
			InterruptedException {
		return key;
	}

	@Override
	public NullWritable getCurrentValue() throws IOException,
			InterruptedException {
		return null;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return processed ? 1.0f : (float)(dealImage * 1.0 / totalImage);
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
