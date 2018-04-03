package com.rsclouds.gtparallel.core.hbase.mapreduce;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;


public class GTDataDataBaseCopy extends Configured implements Tool{
	private final Log LOG = LogFactory.getLog(GTDataDataBaseCopy.class);
	static private final String KEY_SRC_META_NAME = "src_meta_name";
	static private final String KEY_SRC_RES_NAME = "src_res_name";
	static private final String KEY_DST_META_NAME = "dst_meta_name";
	static private final String KEY_DST_RES_NAME = "dst_res_name";
	static private final String KEY_SRC_PATH = "src_path";
	static private final String KEY_DST_PATH = "dst_path";

	static class MetaDataCopy extends TableMapper<BytesWritable, NullWritable>{
		private HTable dstMetaTable = null;
		private HTable dstResTable = null;
		private BytesWritable UrlBytesWritable = new BytesWritable();
		private String srcPath = null;
		private String dstPath = null;
		private StringBuilder rowkeyStrBuilder = new StringBuilder();
		
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			String dstMetaName = conf.get(KEY_DST_META_NAME, null);
			String dstResName = conf.get(KEY_DST_RES_NAME, null);
			srcPath = conf.get(KEY_SRC_PATH, null);
			dstPath = conf.get(KEY_DST_PATH, null);
			if(dstPath != null) {
				while(true) {
					if(dstPath.endsWith("/")) {
						dstPath = dstPath.substring(0, dstPath.length()-1);
					}else {
						break;
					}
				}
			}
			if(srcPath != null) {
				while(true) {
					if(srcPath.endsWith("/")) {
						srcPath = srcPath.substring(0, srcPath.length()-1);
					}else {
						break;
					}
				}
			}
			dstMetaTable = new HTable(conf, dstMetaName);
			dstResTable = new HTable(conf, dstResName);
			
			dstMetaTable.setAutoFlushTo(false);
		}
		
		public void map(ImmutableBytesWritable key, Result values,
				Context context) throws IOException, InterruptedException {
			if(rowkeyStrBuilder.length() > 0) {
				rowkeyStrBuilder.delete(0, rowkeyStrBuilder.length());
			}
			rowkeyStrBuilder.append(new String(key.copyBytes()));
			if(dstPath != null) {
				if(srcPath != null) {
					rowkeyStrBuilder.replace(0, srcPath.length(), dstPath);
				}else {
					rowkeyStrBuilder.insert(0, dstPath);
				}
			}
			Map<byte[], byte[]> qv = values.getFamilyMap("atts".getBytes());
			System.out.println(rowkeyStrBuilder.toString());
			Put put = new Put(Bytes.toBytes(rowkeyStrBuilder.toString()));
			for(byte[] qualifier : qv.keySet()){
				byte[] value = qv.get(qualifier);
				put.add(GtDataConfig.META.FAMILY.byteVal, qualifier, value);
			}	
			dstMetaTable.put(put);
			byte[] url = values.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
			if(url != null && url.length > 0) {
				Get get = new Get(url);
				Result result = dstResTable.get(get);
				if(result == null || result.isEmpty()) {
					UrlBytesWritable.set(url, 0, url.length);
					context.write(UrlBytesWritable, NullWritable.get());
				}
				else {
					System.out.println("======nanlin===== has exit " + new String(url));
					return;
				}
			}
			
		}
		
		protected void cleanup(Context context)throws IOException, InterruptedException {
			String dirPath = "";
			if(dstPath != null) {
				dirPath = dstPath;
			}else {
				dirPath = srcPath;
			}
			if(dirPath.endsWith("/")) {
				dirPath = dirPath.substring(0, dirPath.length()-1);
			}
			int indexof = 0;
			String timeStr = "" + System.currentTimeMillis();
			while(dirPath.length() > 1) {
//				test.mkdir(dstMetaTable, dirPath);
				String rowkeyStr = GtDataUtils.format2GtPath(dirPath);
				Put put = new Put(rowkeyStr.getBytes());
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.CAPACITY.byteVal, GtDataConfig.CONSTANT.DIR_CAPACITY.byteVal);
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.PERMISSON.byteVal, GtDataConfig.CONSTANT.DIR_PERMISSON.byteVal);
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, "".getBytes());
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.TIME.byteVal, timeStr.getBytes());
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal, "-1".getBytes());
				put.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.DFS.byteVal, "0".getBytes());
				dstMetaTable.put(put);
				indexof = dirPath.lastIndexOf('/');
				dirPath = dirPath.substring(0, indexof);
			}
			dstMetaTable.flushCommits();
			dstMetaTable.close();
			dstResTable.close();
		}
		
	}
	
	static class ResourceCopy extends TableReducer<BytesWritable, NullWritable, NullWritable>{
		HTable srcResTable = null;
		
		protected void setup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			String srcResName = conf.get(KEY_SRC_RES_NAME, "test");
			System.out.println("srcResName= " + srcResName);
			srcResTable = new HTable(conf, srcResName);
			if (srcResTable == null) {
				System.out.println("table null");
			}
		}
		
		public void reduce(BytesWritable key, Iterable<NullWritable> value, Context context)
				throws IOException, InterruptedException{
			if(key == null || key.getLength() == 0) {
				System.out.println("===nanlin===debug null.");
				return;
			}
			Get get = new Get(key.copyBytes());
			System.out.println(new String (key.copyBytes()));
			Result result = srcResTable.get(get);
			if(result.isEmpty() || result == null) {
				System.out.println("get resource error" + new String(key.copyBytes()));
				return;
			}
			Map<byte[], byte[]> qv = result.getFamilyMap("img".getBytes());
			System.out.println(new String(key.copyBytes()));
			Put put = new Put(key.copyBytes());
			for(byte[] qualifier : qv.keySet()){
				byte[] columnValue = qv.get(qualifier);
				put.add(GtDataConfig.RESOURCE.FAMILY.byteVal, qualifier, columnValue);
			}	
			context.write(NullWritable.get(), put);
		}
		
		protected void cleanup(Context context)throws IOException, InterruptedException {
			srcResTable.close();
		}
	}
	
	
	@Override
	public int run(String[] args) throws Exception {
		if(args.length < 4) {
			LOG.info("usage: <source metadata table> <source resource table> <destination metadata table> " +
					"<destination resource table> [source path] [dst path]");
			return 1;
		}
		Configuration conf = HBaseConfiguration.create();
		String srcMetaName = args[0];
		String srcResName = args[1];
		String dstMetaName = args[2];
		String dstResName = args[3];
		String srcPath = null;
		String dstPath = null;
		if(args.length > 4) {
			srcPath = args[4];
		}
		if(args.length > 5) {
			dstPath = args[5];
		}
		conf.set(KEY_SRC_META_NAME, srcMetaName);
		conf.set(KEY_SRC_RES_NAME,  srcResName);
		conf.set(KEY_DST_META_NAME, dstMetaName);
		conf.set(KEY_DST_RES_NAME,  dstResName);
		conf.set(TableOutputFormat.OUTPUT_TABLE, dstResName);
		conf.set(TableInputFormat.INPUT_TABLE, srcMetaName);
		conf.set(TableInputFormat.SCAN_ROW_START, srcPath + "/");
		conf.set(TableInputFormat.SCAN_ROW_STOP, srcPath + "{");
		if(srcPath != null)
			conf.set(KEY_SRC_PATH, srcPath);
		if(dstPath != null)
			conf.set(KEY_DST_PATH, dstPath);		
		
		Job job = Job.getInstance(conf, "dlfj");
		job.setJarByClass(GTDataDataBaseCopy.class);
		job.setNumReduceTasks(6);
//		Scan scan = new Scan();
//		scan.addFamily(Bytes.toBytes("atts"));
//		if(srcPath != null) {
//			RowFilter filter = new RowFilter(CompareOp.EQUAL, new BinaryPrefixComparator(srcPath.getBytes()));
//			scan.setFilter(filter);
//			if(!srcPath.endsWith("/")) {
//				srcPath += "/";
//			}
//			scan.setStartRow((srcPath + "/").getBytes());
//			scan.setStopRow((srcPath + "{").getBytes());
//		}
//		TableMapReduceUtil.initTableMapperJob(srcMetaName, scan, MetaDataCopy.class,
//				BytesWritable.class, NullWritable.class , job);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setInputFormatClass(TableInputFormat.class);

		job.setOutputFormatClass(TableOutputFormat.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapOutputKeyClass(BytesWritable.class);
		job.setMapOutputValueClass(NullWritable.class);	
		job.setMapperClass(MetaDataCopy.class);
		job.setReducerClass(ResourceCopy.class);
		

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
//		args = new String[5];
//		args[0] = "meta_2.6";
//		args[1] = "res_2.6";
//		args[2] = "meta_2.6_test";
//		args[3] = "res_2.6_test";
//		args[4] = "/users/yongkun";
		
		int status = ToolRunner.run(new GTDataDataBaseCopy(), args);
		System.exit(status);
	}
}
