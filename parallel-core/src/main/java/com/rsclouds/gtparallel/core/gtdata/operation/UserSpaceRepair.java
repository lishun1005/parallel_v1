package com.rsclouds.gtparallel.core.gtdata.operation;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.gtdata.entity.GtPath;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class UserSpaceRepair extends Configured implements Tool {
	private static Logger LOG = LoggerFactory.getLogger(UserSpaceRepair.class);

	public static class Map extends TableMapper<Text, Text> {
		Configuration conf;
		String groupPath = "";

		@Override
		public void setup(Context context) {
			conf = context.getConfiguration();
			groupPath = conf.get("group_path");
		}

		public void map(ImmutableBytesWritable key, Result result,Context context) throws IOException, InterruptedException
		{
			byte[] size = result.getValue(GtDataConfig.META.FAMILY.byteVal,GtDataConfig.META.SIZE.byteVal);
			if (Arrays.equals(size,GtDataConfig.CONSTANT.NEGATIVE_ONE.byteVal)) {
				return;
			}
			String user = Bytes.toString(key.copyBytes()).replace(groupPath, "").split("/")[1];
			context.write(new Text(user), new Text(size));
		}
	}

	public static class Reduce extends TableReducer<Text, Text, NullWritable> {
		Configuration conf;
		String authTable = "";
		String groupPath = "";
		
		@Override
		public void setup(Context context) {
			conf = context.getConfiguration();
			authTable = conf.get("auth_table");
			groupPath = conf.get("group_path");
			
		}
		
		public void reduce(Text key, Iterable<Text> values, Context context)throws IOException{
			long used = 0;
			for(Text size : values){
				used += Long.valueOf(size.toString());
			}
			HbaseBase.writeRow(authTable, key.toString(), CoreConfig.AUTH_FIMALY, CoreConfig.AUTH_USED,used+"");
//			HbaseBase.writeRow(authTable, key.toString(), Config.AUTH_FIMALY, Config.AUTH_ROLE,"2");
//			HbaseBase.writeRow(authTable, key.toString(), Config.AUTH_FIMALY, Config.AUTH_PREFIX,groupPath+"/"+key.toString()+"/");
			System.out.println(key.toString() + " : " + used);
		}
		
	}

	@Override
	public int run(String[] args) throws Exception {
		if (args.length != 2) {
			return 1;
		}
		String userTable = args[0];
		// 创建auth table
		HbaseBase.createTable(userTable, new String[] { CoreConfig.AUTH_FIMALY });
		GtPath groupPathObj = new GtPath(args[1]);
		String startRow = groupPathObj.getGtPath().replace("//", "/");
		Configuration conf = HBaseConfiguration.create();
		conf.set(TableInputFormat.INPUT_TABLE,GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		conf.set(TableOutputFormat.OUTPUT_TABLE, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
		conf.set(TableInputFormat.SCAN_COLUMN_FAMILY,GtDataConfig.META.FAMILY.strVal);
		conf.set(TableInputFormat.SCAN_ROW_START, startRow + "/");
		conf.set(TableInputFormat.SCAN_ROW_STOP, startRow + "/{");
		conf.set("group_path", startRow);
		conf.set("auth_table", userTable);
		Job job = Job.getInstance(conf, "UserSizeRepair");
		job.setJarByClass(UserSpaceRepair.class);
		job.setInputFormatClass(TableInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setMapperClass(Map.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setReducerClass(Reduce.class);
		boolean status = job.waitForCompletion(true);
		return  status? 0 : 1;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
//		 args = new String[]{"auth","/users"};
		if (args == null || args.length < 2) {
			LOG.info("usage: <user table name> <group path>");
		} else {
			int exitcode = ToolRunner.run(new UserSpaceRepair(), args);
			System.exit(exitcode);
		}
	}

}
