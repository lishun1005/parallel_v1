package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class ScaleRowColText implements WritableComparable<ScaleRowColText>{
	private int scale;
	private long col;
	private long row;
	public int getScale() {
		return scale;
	}
	public void setScale(int scale) {
		this.scale = scale;
	}
	public long getCol() {
		return col;
	}
	public void setCol(long col) {
		this.col = col;
	}
	public long getRow() {
		return row;
	}
	public void setRow(long row) {
		this.row = row;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		this.scale = in.readInt();
		this.col = in.readLong();
		this.row = in.readLong();
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.scale);
		out.writeLong(this.col);
		out.writeLong(this.row);
	}

	
	@Override
	public String toString() {
		return this.scale + "_" + this.row + "_" + this.col;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ScaleRowColText))
			return false;
		ScaleRowColText other = (ScaleRowColText) o;
		if (this.scale == other.getScale()) {
			if (this.row == other.getRow()) {
				if (this.col == other.getCol()) {
					return true;
				}
			}
		}
		return false;
	}

	public int hashCode() {
		return (int) (this.scale * row * 163 / 2 + col*this.scale/2);
	}
	
	@Override
	public int compareTo(ScaleRowColText tmp) {
		long cmpLong = this.scale - tmp.getScale();
		if (cmpLong == 0) {
			cmpLong = this.row - tmp.getRow();
			if ( cmpLong == 0) {
				cmpLong = this.col - tmp.getCol();
			}
		}
		int cmp = 0;
		if (cmpLong < 0) {
			cmp = -1;
		}else if(cmpLong > 0) {
			cmp = 1;
		}
		return cmp;
	}
}
