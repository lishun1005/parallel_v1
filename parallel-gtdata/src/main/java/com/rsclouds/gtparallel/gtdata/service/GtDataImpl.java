package com.rsclouds.gtparallel.gtdata.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.entity.Metadata;
import com.rsclouds.gtparallel.gtdata.exception.GtdataException;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.RedisUtils;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;



public class GtDataImpl {
	// 似有静态内部类, 只有当有引用时, 该类才会被装载
    private static class LazyGtDataImpl {
       public static GtDataImpl instance = new GtDataImpl();
    }
    
    public static GtDataImpl getInstance() {
        return LazyGtDataImpl.instance;
    }
	
	/**
	 * 显示目录列表
	 * @param gtPath
	 * @return
	 * @throws IOException
	 */
	public List<Metadata> list(String gtPath) throws IOException{	
		List<Metadata> fileMapList = new ArrayList<Metadata>();
		try {
			gtPath = GtDataUtils.format2GtPath(gtPath).replace("//", "/");
			if ("/".equals(gtPath)) {
				gtPath = "";
			}
			List<Result> results = HbaseBase.Scan(
					GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0, gtPath
							+ "//", gtPath + "//{", null, null);
			for (Result rs : results) {
				// Map<String,String> fileMap = new HashMap<String,String>();
				// String sizeStr = Bytes.toString(rs.getValue(
				// GtDataConfig.META.FAMILY.byteVal,
				// GtDataConfig.META.SIZE.byteVal));
				// String timeStr = Bytes.toString(rs.getValue(
				// GtDataConfig.META.FAMILY.byteVal,
				// GtDataConfig.META.TIME.byteVal));
				// String rowKey = Bytes.toString(rs.getRow());
				// if(rowKey.contains("%")){
				// rowKey = TransCoding.decode(rowKey, "utf-8");
				// }
				// fileMap.put("path", rowKey.replace("//", "/"));
				// fileMap.put("size", sizeStr);
				// fileMap.put("time", GtDataUtils.timeStrFillZero(timeStr));
				fileMapList.add(new Metadata(rs));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		return fileMapList;
	}

	public boolean mkdir(String path) throws IOException {
		return mkdir(path, true);
	}

	public boolean mkdir(String path, boolean keepTwoFile) throws IOException {
		try {
			path = GtDataUtils.format2GtPath(path);
			if (GtDataUtils.getFileSzie(path) != null) {
				if (!keepTwoFile) {
					return false;
				} else {
					String renamePath = path;
					int times = 0;
					boolean isFileExist = true;
					do {
						times++;
						renamePath = GtDataUtils.genterNewPath(path, times);
						if (GtDataUtils.getFileSzie(renamePath) == null) {
							isFileExist = false;
						} else {
							isFileExist = true;
						}
					} while (isFileExist);
					path = renamePath;
				}
			}
			importMeta("", -1, "0", path);
			// redis check
			//RedisUtils.redisDirCheck(GtDataConfig.REDIS_HOST, path);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public boolean reanme(String dirPath, String oldFileName,
			String newFileName, boolean keepTwoFile) throws Exception {
		try {
			while (dirPath.endsWith("/") && dirPath.length() > 1) {
				dirPath = dirPath.substring(0, dirPath.length() - 1);
			}
			GtPath oldPathObj = new GtPath(dirPath + "/" + oldFileName);
			GtPath newPathObj = new GtPath(dirPath + "/" + newFileName);
			String oldFileSize = GtDataUtils
					.getFileSzie(oldPathObj.getGtPath());
			// 检查旧文件地址是否存在
			if (oldFileSize == null) {
				throw new GtdataException("2003", oldFileName
						+ " does not exist");
			}
			// 检查新文件地址是否存在
			String newPath = newPathObj.getGtPath();
			if (GtDataUtils.getFileSzie(newPath) != null) {
				if (!keepTwoFile) {
					throw new GtdataException("2002", newFileName
							+ " already exist");
				} else {
					String renamePath = newPath;
					int times = 0;
					boolean isFileExist = true;
					do {
						times++;
						renamePath = GtDataUtils.genterNewPath(newPath, times);
						if (GtDataUtils.getFileSzie(renamePath) == null) {
							isFileExist = false;
						} else {
							isFileExist = true;
						}
					} while (isFileExist);
					newPath = renamePath;
				}
			}
			return renameImpl(oldPathObj.getGtPath(), newPath, true);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public boolean copyOrMove(String oldDirPath, String newDirPath,
			String filename, boolean isDel, boolean overwrite,
			boolean keepTwoFile) throws Exception {
		try {
			GtPath oldPathObj = new GtPath(oldDirPath + "/" + filename);
			GtPath newPathObj = new GtPath(newDirPath + "/" + filename);
			String oldPath = oldPathObj.getGtPath();
			String newPath = newPathObj.getGtPath();
			if (newPath.startsWith(oldPath)) {
				throw new GtdataException("2001", "Invalid value of parameter");
			}
			String oldFileSize = GtDataUtils.getFileSzie(oldPath);
			// 检查旧文件地址是否存在
			if (oldFileSize == null) {
				throw new GtdataException("2003", filename + " does not exist");
			}
			// 检查新文件地址是否存在
			if (GtDataUtils.getFileSzie(newPath) != null) {
				if (!keepTwoFile && !overwrite) {
					throw new GtdataException("2002", filename
							+ " already exist");
				} else if (!overwrite && keepTwoFile) {
					String renamePath = newPath;
					int times = 0;
					boolean isFileExist = true;
					do {
						times++;
						renamePath = GtDataUtils.genterNewPath(newPath, times);
						if (GtDataUtils.getFileSzie(renamePath) == null) {
							isFileExist = false;
						} else {
							isFileExist = true;
						}
					} while (isFileExist);
					newPath = renamePath;
				}
			}
			return renameImpl(oldPath, newPath, isDel);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	private boolean renameImpl(String oldPath, String newPath, boolean isDel)
			throws GtdataException, IOException {
		Result result = HbaseBase.selectRow(
				GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), oldPath);
		if (!result.isEmpty()) {
			byte[] sizeByte = result.getValue(GtDataConfig.META.FAMILY.byteVal,
					GtDataConfig.META.SIZE.byteVal);
			List<byte[]> deletes = new ArrayList<byte[]>();
			NavigableMap<byte[], byte[]> map = result
					.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
			if (map != null) {
				HbaseBase.writeRows(
						GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
						Bytes.toBytes(newPath),
						GtDataConfig.META.FAMILY.byteVal, map);
				if (isDel)
					deletes.add(result.getRow());
			}
			if (Arrays.equals(sizeByte,
					GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)) {
				String startRow = oldPath.replace("//", "/");
				List<Result> results = HbaseBase.Scan(
						GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0,
						startRow + "/", startRow + "/{", null, null);
				String replacement = newPath.replace("//", "/");
				for (Result rs : results) {
					String key = Bytes.toString(rs.getRow());
					String newKey = key.replace(startRow, replacement);
					NavigableMap<byte[], byte[]> map2 = rs
							.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
					HbaseBase.writeRows(
							GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
							Bytes.toBytes(newKey),
							GtDataConfig.META.FAMILY.byteVal, map2);
					if (isDel)
						deletes.add(rs.getRow());
				}
			}
			if (isDel) {
				deletes.add(Bytes.toBytes(oldPath));
				HbaseBase
						.deleteRow(
								GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
								deletes);
				List<Delete> list = new ArrayList<Delete>();
				for (byte[] del : deletes) {
					list.add(new Delete(del));
				}
				RedisUtils.redisDel(GtDataConfig.REDIS_HOST, deletes);
			}
			RedisUtils.redisDirCheck(GtDataConfig.REDIS_HOST, newPath);
			return true;
		} else {
			throw new GtdataException("2003", oldPath + " does not exist");
		}
	}

	public boolean delete(String gtPath) throws IOException {
		try {
			gtPath = GtDataUtils.format2GtPath(gtPath);
			Result result = HbaseBase.selectRow(
					GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtPath);
			if (!result.isEmpty()) {
				byte[] size = result.getValue(GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.SIZE.byteVal);
				List<byte[]> rowList = new ArrayList<byte[]>();
				if (Arrays.equals(GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal,
						size)) {
					String startRow = gtPath.replace("//", "/");
					List<Result> resultList = HbaseBase.Scan(
							GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0,
							startRow + "/", startRow + "/{", null, null);
					for (Result rs : resultList) {
						String key = Bytes.toString(rs.getRow());
						NavigableMap<byte[], byte[]> map = rs
								.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
						HbaseBase.writeRows(
								GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
								Bytes.toBytes("-//" + key),
								GtDataConfig.META.FAMILY.byteVal, map);
						HbaseBase.deleteRow(
								GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
								Bytes.toString(rs.getRow()));
						rowList.add(rs.getRow());
					}
					RedisUtils.redisDel(GtDataConfig.REDIS_HOST, rowList);
				}
				NavigableMap<byte[], byte[]> map = result
						.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
				HbaseBase.writeRows(
						GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
						Bytes.toBytes("-//" + gtPath),
						GtDataConfig.META.FAMILY.byteVal, map);
				HbaseBase.deleteRow(
						GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtPath);
				RedisUtils.redisDel(GtDataConfig.REDIS_HOST, gtPath);
				// redis check
				RedisUtils.redisDirCheck(GtDataConfig.REDIS_HOST, gtPath);
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public List<Metadata> search(String gtPath, String keyword)
			throws IOException {
		List<Metadata> fileMapList = new ArrayList<Metadata>();
		try {
			gtPath = GtDataUtils.format2GtPath(gtPath).replace("//", "/");
			if ("/".equals(gtPath)) {
				gtPath = "";
			}
			keyword = TransCoding.UrlEncode(keyword, "utf-8");
			List<Result> results = HbaseBase.Scan(
					GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), 0, gtPath
							+ "/", gtPath + "/{", "\\/\\/.*(" + keyword + ")",
					null);
			for (Result rs : results) {
				// Map<String,String> fileMap = new HashMap<String,String>();
				// String sizeStr = Bytes.toString(rs.getValue(
				// GtDataConfig.META.FAMILY.byteVal,
				// GtDataConfig.META.SIZE.byteVal));
				// String timeStr = Bytes.toString(rs.getValue(
				// GtDataConfig.META.FAMILY.byteVal,
				// GtDataConfig.META.TIME.byteVal));
				// String rowKey = Bytes.toString(rs.getRow());
				// if(rowKey.contains("%")){
				// rowKey = TransCoding.decode(rowKey, "utf-8");
				// }
				// fileMap.put("path", rowKey.replace("//", "/"));
				// fileMap.put("size", sizeStr);
				// fileMap.put("time", GtDataUtils.timeStrFillZero(timeStr));
				fileMapList.add(new Metadata(rs));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		return fileMapList;
	}

	public boolean export(String gtpath, OutputStream out) throws IOException {
		try {
			gtpath = GtDataUtils.format2GtPath(gtpath);
			Result result = HbaseBase.selectRow(
					GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtpath);
			if (!result.isEmpty()) {
				String dfsStr = new String(result.getValue(
						GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.DFS.byteVal));
				int dfsInt = Integer.parseInt(dfsStr);
				byte[] md5Bytes = result.getValue(
						GtDataConfig.META.FAMILY.byteVal,
						GtDataConfig.META.URL.byteVal);
				String md5Str = Bytes.toString(md5Bytes);
				if (dfsInt == 0) {
					Result resultRes = HbaseBase.selectRow(
							GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
							md5Str);
					;
					if (resultRes == null) {
						out.close();
						return false;
					} else {
						byte[] value = resultRes.getValue(
								GtDataConfig.RESOURCE.FAMILY.byteVal,
								GtDataConfig.RESOURCE.DATA.byteVal);
						out.write(value, 0, value.length);
						out.flush();
						out.close();
					}
				} else if (dfsInt == 1) {
					FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
					FSDataInputStream in = fs.open(new Path(
							GtDataConfig.HDFS_MD5_PATH, md5Str));
					int readLen;
					byte[] value = new byte[1024];
					while ((readLen = in.read(value, 0, 1024)) != -1) {
						out.write(value, 0, readLen);
					}
					in.close();
				} else {
					out.close();
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		return true;
	}

	public boolean importByFile(String outPath, String filePath)
			throws IOException {
		try {
			outPath = GtDataUtils.format2GtPath(outPath);
			File file = new File(filePath);
			if (file.isFile()) {
				String md5 = MD5Calculate.LocalfileMD5(filePath);
				InputStream inputStream = new FileInputStream(filePath);
				importByStream(outPath, inputStream, md5, file.length());
				inputStream.close();
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		return true;
	}

	public boolean importByByteArray(String outPath, byte[] contents)
			throws IOException {
		try {
			outPath = GtDataUtils.format2GtPath(outPath);
			String md5 = MD5Calculate.fileByteMD5(contents);
			importResource(contents, md5);
			importMeta(md5, contents.length, outPath);
			RedisUtils.redisDirCheck(GtDataConfig.REDIS_HOST, outPath);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		return true;
	}

	public boolean importByStream(String outPath, InputStream in, String md5,
			long size) throws IOException {
		try {
			outPath = GtDataUtils.format2GtPath(outPath);
			importResource(in, md5, size);
			importMeta(md5, size, outPath);
			RedisUtils.redisDirCheck(GtDataConfig.REDIS_HOST, outPath);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		return true;
	}

	public static void importMeta(String url, long size, String rowKey)
			throws IOException {
		String dfs = GtDataConfig.CONSTANT.ZERO.strVal;
		if (size >= GtDataConfig.HBASE_FILESIZE_MAX)
			dfs = GtDataConfig.CONSTANT.ONE.strVal;
		importMeta(url, size, dfs, rowKey);
	}

	public static void importMeta(String url, long size, String dfs,
			String rowKey) throws IOException {
		// Map<byte[], byte[]> meta = new HashMap<byte[], byte[]>();
		long time = System.currentTimeMillis();
		HbaseBase.writeMetaDataRow(rowKey, url, time, size);
		// meta.put(GtDataConfig.META.URL.byteVal, Bytes.toBytes(url));
		// meta.put(GtDataConfig.META.SIZE.byteVal, Bytes.toBytes(size + ""));
		// meta.put(GtDataConfig.META.DFS.byteVal, Bytes.toBytes(dfs));
		// meta.put(GtDataConfig.META.TIME.byteVal,Bytes.toBytes(System.currentTimeMillis()
		// + ""));
		// HbaseBase.writeRows(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(),
		// Bytes.toBytes(rowKey), GtDataConfig.META.FAMILY.byteVal,
		// meta);
	}

	public static void importResource(byte[] contents, String url)
			throws IOException {
		long size = contents.length;
		importResource(contents, url, size);
	}

	public static void importResource(byte[] contents, String url, long size)
			throws IOException {
		Result rs = HbaseBase.selectRow(
				GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), url);
		size = contents.length;
		if (!rs.isEmpty()) {
			String links = Bytes.toString(rs.getValue(
					GtDataConfig.RESOURCE.FAMILY.byteVal,
					GtDataConfig.RESOURCE.LINKS.byteVal));
			long linksLong = Long.parseLong(links);
			byte[] newLinks = Bytes.toBytes((linksLong + 1) + "");
			HbaseBase.writeRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
					Bytes.toBytes(url), GtDataConfig.RESOURCE.FAMILY.byteVal,
					GtDataConfig.RESOURCE.LINKS.byteVal, newLinks);
		} else {
			Map<byte[], byte[]> kv = new HashMap<byte[], byte[]>();
			if (size < GtDataConfig.HBASE_FILESIZE_MAX)
				kv.put(GtDataConfig.RESOURCE.DATA.byteVal, contents);
			else {
				FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
				FSDataOutputStream out = fs.create(new Path(
						GtDataConfig.HDFS_MD5_PATH, url));
				out.write(contents);
				out.close();
			}
			kv.put(GtDataConfig.RESOURCE.LINKS.byteVal,
					GtDataConfig.CONSTANT.ONE.byteVal);
			kv.put(GtDataConfig.RESOURCE.SIZE.byteVal, Bytes.toBytes(size + ""));
			HbaseBase.writeRows(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
					Bytes.toBytes(url), GtDataConfig.RESOURCE.FAMILY.byteVal,
					kv);
		}
	}

	public static void importResource(InputStream in, String url, long size)
			throws IOException {
		Result rs = HbaseBase.selectRow(
				GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(), url);
		if (!rs.isEmpty()) {
			String links = Bytes.toString(rs.getValue(
					GtDataConfig.RESOURCE.FAMILY.byteVal,
					GtDataConfig.RESOURCE.LINKS.byteVal));
			long linksLong = Long.parseLong(links);
			byte[] newLinks = Bytes.toBytes((linksLong + 1) + "");
			HbaseBase.writeRow(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
					Bytes.toBytes(url), GtDataConfig.RESOURCE.FAMILY.byteVal,
					GtDataConfig.RESOURCE.LINKS.byteVal, newLinks);
		} else {
			Map<byte[], byte[]> kv = new HashMap<byte[], byte[]>();
			if (size < GtDataConfig.HBASE_FILESIZE_MAX) {
				byte[] contents = IOUtils.toByteArray(in);
				kv.put(GtDataConfig.RESOURCE.DATA.byteVal, contents);
			} else {
				FileSystem fs = FileSystem.get(HbaseBase.getHbaseConf());
				FSDataOutputStream out = fs.create(new Path(
						GtDataConfig.HDFS_MD5_PATH, url));
				byte[] contents = new byte[GtDataConfig.BUFFER];
				int len = 0;
				while (-1 != (len = in.read(contents, 0, GtDataConfig.BUFFER))) {
					out.write(contents, 0, len);
				}
				out.close();
			}
			kv.put(GtDataConfig.RESOURCE.LINKS.byteVal,
					GtDataConfig.CONSTANT.ONE.byteVal);
			kv.put(GtDataConfig.RESOURCE.SIZE.byteVal, Bytes.toBytes(size + ""));
			HbaseBase.writeRows(GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal(),
					Bytes.toBytes(url), GtDataConfig.RESOURCE.FAMILY.byteVal,
					kv);
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		// System.out.println(HbaseBase.selectRow(GtDataConfig.META_TABLENAME,
		// "/import//%E6%88%91%E6%98%AF%E4%B8%AD%E6%96%872"));
		// for(Map<String,String> map : search("/import","目录")){
		// System.out.println(map);
		// }
		// mkdir("/import");
		// System.out.println(reanme("/import3/new6/test4","123(1).txt","456.txt",true));
		// System.out.println(copyOrMove("/import3/new6/test3/_alllayers/55555","/import3/new6/test4","123.txt",false,true,false));
		// File file = new File("");
		System.out.println(new Date(Long.parseLong("1416476549832")));

	}

}
