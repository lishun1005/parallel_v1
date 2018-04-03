package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;


import com.rsclouds.gtparallel.core.gtdata.common.FileOperate;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

/**
 * 
 * @author xiaoshaolin input the data to gt-data
 */
public class Import {
	private Configuration conf;
	//private Jedis jedis;
//	private int timeout;

	public Import() {
//		timeout = 30000;
		conf = HBaseConfiguration.create();
		//jedis = new Jedis(GtDataConfig.REDIS_HOST, GtDataConfig.REDIS_PORT, timeout);
	}
	
	public Import(int timeout) {
		conf = HBaseConfiguration.create();
//		this.timeout = timeout;
		//jedis = new Jedis(GtDataConfig.REDIS_HOST, GtDataConfig.REDIS_PORT, this.timeout);
		
	}

	public void init() {
		if(conf == null)
			conf = HBaseConfiguration.create();
//		if(jedis == null) {
//			jedis = new Jedis(GtDataConfig.REDIS_HOST, GtDataConfig.REDIS_PORT, timeout);
//		}
	}
	
	public Import(Configuration conf) {
		this.conf = conf;
//		jedis = new Jedis(GtDataConfig.REDIS_HOST, GtDataConfig.REDIS_PORT);
	}

	public void close() {
//		if(jedis != null)
//			jedis.close();
	}
//	private boolean ImportMetaTable(HTable metaTable, long fileSize,
//			String md5Str, String outputPath) {
//		
//		try {
//			
//			Put metaPut = new Put(Bytes.toBytes(outputPath));
//			metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal,
//					Bytes.toBytes(fileSize+""));
//			metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal,
//					Bytes.toBytes(md5Str));
//			metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal,
//					Bytes.toBytes("" + System.currentTimeMillis()));
//			if (fileSize < 16777216) {
//				metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal,
//						GtDataConfig.CONSTANT.ZERO.byteVal);
//			} else {
//				metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal,
//						GtDataConfig.CONSTANT.ONE.byteVal);
//			}
//			metaTable.put(metaPut);
////			Map<byte[],byte[]> kv = new HashMap<byte[],byte[]>();
////			kv.put(GtDataConfig.META_SIZE, Bytes.toBytes(fileSize+""));
////			kv.put(GtDataConfig.META_URL, Bytes.toBytes(md5Str));
////			kv.put(GtDataConfig.META_TIME, Bytes.toBytes("" + System.currentTimeMillis()));
////			if(fileSize < GtDataConfig.HBASE_FILESIZE_MAX){
////				kv.put(GtDataConfig.META_DFS,GtDataConfig.ZERO);
////			}else{
////				kv.put(GtDataConfig.META_DFS,GtDataConfig.ONE);
////			}
////			HbaseBase.writeRows(metaTable.getName().getNameAsString(), Bytes.toBytes(outputPath), GtDataConfig.META_FAMILY, kv);
//		} catch (IOException e) {
//			return false;
//		}
//		return true;
//	}

	public boolean isExitRowkey(HTable metaTable, String path) {
		String gtPath = GtDataUtils.format2GtPath(path);
		Get get = new Get(gtPath.getBytes());
		get.addFamily(GtDataConfig.META.FAMILY.byteVal);
		get.addColumn(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal);
		try {
			if(metaTable.exists(get))
				return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	private boolean ImportResTable(HTable resTable, String inputPath,
			String md5Str, short replacation) {
		try {
			File file = new File(inputPath);
			long fileSize;
			if (file.isDirectory()) {
				return false;
			} else {
				fileSize = file.length();
			}
			byte[] md5Byte = md5Str.getBytes();
			Get get = new Get(md5Byte);
			Result result = resTable.get(get);
			Put put = new Put(md5Byte);
			if (result.isEmpty()) {
				put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.LINKS.byteVal,GtDataConfig.CONSTANT.ONE.byteVal);
				put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.SIZE.byteVal, Bytes.toBytes(fileSize+""));
				if (fileSize < 16777216) {
					byte[] value = FileOperate.readSmalFile(inputPath);
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
							GtDataConfig.RESOURCE.DATA.byteVal, value);
				} else {
					FileSystem fs = FileSystem.get(conf);
					
					fs.copyFromLocalFile(new Path(inputPath), new Path(
							GtDataConfig.HDFS_MD5_PATH ,md5Str));
					fs.setReplication(new Path(GtDataConfig.HDFS_MD5_PATH ,md5Str), replacation);
					fs.close();
				}
			} else {
				String linksStr = new String(result.getValue(
						GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.LINKS.byteVal));
				int links = Integer.parseInt(linksStr);
				links++;
				byte[] linksByte = Bytes.toBytes("" + links);
				put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.LINKS.byteVal, linksByte);
			}
			resTable.put(put);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 
	 * @param inputPath
	 *            : the local file's path
	 * @param outputPath
	 *            : the gt-data file's path
	 * @return
	 * @throws IOException
	 */
	public boolean ImportFromLocal(String inputPath, String outputPath) {
		HTable metaTable = null;
		HTable resTable = null;
		try {
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			resTable = new HTable(conf, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			return ImportFile(metaTable, resTable,inputPath,outputPath);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (metaTable != null) {
					metaTable.flushCommits();
					metaTable.close();
				}
				if (resTable != null) {
					resTable.flushCommits();
					resTable.close();
				}
			} catch (IOException e) {
				return false;
			}
		}
	}

	
	
	/**
	 * 
	 * @param inputPath
	 *            : the local file's path
	 * @param outputPath
	 *            : the gt-data file's path
	 * @return
	 * @throws IOException
	 */
	public boolean ImportFromLocal(String metatable, String restable, String inputPath, String outputPath) {
		HTable metaTable = null;
		HTable resTable = null;
		try {
			metaTable = new HTable(conf, metatable);
			resTable = new HTable(conf, restable);
			return ImportFile(metaTable, resTable,inputPath,outputPath);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (metaTable != null) {
					metaTable.flushCommits();
					metaTable.close();
				}
				if (resTable != null) {
					resTable.flushCommits();
					resTable.close();
				}
			} catch (IOException e) {
				return false;
			}
		}
	}
	
	
	
	/**
	 * 
	 * @param inputPath
	 *            : the local file's path
	 * @param outputPath
	 *            : the gt-data output directory path
	 * @return
	 * @throws IOException
	 */
	public boolean ImportFileToDir(HTable metaTable, HTable resTable,
			String inputPath, String outputDir) {
		try {
			if (inputPath.isEmpty() || outputDir.isEmpty()) {
				return false;
			}
			if (inputPath.endsWith("/")) {
				inputPath = inputPath.substring(0, inputPath.length() - 1);
			}
			File file = new File(inputPath);
			String filename = file.getName();
			if(outputDir.contains("%")){
				outputDir = TransCoding.decode(outputDir, "utf-8");
			}
			GtDataUtils.genterGtdataDir(outputDir);
			String outputPath = outputDir + "/" + filename;
			outputPath = GtDataUtils.format2GtPath(outputPath);
			String md5Str = "";
			long filesize  = -1;
			if (file.isFile()) {
				md5Str = MD5Calculate.LocalfileMD5(inputPath);
				short defaultReplacation = 3;
				ImportResTable(resTable, inputPath, md5Str, defaultReplacation);
				filesize = file.length();
			}
			HbaseBase.writeMetaDataRow(metaTable, outputPath, md5Str, System.currentTimeMillis(), filesize);
			//ImportMetaTable(metaTable, filesize, md5Str, outputDir);
			//RedisUtils.redisDirCheck(outputPath, GtDataConfig.REDIS_HOST);
//			RedisUtils.redisDirCheck(jedis, outputPath);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param inputPath
	 *            : the local file's path
	 * @param outputPath
	 *            : the gt-data file's path
	 * @return
	 * @throws IOException
	 */
	public boolean ImportFile(HTable metaTable, HTable resTable,
			String inputPath, String outPath) {
		try {
			if (inputPath.isEmpty() || outPath.isEmpty()) {
				return false;
			}
			File file = new File(inputPath);
			if(!file.exists()){
				return false;
			}
			String md5Str = "";
			long fileSize = -1;
			outPath = GtDataUtils.format2GtPath(outPath);
			if (file.isFile()) {
				md5Str = MD5Calculate.LocalfileMD5(inputPath);
				short defaultReplacation = 3;
				ImportResTable(resTable, inputPath, md5Str, defaultReplacation);
				fileSize = file.length();
			}
			HbaseBase.writeMetaDataRow(metaTable, outPath, md5Str, System.currentTimeMillis(), fileSize);
			//ImportMetaTable(metaTable, fileSize, md5Str, outPath);
			//System.out.println(GtDataConfig.REDIS_HOST);
			//RedisUtils.redisDirCheck(outPath, GtDataConfig.REDIS_HOST);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param localDir
	 * @param outputPath
	 * @param filefilter
	 * @return
	 */
	public boolean ImportDirFileFilter(HTable metaTable, HTable resTable,
			String localDir, String outputPath, String keywords) {
		File file = new File(localDir);
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			String filePath = files[i].getPath();
			if (files[i].isDirectory()) {
				if (!ImportFileToDir(metaTable, resTable, filePath,
						outputPath) ) {
					System.out.println("import error" + filePath);
					return false;
				}			
				if (!ImportDirFileFilter(metaTable, resTable, filePath,
						outputPath+"/"+files[i].getName(), keywords)) {
					return false;
				}			
			} else {
				if (keywords != null) {
					if (files[i].getName().contains(keywords)) {
						if (!ImportFileToDir(metaTable, resTable, filePath,
								outputPath)) {
							return false;
						}
					}
				}else{
					if (!ImportFileToDir(metaTable, resTable, filePath,
							outputPath)) {
						System.out.println("import error" + filePath);
						return false;
					}
					System.out.println("import sucessed" + filePath);
				}
			}
		}
		return true;
	}
	
	
	
	/**
	 * 
	 * @param localDir
	 * @param outputPath
	 * @param filefilter
	 * @return
	 */
	public boolean ImportDirFileFilter(HTable metaTable, HTable resTable,
			String localDir, String outputPath, String keywords, boolean bOverwrite) {
		File file = new File(localDir);
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			String filePath = files[i].getPath();
			if (files[i].isDirectory()) {
				if (!ImportFileToDir(metaTable, resTable, filePath,
						outputPath + "/" + files[i].getName()) ) {
					System.out.println("import error" + filePath);
					return false;
				}			
				if (!ImportDirFileFilter(metaTable, resTable, filePath,
						outputPath+"/"+files[i].getName(), keywords)) {
					return false;
				}			
			} else {
				if(!bOverwrite && isExitRowkey(metaTable, filePath)) {
					File file1 = new File(filePath);
					String newPath = file1.getParent() + "_suced/" + file1.getName();
					//System.out.println("new path " + newPath);
					File dst = new File(newPath);
					file1.renameTo(dst);
					continue;
				}
				if (keywords != null) {
					if (files[i].getName().contains(keywords)) {
						if (!ImportFileToDir(metaTable, resTable, filePath,
								outputPath)) {
							return false;
						}
					}
				}else{
					if (!ImportFileToDir(metaTable, resTable, filePath,
							outputPath)) {
						System.out.println("import error" + filePath);
						return false;
					}else {
//						File file1 = new File(filePath);
//						String newPath = file1.getParent() + "_suced/" + file1.getName();
//						//System.out.println("new path " + newPath);
//						File dst = new File(newPath);
//						file1.renameTo(dst);
					}
					//System.out.println("import sucessed" + filePath);
				}
			}
		}
		return true;
	}
	
	
	
	public boolean ImportLocalFileToMapTable(String inputPath, String outputPath) {
		try {
			if (inputPath.isEmpty() || outputPath.isEmpty()) {
				return false;
			}
			File file = new File(inputPath);
			if(!file.exists()){
				return false;
			}
			String md5Str = "";
			long fileSize = -1;
			outputPath = GtDataUtils.format2GtPath(outputPath);
			if (file.isFile()) {
				md5Str = MD5Calculate.LocalfileMD5(inputPath);
				HTable resTable = new HTable(conf, GtDataConfig.TABLE_NAME.MAP_RES_TABLE.getStrVal());
				if(resTable != null) {
					short defaultReplacation = 3;
					ImportResTable(resTable, inputPath, md5Str, defaultReplacation);
					fileSize = file.length();
					resTable.flushCommits();
					resTable.close();
				}
			}else {
				return false;
			}
			HbaseBase.writeMapMetaDataRow(outputPath, md5Str, System.currentTimeMillis(), fileSize);
//			System.out.println(GtDataConfig.REDIS_HOST);
//			RedisUtils.redisDirCheck(outputPath, GtDataConfig.REDIS_HOST);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) {
//		args = new String[5];
//		args[0] = "meta";
//		args[1] = "res";
//		args[2] = "D://nanlin//image//pl_test//2016012902";
//		args[3] = "/users/rscloudmart/convertTif/2016012902";
//		args[4] = "true";
		if (args.length < 5) {
			System.out.println("usage:<meta>,<res>,<InputPath>, <OutputPath> <overwrite [true/false]> [keyworkds]");
			System.exit(0);
		}
		Configuration conf = HBaseConfiguration.create();
		Import importData = new Import(conf);

		HTable metaTable;
		HTable resTable;
		boolean bOverwrite = false;
		String keyword = null;
		try {
			System.out.println("metatable name: " + args[0]);
			System.out.println("resource name" + args[1]);
			metaTable = new HTable(conf,  args[0]);
			resTable = new HTable(conf,  args[1]);
		} catch (IOException e) {
			return;
		}
		System.out.println("start import");
		if(args[4].equals("true")) {
			bOverwrite = true;
		}
		if(args.length == 6) {
			keyword = args[5];
		}
		boolean flag = importData.ImportDirFileFilter(metaTable, resTable, args[2],
				args[3], keyword, bOverwrite);
		System.out.println("end import");
		try {
			metaTable.flushCommits();
			metaTable.close();
			resTable.flushCommits();
			resTable.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (flag) {
			System.exit(0);
		} else {
			System.exit(1);
		}
	}

	public boolean ImportFileToDir(HTable metaTable, HTable resTable,
			String inputPath, String outputDir, short defaultReplacation) {
		try {
			if (inputPath.isEmpty() || outputDir.isEmpty()) {
				return false;
			}
			if (inputPath.endsWith("/")) {
				inputPath = inputPath.substring(0, inputPath.length() - 1);
			}
			File file = new File(inputPath);
			String filename = file.getName();
			if(outputDir.contains("%")){
				outputDir = TransCoding.decode(outputDir, "utf-8");
			}
			GtDataUtils.genterGtdataDir(outputDir);
			String outputPath = outputDir + "/" + filename;
			outputPath = GtDataUtils.format2GtPath(outputPath);
			String md5Str = "";
			long filesize  = -1;
			if (file.isFile()) {
				md5Str = MD5Calculate.LocalfileMD5(inputPath);
				ImportResTable(resTable, inputPath, md5Str, defaultReplacation);
				filesize = file.length();
			}
			HbaseBase.writeMetaDataRow(metaTable, outputPath, md5Str, System.currentTimeMillis(), filesize);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
