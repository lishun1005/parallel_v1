package com.rsclouds.gtparallel.core.hbase.mapreduce;

/**
*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;

/**
* Pass the given key and record as-is to the reduce phase.
*/
@InterfaceAudience.Public
@InterfaceStability.Stable
public class MetaTableMapper
extends TableMapper<ImmutableBytesWritable, Result> {
	
  static long count = 0;

 /**
  * Use this before submitting a TableMap job. It will appropriately set up
  * the job.
  *
  * @param table  The table name.
  * @param scan  The scan with the columns to scan.
  * @param mapper  The mapper class.
  * @param job  The job configuration.
  * @throws IOException When setting up the job fails.
  */
 @SuppressWarnings("rawtypes")
 public static void initJob(String table, Scan scan,
   Class<? extends TableMapper> mapper, Job job) throws IOException {
   TableMapReduceUtil.initTableMapperJob(table, scan, mapper,ImmutableBytesWritable.class, Result.class, job);
//   TableMapReduceUtil.initTableMapperJob(table, scan, mapper, ImmutableBytesWritable.class, Result.class, job,false,true,TableInputFormat.class);
 }

 /**
  * Pass the key, value to reduce.
  *
  * @param key  The current key.
  * @param value  The current value.
  * @param context  The current context.
  * @throws IOException When writing the record fails.
  * @throws InterruptedException When the job is aborted.
  */
 public void map(ImmutableBytesWritable key, Result value, Context context)
 throws IOException, InterruptedException {
	 String row = new String(key.copyBytes());
	 if(row.startsWith("/projects/rscloudmart/data/GF1/20141117Rar")){
		 return;
	 }else if(row.startsWith("/projects/rscloudmart/data/GF1/20140826")){
		 return;
	 }else if(row.startsWith("/projects/rscloudmart/data/GF1/20140828Src")){
		 return;
	 }else if(row.startsWith("/projects/rscloudmart/data/GF1/20140901Src")){
		 return;
	 }else if(row.startsWith("/projects/rscloudmart/data/GF1/20140919Src")){
		 return;
	 }else if(row.startsWith("/projects/rscloudmart/data/GF1/20141103Src")){
		 return;
	 }else if(row.startsWith("/projects/rscloudmart/data/LC8/20140909Src")){
		 return;
	 }else if (row.startsWith("/projects/rscloudmart/userftp")) {
			return;
	 }else if (row.startsWith("/users/xiaoshaolin")) {
		return;
	 }else{
		count++;
//		System.out.println(count + ":" + Bytes.toString(key.copyBytes()));
		context.write(key, value);
	 }
 }

}

