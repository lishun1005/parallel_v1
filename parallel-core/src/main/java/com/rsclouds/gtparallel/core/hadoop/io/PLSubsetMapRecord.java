package com.rsclouds.gtparallel.core.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class PLSubsetMapRecord implements WritableComparable<PLSubsetMapRecord>{
	
	private List<PLSubsetImageInfo> perSplitPLSubsetInfoList;
	
	public List<PLSubsetImageInfo> getPerSplitPLSubsetInfoList() {
		return perSplitPLSubsetInfoList;
	}
	public void setPerSplitPLSubsetInfoList(
			List<PLSubsetImageInfo> perSplitPLSubsetInfoList) {
		this.perSplitPLSubsetInfoList = perSplitPLSubsetInfoList;
	}

	
	public PLSubsetMapRecord() {
		perSplitPLSubsetInfoList = new ArrayList<PLSubsetImageInfo>();
	}


	@Override
	public void write(DataOutput out) throws IOException {
		int size = perSplitPLSubsetInfoList.size();
		out.write(size);
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
		int size = in.readInt();
		perSplitPLSubsetInfoList = new ArrayList<PLSubsetImageInfo>();
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

	public List<PLSubsetImageInfo> getPlSubsetImagePathList() {
		return perSplitPLSubsetInfoList;
	}

	public void setPlSubsetImagePathList(List<PLSubsetImageInfo> plSubsetImagePathList) {
		this.perSplitPLSubsetInfoList = plSubsetImagePathList;
	}

	@Override
	public int compareTo(PLSubsetMapRecord o) {
		return 0;
	}

}
