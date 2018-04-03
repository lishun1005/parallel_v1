package com.rsclouds.gtparallel.core.hadoop.mapreduce;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class ZipFileOutputFormat extends FileOutputFormat<Text, BytesWritable> {
	
    @Override
    public RecordWriter<Text, BytesWritable> getRecordWriter(
            TaskAttemptContext job) throws IOException, InterruptedException {
        Path file = getDefaultWorkFile(job, ".zip");

        FileSystem fs = file.getFileSystem(job.getConfiguration());

        return new ZipRecordWriter(fs.create(file, false));
    }

    public static class ZipRecordWriter extends
            RecordWriter<Text, BytesWritable> {
        protected ZipOutputStream zos;

        public ZipRecordWriter(FSDataOutputStream os) {
            zos = new ZipOutputStream(os);
        }

        @Override
        public void write(Text key, BytesWritable value) throws IOException,
                InterruptedException {
			int length = value.getLength();
			if ( 0 == length){
				
			}else{
				String fname = key.toString();
				ZipEntry ze = new ZipEntry(fname);
				zos.closeEntry();
				zos.putNextEntry(ze);
				zos.write(value.getBytes(), 0, length);
			}
        }

        @Override
        public void close(TaskAttemptContext context) throws IOException,
                InterruptedException {
        	zos.finish();
            zos.close();
        }
    }
    
}


