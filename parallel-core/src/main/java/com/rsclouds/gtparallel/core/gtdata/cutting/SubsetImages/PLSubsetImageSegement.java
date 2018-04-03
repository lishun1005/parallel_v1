package com.rsclouds.gtparallel.core.gtdata.cutting.SubsetImages;


import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.cutting.ImageSegemetReducerBase;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.mapreduce.PLSubsetImagesInputFormat;


public class PLSubsetImageSegement extends Configured implements Tool {
	private static final Logger LOG = LoggerFactory.getLogger(PLSubsetImageSegement.class);
	public  static final String SAVE_STORAGE_BOOLEAN = "save_storage";
	public static final String KEY_FILE_FORMAT_STRING = "file_suffix";
	
	
	public void usage() {
		LOG.info("usage: <rowkey> <input_path(hdfs)> <gt-data ouput path(../../_alllayers)> <orgLayers> <maxLayers>");
		LOG.info("       [-watermark true/false] [-minLayers minlayers] [-picture_format]");
		LOG.info("       [-save_storage true/false] [-nodata int] [-queue queuenname(cutting/default)] [-fileFormat .png/.jpg]");
		LOG.info("       maxLayers is interger number, if it's a negative, we will caclute a right layers by the image's resolution");
		LOG.info("       -picture_format 瓦片输出格式，可选为png或jpeg");
		LOG.info("       -updateMinLayer 需要合并更新的最小图层");
		LOG.info("       -save_storage 为true且和-zero_percentage一起使用，表示超过阈值的存储格式为png，否则为jpeg");
		LOG.info("       orgLayers 分幅影像数据是按照哪一层级的标准进行格网分割");
	}

	public static Path getFile(FileSystem fs, Path pathDir) {
		try {
			FileStatus[] fileStatus = fs.listStatus(pathDir);
			if (fs.isFile(fileStatus[0].getPath())) {
				return fileStatus[0].getPath();
			}else {
				return getFile(fs, fileStatus[0].getPath());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 5) {
			usage();
			return 0;
		}else {
			if (args.length < 5 || (args.length % 2 != 1) ) {
				usage();
				return 1;
			}
			
			int orgLayers = Integer.parseInt(args[3]);
			int maxLayers = Integer.parseInt(args[4]);
			int minlayers = 0;
			boolean watermarkFlag = true;
			boolean saveStorageBoolean = false;
			boolean zeroPercentageBool = false;
			boolean bCover = true;
			int nodataInt = -1;
			double zeroPercentage = 1.20;
			String queueName = null;
			String fileFormat = ".png";
			
			if(args.length > 4) {
				for(int i = 5; i < args.length; i ++) {
					if (args[i].equals("-watermark")) {
						i ++;
						if(args[i].equals("false")){
							watermarkFlag = false;
						}
					}else if (args[i].equals("-minLayers")) {
						i ++;
						minlayers = Integer.parseInt(args[i]);
					}else if(args[i].equals("-save_storage")) {
						i++;
						saveStorageBoolean = BooleanUtils.toBoolean(args[i]);
					}else if(args[i].equals("-nodata")) {
						i++;
						nodataInt = Integer.parseInt(args[i]);
					}else if (args[i].equals("-bcover")) {
						i ++;
						bCover = BooleanUtils.toBoolean(args[i]);
					}else if (args[i].equals("-queue")) {
						i ++;
						queueName = args[i];
						if ( !queueName.equals("cutting") ) {
							queueName = null;
						}	
					}else if (args[i].equalsIgnoreCase("-fileFormat")) {
						i ++;
						fileFormat = args[i];
						if(fileFormat.equalsIgnoreCase(".png") || fileFormat.equalsIgnoreCase(".jpg")
								|| fileFormat.equalsIgnoreCase(".jpeg")) {
							
						}else {
							usage();
							System.exit(0);
						}
					}else if (args[i].equals("-zero_percentage")) {
						i ++;
						zeroPercentage = Double.parseDouble(args[i]);
						zeroPercentageBool = true;
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
//			Path dirPath = new Path("hdfs://node03.rsclouds.cn:8020/nanlin");
//			Dataset dataset = gdal.Open("D:\\nanlin\\image\\beijing_test\\L15-3363E-2539N.tif", gdalconstConstants.GA_ReadOnly);
			FileSystem fs = FileSystem.get(conf);
			Path dirPath = new Path(args[1]);
//			FileStatus[] fileStatus = fs.listStatus(dirPath);
			Path filePath = getFile(fs, dirPath);
			Dataset dataset = gdal.Open(filePath.toString(), gdalconstConstants.GA_ReadOnly);			
			if (dataset.GetProjectionRef().startsWith(CoreConfig.WGS84P_ROJECT)) {
				conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_WGS84);
				conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_WGS84);
			}else if(dataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT) ||
					dataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT1) ||
					dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MERCATOR_PROJECT) ||
					dataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MECATOR_PROJECT_1)){
				conf.setDouble(CoreConfig.KEY_TILEORIGIN_X, CoreConfig.VALUE_TILEORIGIN_X_MERCATOR);
				conf.setDouble(CoreConfig.KEY_TILEORIGIN_Y, CoreConfig.VALUE_TILEORIGIN_Y_MERCATOR);
			}else {
				dataset.delete();
				System.out.println("投影不支持");
				return 1;
			}
			dataset.delete();	
			System.out.println("======nanlin=====debug analysis parameters");
			if(saveStorageBoolean && !zeroPercentageBool) {
				zeroPercentage = 0.0001;
			}
			conf.setDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, zeroPercentage);
			conf.set(CoreConfig.JOBID, args[0]);
			conf.set(CoreConfig.CUTTING_INPUTFILE, args[1]);
			conf.setInt(CoreConfig.KEY_CURRENT_LAYER, maxLayers);
			conf.setInt(CoreConfig.KEY_GRID_LAYER_INT, orgLayers);
			conf.setInt(CoreConfig.KEY_MIN_LAYER, minlayers);
			conf.setBoolean(SAVE_STORAGE_BOOLEAN, saveStorageBoolean);
			conf.setBoolean("bcover", bCover);
			conf.setInt("NO_DATA", nodataInt);
			conf.set(KEY_FILE_FORMAT_STRING, fileFormat);
			conf.set(CoreConfig.CUTTING_OUTPUTPATH, args[2]);
			conf.setBoolean(CoreConfig.KEY_WARTERMARK, watermarkFlag);
			conf.set(TableOutputFormat.OUTPUT_TABLE, CoreConfig.MAP_META_TABLE);
//			conf.setInt("mapreduce.map.memory.mb", 4096);
//			conf.setInt("mapreduce.task.timeout", 86400000);
			
			Job job = Job.getInstance(conf);
			FileInputFormat.addInputPath(job, dirPath);
			job.setJobName("cutting " + dirPath.getName());
			job.setJarByClass(PLSubsetImageSegement.class);
			
			job.setMapperClass(PLSubsetImageMapper.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(FileInfo.class);
			job.setReducerClass(ImageSegemetReducerBase.class);
			job.setNumReduceTasks(6);
			job.setInputFormatClass(PLSubsetImagesInputFormat.class);
			job.setOutputFormatClass(TableOutputFormat.class);
			return job.waitForCompletion(true) ? 0 : 1;
		}
		
	}
	
	
	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0) {
			args = new String[17];
			args[0] = "123456";
			args[1] = "D:\\nanlin\\image\\beijing_test";
			args[2] = "/map/auto_proc/img/warter/yizhangtu_imageTest/beijing/beijing_pl_mecator/ocover1_20160701_20160930_pl_4m/Layers/_alllayers";
			args[3] = "12";
			args[4] = "15";
			args[5] = "-minLayers";
			args[6] = "0";
			args[7] = "-watermark";
			args[8] = "true";
			args[9] = "-save_storage";
			args[10] = "true";
			args[11] = "-zero_percentage";
			args[12] = "0.0001";
			args[13] = "-bcover";
			args[14] = "true";
			args[15] = "-nodata";
			args[16] = "0";
		}
		int status = ToolRunner.run(new PLSubsetImageSegement(), args);
		System.exit(status);
	}
}
