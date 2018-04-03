package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TarRecordReader extends RecordReader<Text, Text> {
	private static final Logger LOG = LoggerFactory
			.getLogger(TarRecordReader.class);
	private Configuration conf;
	private JobConf jobConf;
	private Path path;

	private Text value = new Text();
	private Text key = new Text();
	private boolean processed = false;
	private long readLength;
	private long length;
	private GZIPInputStream zipInputStream;
	private TarArchiveInputStream tarInputStream;
	private BufferedInputStream bufferedInputStream;
	private TarArchiveEntry entry;
	FSDataInputStream in = null;

	public Text createKey() {
		return new Text();
	}

	public BytesWritable createValue() {
		return new BytesWritable();
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	@Override
	public Text getCurrentValue() throws IOException, InterruptedException {
		return value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		float pro;
		if (readLength >= length) {
			pro = 0.99f;
		} else {
			pro = (float) readLength / length;
		}
		return processed ? 1.0f : pro;
	}

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		// 获取到Task的本地临时目录
		conf = context.getConfiguration();
		jobConf = new JobConf(conf);
		path = FileOutputFormat.getWorkOutputPath(jobConf);

		FileSplit filesplit = (FileSplit) split;
		this.conf = context.getConfiguration();
		this.readLength = 0;
		this.length = filesplit.getLength();
		Path filePath = filesplit.getPath();

		try {
			FileSystem fs = filePath.getFileSystem(conf);
			in = fs.open(filePath);

			// modify by chenshang
			zipInputStream = new GZIPInputStream(new BufferedInputStream(in));
			try {
				tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
						.createArchiveInputStream("tar", zipInputStream);
				entry = tarInputStream.getNextTarEntry();
				bufferedInputStream = new BufferedInputStream(tarInputStream);
			} catch (ArchiveException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		// modify by chenshang

		if (entry != null) {
			int filelength = (int) entry.getSize();
			String keyStr = entry.getName();
			if (entry.isDirectory()) {
				if (!keyStr.endsWith("/")) {
					keyStr += "/";
				}
			}
			if (filelength == 0) {
				key.set(keyStr);
				value.set("".getBytes(), 0, 0);
				entry = tarInputStream.getNextTarEntry();
				return true;
			} else {
				// 创建本地文件流，写入到本地文件中
				File file = new File(path.toString() + entry.getName());
				FileOutputStream bos = new FileOutputStream(file);
				try {
					int buf_size = 1024;
					byte[] buffer = new byte[buf_size];
					int len = 0;
					while (-1 != (len = bufferedInputStream.read(buffer, 0,
							buf_size))) {
						bos.write(buffer, 0, len);
					}

					key.set(keyStr);
					value.set(file.getAbsolutePath());

					readLength += entry.getRealSize();
					entry = tarInputStream.getNextTarEntry();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
					throw e;
				} finally {
					bos.close();
				}
			}
		} else {
			processed = true;
			if (bufferedInputStream != null) {
				bufferedInputStream.close();
			}
			if (tarInputStream != null) {
				tarInputStream.close();
			}
			if (zipInputStream != null) {
				zipInputStream.close();
			}
			if (in != null) {
				in.close();
			}
			LOG.info("end nextKeyValue:readLength=" + readLength + ",length="
					+ length);
		}
		return false;
	}

}
