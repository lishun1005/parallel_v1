package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.WritableComparable;

public class CoordinateValue implements WritableComparable<CoordinateValue>{
	private int row_coor;
	private int col_coor;
	private BytesWritable MD5;

	public CoordinateValue(){
		MD5 = new BytesWritable();
	}
	public BytesWritable getMD5() {
		return MD5;
	}
	public void setMD5(BytesWritable mD5) {
		this.MD5 = mD5;
	}
	public void setMD5(byte[] mD5){
		this.MD5.set(mD5, 0, mD5.length);
	}
	public int getRow_coor() {
		return row_coor;
	}

	public void setRow_coor(int row_coor) {
		this.row_coor = row_coor;
	}

	public int getCol_coor() {
		return col_coor;
	}

	public void setCol_coor(int col_coor) {
		this.col_coor = col_coor;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.row_coor = in.readInt();
		this.col_coor = in.readInt();		
		this.MD5.readFields(in);
		
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.row_coor);
		out.writeInt(this.col_coor);
		this.MD5.write(out);
//		value.write(out);
	}

	@Override
	public int compareTo(CoordinateValue coorValue) {
		if (this.row_coor < coorValue.getRow_coor()){
			return -1;
		}else if( this.row_coor > coorValue.getRow_coor() ){
			return 1;
		}else{
			if (this.col_coor < coorValue.getCol_coor()){
				return -1;
			}else if( this.col_coor > coorValue.getCol_coor() ){
				return 1;
			}
		}
		return 0;
	}

}
