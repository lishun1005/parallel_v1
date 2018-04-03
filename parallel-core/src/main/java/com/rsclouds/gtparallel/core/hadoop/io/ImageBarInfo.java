package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class ImageBarInfo implements WritableComparable<ImageBarInfo>{
	private int tileColOffset;
	private int tileRowOffset;
	private int xreadOrigin;
	private int yreadOrigin;
	private int xreadLen;
	private int yreadLen;
	private long colNum;
	private long rowNum;
	private double dstResolution;
	private double oriResolution;	
	private int[] bands;
	private Text filepath = new Text();
	

	public int[] getBands() {
		return bands;
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

	public int getXreadOrigin() {
		return xreadOrigin;
	}

	public void setXreadOrigin(int xreadOrigin) {
		this.xreadOrigin = xreadOrigin;
	}

	public int getYreadOrigin() {
		return yreadOrigin;
	}

	public void setYreadOrigin(int yreadOrigin) {
		this.yreadOrigin = yreadOrigin;
	}

	public int getXreadLen() {
		return xreadLen;
	}

	public void setXreadLen(int xreadLen) {
		this.xreadLen = xreadLen;
	}

	public int getYreadLen() {
		return yreadLen;
	}

	public void setYreadLen(int yreadLen) {
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
		out.writeInt(this.tileColOffset);
		out.writeInt(this.tileRowOffset);
		out.writeInt(this.xreadLen);
		out.writeInt(this.xreadOrigin);
		out.writeInt(this.yreadLen);
		out.writeInt(this.yreadOrigin);
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
		this.tileColOffset = in.readInt();
		this.tileRowOffset = in.readInt();
		this.xreadLen = in.readInt();
		this.xreadOrigin = in.readInt();
		this.yreadLen = in.readInt();
		this.yreadOrigin = in.readInt();
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
	public int compareTo(ImageBarInfo o) {
		int cmp = this.yreadOrigin - o.getYreadOrigin();
		if (cmp == 0)
			cmp = this.xreadOrigin - o.getXreadOrigin();
		return cmp;
	}

}
