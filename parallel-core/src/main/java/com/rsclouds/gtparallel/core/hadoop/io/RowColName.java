package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class RowColName implements WritableComparable<RowColName> {
	private Text colName = new Text();
	private Text rowName = new Text();
	
	public Text getRowName() {
		return rowName;
	}
	public void setRowName(String str) {
		this.rowName.set(str);
	}
	public void setRowName(Text row) {
		this.rowName.set(row);
	}
	
	public Text getColName() {
		return colName;
	}
	public void setColName(Text colName) {
		this.colName.set(colName);
	}
	public void setColName(String str) {
		this.colName.set(str);
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		rowName.write(out);
		colName.write(out);
		
	}
	@Override
	public void readFields(DataInput in) throws IOException {
		rowName.readFields(in);
		colName.readFields(in);
		
	}
	@Override
	public int compareTo(RowColName tmp) {
		int cmp = tmp.getRowName().compareTo(this.rowName);
		if(cmp == 0) {
			cmp = tmp.getColName().compareTo(this.colName);
		}
		return cmp;
	}

}
