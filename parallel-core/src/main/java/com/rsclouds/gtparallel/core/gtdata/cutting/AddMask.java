package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.ImageUtils;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;



public class AddMask extends Configured implements Tool{
	
	private static final Log LOG = LogFactory.getLog(AddMask.class);
	private static final String INPUTPATH = "inputPath";
	private static final String OUTPUTPATH = "outputPath";
	
	static class ReadHbaseMapper extends TableMapper<BytesWritable, BytesWritable> {
		private BytesWritable keyOut = new BytesWritable();
		private BytesWritable valueOut = new BytesWritable();
		private String inputPath;
		private String outputPath;
		private HTable metatable;
		
		protected void setup(Context context)throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			inputPath = conf.get(INPUTPATH, null);
			outputPath = conf.get(OUTPUTPATH, null);
			if(inputPath != null) {
				while(inputPath.endsWith("/"))
					inputPath = inputPath.substring(0, inputPath.length()-1);
				//inputPath = TransCoding.UrlEncode(inputPath, "utf-8");
			}
			
			if(outputPath != null) {
				while(outputPath.endsWith("/"))
					outputPath = outputPath.substring(0, outputPath.length()-1);
				//outputPath = TransCoding.UrlEncode(outputPath, "utf-8");
			}
			metatable = new HTable(conf, GtDataConfig.TABLE_NAME.MAP_META_TABLE.getStrVal());
			metatable.setAutoFlushTo(false);
		}
		
		public void map(ImmutableBytesWritable key, Result result, Context context)throws IOException,
				InterruptedException {
			if( inputPath == null || outputPath == null)
				return;
			String rowkey = new String(key.get());
			rowkey = rowkey.replaceFirst(inputPath, outputPath);
			//插入metatable 表
			Put put = new Put(rowkey.getBytes());
			Map<byte[], byte[]> quaVal = result.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
			for(byte[] qualifier : quaVal.keySet()) {
				byte[] colVal = quaVal.get(qualifier);
				put.add(GtDataConfig.META.FAMILY.byteVal, qualifier, colVal);
			}
			metatable.put(put);
			
			//插入resource表，由reduce阶段实现
			String sizeStr = new String(result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal));
			if (!sizeStr.equals("-1")) {
				byte[] url = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
				keyOut.set(rowkey.getBytes(), 0, rowkey.length());
				valueOut.set(url, 0, url.length);
				context.write(keyOut, valueOut);
			}
		}
		
		protected void cleanup(Context context) throws IOException, InterruptedException {
//			String confXMLInput = inputPath + "Layers//conf.xml";
//			String confCDIInput = inputPath + "Layers//conf.cdi";
//			String confXMLOutput = outputPath + "Layers//conf.xml";
//			String confCDIOutput = outputPath + "Layers//conf.cdi";
			if(metatable != null) {
//				Get get = new Get( confXMLInput.getBytes() );
//				Result result = metatable.get(get);
//				if(!result.isEmpty()) {
//					Put put = new Put(confXMLOutput.getBytes());
//					Map<byte[], byte[]> qv = result.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
//					for(byte[] qualifier : qv.keySet()) {
//						byte[] colVal = qv.get(qualifier);
//						put.add(GtDataConfig.META.FAMILY.byteVal, qualifier, colVal);
//					}
//					metatable.put(put);
//				}
//				
//				get = new Get( confCDIInput.getBytes() );
//				result = metatable.get(get);
//				if(!result.isEmpty()) {
//					Put put = new Put(confCDIOutput.getBytes());
//					Map<byte[], byte[]> qv = result.getFamilyMap(GtDataConfig.META.FAMILY.byteVal);
//					for(byte[] qualifier : qv.keySet()) {
//						byte[] colVal = qv.get(qualifier);
//						put.add(GtDataConfig.META.FAMILY.byteVal, qualifier, colVal);
//					}
//					metatable.put(put);
//				}
				metatable.flushCommits();
				metatable.close();
			}
		}
	}
	
	
	static class AddMaskReducer extends TableReducer<BytesWritable, BytesWritable, NullWritable> {
		private HTable metaTable = null;
		private HTable resTable = null;
		private ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
		private StringBuilder strBuilderMD5 = new StringBuilder();
		private Text sizeText = new Text();
		
		protected void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			metaTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
			metaTable.setAutoFlushTo(false);
			resTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
			
			
		}
		
		public void reduce(BytesWritable key, Iterable<BytesWritable> values, Context context)
				throws IOException, InterruptedException {
			// change the data of byte to BufferedImage
			String rowkey = new String(key.copyBytes());
			if(!rowkey.endsWith("png") && !rowkey.endsWith("jpg"))
				return;
			byteArrayOut.reset();
			Put putMeta = new Put(key.copyBytes());
			for(BytesWritable val : values) {
				strBuilderMD5.delete(0, strBuilderMD5.length());
				Get get = new Get(val.copyBytes());
				Result result = resTable.get(get);
				if(result.isEmpty())
					break;
				byte[] data = result.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.DATA.byteVal);
				ImageIcon imageIcon = new ImageIcon(data);
				BufferedImage bufferImag = new BufferedImage(CoreConfig.WIDTH_DEFAULT, CoreConfig.HEIGHT_DEFAULT, BufferedImage.TYPE_INT_ARGB_PRE);
				Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
				gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
				ImageUtils.pressText(bufferImag, "ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 15, Color.WHITE, -1, -1, 0.4f);
				ImageIO.write(bufferImag, "png", byteArrayOut);
				sizeText.set("" + byteArrayOut.size());
				
				strBuilderMD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));
				byte[] md5Byte = Bytes.toBytes(strBuilderMD5.toString());
				putMeta.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal, md5Byte);
				putMeta.add(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.SIZE.byteVal, sizeText.getBytes());
				System.out.println("rowkey= " + rowkey);
				System.out.println("old url= " + new String(val.copyBytes()));
				System.out.println("new url= " + strBuilderMD5.toString());
				metaTable.put(putMeta);
				metaTable.flushCommits();
				
				get = new Get(md5Byte);
				Result resultRes = resTable.get(get);
				Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
				if (resultRes.isEmpty()) {			
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.DATA.byteVal, byteArrayOut.toByteArray());
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
					context.write(NullWritable.get(), put);
				}
//				} else {
//					String linksStr = new String(result.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.LINKS.byteVal));
//					int linksInt = Integer.parseInt(linksStr) + 1;
//					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("" + linksInt));
//				}
				//context.write(NullWritable.get(), put);
			}
		}
		
		protected void clenanup(Context context)throws IOException, InterruptedException {
			super.cleanup(context);
			if(metaTable != null) {
				metaTable.flushCommits();
				metaTable.close();
			}
			if(resTable != null) {
				resTable.close();
			}
		}
	}
	
	public int run(String[] args) throws Exception {
		LOG.info("Merging agrs : " + ArrayUtils.toString(args));
		String inputPath = args[1];
		String outputPath = args[2];
		if(inputPath != null) {
			while(inputPath.endsWith("/"))
				inputPath = inputPath.substring(0, inputPath.length()-1);
			inputPath = TransCoding.UrlEncode(inputPath, "utf-8");
		}
		
		if(outputPath != null) {
			while(outputPath.endsWith("/"))
				outputPath = outputPath.substring(0, outputPath.length()-1);
			outputPath = TransCoding.UrlEncode(outputPath, "utf-8");
		}
		
		Configuration conf = HBaseConfiguration.create();
		conf.set(TableInputFormat.INPUT_TABLE, GtDataConfig.TABLE_NAME.MAP_META_TABLE.getStrVal());
		conf.set(TableOutputFormat.OUTPUT_TABLE, GtDataConfig.TABLE_NAME.MAP_RES_TABLE.getStrVal());
		conf.set(TableInputFormat.SCAN_ROW_START, inputPath + "//");
		conf.set(TableInputFormat.SCAN_ROW_STOP, inputPath + "/{");
		conf.set(CoreConfig.JOBID, args[0]);
		conf.set(INPUTPATH, inputPath);
		conf.set(OUTPUTPATH, outputPath);
		Job job = Job.getInstance(conf, "AddMask");
		job.setJarByClass(AddMask.class);
		job.setNumReduceTasks(4);
		job.setInputFormatClass(TableInputFormat.class);
		job.setMapperClass(ReadHbaseMapper.class);
		job.setMapOutputKeyClass(BytesWritable.class);
		job.setMapOutputValueClass(BytesWritable.class);
		job.setReducerClass(AddMaskReducer.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		
//		Scan scan = new Scan();
//		scan.addFamily(GtDataConfig.META.FAMILY.byteVal);
//		Filter filter = new RowFilter(CompareOp.EQUAL,
//				new BinaryPrefixComparator(args[1].getBytes()));
//		scan.setFilter(filter);
//		scan.setBatch(400);
//		scan.setCacheBlocks(false);
//		scan.setStartRow(Bytes.toBytes(inputPath + "//"));
//		scan.setStopRow(Bytes.toBytes(inputPath + "/{"));
//
//		TableMapReduceUtil.initTableMapperJob(CoreConfig.MAP_META_TABLE, scan,
//				ReadHbaseMapper.class, BytesWritable.class, BytesWritable.class,
//				job, true);
//		TableMapReduceUtil.initTableReducerJob(CoreConfig.MAP_RES_TABLE,
//				AddMaskReducer.class, job);
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args)throws Exception {
		if(args == null || args.length < 3) {
			args = new String[3];
			args[0] = "123456test";
			args[1] = "/2015042501";
			args[2] = "/2015042501_mask";
		}
		if(args.length < 3) {
			LOG.info("usage: <rowkey> <gt-data input path(don't include Layers and _alllayers)> <gt-data ouput path(don't include Layers and _alllayers)>");
		}else {
			int status = ToolRunner.run(new AddMask(), args);
			System.exit(status);
//			System.out.println(new Date(Long.parseLong("1417968217158")));
//			System.out.println(new Date(Long.parseLong("1418003584031")));
		}
	}
}
