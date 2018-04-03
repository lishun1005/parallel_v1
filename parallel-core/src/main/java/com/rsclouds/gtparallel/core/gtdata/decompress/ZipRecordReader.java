package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class ZipRecordReader extends RecordReader<Text, BytesWritable> {
	private static final Logger LOG = LoggerFactory.getLogger(ZipRecordReader.class);
	private Configuration conf;
	private BytesWritable value = new BytesWritable();
	private Text key = new Text();
	private boolean processed = false;
	private long readLength;
	private long errorLength = -1l;
	private int errorCount = 0;
	private long length;
	private ZipInputStream zipInputStream;
	FSDataInputStream in = null;
//	private static long  checkLength = 0;

	
	public Text createKey() {
		return new Text();
	}

	public BytesWritable createValue() {
		return new BytesWritable();
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub	
	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return key;
	}

	@Override
	public BytesWritable getCurrentValue() throws IOException,
			InterruptedException {
		// TODO Auto-generated method stub
		return value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		float pro;
		if(readLength >= length){
			pro = 0.99f;
		}else {
			pro = (float)readLength / length;
		}
		return processed ? 1.0f : pro;
	}

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		FileSplit filesplit = (FileSplit) split;
		this.conf = context.getConfiguration();
		this.readLength = 0;
		this.length = filesplit.getLength();
		Path filePath = filesplit.getPath();
		
		try {
			FileSystem fs = filePath.getFileSystem(conf);
			in = fs.open(filePath);
			zipInputStream = new ZipInputStream(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean nextKeyValue() {
		ZipEntry zipEntry;
		try {
			zipEntry = zipInputStream.getNextEntry();
			if (zipEntry != null) {
				byte[] contents = new byte[(int) zipEntry.getSize()];	
				LOG.info("contents.length = " + contents.length+",CompressedSize = "+zipEntry.getCompressedSize());
				IOUtils.readFully(zipInputStream, contents, 0, contents.length);		
				String keyStr = zipEntry.getName();
				if(zipEntry.isDirectory()){
					if(!keyStr.endsWith("/")){
						keyStr +="/";
					}
				}
				key.set(keyStr);
				value.set(contents, 0, contents.length);
				readLength += zipEntry.getCompressedSize();
				LOG.info("nextKeyValue:readLength=" + readLength+ ",length=" + length + "filname= " + keyStr);
//				if(checkLength == 0){
//					checkLength = length;
//				}else{
//					if(checkLength != length )
//						LOG.info("checkLength : length "+checkLength +":"+ length);
//				}
//				readLength += 72;
//				if ( zipEntry.getExtra() != null){
//					readLength += zipEntry.getExtra().length;
//				}
				return true;
			}else{
				processed = true;
				if (in != null)
					in.close();
				if (zipInputStream != null)
					zipInputStream.close();
				LOG.info("end nextKeyValue:readLength=" + readLength+ ",length=" + length);
			}
			return false;
		} catch (IOException e) {
			System.err.println("read error. readlength=" + readLength);
			return false;
//			if (errorLength != readLength) {
//				errorCount = 0;
//				errorLength = readLength;
//				return true;
//			} else {
//				errorCount ++;
//				System.out.println(errorCount);
//				if (errorCount == 1000)
//					return false;
//				else
//					return true;
//			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
