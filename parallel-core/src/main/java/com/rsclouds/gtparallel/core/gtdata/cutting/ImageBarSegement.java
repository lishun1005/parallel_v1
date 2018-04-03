package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
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
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.io.ImageBarInfo;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImageBarBlockInputFormat;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

public class ImageBarSegement extends Configured implements Tool{
	private static final Log LOG = LogFactory.getLog(ImageBarSegement.class);
	
	
	/**
	 * 
	 * @author shaolin
	 *insert the record into resource table
	 *and output<filePaht, fileInfo> to reduce
	 *
	 */
	static class ImageSegMapper extends Mapper<ImageBarInfo, NullWritable, Text, FileInfo> {
		private Text path = new Text();
		private FileInfo fileInfo = new FileInfo();
		private HTable resourceTable;
		private StringBuilder strBuilderMD5 = new StringBuilder();
		private String outputpath;	//瓦片输出路径
				
		//瓦片的默认长宽
		private int widthRang;
		private int heightRang;

		//瓦片行列号
		private StringBuilder colStrBuilder = new StringBuilder();
		private StringBuilder rowStrBuilder = new StringBuilder();
		
		
		
		private Dataset dataset = null;
		private byte[] nodataValues; 
		private boolean nodataFlag; //获取影像数据的nodata值是否成功， false->失败，  true->成功
		private int bandCount;
		private int colorInterp; //影像颜色表类型
		private byte[] buffer;   //存放影像二进制数据
		private int[] rgb;       //存放影像数据的rgb数据
		private int[] bands;     //需要读取的波段数组
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();

		private BufferedImage bufferImag;
		private int[] intZeroAreay;
		
		
		
		public void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			resourceTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
			resourceTable.setAutoFlushTo(false);
			outputpath = conf.get(CoreConfig.CUTTING_OUTPUTPATH);
			if (!outputpath.endsWith("/")) {
				outputpath += "/";
			}
			widthRang = CoreConfig.WIDTH_DEFAULT;
			heightRang = CoreConfig.HEIGHT_DEFAULT;
			rgb = new int[widthRang*heightRang];
			intZeroAreay = new int[widthRang*heightRang];
			bufferImag = new BufferedImage(widthRang, heightRang, BufferedImage.TYPE_INT_ARGB_PRE);
			int mutil = CoreConfig.IMGBLOCK_WIDTH;
			buffer = new byte[heightRang*widthRang*mutil*3];		
		}
		
		/**
		 * @breif 读取影像数据（bands全局变量     需要读取的波段）
		 * @param xOrigin    数据读取范围的左上角的x坐标（像素为单位）
		 * @param yOrigin    数据读取范围的左上角的y坐标（像素为单位）
		 * @param xsize      X方向读取的长度（像素为单位）
		 * @param ysize      Y方向读取的长度（像素为单位）
		 * @param buf_xsize  将X方向的读取的长度xsize重采样成新的长度buf_xsize
		 * @param buf_ysize  将Y方向的读取的长度ysize重采样成新的长度buf_ysize
		 * @param buffer     存放读取的数据
		 * @return
		 */
		public boolean readData(int xOrigin, int yOrigin, int xsize, int ysize, int buf_xsize, int buf_ysize, byte[] buffer) {
			if (colorInterp == 2 && bandCount == 1) {
				int count = buf_xsize*buf_ysize;
				if (dataset.ReadRaster(xOrigin, yOrigin, xsize, ysize, buf_xsize, buf_ysize, gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
					Band band = dataset.GetRasterBand(1);
					for (int i = 0; i < count; i ++ ) {
						rgb[i] =  band.GetColorTable().GetColorEntry((buffer[i] & 0xff)).getRGB();
					}
					return true;
				}
				else {
					return false;
				}
				
			}
			else {				
				if (dataset.ReadRaster(xOrigin, yOrigin, xsize, ysize, buf_xsize, buf_ysize, gdalconst.GDT_Byte, buffer, bands, 0) == gdalconst.CE_None){
					return true;
				}
				else {
					return false;
				}
			}
		}
		

		public void changRGBbyMask(byte[] buffer, byte[] mask, int[] rgb, int buf_xsize, int buf_ysize) {
			if (colorInterp == 2 && bandCount == 1) {
				return ;
			}		
			int count = buf_xsize * buf_ysize;
			for(int i = 0; i < count; i ++) {
				if(mask[i] == 0 ) {
					rgb[i]= (0x00000000);
				} else {
					rgb[i] = (0xff000000);
					for(int j = 0; j < bandCount; j ++) {
						rgb[i] += ((buffer[i + count * j] & 0x000000ff) << 8 * (bandCount - j -1));
					}
				}
			}
		}
		
		
		/**
		 * @breif 将按波段读取的二进制数据 转为 rgb格式， 每次只转一部分。比如buffer存储的是256 * 5120范围的数据，那么第一次将一个256*256范围的对应的buffer转为rgb
		 * 第二次将第二个256*256的对应的buffer转为rgb，依次类推，直至将整个buffer转为256*256的rgb
		 * @param buffer	 
		 * @param rgb        存储 宽为rgb_xsize， 长为rgb_ysize的影像数据
		 * @param rgb_xsize  
		 * @param rgb_ysize
		 * @param xreaded    buffer所存储的影像数据在X方向上已读的长度
		 * @param xLength    buffer所存储的影像数据在X方向上的长度
		 */
		public void changRGB(byte[]buffer, int[] rgb, int rgb_xsize, int rgb_ysize, int xreaded, int xLength) {
			if (colorInterp == 2 && bandCount == 1) {
				return ;
			}		
			int band_step = rgb_ysize * xLength;
			int rgb_step = 0;
			int buffer_step = 0;
			for (int i = 0; i < rgb_ysize; i ++) {
				buffer_step = i * xLength + xreaded;
				for (int j = 0; j < rgb_xsize; j ++) {
					rgb[rgb_step] = (0x00000000);//像素设置为透明
					//如果成功获取影像数据的nodata值，并且该像素对应的所有波段的值都为nodata，则需要将该像素置为透明
					if(nodataFlag) {
						for(int band = 0; band < bandCount; band ++) {
							if(buffer[buffer_step + band_step * band] != nodataValues[bands[band]-1]) {
								rgb[rgb_step]= (0xff000000);
								break;
							}
						}
					}else {
						rgb[rgb_step]= (0xff000000);//像素设置为不透明
					}
					for(int band = 0; band < bandCount; band ++) {
						rgb[rgb_step] += ((buffer[buffer_step + band_step * band] & 0x000000ff) << 8 * (bandCount - band -1));
					}
					rgb_step ++;
					buffer_step ++;
				}
			}
		}
		
		public void merge(int[] merge, int[] rgb) {
			int length = merge.length;
			for(int i = 0; i < length; i ++) {
				rgb[i] += merge[i];
			}
		}
		
		/**
		 * @bref 将列号号转换为十六进制，并格式化为字符串“R00000000。png”， 即R后面跟八位数十六进制，不足补零，然后再加“。png”后缀。
		 * @param colNum
		 */
		public void setCol(long colNum) {
			colStrBuilder.replace(0, colStrBuilder.length(), Long.toHexString(colNum));
			int count = 8 - colStrBuilder.length();
			for (int i = 0; i < count; i++) {
				colStrBuilder.insert(0, "0");
			}
			colStrBuilder.insert(0, "C");
			colStrBuilder.append(".png");
		}
		
		/**
		 * @bref 将行号转换为十六进制，并格式化为字符串“R00000000”， 即R后面跟八位数十六进制，不足补零
		 * @param rowNum
		 */
		public void setRow(long rowNum) {
			rowStrBuilder.replace(0, rowStrBuilder.length(), Long.toHexString(rowNum));
			int count = 8 - rowStrBuilder.length();
			for (int i = 0; i < count; i++) {
				rowStrBuilder.insert(0, "0");
			}
			rowStrBuilder.insert(0, "R");
		}
		
	
		public void map(ImageBarInfo key, NullWritable value, Context context) throws
			IOException, InterruptedException {
			if(dataset == null) {
				gdal.AllRegister();
				dataset = gdal.Open(key.getFilepath().toString(), gdalconstConstants.GA_ReadOnly);				
				//dataset = gdal.Open("D://nanlin//t1_byte.tif", gdalconstConstants.GA_ReadOnly);
				//dataset = gdal.Open("F://hunan//hunan_xiangqian_clip.tif", gdalconstConstants.GA_ReadOnly);
				int bands = dataset.getRasterCount();
				nodataValues = new byte[bands];
				Double[] nodata = new Double[1];
				nodataFlag = true;
				for(int i = 1; i <= bands; i ++) {
					dataset.GetRasterBand(i).GetNoDataValue(nodata);
					if(nodata[0] == null)
						nodataFlag = false;
					else if(nodata[0].doubleValue() > 256) {
						nodataFlag = false;
					} else {
						nodataValues[i-1] = (byte)(nodata[0].doubleValue());
					}
				}
			}
			
			int xOrigin = key.getXreadOrigin();//重采样后的x坐标
			int yOrigin = key.getYreadOrigin();//重采样后的y坐标
			int xLength = key.getXreadLen();//重采样后的宽度
			int yLength = key.getYreadLen();//重采样后的高度
			int tileColOffset = key.getTileColOffset();
			int tileRowOffset = key.getTileRowOffset();
			double dstResolution = key.getDstResolution();//重采样后的分辨率
			double oriResoulution = key.getOriResolution();
			long colNum = key.getColNum();
			long rowNum = key.getRowNum();
			setRow(rowNum);
			
			bands = key.getBands();
			if (dataset != null) {
				int nbands = dataset.getRasterCount();
				bandCount = bands.length;
				if (nbands == 1) {
					bandCount = 1;
					bands = new int[1];
					bands[0] = 1;
				}
				colorInterp = dataset.GetRasterBand(1).GetColorInterpretation();
				
				//实际需要读取影像的真实起始点坐标（以像素为单位）
				int xReadCoordinateActual = (int)(xOrigin * dstResolution / oriResoulution);
				int yReadCoordinateActual = (int)(yOrigin * dstResolution / oriResoulution);
				
				//实际需要读取的瓦片数据范围
				int xreadActual = (int)(xLength * (dstResolution) / oriResoulution);
				int yreadActual = (int)(yLength * (dstResolution) / oriResoulution);
				//重采样后需要读取的瓦片数据范围
				int xread;
//				int yread = heightRang - tileRowOffset;
				//重采样后已经读取的影像范围
				int xreaded = 0;
				//写入数据的起始点
				int xWriteOrigin;
				int yWriteOrigin = tileRowOffset;
				//heightRang--;

				int count = 0;				
				if (readData(xReadCoordinateActual, yReadCoordinateActual, xreadActual, yreadActual, xLength, yLength, buffer)) {
					for (xreaded = 0; xreaded < xLength; ) {
						if (xLength - xreaded < widthRang) {
							xread = xLength - xreaded;
						}else if (xreaded == 0) {
							xread = widthRang - tileColOffset;
						}else {
							xread = widthRang;
						}
						if (xreaded == 0) {
							xWriteOrigin = tileColOffset;
						}else {
							xWriteOrigin = 0;
						}
							
						//初始bufferImage
						bufferImag.setRGB(0, 0, widthRang, heightRang, intZeroAreay, 0, widthRang);
						byteArrayOut.reset();
									
						changRGB(buffer, rgb, xread, yLength, xreaded, xLength);
						setCol(colNum);
						colNum++;
						
						bufferImag.setRGB(xWriteOrigin, yWriteOrigin, xread, yLength, rgb, 0, xread);
						ImageIO.write(bufferImag, "png", byteArrayOut);
							
						path.set(outputpath + rowStrBuilder.toString());
						strBuilderMD5.append(MD5Calculate.fileByteMD5(byteArrayOut.toByteArray()));
						fileInfo.setMD5(strBuilderMD5.toString());
						fileInfo.setLength(byteArrayOut.size());
						fileInfo.setFilename(colStrBuilder.toString());
//						Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
//						Result result = resourceTable.get(get);
//						if (result == null || result.isEmpty()) {
							Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
							put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
									GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
							put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
									GtDataConfig.RESOURCE.DATA.byteVal, byteArrayOut.toByteArray());
							resourceTable.put(put);
//						}
						System.out.println(++count);
						context.write(path, fileInfo);
						strBuilderMD5.delete(0, strBuilderMD5.length());
						xreaded += xread;
					}
																	
				} else{
					System.out.println("read data error. xorigin= " + xOrigin + ", yorigin= " + yOrigin + ", xLength= " + xLength + ", yLenght= " + yLength);
					if(dataset != null) {
						dataset.delete();
						dataset = null;
					}
					return;
				}
			}
		}
		
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			super.cleanup(context);
			resourceTable.flushCommits();
			resourceTable.close();
			if(dataset != null) {
				dataset.delete();
			}
			Configuration conf = context.getConfiguration();
			HTable metaTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
			metaTable.setAutoFlush(false, true);
			//insert the Layers information into meta table
			StringBuilder rowkeyStrBuilder = new StringBuilder(conf.get(CoreConfig.CUTTING_OUTPUTPATH));
			rowkeyStrBuilder.insert(rowkeyStrBuilder.lastIndexOf("/L"), "/");
			byte[] keys = Bytes.toBytes(TransCoding.decode(rowkeyStrBuilder.toString(), "UTF-8"));
			Get get = new Get(keys);
			Result result = metaTable.get(get);
			if(result.isEmpty()) {
			Put put = new Put(Bytes.toBytes(TransCoding.decode(rowkeyStrBuilder.toString(), "UTF-8")));
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
			}
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
			//插入行号目录
			Put put = new Put(Bytes.toBytes(TransCoding.decode(rowkey.toString(), "UTF-8")));
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
			//插入列号文件名
			for (FileInfo val : values) {
				rowkey.replace(rowkeyPrefixLen, rowkey.length(), "//"
						+ val.getFilename().toString());
				put = new Put(Bytes.toBytes(TransCoding.decode(rowkey.toString(), "UTF-8")));
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
		gdal.AllRegister();
		Configuration conf = HBaseConfiguration.create();
		int layersCount = Integer.parseInt(args[3]);
		double dstResolution;
		Dataset dataset = gdal.Open(args[1], gdalconstConstants.GA_ReadOnly);
		//Dataset dataset = gdal.Open("D://nanlin//guangzhou_051401.tiff", gdalconstConstants.GA_ReadOnly);
		//Dataset dataset = gdal.Open("D://nanlin//t1_byte1.tif", gdalconstConstants.GA_ReadOnly);
		if (dataset == null) {
			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
			LOG.info(gdal.GetLastErrorMsg());
			return 1;
		}
		if (dataset.GetProjectionRef().equalsIgnoreCase(CoreConfig.WGS84P_ROJECT)) {
			if (layersCount >= CoreConfig.LAYERS_RESOLUTION.length) {
				int length = CoreConfig.LAYERS_RESOLUTION.length - 1;
				int count = layersCount - length;
				dstResolution = CoreConfig.LAYERS_RESOLUTION[length];
				for(int i = 0; i < count; i ++) {
					dstResolution = dstResolution / 2;
				}
			}else {
				dstResolution = CoreConfig.LAYERS_RESOLUTION[layersCount];
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_WGS84);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_WGS84);
		}else if(dataset.GetProjectionRef().equalsIgnoreCase(CoreConfig.MERCATOR_PROJECT)){
			if (layersCount >= CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
				int length = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length - 1;
				int count = layersCount - length;
				dstResolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[length];
				for(int i = 0; i < count; i ++) {
					dstResolution = dstResolution / 2;
				}
			}else {
				dstResolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layersCount];
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_MERCATOR);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_MERCATOR);
		}else {
			return 1;
		}
		dataset.delete();
		conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, dstResolution);
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
		
		
		conf.set(CoreConfig.JOBID, args[0]);
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
		job.setInputFormatClass(ImageBarBlockInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setJarByClass(ImageBarSegement.class);
		job.setNumReduceTasks(5);
		job.setJobName("1/" + jobCount);

		int status = job.waitForCompletion(true) ? 0 : 1;
		//int status = 0;
		//args[2] = jobCount;
//		while (status == 0 && layersCount > 0) {
//
//			LOG.info("imageSegementation of the " + layersCount
//					+ " is sucessed");
//			layersCount --;
//			args[3] = "" + layersCount;
//			status = this.run(args);
//			args[1] = layerName.toString();
//			LOG.info("the layer for read: " + args[1]);
//			String temp = null;
//			
//			
//			LOG.info("imageSegementation of the " + layersCount
//					+ " processing........");
//			if (layersCount == 1) {
//				layersCount--;
//				status = new MergingResamplingMD5().run(args);
//			}else {
//				layersCount -= 2;
//				status = new MergeingResamplingMutil().run(args);
//			}
//			if (layersCount < 16) {
//				temp = "L0" + Integer.toHexString(layersCount);
//			} else {
//				temp = "L" + Integer.toHexString(layersCount);
//			}
//			layerName.replace(layerName.length() - 3, layerName.length(), temp);
//		}
//		if (0 == status) {
//			LOG.info("imageSegementation of the " + layersCount
//					+ " is sucessed");
//		}
		
		return status;
	}

	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0) {
			args = new String[4];
			args[0] = "123456";
			args[1] = "hdfs://192.168.2.3:8020/nanlin/tiff/t1_byte.tif";
			args[2] = "/2015052404/Layers/_alllayers";
			args[3] = "19";
		}
		if (args.length < 4) {
			LOG.info("usage: <rowkey> <input_path> <gt-data ouput path(../../_alllayers)> <the total number of layers>");
		} else {
			int status = ToolRunner.run(new ImageBarSegement(), args);
			int layersCount = Integer.parseInt(args[3]);
			while( status == 0 && layersCount > 0) {
				layersCount --;
				args[3] = "" + layersCount;
				status = ToolRunner.run(new ImageBarSegement(), args);
			}
			System.exit(status);
		}
	}

}
