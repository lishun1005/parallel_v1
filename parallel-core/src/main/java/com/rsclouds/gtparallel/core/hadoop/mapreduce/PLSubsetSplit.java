package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import com.rsclouds.gtparallel.core.hadoop.io.PLSubsetImageInfo;

public class PLSubsetSplit extends FileSplit{
	private List<PLSubsetImageInfo> perSplitPLSubsetInfoList;
	public PLSubsetSplit() {
		setPerSplitPLSubsetInfoList(new ArrayList<PLSubsetImageInfo>());
	}
	
	@SuppressWarnings("unchecked")
	public void deepCopy(List<PLSubsetImageInfo> src) throws IOException,
			ClassNotFoundException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(byteOut);
		out.writeObject(src);

		ByteArrayInputStream byteIn = new ByteArrayInputStream(
				byteOut.toByteArray());
		ObjectInputStream in = new ObjectInputStream(byteIn);
		setPerSplitPLSubsetInfoList((List<PLSubsetImageInfo>) in.readObject());
	} 
	
	public PLSubsetSplit(List<PLSubsetImageInfo> perSplitPLSubsetInfoList) {
		super(new Path(perSplitPLSubsetInfoList.get(0).getImagePath()), 0, 100, null);	
		try {
			deepCopy(perSplitPLSubsetInfoList);
		} catch (ClassNotFoundException e) {
			this.setPerSplitPLSubsetInfoList(new ArrayList<PLSubsetImageInfo>());
			e.printStackTrace();
		} catch (IOException e) {
			this.setPerSplitPLSubsetInfoList(new ArrayList<PLSubsetImageInfo>());
			e.printStackTrace();
		}
	}

	public List<PLSubsetImageInfo> getPerSplitPLSubsetInfoList() {
		return perSplitPLSubsetInfoList;
	}

	public void setPerSplitPLSubsetInfoList(List<PLSubsetImageInfo> perSplitPLSubsetInfoList) {
		this.perSplitPLSubsetInfoList = perSplitPLSubsetInfoList;
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		int size = this.perSplitPLSubsetInfoList.size();
		out.writeInt(size);
		for (int i = 0; i < size; i ++) {
			PLSubsetImageInfo info = perSplitPLSubsetInfoList.get(i);
			out.writeInt(info.getColOrg());
			out.writeInt(info.getRowOrg());
			out.writeInt(info.getLayerOrg());
			out.writeInt(info.getColOut());
			out.writeInt(info.getRowOut());
			out.writeInt(info.getLayerOut());
			out.writeInt(info.getColRemainder());
			out.writeInt(info.getRowRemainder());
			Text.writeString(out, info.getImagePath());
		}
		
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		int size = in.readInt();
		this.perSplitPLSubsetInfoList = new ArrayList<PLSubsetImageInfo>();
		for (int i = 0; i < size; i ++) {
			PLSubsetImageInfo info = new PLSubsetImageInfo(0,0,0);
			info.setColOrg(in.readInt());
			info.setRowOrg(in.readInt());
			info.setLayerOrg(in.readInt());
			info.setColOut(in.readInt());
			info.setRowOut(in.readInt());
			info.setLayerOut(in.readInt());
			info.setColRemainder(in.readInt());
			info.setRowRemainder(in.readInt());
			info.setImagePath(Text.readString(in));
			perSplitPLSubsetInfoList.add(info);
		}
	}
	
}
