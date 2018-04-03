package com.rsclouds.gtparallel.core.gtdata.cutting;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.FileOperate;
import com.rsclouds.gtparallel.core.gtdata.common.GdalDatasetBase;
import com.rsclouds.gtparallel.core.gtdata.common.GeometryBase;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.io.ImageMutilLayersInfo;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.ImageMutilLayersInputFormat;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
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
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.Geometry;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImageSegementBase extends Configured implements Tool {
	private static final Log LOG = LogFactory.getLog(ImageSegementBase.class);
	public static final String SAVE_STORAGE_BOOLEAN = "save_storage";
	public static final String KEY_FILE_FORMAT_STRING = "file_suffix";
	public static final String KEY_PRESSTEXT_STRING = "pressText";
	public static final String KEY_PRESSTEXTSIZE_INT = "pressTextSize";
	public static final String KEY_WATERMARKSCHEME_INT = "watermarkScheme";


	static class ImageSegMapper extends Mapper<ImageMutilLayersInfo, NullWritable, Text, FileInfo> {
		private Text path = new Text();
		private FileInfo fileInfo = new FileInfo();
		private HTable resourceTable;
		private HTable metadataTable;
		private StringBuilder strBuilderMD5 = new StringBuilder();

		private String outputpath = null;    //瓦片输出路径
		private int pictureFormatInt;
		private boolean wartermarkFlag = true;
		private boolean saveStorageBool;
		private int nodataInt;

		//瓦片的默认长宽
		private int widthRang;
		private int heightRang;
		private int width;
		private int height;

		//瓦片行列号
		private StringBuilder colStrBuilder = new StringBuilder();
		private StringBuilder rowStrBuilder = new StringBuilder();

		private double[] adfGeoTransform = null;
		private GdalDatasetBase gdalDatasetBase = null;
		private String shpFilePath = null;
		private String localShpFilePath = null;
		private GeometryBase geometryBase = null;
		private GeometryBase geoBase = null;
		private boolean bOutput = false;
		private boolean bContains = false;
		private boolean bIntersection = false;
		private double x_coor;
		private double y_coor;
		private double KEY_TILEORIGIN_X;
		private double KEY_TILEORIGIN_Y;
		private String fileSuffixStr;

		private int[] intZeroAreay;
		private BufferedImage bufferImag;
		int[] rgbArrayTemp;
		private boolean bCover = true;

		private double oriResoulutionX; //原始影像东西方向分辨率(横向 = adfGeoTransform[1])
		private double oriResoulutionY; //原始影像南北方向分辨率 (纵向 = -adfGeoTransform[1])

		String pressTextStr = "ChinaRS中科遥感";//水印字体内容
		int pressTextSize = 13;                 //水印字体大小
		int waterMarkScheme = 0;             //水印方案

		/**
		 * 初始化参数
		 */
		public void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
			saveStorageBool = conf.getBoolean(SAVE_STORAGE_BOOLEAN, false);
			nodataInt = conf.getInt("NO_DATA", -1);
			shpFilePath = conf.get("shpFilePath", null);
//			shpFilePath = null;
			bCover = conf.getBoolean("bcover", true);
			fileSuffixStr = conf.get(KEY_FILE_FORMAT_STRING, ".png");
			pressTextStr = conf.get(KEY_PRESSTEXT_STRING, "ChinaRS中科遥感");
			pressTextSize = conf.getInt(KEY_PRESSTEXTSIZE_INT, 13);
			waterMarkScheme = conf.getInt(KEY_WATERMARKSCHEME_INT, 0);

			KEY_TILEORIGIN_X = conf.getDouble(CoreConfig.KEY_TILEORIGIN_X, 180);
			KEY_TILEORIGIN_Y = conf.getDouble(CoreConfig.KEY_TILEORIGIN_Y, 90);

			resourceTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
			metadataTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
			resourceTable.setAutoFlushTo(false);
			widthRang = CoreConfig.WIDTH_DEFAULT;
			heightRang = CoreConfig.HEIGHT_DEFAULT;
			if (shpFilePath != null) {
				int indexof = shpFilePath.lastIndexOf('/');
//				localShpFilePath = "F://home//yarn//cutting_temp//" + context.getTaskAttemptID() + "//" + shpFilePath.substring(indexof+1);
//				File file = new File("F://home//yarn//cutting_temp//");
				localShpFilePath = "/home/yarn/cutting_temp/" + context.getTaskAttemptID() + "/" + shpFilePath.substring(indexof + 1);
				System.out.println(localShpFilePath);
				File file = new File("/home/yarn/cutting_temp/");
				if (!file.exists()) {
					file.mkdirs();
				}
				FileSystem fs = FileSystem.get(conf);
				Path inputPath = new Path(shpFilePath);
//				fs.copyToLocalFile(inputPath.getParent(), new Path("F://home//yarn//cutting_temp//" + context.getTaskAttemptID()));
				fs.copyToLocalFile(inputPath.getParent(), new Path("/home/yarn/cutting_temp/" + context.getTaskAttemptID()));
				geoBase = new GeometryBase(localShpFilePath);
			}
			geometryBase = new GeometryBase();

			intZeroAreay = new int[widthRang * heightRang];
			rgbArrayTemp = new int[widthRang * heightRang];
			bufferImag = new BufferedImage(widthRang, heightRang, BufferedImage.TYPE_INT_ARGB_PRE);
		}


		/**
		 * @param colNum
		 * @bref 将列号号转换为十六进制，并格式化为字符串“R00000000。png”， 即R后面跟八位数十六进制，不足补零，然后再加“。png”后缀。
		 */
		public void setCol(long colNum) {
			colStrBuilder.replace(0, colStrBuilder.length(), Long.toHexString(colNum));
			int count = 8 - colStrBuilder.length();
			for (int i = 0; i < count; i++) {
				colStrBuilder.insert(0, "0");
			}
			colStrBuilder.insert(0, "C");
		}


		/**
		 * @param rowNum
		 * @bref 将行号转换为十六进制，并格式化为字符串“R00000000”， 即R后面跟八位数十六进制，不足补零
		 */
		public void setRow(long rowNum) {
			rowStrBuilder.replace(0, rowStrBuilder.length(), Long.toHexString(rowNum));
			int count = 8 - rowStrBuilder.length();
			for (int i = 0; i < count; i++) {
				rowStrBuilder.insert(0, "0");
			}
			rowStrBuilder.insert(0, "R");
		}

		public boolean isSingularRowColSum(long row, long col) {
			if ((row + col) % 2 == 0) {
				return false;
			} else {
				return true;
			}
		}

		public boolean output(Context context, String outputpath, long row, long col) throws IOException, InterruptedException {
			path.set(outputpath + rowStrBuilder.toString());
			if (!bCover) {
				Get getTemp = new Get(Bytes.toBytes(TransCoding.decode(path.toString() + "//" + colStrBuilder.toString() + fileSuffixStr, "UTF-8")));
				Result resultTemp = metadataTable.get(getTemp);
				if (!resultTemp.isEmpty()) {
					System.out.println("has exit ");
					byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
					getTemp = new Get(md5Byte);
					resultTemp = resourceTable.get(getTemp);
					ImageIcon imageIcon = new ImageIcon(resultTemp.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
					bufferImag.setRGB(0, 0, widthRang, heightRang, intZeroAreay, 0, widthRang);
					Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
					gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
					gs.dispose();

					bufferImag.getRGB(0, 0, widthRang, heightRang, rgbArrayTemp, 0, widthRang);
					if (gdalDatasetBase.merge(rgbArrayTemp, widthRang, heightRang)) {
						pictureFormatInt = GdalDatasetBase.PNG_FORMAT_INT;
					} else {
						pictureFormatInt = GdalDatasetBase.JPEG_FORMAT_INT;
					}
					gdalDatasetBase.setBufferedImage(0, 0, widthRang, heightRang, pictureFormatInt);
				}
			}
			if (wartermarkFlag) {
				if (waterMarkScheme == 0) {//全部瓦片添加水印
					gdalDatasetBase.setWarterMask(pressTextStr, "宋体", Font.BOLD | Font.ITALIC, pressTextSize, Color.white, -1, -1, 0.4f, pictureFormatInt);
				} else if (waterMarkScheme == 1) {//只有行列号的和为奇数才添加水印
					if (isSingularRowColSum(row, col)) {
						gdalDatasetBase.setWarterMask(pressTextStr, "宋体", Font.BOLD | Font.ITALIC, pressTextSize, Color.white, -1, -1, 0.4f, pictureFormatInt);
					}
				} else if (waterMarkScheme == 2) {//只有行列号的和为偶数才添加水印
					if (!isSingularRowColSum(row, col)) {
						gdalDatasetBase.setWarterMask(pressTextStr, "宋体", Font.BOLD | Font.ITALIC, pressTextSize, Color.white, -1, -1, 0.4f, pictureFormatInt);
					}
				}
			}

//			gdalDatasetBase.write2LocalFile("D://nanlin//test//" + rowStrBuilder.toString() + "_" + colStrBuilder.toString()+".png", pictureFormatInt);
			strBuilderMD5.append(MD5Calculate.fileByteMD5(gdalDatasetBase.getBufferImagedata(pictureFormatInt)));
			fileInfo.setMD5(strBuilderMD5.toString());
			fileInfo.setLength(gdalDatasetBase.getBufferImageSize());
			fileInfo.setFilename(colStrBuilder.toString() + fileSuffixStr);
			Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
			Result result = resourceTable.get(get);
			if (result == null || result.isEmpty()) {
				Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
				put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
				put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
						GtDataConfig.RESOURCE.DATA.byteVal, gdalDatasetBase.getBufferImagedata(pictureFormatInt));
				resourceTable.put(put);
			}
			context.write(path, fileInfo);
//			System.out.println("outWrite path=" + path.toString());
			strBuilderMD5.delete(0, strBuilderMD5.length());
			return true;
		}


		public void map(ImageMutilLayersInfo key, NullWritable value, Context context) throws
				IOException, InterruptedException {
			System.out.println("map key: " + key.getGtdataOutputPath().toString());
			if (gdalDatasetBase == null) {
				int mutil = CoreConfig.IMGBLOCK_WIDTH;
				int[] bands = key.getBands();
				gdalDatasetBase = new GdalDatasetBase(key.getFilepath().toString(), 0.001, widthRang, heightRang, mutil, bands, nodataInt);
				width = gdalDatasetBase.getWidth();
				height = gdalDatasetBase.getHeight();

				adfGeoTransform = gdalDatasetBase.getAdfGeoTransform();
				oriResoulutionX = adfGeoTransform[1];
				oriResoulutionY = -adfGeoTransform[5];
				adfGeoTransform[1] = key.getDstResolution();
				adfGeoTransform[5] = -key.getDstResolution();

				outputpath = key.getGtdataOutputPath().toString();
				if (!outputpath.endsWith("/")) {
					outputpath += "/";
				}
			}


			long xOrigin = key.getXreadOrigin();//重采样后的x坐标
			long yOrigin = key.getYreadOrigin();//重采样后的y坐标
			int xLength = (int) key.getXreadLen();//重采样后的宽度
			int yLength = (int) key.getYreadLen();//重采样后的高度
			int tileColOffset = key.getTileColOffset();
			int tileRowOffset = key.getTileRowOffset();
			double dstResolution = key.getDstResolution();//重采样后的分辨率
			long colNum = key.getColNum();
			long rowNum = key.getRowNum();

			if (gdalDatasetBase != null) {

				//实际需要读取影像的真实起始点坐标（以像素为单位）
				int xReadCoordinateActual = (int) (xOrigin * dstResolution / oriResoulutionX);
				int yReadCoordinateActual = (int) (yOrigin * dstResolution / oriResoulutionY);

				//实际需要读取的瓦片数据范围
				int xreadActual = (int) (xLength * (dstResolution) / oriResoulutionX);
				int yreadActual = (int) (yLength * (dstResolution) / oriResoulutionY);
				if (xReadCoordinateActual + xreadActual > width) {
					xreadActual = width - xReadCoordinateActual;
				}
				if (yReadCoordinateActual + yreadActual > height) {
					yreadActual = height - yReadCoordinateActual;
				}

				//重采样后需要读取的瓦片数据范围
				int xread;
				//重采样后已经读取的影像范围
				int xreaded = 0;
				//写入数据的起始点
				int xWriteOrigin;
				int yWriteOrigin = tileRowOffset;
				//y_coor = adfGeoTransform[3] - (yReadCoordinateActual) * oriResoulutionY;
				y_coor = (KEY_TILEORIGIN_Y - rowNum * dstResolution * CoreConfig.HEIGHT_DEFAULT) - yWriteOrigin * dstResolution;
				if (gdalDatasetBase.readData(xReadCoordinateActual, yReadCoordinateActual, xreadActual, yreadActual, xLength, yLength)) {
					for (xreaded = 0; xreaded < xLength; ) {
						if (xreaded == 0) {
							xread = widthRang - tileColOffset;
							if (xread > xLength) {
								xread = xLength;
							}
							xWriteOrigin = tileColOffset;
						} else {
							if (xLength - xreaded < widthRang) {
								xread = xLength - xreaded;
							} else {
								xread = widthRang;
							}
							xWriteOrigin = 0;
						}
						bOutput = true;
						//初始bufferImage
						gdalDatasetBase.resetBufferedImage();
						if (geoBase != null && geoBase.getGeometry() != null) {//有矢量文件，按照矢量文件范围读取数据
							Geometry geo = geometryBase.createPolygon(dstResolution, colNum, rowNum, KEY_TILEORIGIN_X, KEY_TILEORIGIN_Y, 0);
							if (geo != null) {
								bContains = geoBase.getGeometry().Contains(geo);
								//System.out.println(geoBase.getJSON());
								//System.out.println(geo.ExportToJson());
								bIntersection = geoBase.getGeometry().Intersect(geo);
								if (bContains) {//包含关系，直接读取
									gdalDatasetBase.changRGB(xread, yLength, xreaded, xLength, true);
								} else if (bIntersection) {//交集关系，需要按照交集矢量范围读取数据
									x_coor = (colNum * dstResolution * CoreConfig.WIDTH_DEFAULT - KEY_TILEORIGIN_X) + xWriteOrigin * dstResolution;
									//x_coor = adfGeoTransform[0] + xReadCoordinateActual * oriResoulutionX + (xreaded) * dstResolution;		
									bOutput = gdalDatasetBase.changRGB(x_coor, y_coor, dstResolution, xread, yLength, xreaded, xLength,
											geoBase.getGeometry());
								} else {
									bOutput = false;
								}
							} else {
								bOutput = false;
							}
						} else {
							bOutput = gdalDatasetBase.changRGB(xread, yLength, xreaded, xLength, true);
						}
						if (bOutput || geoBase == null) {

							if ((bContains && saveStorageBool) || (saveStorageBool && bOutput && geoBase == null)) {
								pictureFormatInt = GdalDatasetBase.JPEG_FORMAT_INT;
							} else {
								pictureFormatInt = GdalDatasetBase.PNG_FORMAT_INT;
							}
							gdalDatasetBase.setBufferedImage(xWriteOrigin, yWriteOrigin, xread, yLength, pictureFormatInt);
//							gdalDatasetBase.write2LocalFile("F://nanlin//test1.png", GdalDatasetBase.PNG_FORMAT_INT);
//							if(wartermarkFlag) {
//								gdalDatasetBase.setWarterMask("ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 13, Color.white, -1, -1, 0.4f, pictureFormatInt);
//							}
							setCol(colNum);
							setRow(rowNum);
							fileInfo.setBupdate(false);
							bOutput = output(context, outputpath, rowNum, colNum);
						}
						xreaded += xread;
						colNum++;
					}

				}
			}
		}

		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			super.cleanup(context);
			resourceTable.flushCommits();
			resourceTable.close();
			if (gdalDatasetBase != null) {
				gdalDatasetBase.close();
			}
			if (outputpath != null) {
				Configuration conf = context.getConfiguration();
				HTable metaTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
				metaTable.setAutoFlush(false, true);
				//insert the Layers information into meta table
				StringBuilder rowkeyStrBuilder = new StringBuilder(outputpath);
				rowkeyStrBuilder.insert(rowkeyStrBuilder.lastIndexOf("/L"), "/");
				rowkeyStrBuilder.deleteCharAt(rowkeyStrBuilder.length() - 1);
				byte[] keys = Bytes.toBytes(TransCoding.decode(rowkeyStrBuilder.toString(), "UTF-8"));
				Get get = new Get(keys);
				Result result = metaTable.get(get);
				if (result.isEmpty()) {
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
			metadataTable.close();

			if (localShpFilePath != null) {
				File file = new File(localShpFilePath);
				File dir = file.getParentFile();
				File[] fileList = dir.listFiles();
				for (int i = 0; fileList != null && i < fileList.length; i++) {
					fileList[i].delete();
				}
				dir.delete();
			}
		}
	}

	/**
	 * @author shaolin
	 * insert the file's record into meta table
	 */
	static class MetaTableInsertReducer extends
			TableReducer<Text, FileInfo, NullWritable> {

		public void reduce(Text key, Iterable<FileInfo> values, Context context)
				throws IOException, InterruptedException {
			String gtpath = GtDataUtils.format2GtPath(key.toString());
			//插入行号目录
			Put put = new Put(Bytes.toBytes(gtpath));
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

			for (FileInfo val : values) {
				if (val.getTimeText() == null || val.getTimeText().getLength() == 0) {//不含时间的瓦片更新
					gtpath = GtDataUtils.format2GtPath(key.toString() + "/" + val.getFilename());
					put = new Put(Bytes.toBytes(gtpath));
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
			}// end for
		}
	}


	public int cacluteDstResolution(double srcResolution, double zeroLayersResolution) {
		int maxLayers = 0;
		double resolutionTmp = zeroLayersResolution;
		while (true) {
			if (srcResolution - resolutionTmp > 0.0000000000000) {
				maxLayers--;
				break;
			} else if (srcResolution == resolutionTmp) {
				break;
			}
			resolutionTmp = resolutionTmp / 2.0;
			maxLayers++;
		}
		return maxLayers;
	}

	public int run(String[] args) throws Exception {
		String jobid = args[0];
		String imageInputPath = args[1];
		List<String> filePaths = new ArrayList<String>();
		boolean resultFlag = FileOperate.listFiles(imageInputPath, filePaths);
		System.out.println("-------------");
		int status = 0;
		Map<String, String> map = new HashMap<String, String>();
		if (resultFlag && filePaths.size() > 0) {
			int totalFileCount = filePaths.size();
			map.put(CoreConfig.JOB.JID.strVal, "");
			map.put(CoreConfig.JOB.TOTAL.strVal, "" + totalFileCount);
			HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, map);
			map.remove(CoreConfig.JOB.TOTAL.strVal);
			for (int i = 0; i < totalFileCount; i++) {
				map.put(CoreConfig.JOB.CURRNT.strVal, i + "");
				HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, map);
				map.remove(CoreConfig.JOB.CURRNT.strVal);
				args[1] = filePaths.get(i);
				if (i == 1) {
					for (int j = 0; j < args.length; j++) {
						if (args[j].equals("-bcover")) {
							args[j + 1] = "false";
							break;
						}
					}
				}
				System.out.println("-------------");
				status = runFile(args);
				LOG.error("test runFile-------------" + status);

				if (status != 0) {
					break;
				}
			}
		} else {
			status = 1;
		}
		map.clear();
		LOG.error("test-------------" + status);
		if (status != 0) {
			map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.FAILED.toString());
		} else {
			map.put(CoreConfig.JOB.STATE.strVal, CoreConfig.JOB_STATE.SUCCEEDED.toString());
		}
		HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, map);
		return 1;

	}

	public int runFile(String[] args) throws Exception {
		if (args.length < 4 || (args.length % 2 != 0)) {
			usage();
			return 1;
		}

		int layers = Integer.parseInt(args[3]);
		int minlayers = 0;
		int updateMinLayer = -1;
		double dstresolution = 0;
		double maxLayerResolution = 0;
		double minLayerResolution = 0;
		boolean maxLayerResolutionFlag = false;
		boolean watermarkFlag = true;
		boolean minLayerResolutionFlag = false;
		boolean saveStorageBoolean = false;
		boolean bCover = true;
		int nodataInt = -1;
		String shpFilePath = null;
		String queueName = null;
		String fileFormat = ".png";
		String pressTextStr = "ChinaRS中科遥感";
		int pressTextSize = 13;
		int waterMarkScheme = 0;

		if (args.length > 4) {
			for (int i = 4; i < args.length; i++) {
				if (args[i].equals("-maxLayer_resolution")) {
					maxLayerResolutionFlag = true;
					i++;
					maxLayerResolution = Double.parseDouble(args[i]);
				} else if (args[i].equals("minLayer_resolution")) {
					minLayerResolutionFlag = true;
					i++;
					minLayerResolution = Double.parseDouble(args[i]);
				} else if (args[i].equals("-watermark")) {
					i++;
					if (args[i].equals("false")) {
						watermarkFlag = false;
					}
				} else if (args[i].equals("-minLayers")) {
					i++;
					minlayers = Integer.parseInt(args[i]);
				} else if (args[i].equals("-save_storage")) {
					i++;
					saveStorageBoolean = BooleanUtils.toBoolean(args[i]);
				} else if (args[i].equals("-nodata")) {
					i++;
					nodataInt = Integer.parseInt(args[i]);
				} else if (args[i].equals("-shpfile")) {
					i++;
					shpFilePath = args[i];
				} else if (args[i].equals("-bcover")) {
					i++;
					bCover = BooleanUtils.toBoolean(args[i]);
				} else if (args[i].equals("-queue")) {
					i++;
					queueName = args[i];
					if (!queueName.equals("cutting")) {
						queueName = null;
					}
				} else if (args[i].equalsIgnoreCase("-fileFormat")) {
					i++;
					fileFormat = args[i];
					if (fileFormat.equalsIgnoreCase(".png") || fileFormat.equalsIgnoreCase(".jpg")
							|| fileFormat.equalsIgnoreCase(".jpeg")) {

					} else {
						usage();
						System.exit(0);
					}
				} else if (args[i].equalsIgnoreCase("-pressText")) {
					i++;
					pressTextStr = args[i];
				} else if (args[i].equalsIgnoreCase("-pressTextSize")) {
					i++;
					pressTextSize = Integer.parseInt(args[i]);
				} else if (args[i].equalsIgnoreCase("-waterMarkScheme")) {
					i++;
					waterMarkScheme = Integer.parseInt(args[i]);
				}
			}
		}

		Configuration conf = HBaseConfiguration.create();
		if (queueName != null) {
			conf.set("mapreduce.job.queuename", queueName);
			System.out.println("queuename= " + conf.get("mapreduce.job.queuename"));
		}

		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		Dataset dataset = gdal.Open(args[1], gdalconstConstants.GA_ReadOnly);
		double[] adfGeoTransform = dataset.GetGeoTransform();
		System.out.println(dataset.GetProjectionRef());
		System.out.println(CoreConfig.MERCATOR_PROJECT);
		if (dataset.GetProjectionRef().startsWith(CoreConfig.WGS84P_ROJECT)) {
			if (maxLayerResolutionFlag) {
				dstresolution = maxLayerResolution;
			} else if (minLayerResolutionFlag) {
				dstresolution = minLayerResolution;
				for (int i = 0; i < layers; i++) {
					dstresolution = dstresolution / 2.0;
				}
			} else if (layers < 0) {
				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.LAYERS_RESOLUTION[0]);
				dstresolution = 0.703125 / Math.pow(2, layers);
			} else {
				dstresolution = 0.703125 / Math.pow(2, layers);
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_WGS84);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_WGS84);
		} else if (dataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT1) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MERCATOR_PROJECT) ||
				dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MECATOR_PROJECT_1)) {
			if (maxLayerResolutionFlag) {
				dstresolution = maxLayerResolution;
			} else if (minLayerResolutionFlag) {
				dstresolution = minLayerResolution;
				for (int i = 0; i < layers; i++) {
					dstresolution = dstresolution / 2.0;
				}
			} else if (layers < 0) {
				layers = cacluteDstResolution(adfGeoTransform[1], CoreConfig.MERCATOR_LAYERS_RESOLUTION[0]);
				dstresolution = 156543.033928 / Math.pow(2, layers);
			} else {
				dstresolution = 156543.033928 / Math.pow(2, layers);
			}
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_MERCATOR);
			conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_MERCATOR);
		} else {
			dataset.delete();
			System.out.println("投影不支持");
			return 1;
		}
		dataset.delete();
		System.out.println("======nanlin=====debug analysis parameters");
		conf.set(CoreConfig.JOBID, args[0]);
		conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
		conf.setInt(CoreConfig.KEY_CURRENT_LAYER, layers);
		conf.setInt(CoreConfig.KEY_MIN_LAYER, minlayers);
		conf.setInt(CoreConfig.KEY_UPDATE_MINLAYER_INT, updateMinLayer);

		conf.setDouble(CoreConfig.KEY_DST_RESOLUTION, dstresolution);
		conf.setBoolean(SAVE_STORAGE_BOOLEAN, saveStorageBoolean);
		conf.setBoolean("bcover", bCover);
		conf.setInt("NO_DATA", nodataInt);
		conf.set(KEY_FILE_FORMAT_STRING, fileFormat);
		conf.set(KEY_PRESSTEXT_STRING, pressTextStr);
		conf.setInt(KEY_PRESSTEXTSIZE_INT, pressTextSize);
		conf.setInt(KEY_WATERMARKSCHEME_INT, waterMarkScheme);

		conf.set(CoreConfig.CUTTING_OUTPUTPATH, args[2]);
		System.out.println(CoreConfig.CUTTING_OUTPUTPATH + ": outputPaht: " + args[2]);
		conf.setBoolean(CoreConfig.KEY_WARTERMARK, watermarkFlag);
		conf.set(TableOutputFormat.OUTPUT_TABLE, CoreConfig.MAP_META_TABLE);
		if (shpFilePath != null) {
			conf.set("shpFilePath", shpFilePath);
		}

		Job job = Job.getInstance(conf);
//		Path path = new Path("hdfs://node03.rsclouds.cn:8020/nanlin/pl_langfang_4_5.log");
		Path path = new Path(args[1]);
		if (args[1].startsWith("gtdata")) {
			Configuration confHbase = HBaseConfiguration.create();
			HTable meta = new HTable(confHbase, GtDataConfig.TABLE_NAME.META_TABLE.getByteVal());
			HTable res = new HTable(confHbase, GtDataConfig.TABLE_NAME.RES_TABLE.getByteVal());
			String gtpath = args[1].substring("gtdata://".length());
			gtpath = GtDataUtils.format2GtPath(gtpath);
			System.out.println("gtpath==" + gtpath);
			Get metaGet = new Get(gtpath.getBytes());
			Result result = meta.get(metaGet);
			if (result.isEmpty()) {
				System.out.println("meta result is empty");
				meta.close();
				res.close();
				return 1;
			}
			byte[] url = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
			if (url == null || url.length == 0) {
				System.out.println("url  is empty");
				meta.close();
				res.close();
				return 1;
			}
			Get resGet = new Get(url);
			result = res.get(resGet);
			if (result.isEmpty()) {
				System.out.println("res result is empty");
				meta.close();
				res.close();
				return 1;
			}
			byte[] prefixBytes = result.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal, GtDataConfig.RESOURCE.PREFIX.byteVal);
			if (prefixBytes == null || prefixBytes.length == 0) {
				path = new Path(GtDataConfig.HDFS_MD5_PATH, new String(url));
			} else {
				path = new Path(GtDataConfig.HDFS_ROOT_PATH, new String(prefixBytes) + new String(url));
			}
			meta.close();
			res.close();
		}
		FileInputFormat.addInputPath(job, path);
		job.setJarByClass(ImageSegementBase.class);
		job.setInputFormatClass(ImageMutilLayersInputFormat.class);
//		job.setInputFormatClass(ImageBlockByShpInputFormat.class);
		job.setMapperClass(ImageSegMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FileInfo.class);
		job.setReducerClass(MetaTableInsertReducer.class);
		job.setNumReduceTasks(6);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.setJobName("cutting " + path.getName());
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void usage() {
		LOG.info("usage: <rowkey> <input_path(hdfs)> <gt-data ouput path(../../_alllayers)> <maxLayers>");
		LOG.info("       [-maxLayer_resolution maxResolution] [-minLayer_resolution minresolution] [-watermark true/false]");
		LOG.info("       [-minLayers minlayers] [-picture_format] [-shpfile] [-pressText] [-pressTextSize] [-waterMarkScheme 0/1]");
		LOG.info("       [-save_storage true/false] [-nodata int] [-queue queuenname(cutting/default)] [-fileFormat .png/.jpg]");
		LOG.info("       maxLayers is interger number, if it's a negative, we will caclute a right layers by the image's resolution");
		LOG.info("       -picture_format 瓦片输出格式，可选为png或jpeg");
		LOG.info("       -updateMinLayer 需要合并更新的最小图层");
		LOG.info("       -pressText      水印字体内容");
		LOG.info("       -pressTextSize  水印字体大小");
		LOG.info("       -waterMarkScheme 添加水印方案， 0表示全部加水印，1表示只有行列号的和为奇数才加水印");
		LOG.info("       -save_storage 为true且和-zero_percentage一起使用，表示超过阈值的存储格式为png，否则为jpeg");
	}

	public static boolean getPaths(FileSystem fs, Path dir, List<String> paths) {
		try {
			FileStatus[] fileStatus = fs.listStatus(dir);
			for (int i = 0; i < fileStatus.length; i++) {
				Path p = fileStatus[i].getPath();
				if (fs.isFile(p)) {
					paths.add(p.toString());
				} else {
					if (!getPaths(fs, p, paths)) {
						return false;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0) {
			args = new String[16];
			args[0] = "123456";
			args[1] = "D://nanlin//image//20161028_021647_0e0e_visual-three-wgs84-dehaze-possion-webmercator-valid-lzw.tiff";
			args[2] = "/home/yarn";
			args[3] = "14";
			args[4] = "-minLayers";
			args[5] = "14";
			args[6] = "-watermark";
			args[7] = "true";
			args[8] = "-save_storage";
			args[9] = "true";
			args[10] = "-zero_percentage";
			args[11] = "0.0001";
			args[12] = "-bcover";
			args[13] = "true";
			args[14] = "-nodata";
			args[15] = "0";
//			args[16] = "-shpfile";
//			args[17] = "hdfs://node03.rsclouds.cn:8020/nanlin/baoding/baoding.shp";

		}
		int status = 0;
		if (args.length < 4) {
			usage();
		} else {
			Path path = new Path(args[1]);
			Configuration conf = new Configuration();
			FileSystem fs = FileSystem.get(conf);
			if (fs.isFile(path)) {
				status = ToolRunner.run(new ImageSegementBase(), args);
			} else {
				List<String> paths = new ArrayList<String>();
				if (getPaths(fs, path, paths)) {
					for (int i = 0; i < paths.size(); i++) {
						args[1] = paths.get(i);
						System.out.println("current cuting: " + i + " total size: " + paths.size());
						status = ToolRunner.run(new ImageSegementBase(), args);
						if (status != 0) {
							System.out.println("cutting error: " + args[1]);
							break;
						}
					}
				} else {
					System.out.println("getPaths error: " + paths.toString());
				}
			}
		}
		System.exit(status);
	}
}
