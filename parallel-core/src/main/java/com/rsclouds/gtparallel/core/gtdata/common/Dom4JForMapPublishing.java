package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.operation.Import;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

@Service("Dom4JForMapPublishing")
@Scope("singleton")
public class Dom4JForMapPublishing {

	private static final Log LOG = LogFactory.getLog(Dom4JForMapPublishing.class);

	/**
	 * 
	 * @param resolution
	 * @param layers
	 * @return
	 */
	public double[][] calScaleResolution(double resolution, double scale, int layers) {
		double[][] scaleResolution = new double[layers + 1][2];
		for (int i = layers; i >= 0; i--) {
			scaleResolution[i][0] = scale;
			scaleResolution[i][1] = resolution;
			resolution *= 2.0;
			scale *= 2.0;
		}
		return scaleResolution;
	}

	public boolean isMapNameExist(String mapname, String filePath) {
		String geowebcacheXmlPath = filePath;
		String mapNode = "/*[name()='gwcConfiguration']/*[name()='layers']/*[name()='arcgisLayer']/*[name()='name']";
		if (!isStringEmpty(geowebcacheXmlPath)) {
			Document doc = XmlDom4J.parse2Document(geowebcacheXmlPath);
			if (doc != null && !isStringEmpty(mapname)) {
				if (XmlDom4J.selectNodeFirst(doc, mapNode, mapname) != null) {/// arcgisLayer/name
					return true;
				}
			}
		}
		return false;
	}

	public boolean isMapNameExist(String mapname) {
		return isMapNameExist(mapname, CoreConfig.GEOWEBCACHE_XML_PATH);
	}

	public boolean isStringEmpty(final String str) {
		if (str == null || str.equals("")) {
			return true;
		}
		return false;
	}

	/**
	 * generate conf.xml
	 * 
	 * @param filename
	 * @param scale_resolution
	 * @param layers
	 *            the number of last Layers (0~layers)
	 * @return
	 */
	public boolean generateConfXML(String defaultFile, String filename, double[][] scale_resolution, int layers) {
		String LODInfos = "/CacheInfo/TileCacheInfo/LODInfos";
		FileOperate.copyTemplateFile(defaultFile, filename);
		Document document = XmlDom4J.parse2Document(filename);
		if (document != null) {
			Element element = XmlDom4J.selectNodeFirst(document, LODInfos, null);
			if (element != null) {
				for (int i = 0; i <= layers; i++) {
					Element tmp = XmlDom4J.addNode(element, "LODInfo", null, "xsi:type", "typens:LODInfo");
					XmlDom4J.addNode(tmp, "LevelID", "" + i, null, null);
					XmlDom4J.addNode(tmp, "Scale", "" + scale_resolution[i][0], null, null);
					XmlDom4J.addNode(tmp, "Resolution", "" + scale_resolution[i][1], null, null);
				}
				XMLWriter output;
				try {
					output = new XMLWriter(new FileWriter(new File(filename)));
					output.write(document);
					output.close();
					return true;
				} catch (IOException e) {
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * generate conf.cdi
	 * 
	 * @param filename
	 * @param xMin
	 * @param yMin
	 * @param xMax
	 * @param yMax
	 * @return
	 */
	public boolean generateConfCDI(String filename, double xMin, double yMin, double xMax, double yMax) {
		FileOperate.copyTemplateFile("conf.cdi", filename);
		Document document = XmlDom4J.parse2Document(filename);
		if (document != null) {
			XmlDom4J.changNodeText(document, "/EnvelopeN/XMin", "" + xMin);
			XmlDom4J.changNodeText(document, "/EnvelopeN/YMin", "" + yMin);
			XmlDom4J.changNodeText(document, "/EnvelopeN/XMax", "" + xMax);
			XmlDom4J.changNodeText(document, "/EnvelopeN/YMax", "" + yMax);
			XMLWriter output;
			try {
				output = new XMLWriter(new FileWriter(new File(filename)));
				output.write(document);
				output.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean addGeowebcacheXML(String mapname, String xmlPath, String alllayersPath, String filePath) {
		String geoxml_layers_node = "/*[name()='gwcConfiguration']/*[name()='layers']";
		if (!(isStringEmpty(mapname) || isStringEmpty(xmlPath) || isStringEmpty(alllayersPath))) {
			if (xmlPath.endsWith("/") && xmlPath.length() > 1) {
				xmlPath = xmlPath.substring(0, xmlPath.length() - 1);
			}
			if (!isStringEmpty(filePath)) {
				Document document = XmlDom4J.parse2Document(filePath);
				Element ele = XmlDom4J.selectNodeFirst(document, geoxml_layers_node, null);
				if (ele != null) {
					Element arcgisLayer = XmlDom4J.addNode(ele, "arcgisLayer", null, null, null);
					XmlDom4J.addNode(arcgisLayer, "name", mapname, null, null);
					XmlDom4J.addNode(arcgisLayer, "tilingScheme", xmlPath + "/conf.xml", null, null);
					XmlDom4J.addNode(arcgisLayer, "tileCachePath", alllayersPath, null, null);
					XMLWriter output = null;
					try {
						output = new XMLWriter(new FileWriter(new File(filePath)));
						output.write(document);

						return true;
					} catch (IOException e) {
						return false;
					} finally {
						try {
							if (output != null) {
								output.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return false;
	}

	public boolean addGeowebcacheXML(String mapname, String xmlPath, String alllayersPath) {
		return addGeowebcacheXML(mapname, xmlPath, alllayersPath, CoreConfig.GEOWEBCACHE_XML_PATH);
	}

	/**
	 * @deprecated
	 * @param mapname
	 * @param imgaePath
	 *            imgaeFile the path of image file on the hdfs
	 * @param xmlPath
	 *            xmlPath the path of conf.xml and conf.cdi on gt-data
	 * @param alllayersPath
	 *            the path of alllayers which storage file of map
	 * @param layers
	 * @return
	 */
	public boolean mapPublishing(String mapname, String imgaePath, String xmlPath, String alllayersPath, int layers,
			String[] args) {
		if (generateMapConf(imgaePath, xmlPath, alllayersPath, layers, args)) {
			if (addGeowebcacheXML(mapname, xmlPath, alllayersPath)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @deprecated
	 * @param imgaePath
	 * @param xmlPath
	 * @param alllayersPath
	 * @param layers
	 * @param param
	 * @return
	 */
	public boolean generateMapConf(String imgaePath, String xmlPath, String alllayersPath, int layers, String[] param) {
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		Configuration conf = new Configuration();
		FileSystem fs;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		List<String> paths = new ArrayList<String>();
		Path path = new Path(imgaePath);
		String imagePathFirst = null;
		if (FileOperate.getPaths(fs, path, paths)) {
			imagePathFirst = paths.get(0);
		} else {
			return false;
		}
		Dataset hDataset = gdal.Open(imagePathFirst, gdalconstConstants.GA_ReadOnly);
		if (hDataset == null) {
			LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
			LOG.info(gdal.GetLastErrorMsg());
			return false;
		}
		String defaultConfXML = null;

		double dstResolution;
		double scale;
		boolean maxLayerResolutionFlag = false;
		double maxLayerResolution = 0;
		boolean minLayerResolutionFlag = false;
		double minLayerResolution = 0;
		if (param != null) {
			if (param.length % 2 != 0) {
				return false;
			} else {
				for (int i = 0; i < param.length; i++) {
					if (param[i].equals("-maxLayer_resolution")) {
						maxLayerResolutionFlag = true;
						i++;
						maxLayerResolution = Double.parseDouble(param[i]);
					} else if (param[i].equals("-minLayer_resolution")) {
						minLayerResolutionFlag = true;
						i++;
						minLayerResolution = Double.parseDouble(param[i]);
					}
				}
			}
		}

		if (hDataset.GetProjectionRef().startsWith(CoreConfig.WGS84P_ROJECT)) {
			if (layers >= CoreConfig.LAYERS_RESOLUTION.length) {
				int length = CoreConfig.LAYERS_RESOLUTION.length - 1;
				int count = layers - length;
				dstResolution = CoreConfig.LAYERS_RESOLUTION[length];
				scale = CoreConfig.LAYERS_SCALE[length];
				for (int i = 0; i < count; i++) {
					dstResolution = dstResolution / 2;
					scale = scale / 2;
				}
			} else {
				dstResolution = CoreConfig.LAYERS_RESOLUTION[layers];
				scale = CoreConfig.LAYERS_SCALE[layers];
			}
			defaultConfXML = "png32_hyaline.xml";
		} else if (hDataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT)
				|| hDataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MERCATOR_PROJECT)) {
			if (layers >= CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
				int length = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length - 1;
				int count = layers - length;
				dstResolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[length];
				scale = CoreConfig.MERCATOR_LAYERS_SCALE[length];
				for (int i = 0; i < count; i++) {
					dstResolution = dstResolution / 2;
					scale = scale / 2;
				}
			} else {
				dstResolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers];
				scale = CoreConfig.MERCATOR_LAYERS_SCALE[layers];
			}

			defaultConfXML = "png32_Mercator.xml";
		} else {
			System.out.println("projects not support!");
			hDataset.delete();
			return false;
		}
		if (maxLayerResolutionFlag) {
			scale = maxLayerResolution * (scale / dstResolution);
			dstResolution = maxLayerResolution;
		} else if (minLayerResolutionFlag) {
			maxLayerResolution = minLayerResolution;
			for (int i = 0; i < layers; i++) {
				maxLayerResolution = maxLayerResolution / 2.0;
			}
			scale = maxLayerResolution * (scale / dstResolution);
			dstResolution = maxLayerResolution;
		}

		double[][] scaleResolution = calScaleResolution(dstResolution, scale, layers);
		Import importFile = new Import();
		String tempDir = CoreConfig.LOCA_LTEMP_DIR;
		if (tempDir.endsWith("/")) {
			tempDir = tempDir.substring(0, tempDir.length() - 1);
		}
		String confTempDir = tempDir + "/" + UUID.randomUUID();
		File file = new File(confTempDir);
		file.mkdir();
		String conXML = confTempDir + "/conf.xml";
		if (generateConfXML(defaultConfXML, conXML, scaleResolution, layers)) {
			if (xmlPath.endsWith("/")) {
				xmlPath = xmlPath.substring(0, xmlPath.length() - 1);
			}
			if (importFile.ImportLocalFileToMapTable(conXML, xmlPath + "/conf.xml")) {
				String confCDI = confTempDir + "/conf.cdi";
				double[] range = new double[4];
				if (getImageRange(imgaePath, range))
					if (generateConfCDI(confCDI, range[0], range[1], range[2], range[3])) {
						if (importFile.ImportLocalFileToMapTable(confCDI, xmlPath + "/conf.cdi")) {
							FileOperate.deleteDir(file);
							hDataset.delete();
							return true;
						}
					} else {
						System.out.println("generateConfCDI 失败");
					}
			} else {
				System.out.println("ImportLocalFileToMapTable 失败");
			}
		} else {
			System.out.println("generateConfXML 失败");
		}
		hDataset.delete();
		FileOperate.deleteDir(file);
		return false;
	}

	/**
	 * 
	 * @param imageInputPath
	 *            影像路径（可以是目录）
	 * @param xmlPath
	 *            配置文件输出路径
	 * @param layers
	 *            最大层级
	 * @param bXmlUpdate
	 *            是否更新conf.xml文件
	 * @return true or false
	 */
	public boolean generateMapConf(String imageInputPath, String xmlPath, int layers, boolean bXmlUpdate) {
		boolean resultFlag = false;
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		List<String> filePaths = new ArrayList<String>();
		try {
			resultFlag = FileOperate.listFiles(imageInputPath, filePaths);
			String tempDir = CoreConfig.LOCA_LTEMP_DIR;
			if (tempDir.endsWith("/")) {
				tempDir = tempDir.substring(0, tempDir.length() - 1);
			}
			String confTempDir = tempDir + "/" + UUID.randomUUID();
			File file = new File(confTempDir);
			file.mkdirs();
			Import importFile = new Import();

			// 生成conf.xml文件
			if (resultFlag && bXmlUpdate) {
				resultFlag = false;
				String imagePathFirst = filePaths.get(0);
				Dataset hDataset = gdal.Open(imagePathFirst, gdalconstConstants.GA_ReadOnly);
				if (hDataset == null) {
					LOG.info("GDALOpen failed - " + gdal.GetLastErrorNo());
					LOG.info(gdal.GetLastErrorMsg());
					return false;
				}
				String defaultConfXML = null;
				double dstResolution = 0.0;
				double scale = 0.0;
				if (hDataset.GetProjectionRef().startsWith(CoreConfig.WGS84P_ROJECT)) {
					if (layers >= CoreConfig.LAYERS_RESOLUTION.length) {
						int length = CoreConfig.LAYERS_RESOLUTION.length - 1;
						int count = layers - length;
						dstResolution = CoreConfig.LAYERS_RESOLUTION[length];
						scale = CoreConfig.LAYERS_SCALE[length];
						for (int i = 0; i < count; i++) {
							dstResolution = dstResolution / 2;
							scale = scale / 2;
						}
					} else {
						dstResolution = CoreConfig.LAYERS_RESOLUTION[layers];
						scale = CoreConfig.LAYERS_SCALE[layers];
					}
					defaultConfXML = "png32_hyaline.xml";
				} else if (hDataset.GetProjectionRef().startsWith(CoreConfig.MERCATOR_PROJECT)
						|| hDataset.GetProjectionRef().startsWith(CoreConfig.PSEUDO_MERCATOR_PROJECT)) {
					dstResolution = 156543.033928 / Math.pow(2, layers);
					if (layers >= CoreConfig.MERCATOR_LAYERS_RESOLUTION.length) {
						int length = CoreConfig.MERCATOR_LAYERS_RESOLUTION.length - 1;
						int count = layers - length;
						dstResolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[length];
						scale = CoreConfig.MERCATOR_LAYERS_SCALE[length];
						for (int i = 0; i < count; i++) {
							dstResolution = dstResolution / 2;
							scale = scale / 2;
						}
					} else {
						dstResolution = CoreConfig.MERCATOR_LAYERS_RESOLUTION[layers];
						scale = CoreConfig.MERCATOR_LAYERS_SCALE[layers];
					}
					defaultConfXML = "png32_Mercator.xml";

				} else {
					System.out.println("projects not support!");
					hDataset.delete();
					resultFlag = false;
				}
				double[][] scaleResolution = calScaleResolution(dstResolution, scale, layers);

				String conXML = confTempDir + "/conf.xml";
				if (generateConfXML(defaultConfXML, conXML, scaleResolution, layers)) {
					if (xmlPath.endsWith("/")) {
						xmlPath = xmlPath.substring(0, xmlPath.length() - 1);
					}
					if (importFile.ImportLocalFileToMapTable(conXML, xmlPath + "/conf.xml")) {
						resultFlag = true;
					}
				} else {
					LOG.info("generateConfXML error");
				}
				hDataset.delete();
			}
			// 生成conf.cdi文件
			if (resultFlag) {
				resultFlag = false;
				String confCDI = confTempDir + "/conf.cdi";
				double[] range = new double[4];
				if (getImageRange(xmlPath + "/conf.cdi", filePaths, range))
					if (generateConfCDI(confCDI, range[0], range[1], range[2], range[3])) {
						if (importFile.ImportLocalFileToMapTable(confCDI, xmlPath + "/conf.cdi")) {
							FileOperate.deleteDir(file);
							resultFlag = true;
						}
					} else {
						System.out.println("generateConfCDI 失败");
					}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return resultFlag;
	}

	/**
	 * 
	 * @param cdiPath
	 * @param filePaths
	 * @param range
	 * @return
	 * @throws IOException
	 */
	public static boolean getImageRange(String cdiPath, List<String> filePaths, double[] range) throws IOException {
		if (range.length < 4) {
			range = new double[4];
		}
		boolean resultFlag = true;
		gdal.AllRegister();
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		for (int i = 0; i < filePaths.size(); i++) {
			Dataset hDataset = gdal.Open(filePaths.get(i), gdalconstConstants.GA_ReadOnly);
			if (hDataset != null) {
				double[] adfGeoTransform = hDataset.GetGeoTransform();
				int xsize = hDataset.GetRasterXSize();
				int ysize = hDataset.GetRasterYSize();
				double XMax = adfGeoTransform[0] + xsize * adfGeoTransform[1] + ysize * adfGeoTransform[2];
				double YMin = adfGeoTransform[3] + xsize * adfGeoTransform[4] + ysize * adfGeoTransform[5];
				if (i == 0) {
					range[0] = adfGeoTransform[0];
					range[1] = YMin;
					range[2] = XMax;
					range[3] = adfGeoTransform[3];
				} else {
					range[0] = range[0] > adfGeoTransform[0] ? adfGeoTransform[0] : range[0];
					range[1] = range[1] > YMin ? YMin : range[1];
					range[2] = range[2] < XMax ? XMax : range[2];
					range[3] = range[3] < adfGeoTransform[3] ? adfGeoTransform[3] : range[3];
				}
				hDataset.delete();
			} else {
				resultFlag = false;
			}
		}
		if (resultFlag) {
			Configuration conf = HBaseConfiguration.create();
			HTable metaHTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getByteVal());

			String cdiGTPath = GtDataUtils.format2GtPath(cdiPath);
			Get get = new Get(cdiGTPath.getBytes());
			Result result = metaHTable.get(get);
			if (!result.isEmpty()) {
				byte[] url = result.getValue(GtDataConfig.META.FAMILY.byteVal, GtDataConfig.META.URL.byteVal);
				if (url != null && url.length > 0) {
					HTable resHTable = new HTable(conf, GtDataConfig.TABLE_NAME.RES_TABLE.getByteVal());
					Get resGet = new Get(url);
					result = resHTable.get(resGet);
					if (!result.isEmpty()) {
						byte[] data = result.getValue(GtDataConfig.RESOURCE.FAMILY.byteVal,
								GtDataConfig.RESOURCE.DATA.byteVal);
						if (data != null && data.length > 0) {
							String dataStr = new String(data);
							int indexStart = 0, indexEnd = 0;
							indexStart = dataStr.indexOf("<XMin>");
							indexEnd = dataStr.indexOf("</XMin>");
							double xmin = Double.parseDouble(dataStr.substring(indexStart + 6, indexEnd));
							indexStart = dataStr.indexOf("<XMax>");
							indexEnd = dataStr.indexOf("</XMax>");
							double xmax = Double.parseDouble(dataStr.substring(indexStart + 6, indexEnd));
							indexStart = dataStr.indexOf("<YMin>");
							indexEnd = dataStr.indexOf("</YMin>");
							double ymin = Double.parseDouble(dataStr.substring(indexStart + 6, indexEnd));
							indexStart = dataStr.indexOf("<YMax>");
							indexEnd = dataStr.indexOf("</YMax>");
							double ymax = Double.parseDouble(dataStr.substring(indexStart + 6, indexEnd));
							range[0] = range[0] < xmin ? range[0] : xmin;
							range[1] = range[1] < ymin ? range[0] : ymin;
							range[2] = range[2] > xmax ? range[0] : xmax;
							range[3] = range[3] > ymax ? range[0] : ymax;
						}
					}
					if (resHTable != null) {
						resHTable.close();
					}
				}
			}
			if (metaHTable != null) {
				metaHTable.close();
			}

		}
		return resultFlag;
	}

	/**
	 * 计算影像范围
	 * 
	 * @param fs
	 * @param path
	 * @param range
	 *            minX, minY, maxX, maxY
	 * @return
	 */
	public static boolean getImageRange(String dir, double[] range) {
		if (range.length < 4) {
			range = new double[4];
		}
		boolean resultFlag = false;
		Configuration conf = new Configuration();
		FileSystem fs;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		List<String> paths = new ArrayList<String>();
		Path path = new Path(dir);
		if (FileOperate.getPaths(fs, path, paths)) {
			gdal.AllRegister();
			gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
			gdal.SetConfigOption("SHAPE_ENCODING", "");
			for (int i = 0; i < paths.size(); i++) {
				Dataset hDataset = gdal.Open(paths.get(i), gdalconstConstants.GA_ReadOnly);
				double[] adfGeoTransform = hDataset.GetGeoTransform();
				int xsize = hDataset.GetRasterXSize();
				int ysize = hDataset.GetRasterYSize();
				double XMax = adfGeoTransform[0] + xsize * adfGeoTransform[1] + ysize * adfGeoTransform[2];
				double YMin = adfGeoTransform[3] + xsize * adfGeoTransform[4] + ysize * adfGeoTransform[5];
				if (i == 0) {
					range[0] = adfGeoTransform[0];
					range[1] = YMin;
					range[2] = XMax;
					range[3] = adfGeoTransform[3];
				} else {
					range[0] = range[0] > adfGeoTransform[0] ? adfGeoTransform[0] : range[0];
					range[1] = range[1] > YMin ? YMin : range[1];
					range[2] = range[2] < XMax ? XMax : range[2];
					range[3] = range[3] < adfGeoTransform[3] ? adfGeoTransform[3] : range[3];
				}
				hDataset.delete();
			}
			resultFlag = true;
		} else {
			resultFlag = false;
		}
		if (fs != null) {
			try {
				fs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return resultFlag;
	}

}
