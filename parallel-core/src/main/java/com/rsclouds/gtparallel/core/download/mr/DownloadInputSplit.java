/*
 * 定义Split
 * 2014-7-22 made by chenshangshang
 */
package com.rsclouds.gtparallel.core.download.mr;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import com.rsclouds.gtparallel.core.common.CoreConfig;

public class DownloadInputSplit extends FileSplit {

	private String jobid = "";

	private DownloadInputSplit() {
		super();
	}

	public DownloadInputSplit(String jobid) {
		super(new Path(CoreConfig.DOWNLOAD_HDFS_PATH), 0, 10,(String[]) null);
		this.jobid = jobid;
	}

	public DownloadInputSplit(String jobid, String[] ip) {
		super(new Path(CoreConfig.DOWNLOAD_HDFS_PATH), 0, 10, ip);
		this.jobid = jobid;
	}

	public String getJobid() {
		return this.jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		out.writeUTF(jobid);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		jobid = in.readUTF();
	}
}
