package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipInputFormat  extends FileInputFormat<Text, BytesWritable> {

	public static final Logger LOG = LoggerFactory.getLogger(ZipInputFormat.class);
	
	@Override
	public boolean isSplitable(JobContext context, Path filename) {
		return false;
	}

	
	@Override
	public RecordReader<Text, BytesWritable> createRecordReader(
			InputSplit arg0, TaskAttemptContext arg1) throws IOException,
			InterruptedException {
		// TODO Auto-generated method stub
		ZipRecordReader reader = new ZipRecordReader();
		return reader;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}
