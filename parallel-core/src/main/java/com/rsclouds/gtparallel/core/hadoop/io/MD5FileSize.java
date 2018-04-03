package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class MD5FileSize implements WritableComparable<MD5FileSize>{
	Text md5;
	LongWritable fileSize;
	
	public MD5FileSize(){
		md5 = new Text();
		fileSize = new LongWritable();
	}
	
	public MD5FileSize(String md5, long fileSize) {
		this.md5 = new Text(md5);
		this.fileSize = new LongWritable(fileSize);
	}

	public MD5FileSize(Text md5, LongWritable fileSize) {
		this.md5 = md5;
		this.fileSize = fileSize;
	}

	public Text getMd5() {
		return md5;
	}

	public void setMd5(Text md5) {
		this.md5 = md5;
	}
	
	public void setMd5(String md5) {
		this.md5.set(md5);
	}

	public LongWritable getFileSize() {
		return fileSize;
	}

	public void setFileSize(LongWritable fileSize) {
		this.fileSize = fileSize;
	}
	
	public void setFileSize(long fileSize) {
		this.fileSize.set(fileSize);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		md5.write(out);
		fileSize.write(out);
		
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		md5.readFields(in);
		fileSize.readFields(in);
		
	}

	@Override
	public int compareTo(MD5FileSize temp) {
		int compareInt = md5.compareTo(temp.md5);
		if ( 0 == compareInt)
			compareInt = fileSize.compareTo(temp.fileSize);
		return compareInt;
	}
}
