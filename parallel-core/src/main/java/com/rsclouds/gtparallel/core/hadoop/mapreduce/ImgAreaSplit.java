package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;


public class ImgAreaSplit extends FileSplit {
	private int xOrigin;
	private int yOrigin;
	private int xLength;
	private int yLength;
	
	public ImgAreaSplit(){
		super();
		xOrigin = 0;
		yOrigin = 0;
		xLength = 0;
		yLength = 0;
	}
	
	public ImgAreaSplit(String fileName, int x, int xLength, int y, int yLength){
		super(new Path(fileName), x, y, null);
		this.xOrigin = x;
		this.xLength = xLength;
		this.yOrigin = y;
		this.yLength = yLength;
	}

	public int getxLength() {
		return xLength;
	}

	public void setxLength(int xLength) {
		this.xLength = xLength;
	}

	public int getyLength() {
		return yLength;
	}

	public void setyLength(int yLength) {
		this.yLength = yLength;
	}

	public void setxOrigin(int xOrigin) {
		this.xOrigin = xOrigin;
	}
	public void setyOrigin(int yOrigin) {
		this.yOrigin = yOrigin;
	}
	public int getxOrigin() {
		return xOrigin;
	}
	public int getyOrigin() {
		return yOrigin;
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		out.writeInt(xOrigin);
		out.writeInt(xLength);
		out.writeInt(yOrigin);
		out.writeInt(yLength);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		this.xOrigin = in.readInt();
		this.xLength = in.readInt();
		this.yOrigin = in.readInt();
		this.yLength = in.readInt();
	} 
	

}
