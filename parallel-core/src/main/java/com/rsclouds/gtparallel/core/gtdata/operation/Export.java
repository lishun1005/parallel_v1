package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

/**
 * 
 * @author xiaoshaolin
 * copy data from gt-data to local fileSystem
 */
public class Export {
	
	private Configuration conf;

	public Export() {
		conf = HBaseConfiguration.create();
	}

	public Export(Configuration conf) {
		this.conf = conf;
	}
	
	/**
	 * 
	 * @param inputPath
	 *            : the gt-data file's path
	 * @param outputPath
	 *            : the local file's path
	 * @return
	 * @throws IOException
	 */
	public boolean ExportToLocal (String inputPath, String outputPath) {
		HTable metaTable = null;
		HTable resTable = null;
		try {
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			int indexof = inputPath.lastIndexOf('/');
			StringBuilder strBuilder = new StringBuilder(inputPath);
			strBuilder.insert(indexof, '/');
			//decode path
			strBuilder = new StringBuilder(TransCoding.decode(strBuilder.toString(), "UTF-8"));
			Get get = new Get(Bytes.toBytes(strBuilder.toString()));
			Result result = metaTable.get(get);
			
			if (result == null || result.isEmpty()) {
				System.out.println("=====nanlin===== get result is empty, can't not find: " + strBuilder.toString());
				return false;
			}
			String dfsStr = new String(result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal));
			System.out.println("====nanlin=====debug: atts:dfs = " + dfsStr);
			int dfsInt = Integer.parseInt(dfsStr);
			byte[] md5Bytes = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
			System.out.println("=====nanlin=====debug: outputPath is " + outputPath + inputPath.substring(indexof));
			OutputStream out = new FileOutputStream(outputPath + inputPath.substring(indexof));
			
			if (dfsInt == 0) {
				resTable = new HTable(conf, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
				Get getRes = new Get(md5Bytes);
				Result resultRes;
				if ( (resultRes = resTable.get(getRes)) == null) {
					out.close();
					return false;
				}
				byte[] value = resultRes.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.DATA.byteVal);
				out.write(value, 0, value.length);
				out.flush();
				out.close();
			}else if (dfsInt == 1){
				FileSystem fs = FileSystem.get(conf);
				FSDataInputStream in = fs.open(new Path(GtDataConfig.HDFS_MD5_PATH ,new String(md5Bytes)));
				int readLen;
				byte[] value = new byte[1024];
				while ( (readLen = in.read(value, 0, 1024)) != -1){
					out.write(value, 0, readLen);
				}
				in.close();
			}else {
				out.close();
				return false;
			}
			out.flush();
			out.close();

		}catch (IOException e){
			e.printStackTrace();
			return false;
		}finally {
			try {
				if (metaTable != null){
					metaTable.flushCommits();
					metaTable.close();
				}
				if (resTable != null) {
					resTable.flushCommits();
					resTable.close();
				}
			}catch (IOException e) {
				return false;
			}
		}
		return true;
	}
	
	
	
	/**
	 * 
	 * @param inputPath
	 *            : the gt-data file's path
	 * @param outputPath
	 *            : the local file's path
	 * @return
	 * @throws IOException
	 */
	public boolean ExportToLocal (HTable metaTable, HTable resTable, String inputPath, String outputPath) {
		try {
			String gtpath = GtDataUtils.format2GtPath(inputPath);
			Get get = new Get(gtpath.getBytes());
			Result result = metaTable.get(get);
			
			if (result == null || result.isEmpty()) {
				System.out.println("=====nanlin===== get result is empty, can't not find: " + inputPath);
				return false;
			}
			String dfsStr = new String(result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal));
			int dfsInt = Integer.parseInt(dfsStr);
			byte[] md5Bytes = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
			
			File file = new File(outputPath);
			File fileParent = file.getParentFile();
			fileParent.mkdirs();
			OutputStream out = new FileOutputStream(outputPath);
			
			if (dfsInt == 0) {
				Get getRes = new Get(md5Bytes);
				Result resultRes = resTable.get(getRes);
				if ( resultRes.isEmpty()) {
					out.close();
					return false;
				}
				byte[] value = resultRes.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.DATA.byteVal);
				out.write(value, 0, value.length);
				out.flush();
				out.close();
			}else if (dfsInt == 1){
				FileSystem fs = FileSystem.get(conf);
				Get getRes = new Get(md5Bytes);
				Result resResult = resTable.get(getRes);
				byte[] prefixBytes = resResult.getValue("img".getBytes(), "prefix".getBytes());
				Path src = null;
				if (prefixBytes == null || prefixBytes.length == 0) {
					src = new Path(GtDataConfig.HDFS_MD5_PATH, new String(md5Bytes));
				}else {
					src = new Path(GtDataConfig.HDFS_ROOT_PATH + new String(prefixBytes), new String(md5Bytes));
				}
				FSDataInputStream in = fs.open(src);
				int readLen;
				byte[] value = new byte[1024];
				while ( (readLen = in.read(value, 0, 1024)) != -1){
					out.write(value, 0, readLen);
				}
				in.close();
			}else {
				out.close();
				return false;
			}
			out.flush();
			out.close();

		}catch (IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	
	public static void main(String[] args){
		args = new String[2];
		args[0] = "/map/auto_proc/img/warter/RealtimeChinaTest20150817/Layers/_alllayers/L0b/R0000031b/C000019c5.png";
		args[1] = "E://";
		if ( args.length != 2){
			System.out.println("usage:<InputPath>, <OutputPath>");
			System.exit(1);
		}
		Export export = new Export();
		boolean flag = export.ExportToLocal(args[0], args[1]);
		if (flag){
			System.exit(0);
		}else {
			System.exit(1);
		}
	}
}
