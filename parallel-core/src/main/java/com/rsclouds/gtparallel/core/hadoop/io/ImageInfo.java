package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class ImageInfo implements WritableComparable<ImageInfo>{
	
	private int xOrigin;
	private int yOrigin;
	private int xLength;
	private int yLength;
	private double[] max;
	private double[] min;	
	private int[] bands;
	private double dstResolution;
	private Text filepath = new Text();;

	
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

	public Text getFilepath() {
		return filepath;
	}
	
	public double getDstResolution() {
		return dstResolution;
	}

	public void setDstResolution(double dstResolution) {
		this.dstResolution = dstResolution;
	}

	public void setFilepath(Text filepath) {
		this.filepath.set(filepath);
	}
	
	public void setFilepath(String filepath) {
		this.filepath.set(filepath);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(xOrigin);
		out.writeInt(xLength);
		out.writeInt(yOrigin);
		out.writeInt(yLength);
		if(max!= null && min != null) {
			for(int i = 0; i < max.length; i ++) {
				out.writeDouble(max[i]);
			}
			for(int i = 0; i < min.length; i ++) {
				out.writeDouble(min[i]);
			}
		}
		if (bands != null) {
			for (int i = 0; i < bands.length; i ++) {
				out.writeInt(bands[i]);
			}
		}
		out.writeDouble(dstResolution);
		filepath.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.xOrigin = in.readInt();
		this.xLength = in.readInt();
		this.yOrigin = in.readInt();
		this.yLength = in.readInt();
		if (max != null && min != null) {
			for(int i = 0; i < max.length; i ++) {
				max[i] = in.readDouble();
			}
			for(int i = 0; i < min.length; i ++) {
				min[i] = in.readDouble();
			}
		}
		if (bands != null) {
			for (int i = 0; i < bands.length; i ++) {
				bands[i] = in.readInt();
			}
		}
		dstResolution = in.readDouble();
		filepath.readFields(in);
	}

	@Override
	public int compareTo(ImageInfo tmp) {
		int cmp = this.xOrigin - tmp.xOrigin;
		if (cmp == 0) {
			cmp = this.yOrigin -tmp.yOrigin;
		}
		return cmp;
	}

	

}
