package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class FileInfo implements WritableComparable<FileInfo>{
	
	private int length;
	private int rowCoor;
	private int colCoor;
	private int width;
	private int xOringin;
	private int yOrigin;
	private int readwidth;
	private int readheight;
	private int height;
	private boolean bupdate;
	private boolean isGeometry;
	
	private BytesWritable values;
	private Text filename;
	private Text MD5;
	private Text timeText;

	public FileInfo(){		
		length = 0;
		rowCoor = 0;
		colCoor = 0;
		width = 0;
		height = 0;
		xOringin = 0;
		yOrigin = 0;
		readwidth = 0;
		readheight = 0;
		bupdate = false;
		setGeometry(false);
		
		filename = new Text();
		MD5 = new Text();
		setTimeText(new Text());
		setValues(new BytesWritable());
	}
	
	public FileInfo(Text filename, Text MD5, Text timeText, int length, int rowCoor, int colCoor){
		this.filename = filename;
		this.MD5 = MD5;
		this.setTimeText(timeText);
		this.length = length;
		this.setRowCoor(rowCoor);
		this.setColCoor(colCoor);
	}
	
	public int getxOringin() {
		return xOringin;
	}
	public void setxOringin(int xOringin) {
		this.xOringin = xOringin;
	}
	public int getyOrigin() {
		return yOrigin;
	}
	public void setyOrigin(int yOrigin) {
		this.yOrigin = yOrigin;
	}
	public int getReadwidth() {
		return readwidth;
	}
	public void setReadwidth(int readwidth) {
		this.readwidth = readwidth;
	}
	public int getReadheight() {
		return readheight;
	}
	public void setReadheight(int readheight) {
		this.readheight = readheight;
	}
	public void setBupdate(boolean bupdate) {
		this.bupdate = bupdate;
	}
	public boolean getBupdate() {
		return bupdate;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	
	public Text getFilename() {
		return filename;
	}
	
	public void setMD5(Text mD5) {
		MD5 = mD5;
	}
	
	public void setMD5(String mD5){
		MD5.set(mD5);
	}
	
	public Text getMD5(){
		return this.MD5;
	}

	public void setFilename(Text filename) {
		this.filename = filename;
	}
	
	public void setFilename(String str){
		filename.set(str);
	}
	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
	
	public Text getTimeText() {
		return timeText;
	}
	public void setTimeText(Text timeText) {
		this.timeText = timeText;
	}
	public void setTimeText(String timeStr) {
		this.timeText.set(timeStr);
	}

	public int getRowCoor() {
		return rowCoor;
	}
	public void setRowCoor(int rowCoor) {
		this.rowCoor = rowCoor;
	}
	public int getColCoor() {
		return colCoor;
	}
	public void setColCoor(int colCoor) {
		this.colCoor = colCoor;
	}
	public BytesWritable getValues() {
		return values;
	}
	public void setValues(BytesWritable values) {
		this.values = values;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		length = in.readInt();
		colCoor = in.readInt();
		rowCoor = in.readInt();
		width = in.readInt();
		height = in.readInt();
		xOringin = in.readInt();
		yOrigin = in.readInt();
		readwidth = in.readInt();
		readheight = in.readInt();
		bupdate = in.readBoolean();
		isGeometry = in.readBoolean();
		
		filename.readFields(in);
		MD5.readFields(in);
		timeText.readFields(in);
		values.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {		
		out.writeInt(length);
		out.writeInt(colCoor);
		out.writeInt(rowCoor);
		out.writeInt(width);
		out.writeInt(height);
		out.writeInt(this.xOringin);
		out.writeInt(this.yOrigin);
		out.writeInt(this.readwidth);
		out.writeInt(this.readheight);
		out.writeBoolean(bupdate);
		out.writeBoolean(isGeometry);
		
		filename.write(out);
		MD5.write(out);
		timeText.write(out);
		values.write(out);
	}

	@Override
	public int compareTo(FileInfo o) {
		return this.filename.compareTo(o.filename);
	}

	public boolean isGeometry() {
		return isGeometry;
	}

	public void setGeometry(boolean isGeometry) {
		this.isGeometry = isGeometry;
	}
	

}
