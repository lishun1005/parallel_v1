package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class ImageMutilLayersInfo implements WritableComparable<ImageMutilLayersInfo>{
	private int tileColOffset;
	private int tileRowOffset;
	private int curentLayer = 0;
	private long xreadOrigin;
	private long yreadOrigin;
	private int  xreadOriginActual;
	private int  yreadOriginActual;
	private long xreadLen;
	private long yreadLen;
	private long colNum;
	private long rowNum;
	private double dstResolution;
	private double oriResolution;	
	private int[] bands;
	private Text filepath = new Text();
	private Text gtdataOutputPath = new Text();
	

	public int[] getBands() {
		return bands;
	}
	
	public Text getGtdataOutputPath() {
		return gtdataOutputPath;
	}

	public void setGtdataOutputPath(Text gtdataOutputPath) {
		this.gtdataOutputPath = gtdataOutputPath;
	}

	public void setGtDataOutputPath(String outputpath) {
		this.gtdataOutputPath.set(outputpath);
	}
	
	public void setBands(int[] bands) {
		this.bands = bands;
	}

	public Text getFilepath() {
		return filepath;
	}

	public void setFilepath(Text filepath) {
		this.filepath = filepath;
	}
	
	public void setFilePath(String filepath) {
		this.filepath.set(filepath);
	}
	
	public int getTileColOffset() {
		return tileColOffset;
	}

	public void setTileColOffset(int tileColOffset) {
		this.tileColOffset = tileColOffset;
	}

	public int getTileRowOffset() {
		return tileRowOffset;
	}

	public void setTileRowOffset(int tileRowOffset) {
		this.tileRowOffset = tileRowOffset;
	}

	public long getColNum() {
		return colNum;
	}

	public void setColNum(long colNum) {
		this.colNum = colNum;
	}

	public long getRowNum() {
		return rowNum;
	}

	public void setRowNum(long rowNum) {
		this.rowNum = rowNum;
	}

	public long getXreadOrigin() {
		return xreadOrigin;
	}

	public void setXreadOrigin(long xreadOrigin) {
		this.xreadOrigin = xreadOrigin;
	}

	public long getYreadOrigin() {
		return yreadOrigin;
	}
	
	public int getXreadOriginActual() {
		return xreadOriginActual;
	}

	public void setXreadOriginActual(int xreadOriginActual) {
		this.xreadOriginActual = xreadOriginActual;
	}

	public int getYreadOriginActual() {
		return yreadOriginActual;
	}

	public void setYreadOriginActual(int yreadOriginActual) {
		this.yreadOriginActual = yreadOriginActual;
	}

	public void setYreadOrigin(long yreadOrigin) {
		this.yreadOrigin = yreadOrigin;
	}

	public long getXreadLen() {
		return xreadLen;
	}

	public void setXreadLen(long xreadLen) {
		this.xreadLen = xreadLen;
	}

	public long getYreadLen() {
		return yreadLen;
	}

	public void setYreadLen(long yreadLen) {
		this.yreadLen = yreadLen;
	}

	public double getDstResolution() {
		return dstResolution;
	}

	public void setDstResolution(double dstResolution) {
		this.dstResolution = dstResolution;
	}

	public double getOriResolution() {
		return oriResolution;
	}

	public void setOriResolution(double oriResolution) {
		this.oriResolution = oriResolution;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.curentLayer);
		out.writeInt(this.tileColOffset);
		out.writeInt(this.tileRowOffset);
		out.writeLong(this.xreadLen);
		out.writeLong(this.xreadOrigin);
		out.writeLong(this.yreadLen);
		out.writeLong(this.yreadOrigin);
		out.writeInt(this.xreadOriginActual);
		out.writeInt(this.yreadOriginActual);
		out.writeLong(this.colNum);
		out.writeLong(this.rowNum);
		out.writeDouble(this.dstResolution);
		out.writeDouble(this.oriResolution);
		if (bands != null) {
			out.writeInt(bands.length);
			for (int i = 0; i < bands.length; i ++) {
				out.writeInt(bands[i]);
			}
		}else {
			out.writeInt(0);
		}
		filepath.write(out);
		
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.curentLayer = in.readInt();
		this.tileColOffset = in.readInt();
		this.tileRowOffset = in.readInt();
		this.xreadLen = in.readLong();
		this.xreadOrigin = in.readLong();
		this.yreadLen = in.readLong();
		this.yreadOrigin = in.readLong();
		this.xreadOriginActual = in.readInt();
		this.yreadOriginActual = in.readInt();
		this.colNum = in.readLong();
		this.rowNum = in.readLong();
		this.dstResolution = in.readDouble();
		this.oriResolution = in.readDouble();
		int count = in.readInt();
		if (count != 0) {
			bands = new int[count];
			for (int i = 0; i < count; i ++) {
				bands[i] = in.readInt();
			}
		}
		filepath.readFields(in);
		
	}

	@Override
	public int compareTo(ImageMutilLayersInfo o) {
		int cmp = (int)(this.yreadOrigin - o.getYreadOrigin());
		if (cmp == 0)
			cmp = (int)(this.xreadOrigin - o.getXreadOrigin());
		return cmp;
	}

	public int getCurentLayer() {
		return curentLayer;
	}

	public void setCurentLayer(int curentLayer) {
		this.curentLayer = curentLayer;
	}
}
