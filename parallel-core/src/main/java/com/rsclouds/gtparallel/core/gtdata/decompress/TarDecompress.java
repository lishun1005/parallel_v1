package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.core.gtdata.common.RedisUtils;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

public class TarDecompress extends Configured implements Tool {
	private static final Logger LOG = LoggerFactory
			.getLogger(TarDecompress.class);

	static class TarDecompressMapper extends Mapper<Text, Text, Text, Text> {
		final String ONE = "1";
		final String NEGATIVE_ONE = "-1";
		final String ZERO = "0";
		String storagePath = "storage_path";
		Configuration conf;
		Configuration hbaseConfig;
		HTable resTable = null;
		StringBuilder filePathBuild = new StringBuilder("");
		Text keyOut = new Text();
		Text valueOut = new Text();
		private BufferedInputStream inputStream;

		public void setup(Context context) throws IOException,
				InterruptedException {
			conf = context.getConfiguration();
			storagePath = conf.get("storage_path");
			hbaseConfig = HBaseConfiguration.create();
			resTable = new HTable(hbaseConfig, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			while (storagePath.length() > 1 && storagePath.endsWith("/")) {
				storagePath = storagePath
						.substring(0, storagePath.length() - 1);
			}
		}

		public void map(Text key, Text value, Context context)
				throws IOException, InterruptedException {
			File file = new File(value.toString());
			try {

				filePathBuild.append(storagePath + "/" + key.toString());

				if (filePathBuild.lastIndexOf("/") == filePathBuild.length() - 1) {
					filePathBuild.deleteCharAt(filePathBuild.length() - 1);
				}

				filePathBuild.replace(0, filePathBuild.length(), TransCoding
						.UrlEncode(filePathBuild.toString(), "utf-8"));
				filePathBuild.insert(filePathBuild.lastIndexOf("/"), "/");
				// filePathBuild.replace(0, filePathBuild.length(), GtDataUtils
				// .replaceLast(filePathBuild.toString(), "/", "//"));
				byte[] md5Bytes = null;
				String md5Str = "";
				long fileSize = file.length();
				if (key.toString().endsWith("/")) {
					// 写入文件夹
					// LOG.info("========写入文件夹:"+filePath+"========");
				} else if (fileSize == 0) {
					// 空文件使用一样的MD5
					md5Str = "d41d8cd98f00b204e9800998ecf8427e";
					md5Bytes = Bytes.toBytes(md5Str);
					// LOG.info("========写入空文件:"+filePath+"========MD5:"+md5Str);
				} else {
					md5Str = MD5Calculate.LocalfileMD5(value.toString());
					md5Bytes = Bytes.toBytes(md5Str);
					// LOG.info("========写入文件:"+filePath+"========MD5:"+md5Str+"========size:"+fileSize);
				}

				String url = "";
				if (md5Bytes == null) {
					fileSize = -1;
					url = "";
				} else {
					url = md5Str;

					Put resPut = new Put(md5Bytes);
					Get get = new Get(md5Bytes);
					Result result = resTable.get(get);
					if (result != null && !result.isEmpty()) {

					} else {// file doesn't exists on gt-data
						resPut.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
								GtDataConfig.RESOURCE.LINKS.byteVal, GtDataConfig.CONSTANT.ONE.byteVal);
						if (fileSize < 16777216) {// less than 16MB,input hbase

							inputStream = new BufferedInputStream(
									new FileInputStream(file));
							ByteArrayOutputStream out = new ByteArrayOutputStream(
									(int) file.length());
							byte[] temp = new byte[1024];
							int size = 0;
							while ((size = inputStream.read(temp)) != -1) {
								out.write(temp, 0, size);
							}
							byte[] content = out.toByteArray();
							resPut.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
									GtDataConfig.RESOURCE.DATA.byteVal, content);
							inputStream.close();
							out.close();
						} else {// more than 16MB, input hdfs
							FileSystem fs = FileSystem.get(hbaseConfig);
							FSDataOutputStream out = fs.create(new Path(GtDataConfig.HDFS_MD5_PATH,new String(md5Bytes)));
							inputStream = new BufferedInputStream(
									new FileInputStream(file));

							byte[] temp = new byte[1024];
							int size = 0;
							while ((size = inputStream.read(temp)) != -1) {
								out.write(temp, 0, size);
							}

							inputStream.close();
							out.close();
							fs.close();
						}
						resTable.put(resPut);
					}
				}
				keyOut.set(filePathBuild.toString());
				if (fileSize < 16777216) {
					valueOut.set(fileSize + "," + url + "," + ZERO);
				} else {
					valueOut.set(fileSize + "," + url + "," + ONE);
				}

				context.write(keyOut, valueOut);
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			} finally {
				filePathBuild.delete(0, filePathBuild.length());
			}
		}

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			if (resTable != null) {
				resTable.flushCommits();
				resTable.close();
			}
		}
	}

	static class TarDecompressReducer extends
			TableReducer<Text, Text, NullWritable> {
		Configuration hbaseConfig = HBaseConfiguration.create();
		HTable metaTable = null;

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			if (metaTable == null) {
				metaTable = new HTable(hbaseConfig, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			}
			Iterator<Text> it = values.iterator();
			while (it.hasNext()) {
				Text value = it.next();
				LOG.info(key.toString() + " : " + value.toString());
				String[] args = value.toString().split(",");
				if (args.length != 3) {
					// error
				}
				byte[] rowkey = Bytes.toBytes(key.toString());
				Get get = new Get(rowkey);
				Result result = metaTable.get(get);
				if (result == null || result.isEmpty()) {
					Put metaPut = new Put(rowkey);
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.SIZE.byteVal, Bytes.toBytes(args[0]));
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.URL.byteVal, Bytes.toBytes(args[1]));
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.DFS.byteVal, Bytes.toBytes(args[2]));
					metaPut.add(GtDataConfig.META.FAMILY.byteVal,
							GtDataConfig.META.TIME.byteVal,
							Bytes.toBytes("" + System.currentTimeMillis()));
					if(args[1].length() == 0) {
						metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal,
								GtDataConfig.CONSTANT.DIR_CAPACITY.byteVal);
						metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal,
								GtDataConfig.CONSTANT.DIR_PERMISSON.byteVal);
					}else {
						metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal,
								GtDataConfig.CONSTANT.FILE_CAPACITY.byteVal);
						metaPut.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal,
								GtDataConfig.CONSTANT.FILE_PERMISSON.byteVal);
					}
					context.write(NullWritable.get(), metaPut);
				}
			}
		}
	}


	public int run(String[] args) throws Exception {
		if (args.length < 2) {
			LOG.info("usage: <inputpath> <Storage_path>");
			return 0;
		}
		String urlcode = args[1];
		if (!urlcode.contains("%")){
			urlcode = TransCoding.UrlEncode(urlcode, "utf-8");
		}
		Configuration conf = getConf() == null ? HBaseConfiguration.create()
				: getConf();
		conf.set("mapred.child.java.opts", "-Xmx4096m");
		conf.setInt("mapred.tasktracker.map.tasks.maximum", 1);

		conf.set("storage_path", args[1]);
		conf.set(TableOutputFormat.OUTPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		Job job = Job.getInstance(conf);

		job.setJarByClass(TarDecompress.class);
		job.setMapperClass(TarDecompressMapper.class);
		job.setReducerClass(TarDecompressReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setInputFormatClass(TarInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setNumReduceTasks(6);

		FileSystem fs = FileSystem.get(conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(args[0]));
		List<Path> paths = new ArrayList<Path>();
		for (int i = 0; i < fileStatus.length; i++) {
			Path p = fileStatus[i].getPath();
			if (p.getName().endsWith(".tar.gz")) {
				paths.add(p);
			}
		}
		FileInputFormat.setInputPaths(job,
				paths.toArray(new Path[paths.size()]));

		
		
		boolean flag = job.waitForCompletion(true);
		if(flag){
			// 生成gtdata输出目录结构
			if (!GtDataUtils.genterGtdataDir(args[1])) {
				LOG.info("ERROR <Storage_path> : " + args[1]);
				return 1;
			}
			// redis check
			RedisUtils.redisDirCheck(new GtPath(args[1]).getGtPath(), GtDataConfig.REDIS_HOST);
		}
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		// args = new
		// String[]{"hdfs://node03.rsclouds.cn:8020/nanlin_root/test.tar.gz","/import/test"};
		long start = System.currentTimeMillis();
		int exitCode = ToolRunner.run(new TarDecompress(), args);
		long end = System.currentTimeMillis();
		LOG.info("Tar decompress time : " + (end - start) + "(ms)");
		System.exit(exitCode);
	}

}
