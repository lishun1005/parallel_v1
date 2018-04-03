package com.rsclouds.gtparallel.core.hadoop.io;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class RowColText implements WritableComparable<RowColText> {

	private long col;
	private long row;

	public long getRow() {
		return row;
	}

	public void setRow(long row) {
		this.row = row;
	}

	public long getCol() {
		return col;
	}

	public void setCol(long col) {
		this.col = col;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		row = in.readLong();
		col = in.readLong();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(row);
		out.writeLong(col);
	}

	@Override
	public String toString() {
		return row + "_" + col;
	}

	public boolean equals(Object o) {
		if (!(o instanceof RowColText))
			return false;
		RowColText other = (RowColText) o;
		if (row == other.getRow()) {
			if (col == other.getCol()) {
				return true;
			}
		}
		return false;
	}

	public int hashCode() {
		return (int) (row * 163 + col);
	}

	@Override
	public int compareTo(RowColText rc) {
		if (this.row < rc.getRow()){
			return -1;
		}else if( this.row > rc.getRow() ){
			return 1;
		}else{
			if (this.col < rc.getCol()){
				return -1;
			}else if( this.col > rc.getCol() ){
				return 1;
			}
		}
		return 0;
	}

}
