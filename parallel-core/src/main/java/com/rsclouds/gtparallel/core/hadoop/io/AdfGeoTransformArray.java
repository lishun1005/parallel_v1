package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class AdfGeoTransformArray implements WritableComparable<AdfGeoTransformArray> {

	// Storage the affine transformation coefficients
	private double[] adfGeoTransform;

	public AdfGeoTransformArray(){
		this.adfGeoTransform = new double[6];
	}
	
	public AdfGeoTransformArray(double[] doubleArray){
		this.adfGeoTransform = doubleArray;
	}
	public double[] getAdfGeoTransform() {
		return adfGeoTransform;
	}

	public void setAdfGeoTransform(double[] adfGeoTransform) {
		this.adfGeoTransform = adfGeoTransform;
	}

	@Override
	public int compareTo(AdfGeoTransformArray tmp) {
		double[] temp = tmp.getAdfGeoTransform();
		if (this.adfGeoTransform[0] != temp[0]) {
			return this.adfGeoTransform[0] - temp[0] > 0 ? 1 : -1;
		}
		if (this.adfGeoTransform[1] != temp[1]) {
			return this.adfGeoTransform[1] - temp[1] > 0 ? 1 : -1;
		}
		if (this.adfGeoTransform[2] != temp[2]) {
			return this.adfGeoTransform[2] - temp[2] > 0 ? 1 : -1;
		}
		if (this.adfGeoTransform[3] != temp[3]) {
			return this.adfGeoTransform[3] - temp[3] > 0 ? 1 : -1;
		}
		if (this.adfGeoTransform[4] != temp[4]) {
			return this.adfGeoTransform[0] - temp[0] > 0 ? 1 : -1;
		}
		if (this.adfGeoTransform[5] != temp[5]) {
			return this.adfGeoTransform[5] - temp[5] > 0 ? 1 : -1;
		}
		return 0;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		for (int i = 0; i < 6; i ++){
			adfGeoTransform[i] = in.readDouble();
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		for (int i = 0; i < 6; i ++){
			out.writeDouble(adfGeoTransform[i]);
		}
	}

}
