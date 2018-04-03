package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.hadoop.io.ImageInfo;

public class ImageRecordReader extends
RecordReader<ImageInfo, NullWritable>{

	private boolean processed = false;
	private ImageInfo key = new ImageInfo();
	private ImageSplit fileSplit;
	
	
	
	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		double dstResolution = context.getConfiguration().getDouble(CoreConfig.KEY_DST_RESOLUTION, 0.0);
		fileSplit = (ImageSplit) split;
		key.setBands(fileSplit.getBands());
		System.out.println(fileSplit.getPath().toString());
		key.setFilepath(fileSplit.getPath().toString());
		key.setMax(fileSplit.getMax());
		key.setMin(fileSplit.getMin());
		key.setxLength(fileSplit.getxLength());
		key.setxOrigin(fileSplit.getxOrigin());
		key.setyLength(fileSplit.getyLength());
		key.setyOrigin(fileSplit.getyOrigin());
		key.setDstResolution(dstResolution);
		
	}


	@Override
	public NullWritable getCurrentValue() throws IOException,
			InterruptedException {
		return NullWritable.get();
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return processed ? 1 : 0;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public ImageInfo getCurrentKey() throws IOException, InterruptedException {
		return key;
	}
	
	
	
	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if (!processed) {
			processed = true;
			return true;
		}
		return false;
	}

}
