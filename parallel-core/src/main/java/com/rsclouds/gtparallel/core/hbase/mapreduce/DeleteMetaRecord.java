package com.rsclouds.gtparallel.core.hbase.mapreduce;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;


public class DeleteMetaRecord extends Configured implements Tool{
	public static final Logger LOG = LoggerFactory.getLogger(DeleteMetaRecord.class);
	public static final String KEY_END_TIME_LONG = "end_time";
	public static final String KEY_PATH_FILTER_STR = "path_filter";
	public static final String KEY_METATABLE_NAME_STR = "metatable_name";
	public static final String KEY_AUTHTABLE_NAME_STR = "authtalbe_name";
	public static final String KEY_USER_NAME_STR = "user_name";
	public static final String KEY_TEST_BOOLEAN = "btest";
	
	public static class MapMetaFilter extends TableMapper<ImmutableBytesWritable, LongWritable> {
		public long longEndTime;
		public String strPathFilter;
		public HTable metaHTable = null;
		public byte[] META_FAMILY = "atts".getBytes();
		public byte[] META_QUALIFIER_SIZE = "size".getBytes();
		public LongWritable fileSizeLongWritable = new LongWritable();
		public boolean bText;
		
		protected void setup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			longEndTime = conf.getLong(KEY_END_TIME_LONG, 0);
			strPathFilter = conf.get(KEY_PATH_FILTER_STR, null);
			String strMetaTable = conf.get(KEY_METATABLE_NAME_STR, null);
			if (strMetaTable != null) {
				metaHTable = new HTable(conf, strMetaTable);
			}
			bText = conf.getBoolean(KEY_TEST_BOOLEAN, true);
		}
		
		public void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException {
			if (metaHTable == null) {
				return;
			}
			Cell cell = value.getColumnLatestCell(META_FAMILY, META_QUALIFIER_SIZE);
			long timeStamp = cell.getTimestamp();
			if (timeStamp < longEndTime) {
				
				if (!bText) {
					String newRowkey = "-//" + new String(key.copyBytes());
					Put put = new Put(newRowkey.getBytes());
					Map<byte[], byte[]> qv = value.getFamilyMap("atts".getBytes());
					for(byte[] qualifier : qv.keySet()){
						byte[] bytesValue = qv.get(qualifier);
						put.add(GtDataConfig.META.FAMILY.byteVal, qualifier, bytesValue);
					}	
					metaHTable.put(put);
					Delete del = new Delete(key.get());
					metaHTable.delete(del);
				}
				String sizeStr = new String(value.getValue(META_FAMILY, META_QUALIFIER_SIZE));
				long longSize = Long.parseLong(sizeStr);
				if (longSize > 0) {
					fileSizeLongWritable.set(longSize);
					context.write(key, fileSizeLongWritable);
				}
			}
		}
		
		public void cleanup(Context context)throws IOException, InterruptedException {
			if (metaHTable != null) {
				metaHTable.flushCommits();
				metaHTable.close();
			}
		}
	}
	
	public static class ReduceChangeUsedSize extends Reducer<ImmutableBytesWritable, LongWritable, Text, LongWritable> {
		
		private long sum = 0;
		private Text keyText = new Text();
		
		public void reduce(ImmutableBytesWritable key, Iterable<LongWritable> values, Context context)
				throws IOException, InterruptedException {
			keyText.set(key.get());
			for (LongWritable val: values) {
				sum += val.get();
				context.write(keyText, val);
			}	
		}
		
		public void cleanup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			String strAuthName = conf.get(KEY_AUTHTABLE_NAME_STR, "auth");
			String strUserName = conf.get(KEY_USER_NAME_STR, "print1507");
			HTable authHTable = new HTable(conf, strAuthName);
			boolean bTest = conf.getBoolean(KEY_TEST_BOOLEAN, true);
			Get get = new Get(strUserName.getBytes());
			Result result = authHTable.get(get);
			String strUsedSize = new String (result.getValue("atts".getBytes(), "used".getBytes()));
			sum = Long.parseLong(strUsedSize) - sum;
			if (!bTest) {
				Put put = new Put(strUserName.getBytes());
				put.add("atts".getBytes(), "used".getBytes(), Bytes.toBytes(""+sum));
				authHTable.put(put);
				authHTable.flushCommits();
			}
			System.out.println("now usedSize= " + sum);
			authHTable.close();		
		}
	}
	
	public int run(String[] args)throws Exception {
		if (args.length < 5) {
			usage();
			return 0;
		}
		System.out.println("access run");
		boolean bTest = BooleanUtils.toBoolean(args[4]);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date = format.parse(args[2]);
		long longEndTime = date.getTime();
		
		Configuration conf = HBaseConfiguration.create();
		String strPathFilter = args[1];
		while (strPathFilter.endsWith("/")) {
			strPathFilter = strPathFilter.substring(0, strPathFilter.length()-1);
		}
		conf.set(TableInputFormat.INPUT_TABLE, args[0]);
		conf.set(TableInputFormat.SCAN_ROW_START, strPathFilter + "//");
		conf.set(TableInputFormat.SCAN_ROW_STOP, strPathFilter + "/{");
		conf.setLong(KEY_END_TIME_LONG, longEndTime);
		conf.set(KEY_USER_NAME_STR, args[3]);
		conf.set(KEY_METATABLE_NAME_STR, args[0]);
		conf.setBoolean(KEY_TEST_BOOLEAN, bTest);
		
		Job job = Job.getInstance(conf, "delete meta(" + strPathFilter + ")");
		job.setJarByClass(DeleteMetaRecord.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapOutputKeyClass(ImmutableBytesWritable.class);
		job.setMapOutputValueClass(LongWritable.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapperClass(MapMetaFilter.class);
		job.setReducerClass(ReduceChangeUsedSize.class);
		Path path = new Path(GtDataConfig.HDFS_ROOT_PATH + "temp/" + System.currentTimeMillis());
		System.out.println(path.toString());
		FileOutputFormat.setOutputPath(job, path);
		
		System.out.println("sumbit job");
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args)throws Exception{
		if (args == null || args.length == 0) {
			args = new String[5];
			args[0] = "meta_2.6";
			args[1] = "/users/yongkun";
			args[2] = "2015-10-10";
			args[3] = "yongkun";
			args[4] = "true";
		}
		int status = ToolRunner.run(new DeleteMetaRecord(), args);
		System.exit(status);
	}
	
	public void usage() {
		LOG.info("uage: <metatable name> <path filter> <date(yyyy-MM-dd)> <user name> <test true/false>");
	}
}
