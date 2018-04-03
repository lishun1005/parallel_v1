package com.rsclouds.gtparallel.core.gtdata.cutting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsclouds.gtparallel.core.gtdata.common.Dom4JForMapPublishing;

public class GenerateMapConf {
	
	private static final Log LOG = LogFactory.getLog(GenerateMapConf.class);

	public static void main(String[] args) {
//		args = new String[4];
//		args[0] = "d://nanlin//image//nanchang.tiff";//gtdata:///users/wu0769/image/20170619_021528_0f31_3B_AnalyticMS.tif
//		args[1] = "/test/Layers";
//		args[2] = "6";
//		args[3] = "true";
		if(args == null || args.length != 4){
			LOG.info("usage: <hdfsPath> <layersPath)> <layers> <bxmlUpdate> [-maxLayer_resolution maxResolution] [-minLayer_resolution minresolution]");
		}else{
			Dom4JForMapPublishing dom = new Dom4JForMapPublishing();
			String hdfsPath = args[0];//"hdfs://192.168.2.3:8020/temp/8574397110356_testUser_e0ac75e6-8bf5-4eaa-8928-2c95d46532be/guangdong0310.tiff";
			String layersPath = args[1];
			int layers = Integer.parseInt(args[2]);
			boolean bxmlUpdate = Boolean.parseBoolean(args[3]);
			String[] resolutionargs = null;
			if(args.length > 4){
				resolutionargs = new String[args.length - 4];
				System.arraycopy(args, 4, resolutionargs, 0, resolutionargs.length -1);
			}	
			boolean flag = dom.generateMapConf(hdfsPath, layersPath, layers, bxmlUpdate);
			if(flag){
				System.exit(0);
			}else{
				System.exit(1);
			}
		}
	}

}
