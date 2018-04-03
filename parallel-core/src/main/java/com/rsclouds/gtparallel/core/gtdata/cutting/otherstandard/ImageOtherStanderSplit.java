package com.rsclouds.gtparallel.core.gtdata.cutting.otherstandard;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class ImageOtherStanderSplit extends FileSplit{
	private int currentLayers;
	private long xOrigin;
	private long yOrigin;
	private long xLength;
	private long yLength;
	private double[] max;
	private double[] min;
	private int[] bands;
	private Text gtdataOutputPath = new Text();
	private double dstresolution;
	
	public ImageOtherStanderSplit() {
		super();
		setCurrentLayers(0);
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
		dstresolution = 0;
	}
	
	public ImageOtherStanderSplit(String fileName, String outputPath, long xOrigin, long yOrigin, long xLength, long yLength, double resolution, int currentLayers) {
		super(new Path(fileName), xLength, yLength, null);
		this.setCurrentLayers(currentLayers);
		this.gtdataOutputPath.set(outputPath);
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
		dstresolution = resolution;
		
	}
	
	public ImageOtherStanderSplit(String fileName, String outputPath, long xOrigin, long yOrigin, long xLength, long yLength, double[] max, double[] min, double resolution, int[] bands, int currentLayers) {
		super(new Path(fileName), xLength, yLength, null);
		this.gtdataOutputPath.set(outputPath);
		this.xOrigin = xOrigin;
		this.yOrigin = yOrigin;
		this.xLength = xLength;
		this.yLength = yLength;
		this.max = max;
		this.min = min;
		this.bands = bands;
		this.dstresolution = resolution;
		this.setCurrentLayers(currentLayers);
	}

	public Text getGtdataOutputPath() {
		return gtdataOutputPath;
	}

	public void setGtdataOutputPath(Text gtdataOutputPath) {
		this.gtdataOutputPath = gtdataOutputPath;
	}
	
	public void setGtdataOutputPath(String gtdataOutputPath) {
		this.gtdataOutputPath.set(gtdataOutputPath);
	}

	public double getDstresolution() {
		return dstresolution;
	}

	public void setDstresolution(double dstresolution) {
		this.dstresolution = dstresolution;
	}

	public long getxOrigin() {
		return xOrigin;
	}

	public void setxOrigin(long xOrigin) {
		this.xOrigin = xOrigin;
	}

	public long getyOrigin() {
		return yOrigin;
	}

	public void setyOrigin(long yOrigin) {
		this.yOrigin = yOrigin;
	}

	public long getxLength() {
		return xLength;
	}

	public void setxLength(long xLength) {
		this.xLength = xLength;
	}

	public long getyLength() {
		return yLength;
	}

	public void setyLength(long yLength) {
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
		out.writeInt(this.currentLayers);
		out.writeLong(xOrigin);
		out.writeLong(xLength);
		out.writeLong(yOrigin);
		out.writeLong(yLength);
		this.gtdataOutputPath.write(out);
		out.writeDouble(dstresolution);
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
		this.currentLayers = in.readInt();
		this.xOrigin = in.readLong();
		this.xLength = in.readLong();
		this.yOrigin = in.readLong();
		this.yLength = in.readLong();
		this.gtdataOutputPath.readFields(in);
		this.dstresolution = in.readDouble();
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

	public int getCurrentLayers() {
		return currentLayers;
	}

	public void setCurrentLayers(int currentLayers) {
		this.currentLayers = currentLayers;
	}
}
