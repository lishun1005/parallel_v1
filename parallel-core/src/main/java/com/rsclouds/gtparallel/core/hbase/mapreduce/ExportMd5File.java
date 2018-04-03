package com.rsclouds.gtparallel.core.hbase.mapreduce;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;

import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class ExportMd5File {
	
	 private static void usage(final String errorMsg) {
		    if (errorMsg != null && errorMsg.length() > 0) {
		      System.err.println("ERROR: " + errorMsg);
		    }
		    System.err.println("Usage: Export <md5file(hdfs)> <localfile_path> \n");
	 }

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		args = new String[]{"nanlin_root/export-md5","E:/myJob/55555"};
		if (args.length < 2) {
		      usage("Wrong number of arguments: " + args.length);
//		      System.exit(-1);
		}
		FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
		for(FileStatus file : fs.listStatus(new Path(GtDataConfig.HDFS_ROOT_PATH,args[0]))){			
			FSDataInputStream in = fs.open(file.getPath());
			Text line = new Text();
			LineReader reader = new LineReader(in); //一行一行的读 使用LineReader
			while(reader.readLine(line) > 0) {
//			     System.out.println(line);//输出
			     ExportMd5FileProcessor.getInstance().processExport(line.toString(), args[1]);
			} 
			in.close();
		}
//		fs.close();
	}

}
