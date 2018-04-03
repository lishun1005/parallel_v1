package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;

import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class ImageSegemetReducerBase extends
		TableReducer<Text, FileInfo, NullWritable> {

	public void reduce(Text key, Iterable<FileInfo> values, Context context)
			throws IOException, InterruptedException {
		String gtpath = GtDataUtils.format2GtPath(key.toString());
		// 插入行号目录
		Put put = new Put(Bytes.toBytes(gtpath));
		put.add(GtDataConfig.META.FAMILY.byteVal,
				GtDataConfig.META.DFS.byteVal, Bytes.toBytes("0"));
		put.add(GtDataConfig.META.FAMILY.byteVal,
				GtDataConfig.META.SIZE.byteVal, Bytes.toBytes("-1"));
		put.add(GtDataConfig.META.FAMILY.byteVal,
				GtDataConfig.META.URL.byteVal, Bytes.toBytes(""));
		put.add(GtDataConfig.META.FAMILY.byteVal,
				GtDataConfig.META.TIME.byteVal,
				Bytes.toBytes("" + System.currentTimeMillis()));
		context.write(NullWritable.get(), put);

		for (FileInfo val : values) {
			if (val.getTimeText() == null || val.getTimeText().getLength() == 0) {
				gtpath = GtDataUtils.format2GtPath(key.toString() + "/"
						+ val.getFilename());
				put = new Put(Bytes.toBytes(gtpath));
				put.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.DFS.byteVal, Bytes.toBytes("0"));
				put.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.SIZE.byteVal,
						Bytes.toBytes(String.valueOf(val.getLength())));
				put.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.URL.byteVal,
						Bytes.toBytes(val.getMD5().toString()));
				put.add(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.TIME.byteVal,
						Bytes.toBytes("" + System.currentTimeMillis()));
				context.write(NullWritable.get(), put);
			}
		}
	}

}
