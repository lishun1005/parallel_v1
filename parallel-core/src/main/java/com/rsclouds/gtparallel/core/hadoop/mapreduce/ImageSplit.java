package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class ImageSplit extends FileSplit{
	private int xOrigin;
	private int yOrigin;
	private int xLength;
	private int yLength;
	private double[] max;
	private double[] min;
	private int[] bands;
	
	public ImageSplit() {
		super();
		xOrigin = 0;
		yOrigin = 0;
		xLength = 0;
		yLength = 0;
		max = null;
		min = null;
		bands = new int[3];
		bands[0] = 1;
		bands[1] = 2;
		bands[2] = 3;
	}
	
	public ImageSplit(String fileName, int xOrigin, int yOrigin, int xLength, int yLength) {
		super(new Path(fileName), xLength, yLength, null);
		this.xOrigin = xOrigin;
		this.yOrigin = yOrigin;
		this.xLength = xLength;
		this.yLength = yLength;
		max = null;
		min = null;
		bands = new int[3];
		bands[0] = 1;
		bands[1] = 2;
		bands[2] = 3;	
	}
	
	public ImageSplit(String fileName, int xOrigin, int yOrigin, int xLength, int yLength, double[] max, double[] min, int[] bands) {
		super(new Path(fileName), xLength, yLength, null);
		this.xOrigin = xOrigin;
		this.yOrigin = yOrigin;
		this.xLength = xLength;
		this.yLength = yLength;
		this.max = max;
		this.min = min;
		this.bands = bands;
	}

	public int getxOrigin() {
		return xOrigin;
	}

	public void setxOrigin(int xOrigin) {
		this.xOrigin = xOrigin;
	}

	public int getyOrigin() {
		return yOrigin;
	}

	public void setyOrigin(int yOrigin) {
		this.yOrigin = yOrigin;
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

	public double[] getMax() {
		return max;
	}

	public void setMax(double[] max) {
		this.max = max;
	}

	public double[] getMin() {
		return min;
	}

	public void setMin(double[] min) {
		this.min = min;
	}
	
	
	public int[] getBands() {
		return bands;
	}

	public void setBands(int[] bands) {
		this.bands = bands;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		out.writeInt(xOrigin);
		out.writeInt(xLength);
		out.writeInt(yOrigin);
		out.writeInt(yLength);
		if(max!= null && min != null) {
			out.writeInt(max.length);
			for(int i = 0; i < max.length; i ++) {
				out.writeDouble(max[i]);
			}
			out.writeInt(min.length);
			for(int i = 0; i < min.length; i ++) {
				out.writeDouble(min[i]);
			}
		}else {
			out.writeInt(0);
			out.writeInt(0);
		}
		if (bands != null) {
			out.writeInt(bands.length);
			for (int i = 0; i < bands.length; i ++) {
				out.writeInt(bands[i]);
			}
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		this.xOrigin = in.readInt();
		this.xLength = in.readInt();
		this.yOrigin = in.readInt();
		this.yLength = in.readInt();
		int maxCount = in.readInt();
		if (maxCount != 0) {
			max = new double[maxCount];
			for(int i = 0; i < maxCount; i ++) {
				max[i] = in.readDouble();
			}
		}
		int minCount = in.readInt();
		if (minCount != 0) {
			max = new double[minCount];
			for(int i = 0; i < minCount; i ++) {
				min[i] = in.readDouble();
			}
		}
		int bandCount = in.readInt();
		if (bandCount != 0) {
			bands = new int[bandCount];
			for (int i = 0; i < bandCount; i ++) {
				bands[i] = in.readInt();
			}
		}
	}

}
