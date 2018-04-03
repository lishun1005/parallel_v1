package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.hadoop.io.AdfGeoTransformArray;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImgBlockCuttingInputFormat;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;




public class ImageSegementMD5 extends Configured implements Tool {

	private static final Log LOG = LogFactory
			.getLog(ImageSegementMD5.class);

	/**
	 * 
	 * @author shaolin
	 *insert the record into resource table
	 *and output<filePaht, fileInfo> to reduce
	 *
	 */
	static class ImageSegMapper extends
			Mapper<AdfGeoTransformArray, BytesWritable, Text, FileInfo> {

		private double[] adfGeoTransform;
		private Text path = new Text();
		private FileInfo fileInfo = new FileInfo();
		private HTable resourceTable;
		private StringBuilder rowkey;
		private StringBuilder rowName;
		private StringBuilder colName;
		private int rowkeyPrefixLen;
		private StringBuilder filename;
		private StringBuilder MD5;
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();

		protected void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			resourceTable = new HTable(conf,CoreConfig.MAP_RES_TABLE);
			resourceTable.setAutoFlush(false, true);
			rowkey = new StringBuilder(conf.get(CoreConfig.CUTTING_OUTPUTPATH, CoreConfig.TRASH + "/" + context.getJobID().toString()));
			rowkeyPrefixLen = rowkey.length();
			rowName = new StringBuilder();
			colName = new StringBuilder();
			filename = new StringBuilder("C00000000.png");
			MD5 = new StringBuilder();
		}

		/**
		 * cutting the image and put the data into resource table
		 * 
		 * @param key
		 * @param value
		 * @param context
		 * @throws IOException
		 * @throws InterruptedException
		 */
		public void map(AdfGeoTransformArray key, BytesWritable value, Context context)
				throws IOException, InterruptedException {
			// change the data of byte to BufferedImage
			ImageIcon imageIcon = new ImageIcon(value.getBytes());
			int width = imageIcon.getIconWidth();
			int height = imageIcon.getIconHeight();
			BufferedImage img = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_ARGB_PRE);
			Graphics2D gs = (Graphics2D) img.getGraphics();
			gs.drawImage(imageIcon.getImage(), 0, 0,
					imageIcon.getImageObserver());

			//cutting the bufferedImage
			adfGeoTransform = key.getAdfGeoTransform().clone();
			int widthRead = 0;
			int heightRead = 0;
			int widthRang = CoreConfig.WIDTH_DEFAULT;
			int heightRang = CoreConfig.HEIGHT_DEFAULT;
			double xCoordinate;
			double yCoordinate;
			long tilerow, tilerowTopLeft, tilerowOffset;
			long tilecol, tilecolTopLeft, tilecolOffset;
			int count , i;
			int widthLength, heightLength, xOrign, yOrign;
			//calculate the tile of ranks No.
			tilerowTopLeft = (long) Math.floor((90 - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / Math.abs(adfGeoTransform[5]));
			tilecolTopLeft = (long) Math.floor((180 + adfGeoTransform[0]) / CoreConfig.WIDTH_DEFAULT / adfGeoTransform[1]);
			//calculate the coordinate of the upper left corner
			yCoordinate = 90 - tilerowTopLeft * Math.abs(adfGeoTransform[5]) * CoreConfig.HEIGHT_DEFAULT; 
			xCoordinate = tilecolTopLeft * adfGeoTransform[1] * CoreConfig.WIDTH_DEFAULT - 180;
			//caclute the offet between image's upper left corner and tile's upper left corner
			tilecolOffset =(long) ((adfGeoTransform[0] - xCoordinate)/adfGeoTransform[1]);
			tilerowOffset =(long) ((adfGeoTransform[3] - yCoordinate)/adfGeoTransform[5]);
			//adfGeoTransform[0] = xCoordinate;
			//adfGeoTransform[3] = yCoordinate;
			System.out.println("*********************DEBUG*********************");
			System.out.println( adfGeoTransform[0] + " " + adfGeoTransform[3]);
			System.out.println("width= " + width + "; height= " + height);
			System.out.println(tilecolOffset + " " + tilerowOffset);
			System.out.println("*********************DEBUG*********************");
			for (; widthRead < width;) {
				if (width - widthRead < widthRang) {
					widthLength = width - widthRead;
				}else if (widthRead == 0) {
					widthLength =(int) (widthRang - tilecolOffset);
				}else {
					widthLength = widthRang;
				}
				
				if (widthRead == 0) {
					tilecol = tilecolTopLeft;
					xOrign = (int)tilecolOffset;
					System.out.println("*********************DEBUG*********************");
					System.out.println("xOrign= " + xOrign);
					System.out.println("*********************DEBUG*********************");
				} else {
					xOrign = 0;
					xCoordinate = adfGeoTransform[0] + widthRead * adfGeoTransform[1] + heightRead* adfGeoTransform[2];
					tilecol = (long) Math.floor((180 + xCoordinate) / CoreConfig.WIDTH_DEFAULT / adfGeoTransform[1]);
				}
				colName.replace(0, rowName.length(),
						Long.toHexString(tilecol));
				count = 8 - colName.length();
				for (i = 0; i < count; i++) {
					colName.insert(0, "0");
				}
				colName.insert(0, "C");
				
				for (heightRead = 0; heightRead < height;) {
					if (height - heightRead < heightRang) {
						heightLength = height - heightRead;
					}else if (heightRead == 0) {
						heightLength = (int)(heightRang - tilerowOffset);
					}else {
						heightLength = heightRang;	
					}
					
					if (heightRead == 0) {
						tilerow = tilerowTopLeft;
						yOrign = (int)tilerowOffset;
						System.out.println("*********************DEBUG*********************");
						System.out.println("heightLength= " + heightLength);
						System.out.println("yOrign= " + yOrign);
						System.out.println("*********************DEBUG*********************");
					}else {
						yOrign = 0;
						//adfGeoTransform[0]  左上角x坐标
						//adfGeoTransform[1]  东西方向分辨率
						//adfGeoTransform[2]  旋转角度, 0表示图像 "北方朝上"
						//adfGeoTransform[3]  左上角y坐标
						//adfGeoTransform[4]  旋转角度, 0表示图像 "北方朝上"
						//adfGeoTransform[5]  南北方向分辨率
						//cacluate the upper left corner's coordinate(x,y)				
						yCoordinate = adfGeoTransform[3] + widthRead * adfGeoTransform[4] + heightRead* adfGeoTransform[5];				
						//change the coordiante to linenum and column number
						tilerow = (long) Math.floor((90 - yCoordinate) / CoreConfig.HEIGHT_DEFAULT / Math.abs(adfGeoTransform[5]));
					}
					
					// formatting the rowName and colName
					//like R00000001 C00000001
					rowName.replace(0, rowName.length(),
							Long.toHexString(tilerow));
					count = 8 - rowName.length();
					for (i = 0; i < count; i++) {
						rowName.insert(0, "0");
					}
					rowName.insert(0, "R");
			
					//cut out a block with 256*256
					int[] rgbArray = new int[widthRang * heightRang];
					
					rgbArray = img.getRGB(widthRead, heightRead, widthLength,
							heightLength, rgbArray, 0, widthLength);
					BufferedImage imgBlock = new BufferedImage(256, 256,
							BufferedImage.TYPE_INT_ARGB_PRE);
					if (yOrign != 0)
						System.out.println("yOrign= " + yOrign);
					if (xOrign != 0)
						System.out.println("xOrign= " + xOrign);
					imgBlock.setRGB(xOrign, yOrign, widthLength, heightLength, rgbArray, 0,
							widthLength);
					ImageIO.write(imgBlock, "png", byteArrayOut);
					MD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));					

					// format the rowkey
					rowkey.replace(rowkeyPrefixLen, rowkey.length(), "/"
							+ rowName.toString());
					path.set(rowkey.toString());
					filename.replace(0, 9, colName.toString());
					fileInfo.setFilename(filename.toString());
					fileInfo.setMD5(MD5.toString());
					fileInfo.setLength(byteArrayOut.size());
					rowkey.append("/" + filename);
					context.write(path, fileInfo);
					Put put = new Put(Bytes.toBytes(MD5.toString()));
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
							GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
							GtDataConfig.RESOURCE.DATA.byteVal,
							byteArrayOut.toByteArray());
					resourceTable.put(put);
					heightRead += heightLength;
					byteArrayOut.reset();
					MD5.delete(0, MD5.length());

				}
				widthRead += widthLength;
			}

		}

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			super.cleanup(context);
			byteArrayOut.close();
			resourceTable.flushCommits();
			resourceTable.close();
			Configuration conf = context.getConfiguration();
			HTable metaTable = new HTable(conf,
					CoreConfig.MAP_META_TABLE);
			metaTable.setAutoFlush(false, true);
			//insert the Layers information into meta table
			StringBuilder rowkeyStrBuilder = new StringBuilder(conf.get(CoreConfig.CUTTING_OUTPUTPATH));
			rowkeyStrBuilder.insert(rowkeyStrBuilder.lastIndexOf("/L"), "/");
			Put put = new Put(Bytes.toBytes(rowkeyStrBuilder.toString()));
			put.add(GtDataConfig.META.FAMILY.byteVal,
					GtDataConfig.META.DFS.byteVal,
					Bytes.toBytes("0"));
			put.add(GtDataConfig.META.FAMILY.byteVal, 
					GtDataConfig.META.SIZE.byteVal,
					Bytes.toBytes("-1"));
			put.add(GtDataConfig.META.FAMILY.byteVal, 
					GtDataConfig.META.URL.byteVal,
					Bytes.toBytes(""));
			put.add(GtDataConfig.META.FAMILY.byteVal,
					GtDataConfig.META.TIME.byteVal,
					Bytes.toBytes("" + System.currentTimeMillis()));
			metaTable.put(put);
			metaTable.flushCommits();
			metaTable.close();
		}
	}

	/**
	 * 
	 * @author shaolin
	 *insert the file's record into meta table
	 */
	static class MetaTableInsertReducer extends
			TableReducer<Text, FileInfo, NullWritable> {
		private StringBuilder rowkey = new StringBuilder();
		int indexof;
		int rowkeyPrefixLen;

		public void reduce(Text key, Iterable<FileInfo> values, Context context)
				throws IOException, InterruptedException {
			rowkey.replace(0, rowkey.length(), key.toString());
			rowkeyPrefixLen = rowkey.length();
			indexof = rowkey.lastIndexOf("/");
			rowkey.insert(indexof, "/");
			Put put = new Put(Bytes.toBytes(rowkey.toString()));
			put.add(GtDataConfig.META.FAMILY.byteVal, 
					GtDataConfig.META.DFS.byteVal,
					Bytes.toBytes("0"));
			put.add(GtDataConfig.META.FAMILY.byteVal, 
					GtDataConfig.META.SIZE.byteVal,
					Bytes.toBytes("-1"));
			put.add(GtDataConfig.META.FAMILY.byteVal, 
					GtDataConfig.META.URL.byteVal,
					Bytes.toBytes(""));
			put.add(GtDataConfig.META.FAMILY.byteVal, 
					GtDataConfig.META.TIME.byteVal,
					Bytes.toBytes("" + System.currentTimeMillis()));
			context.write(NullWritable.get(), put);
			rowkey.replace(0, rowkey.length(), key.toString());
			System.out.println(rowkey.toString());
			for (FileInfo val : values) {
				rowkey.replace(rowkeyPrefixLen, rowkey.length(), "//"
						+ val.getFilename().toString());
				put = new Put(Bytes.toBytes(rowkey.toString()));
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.DFS.byteVal,
						Bytes.toBytes("0"));
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.SIZE.byteVal,
						Bytes.toBytes(String.valueOf(val.getLength())));
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.URL.byteVal,
						Bytes.toBytes(val.getMD5().toString()));
				put.add(GtDataConfig.META.FAMILY.byteVal, 
						GtDataConfig.META.TIME.byteVal,
						Bytes.toBytes("" + System.currentTimeMillis()));
				context.write(NullWritable.get(), put);
			}
		}
	}

	public int run(String[] args) throws Exception {

		int layersCount = Integer.parseInt(args[3]);
		int totalJobNum = layersCount + 1;
		String urlcode = TransCoding.UrlEncode(args[2], "utf-8");
		String jobCount = "" + totalJobNum;
		StringBuilder layerName = new StringBuilder(
				Integer.toHexString(layersCount));
		if (layerName.length() == 1) {
			layerName.insert(0, "L0");
		} else {
			layerName.insert(0, "L");
		}
		
		
		if ( args[2].endsWith("/")){
			layerName.insert(0, urlcode);
		}else{
			layerName.insert(0, urlcode + "/");
		}
		
		Configuration conf = HBaseConfiguration.create();
		conf.setStrings("mapred.child.java.opts", "-Xmx2048m");
		conf.set(CoreConfig.JOBID, args[0]);
		conf.setInt("mapreduce.map.memory.mb", 3072);
		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
		conf.set(CoreConfig.CUTTING_OUTPUTPATH, layerName.toString());
		conf.set(TableOutputFormat.OUTPUT_TABLE,
				CoreConfig.MAP_META_TABLE);
		Job job = Job.getInstance(conf);

		FileInputFormat.addInputPath(job, new Path(args[1]));
		job.setMapperClass(ImageSegMapper.class);
		job.setReducerClass(MetaTableInsertReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FileInfo.class);
		job.setInputFormatClass(ImgBlockCuttingInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setJarByClass(ImageSegementMD5.class);
		job.setNumReduceTasks(6);
		job.setJobName("1/" + jobCount);

		int status = job.waitForCompletion(true) ? 0 : 1;
		//int status = 0;
		args[2] = jobCount;
		while (status == 0 && layersCount > 0) {

			LOG.info("imageSegementation of the " + layersCount
					+ " is sucessed");
			args[1] = layerName.toString();
			LOG.info("the layer for read: " + args[1]);
			String temp = null;
			layersCount--;
			if (layersCount < 16) {
				temp = "L0" + Integer.toHexString(layersCount);
			} else {
				temp = "L" + Integer.toHexString(layersCount);
			}
			LOG.info("imageSegementation of the " + layersCount
					+ " processing........");
			status = new MergingResamplingMD5().run(args);
			layerName.replace(layerName.length() - 3, layerName.length(), temp);
		}
		if (0 == status) {
			LOG.info("imageSegementation of the " + layersCount
					+ " is sucessed");
		}
		
		return status;
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			LOG.info("usage: <rowkey> <input_path> <gt-data ouput path(../../_alllayers)> <the total number of layers>");
		} else {
			int status = ToolRunner.run(new ImageSegementMD5(), args);
			System.exit(status);
		}
	}
}


//public class ImageSegementMD5 extends Configured implements Tool {
//
//	private static final Log LOG = LogFactory
//			.getLog(ImageSegementMD5.class);
//
//	/**
//	 * 
//	 * @author shaolin
//	 *insert the record into resource table
//	 *and output<filePaht, fileInfo> to reduce
//	 *
//	 */
//	static class ImageSegMapper extends
//			Mapper<AdfGeoTransformArray, BytesWritable, Text, FileInfo> {
//
//		private double[] adfGeoTransform;
//		private Text path = new Text();
//		private FileInfo fileInfo = new FileInfo();
//		private HTable resourceTable;
//		private StringBuilder rowkey;
//		private StringBuilder rowName;
//		private StringBuilder colName;
//		private int rowkeyPrefixLen;
//		private StringBuilder filename;
//		private StringBuilder MD5;
//		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
//
//		protected void setup(Context context) throws IOException,
//				InterruptedException {
//			super.setup(context);
//			Configuration conf = context.getConfiguration();
//			resourceTable = new HTable(conf,GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
//			resourceTable.setAutoFlush(false, true);
//			rowkey = new StringBuilder(conf.get(CoreConfig.CUTTING_OUTPUTPATH, CoreConfig.TRASH + "/" + context.getJobID().toString()));
//			rowkeyPrefixLen = rowkey.length();
//			rowName = new StringBuilder();
//			colName = new StringBuilder();
//			filename = new StringBuilder("C00000000.png");
//			MD5 = new StringBuilder();
//		}
//
//		/**
//		 * cutting the image and put the data into resource table
//		 * 
//		 * @param key
//		 * @param value
//		 * @param context
//		 * @throws IOException
//		 * @throws InterruptedException
//		 */
//		public void map(AdfGeoTransformArray key, BytesWritable value, Context context)
//				throws IOException, InterruptedException {
//			// change the data of byte to BufferedImage
//			ImageIcon imageIcon = new ImageIcon(value.getBytes());
//			int width = imageIcon.getIconWidth();
//			int height = imageIcon.getIconHeight();
//			BufferedImage img = new BufferedImage(width, height,
//					BufferedImage.TYPE_INT_ARGB_PRE);
//			Graphics2D gs = (Graphics2D) img.getGraphics();
//			gs.drawImage(imageIcon.getImage(), 0, 0,
//					imageIcon.getImageObserver());
//
//			//cutting the bufferedImage
//			adfGeoTransform = key.getAdfGeoTransform().clone();
//			int widthRead = 0;
//			int heightRead = 0;
//			int widthRang = CoreConfig.WIDTH_DEFAULT;
//			int heightRang = CoreConfig.HEIGHT_DEFAULT;
//			double xCoordinate;
//			double yCoordinate;
//			long tilerow;
//			long tilecol;
//			for (; widthRead < width;) {
//				if (width - widthRead < widthRang) {
//					widthRang = width - widthRead;
//				}
//				for (heightRead = 0; heightRead < height;) {
//					if (height - heightRead < heightRang) {
//						heightRang = height - heightRead;
//					}
//					//cacluate the upper left corner's coordinate(x,y)
//					xCoordinate = adfGeoTransform[0] + widthRead
//							* adfGeoTransform[1] + heightRead
//							* adfGeoTransform[2];
//					yCoordinate = adfGeoTransform[3] + widthRead
//							* adfGeoTransform[4] + heightRead
//							* adfGeoTransform[5];
//					
//					//change the coordiante to linenum and column number
//					tilerow = (long) Math.floor((90 - yCoordinate)
//							/ CoreConfig.HEIGHT_DEFAULT
//							/ Math.abs(adfGeoTransform[5]));
//					tilecol = (long) Math.floor((180 + xCoordinate)
//							/ CoreConfig.WIDTH_DEFAULT
//							/ adfGeoTransform[1]);
//					
//					// formatting the rowName and colName
//					//like R00000001 C00000001
//					rowName.replace(0, rowName.length(),
//							Long.toHexString(tilerow));
//					int count = 8 - rowName.length();
//					int i;
//					for (i = 0; i < count; i++) {
//						rowName.insert(0, "0");
//					}
//					rowName.insert(0, "R");
//
//					colName.replace(0, rowName.length(),
//							Long.toHexString(tilecol));
//					count = 8 - colName.length();
//					for (i = 0; i < count; i++) {
//						colName.insert(0, "0");
//					}
//					colName.insert(0, "C");
//					
//					//cut out a block with 256*256
//					int[] rgbArray = new int[widthRang * heightRang];
//					rgbArray = img.getRGB(widthRead, heightRead, widthRang,
//							heightRang, rgbArray, 0, widthRang);
//					BufferedImage imgBlock = new BufferedImage(256, 256,
//							BufferedImage.TYPE_INT_ARGB_PRE);
//					imgBlock.setRGB(0, 0, widthRang, heightRang, rgbArray, 0,
//							widthRang);
//					ImageIO.write(imgBlock, "png", byteArrayOut);
//					MD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));					
//
//					// format the rowkey
//					rowkey.replace(rowkeyPrefixLen, rowkey.length(), "/"
//							+ rowName.toString());
//					path.set(rowkey.toString());
//					filename.replace(0, 9, colName.toString());
//					fileInfo.setFilename(filename.toString());
//					fileInfo.setMD5(MD5.toString());
//					fileInfo.setLength(byteArrayOut.size());
//					rowkey.append("/" + filename);
//					context.write(path, fileInfo);
//					Put put = new Put(Bytes.toBytes(MD5.toString()));
//					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//							GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
//					put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
//							GtDataConfig.RESOURCE.DATA.byteVal,
//							byteArrayOut.toByteArray());
//					resourceTable.put(put);
//					heightRead += heightRang;
//					byteArrayOut.reset();
//					MD5.delete(0, MD5.length());
//
//				}
//				heightRang = 256;
//				widthRead += widthRang;
//			}
//
//		}
//
//		protected void cleanup(Context context) throws IOException,
//				InterruptedException {
//			super.cleanup(context);
//			byteArrayOut.close();
//			resourceTable.flushCommits();
//			resourceTable.close();
//			Configuration conf = context.getConfiguration();
//			HTable metaTable = new HTable(conf,
//					GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
//			metaTable.setAutoFlush(false, true);
//			//insert the Layers information into meta table
//			StringBuilder rowkeyStrBuilder = new StringBuilder(conf.get(CoreConfig.CUTTING_OUTPUTPATH));
//			rowkeyStrBuilder.insert(rowkeyStrBuilder.lastIndexOf("/L"), "/");
//			Put put = new Put(Bytes.toBytes(rowkeyStrBuilder.toString()));
//			put.add(GtDataConfig.META.FAMILY.byteVal,
//					GtDataConfig.META.DFS.byteVal,
//					Bytes.toBytes("0"));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.SIZE.byteVal,
//					Bytes.toBytes("-1"));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.URL.byteVal,
//					Bytes.toBytes(""));
//			put.add(GtDataConfig.META.FAMILY.byteVal,
//					GtDataConfig.META.TIME.byteVal,
//					Bytes.toBytes("" + System.currentTimeMillis()));
//			metaTable.put(put);
//			metaTable.flushCommits();
//			metaTable.close();
//		}
//	}
//
//	/**
//	 * 
//	 * @author shaolin
//	 *insert the file's record into meta table
//	 */
//	static class MetaTableInsertReducer extends
//			TableReducer<Text, FileInfo, NullWritable> {
//		private StringBuilder rowkey = new StringBuilder();
//		int indexof;
//		int rowkeyPrefixLen;
//
//		public void reduce(Text key, Iterable<FileInfo> values, Context context)
//				throws IOException, InterruptedException {
//			rowkey.replace(0, rowkey.length(), key.toString());
//			rowkeyPrefixLen = rowkey.length();
//			indexof = rowkey.lastIndexOf("/");
//			rowkey.insert(indexof, "/");
//			Put put = new Put(Bytes.toBytes(rowkey.toString()));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.DFS.byteVal,
//					Bytes.toBytes("0"));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.SIZE.byteVal,
//					Bytes.toBytes("-1"));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.URL.byteVal,
//					Bytes.toBytes(""));
//			put.add(GtDataConfig.META.FAMILY.byteVal, 
//					GtDataConfig.META.TIME.byteVal,
//					Bytes.toBytes("" + System.currentTimeMillis()));
//			context.write(NullWritable.get(), put);
//			rowkey.replace(0, rowkey.length(), key.toString());
//			System.out.println(rowkey.toString());
//			for (FileInfo val : values) {
//				rowkey.replace(rowkeyPrefixLen, rowkey.length(), "//"
//						+ val.getFilename().toString());
//				put = new Put(Bytes.toBytes(rowkey.toString()));
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.DFS.byteVal,
//						Bytes.toBytes("0"));
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.SIZE.byteVal,
//						Bytes.toBytes(String.valueOf(val.getLength())));
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.URL.byteVal,
//						Bytes.toBytes(val.getMD5().toString()));
//				put.add(GtDataConfig.META.FAMILY.byteVal, 
//						GtDataConfig.META.TIME.byteVal,
//						Bytes.toBytes("" + System.currentTimeMillis()));
//				context.write(NullWritable.get(), put);
//			}
//		}
//	}
//
//	public int run(String[] args) throws Exception {
//
//		int layersCount = Integer.parseInt(args[3]);
//		int totalJobNum = layersCount + 1;
//		String urlcode = TransCoding.UrlEncode(args[2], "utf-8");
//		String jobCount = "" + totalJobNum;
//		StringBuilder layerName = new StringBuilder(
//				Integer.toHexString(layersCount));
//		if (layerName.length() == 1) {
//			layerName.insert(0, "L0");
//		} else {
//			layerName.insert(0, "L");
//		}
//		
//		
//		if ( args[2].endsWith("/")){
//			layerName.insert(0, urlcode);
//		}else{
//			layerName.insert(0, urlcode + "/");
//		}
//		
//		Configuration conf = HBaseConfiguration.create();
//		conf.setStrings("mapred.child.java.opts", "-Xmx2048m");
//		conf.set(CoreConfig.JOBID, args[0]);
//		conf.setInt("mapreduce.map.memory.mb", 3072);
//		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
//		conf.set(CoreConfig.CUTTING_OUTPUTPATH, layerName.toString());
//		conf.set(TableOutputFormat.OUTPUT_TABLE,
//				GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
//		Job job = Job.getInstance(conf);
//
//		FileInputFormat.addInputPath(job, new Path(args[1]));
//		job.setMapperClass(ImageSegMapper.class);
//		job.setReducerClass(MetaTableInsertReducer.class);
//		job.setMapOutputKeyClass(Text.class);
//		job.setMapOutputValueClass(FileInfo.class);
//		job.setInputFormatClass(ImgBlockCuttingInputFormat.class);
//		job.setOutputFormatClass(TableOutputFormat.class);
//		job.setJarByClass(ImageSegementMD5.class);
//		job.setNumReduceTasks(6);
//		job.setJobName("1/" + jobCount);
//
//		int status = job.waitForCompletion(true) ? 0 : 1;
////		args[2] = jobCount;
////		while (status == 0 && layersCount > 0) {
////
////			LOG.info("imageSegementation of the " + layersCount
////					+ " is sucessed");
////			args[1] = layerName.toString();
////			LOG.info("the layer for read: " + args[1]);
////			String temp = null;
////			layersCount--;
////			if (layersCount < 16) {
////				temp = "L0" + Integer.toHexString(layersCount);
////			} else {
////				temp = "L" + Integer.toHexString(layersCount);
////			}
////			LOG.info("imageSegementation of the " + layersCount
////					+ " processing........");
////			status = new MergingResamplingMD5().run(args);
////			layerName.replace(layerName.length() - 3, layerName.length(), temp);
////		}
////		if (0 == status) {
////			LOG.info("imageSegementation of the " + layersCount
////					+ " is sucessed");
////		}
//		
//		return status;
//	}
//
//	public static void main(String[] args) throws Exception {
//		if (args.length < 4) {
//			LOG.info("usage: <rowkey> <input_path> <gt-data ouput path(../../_alllayers)> <the total number of layers>");
//		} else {
//			int status = ToolRunner.run(new ImageSegementMD5(), args);
//			int layersCount = Integer.parseInt(args[3]);
//			int totalJobNum = layersCount + 1;
//			String jobCount = "" + totalJobNum;
//			String urlcode = TransCoding.UrlEncode(args[2], "utf-8");
//			StringBuilder layerName = new StringBuilder(
//					Integer.toHexString(layersCount));
//			if (layerName.length() == 1) {
//				layerName.insert(0, "L0");
//			} else {
//				layerName.insert(0, "L");
//			}
//			
//			
//			if ( args[2].endsWith("/")){
//				layerName.insert(0, urlcode);
//			}else{
//				layerName.insert(0, urlcode + "/");
//			}
//			args[2] = jobCount;
//			while (status == 0 && layersCount > 0) {
//	
//				LOG.info("imageSegementation of the " + layersCount
//						+ " is sucessed");
//				args[1] = layerName.toString();
//				LOG.info("the layer for read: " + args[1]);
//				String temp = null;
//				layersCount--;
//				if (layersCount < 16) {
//					temp = "L0" + Integer.toHexString(layersCount);
//				} else {
//					temp = "L" + Integer.toHexString(layersCount);
//				}
//				LOG.info("imageSegementation of the " + layersCount
//						+ " processing........");
//				status = new MergingResamplingMD5().run(args);
//				layerName.replace(layerName.length() - 3, layerName.length(), temp);
//			}
//			if (0 == status) {
//				LOG.info("imageSegementation of the " + layersCount
//						+ " is sucessed");
//			}
//			System.exit(status);
//		}
//	}
//}
