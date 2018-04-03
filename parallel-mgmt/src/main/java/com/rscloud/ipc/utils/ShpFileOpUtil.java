package com.rscloud.ipc.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;

public class ShpFileOpUtil {
	/**
	 * 
	* Description: TODO
	*  @param fileContent 
	*  @param fileName
	*  @param tmpDir
	*  @return 
	* @author lishun 
	* @date 2017年7月4日 
	* @return Map<String,Object>
	 */
	public static Map<String, Object> readFileAndPareseByStream(byte[] fileContent, String fileName,String tmpDir) {
		InputStream in = new ByteArrayInputStream(fileContent);
		String shpFilePath = null;
		Map<String, Object> map = new HashMap<String, Object>();
		File zipFile = new File(tmpDir + File.separator + fileName);
		try {
			if(!new File(tmpDir).exists()){//先创建文件夹
				new File(tmpDir).mkdirs();
			}
			if (!zipFile.exists()) {
				zipFile.createNewFile();
			}
			FileUtils.copyInputStreamToFile(in, zipFile);
		} catch (IOException e) {
			e.printStackTrace();
			map.put("code", 0);
			map.put("message", "文件流传输异常，请重试！");
			FileUtils.deleteQuietly(zipFile);
			return map;
		}
		
		int lastIndex = 0;
		if (zipFile.exists()) {// 解压缩压缩包文件
			try {
				lastIndex = zipFile.getPath().lastIndexOf(".");// 文件上传成功后，解压缩当前压缩包
				FileZipUtil.readByApacheZipFile(zipFile.getAbsolutePath(),zipFile.getPath().substring(0, lastIndex));// 文件解压缩
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 判断解压缩的后的文件夹是否存在，存在则解压缩成功
			File unzip = new File(zipFile.getPath().substring(0, lastIndex));
			if (unzip.exists() && unzip.isDirectory()) {
				// 文件压缩成功后，先验证shp文件压缩包内文件是否包含（*.shp,*.dbf,*.shx）解析shp文件，返回文件存储的坐标点以json形式返回

				File[] files = unzip.listFiles();
				String shpFileName = getFileNameIsNull(files, "shp");
				String shxFileName = getFileNameIsNull(files, "shx");
				String dbfFileName = getFileNameIsNull(files, "dbf");

				if (StringUtils.isNoneBlank(shpFileName) && StringUtils.isNoneBlank(shxFileName)&& StringUtils.isNoneBlank(dbfFileName)) {
					shpFilePath = getFilePathByFileBox(files);
					if (shpFilePath != null && !shpFilePath.equals("")) {
						map.putAll(getShpsPointShowPointRetuabBf(shpFilePath));
					} else {
						map.put("code", 0);
						map.put("message", "找不到shp文件！");
					}
					FileTools.delete(zipFile);
					FileTools.delete(unzip);
				} else {
					String message = "压缩包必须包含（*.shp,*.dbf,*.shx）文件  ";
					if (shpFileName == null || shpFileName.equals("")) {
						message += ",  缺少.shp 文件！";
					}
					if (shxFileName == null || shxFileName.equals("")) {
						message += ",  缺少.shx 文件！";
					}
					if (dbfFileName == null || dbfFileName.equals("")) {
						message += ",  缺少.dbf 文件！";
					}
					map.put("code", 0);
					map.put("message", message);
				}
			} else {
				map.put("code", 0);
				map.put("message", "文件解压缩失败！");
			}
		} else {
			map.put("code", 0);
			map.put("message", "文件上传失败！");
		}

		return map;
	}

	/**
	 * Description：遍历文件最后一层，返回shp文件路径
	 * 
	 * @param files
	 *            文件夹内所有文件
	 * @return shp文件路径
	 */
	public static String getFilePathByFileBox(File[] files) {
		String shpFilePath = null;
		if (files != null && files.length > 0) {
			for (File ff : files) {
				if (shpFilePath == null) {
					if (ff.isDirectory()) {
						files = ff.listFiles();
						shpFilePath = getFilePathByFileBox(files);
					} else {
						if ((ff.getName().substring(
								ff.getName().lastIndexOf(".") + 1, ff.getName()
										.length())).toLowerCase().equals("shp")) {
							shpFilePath = ff.getAbsolutePath();
							return shpFilePath;
						}
					}
				} else {
					break;
				}
			}
		}
		return shpFilePath;
	}

	/**
	 * Description：遍历文件最后一层，返回shp文件路径
	 * 
	 * @param files
	 *            文件夹内所有文件
	 * @return shp文件路径
	 */
	public static String getFileNameIsNull(File[] files, String fileLastName) {
		String fileName = null;
		if (files != null && files.length > 0) {
			for (File ff : files) {
				if (fileName == null) {
					if (ff.isDirectory()) {
						files = ff.listFiles();
						fileName = getFilePathByFileBox(files);
					} else {
						if ((ff.getName().substring(
								ff.getName().lastIndexOf(".") + 1, ff.getName()
										.length())).toLowerCase().equals(
								fileLastName)) {
							fileName = ff.getName();
							return fileName;
						}
					}
				} else {
					break;
				}
			}
		}
		return fileName;
	}

	/**
	 * Description：读取shp文件，获取文件内所有点坐标信息，返回前端在页面显示
	 * 
	 * @param shpFilePath
	 *            shp文件路径
	 * @return
	 */
	public static Map<String, Object> getShpsPointShowPointRetuabBf(
			String shpFilePath) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (shpFilePath != "" && shpFilePath != null) {
			// 注册所有的驱动
			ogr.RegisterAll();
			// 为了支持中文路径，请添加下面这句代码
			gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
			// 为了使属性表字段支持中文，请添加下面这句
			gdal.SetConfigOption("SHAPE_ENCODING", "");
			// 打开数据
			DataSource ds = ogr.Open(shpFilePath, 0);
			if (ds == null) {
				map.put("code", "0");
				map.put("message", "注册gdal驱动失败");
			} else {
				// 获取该数据源中的第一个图层，一般shp数据图层只有一个，如果是mdb、dxf等图层就会有多个
				Layer oLayer = ds.GetLayerByIndex(0);
				if (oLayer == null) {
					map.put("code", "0");
					map.put("message", "获取图层失败！");
				} else {
					// 对图层进行初始化，如果对图层进行了过滤操作，执行这句后，之前的过滤全部清空
					oLayer.ResetReading();
					// 获取要素中的几何体应转换json数据格式返回
					Geometry oGeometry = oLayer.GetFeature(0).GetGeometryRef();
					// System.out.println(oGeometry.ExportToJson());
					map.put("code", "1");
					map.put("message", "查询数据成功！");
					map.put("data", oGeometry.ExportToWkt());
					// System.out.println(oGeometry.ExportToWkt());
				}
			}
		} else {
			map.put("code", "0");
			map.put("message", " 存储文件路径为空！");
		}
		return map;
	}
	
	public static Map<String, Object> WriteVectorFileZip(String geomWKT,String tmpDir) {
		Map<String, Object> map=new HashMap<String, Object>();
		ogr.RegisterAll();
		// 为了支持中文路径，请添加下面这句代码
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "NO");
		// 为了使属性表字段支持中文，请添加下面这句
		gdal.SetConfigOption("SHAPE_ENCODING", "");
		// 创建数据，这里以创建ESRI的shp文件为例
		String strDriverName = "ESRI Shapefile";
		org.gdal.ogr.Driver oDriver = ogr.GetDriverByName(strDriverName);
		if (oDriver == null) {
			map.put("code", "0");
			map.put("message", strDriverName + " 驱动不可用！\n");
			return  map;
		}
		//String uploadTmpFile=((PropertyOputils) SpringContextUtil.getBean("systemProperty")).getKeyValue("shapeFilePath");
		String randomfilerlpath = UUID.randomUUID().toString() ;
		if(!new File(tmpDir).exists()){//先临时保存创建文件夹
			new File(tmpDir).mkdirs();
		}
		File filesfile = new File(tmpDir + File.separator + randomfilerlpath);
		if (!filesfile.exists()) {
			filesfile.mkdir();
		}
		String strVectorFile=tmpDir + File.separator + randomfilerlpath+ File.separator + randomfilerlpath+".shp";
		// 创建数据源
		DataSource oDS = oDriver.CreateDataSource(strVectorFile, null);
		if (oDS == null) {
			map.put("code", "0");
			map.put("message", "创建矢量文件【" + strVectorFile + "】失败！\n");
			return  map;
		}

		// 创建图层，创建一个多边形图层，这里没有指定空间参考，如果需要的话，需要在这里进行指定
		Layer oLayer = oDS.CreateLayer("Polygon", null, ogr.wkbPolygon,
				null);
		if (oLayer == null) {
			map.put("code", "0");
			map.put("message", "图层创建失败！\n");
			return  map;
		}
		FeatureDefn oDefn = oLayer.GetLayerDefn();
		// 创建三角形要素
		Feature oFeatureTriangle = new Feature(oDefn);
		Geometry geomTriangle = Geometry.CreateFromWkt(geomWKT);
		oFeatureTriangle.SetGeometry(geomTriangle);
		oLayer.CreateFeature(oFeatureTriangle);
		oLayer.SyncToDisk();
		File zipFile=new File(filesfile.getAbsolutePath()+".zip");
		map= compress( filesfile.getAbsolutePath(), zipFile );
		oDS.delete();
		deleteDir(filesfile);
		return map;
	}
	public static Map<String, Object> compress(String srcPathName,File zipFile ) {
		Map<String, Object> map=new HashMap<String, Object>();
		File srcdir = new File(srcPathName);
		if (!srcdir.exists())
			throw new RuntimeException(srcPathName + "不存在！");
		Project prj = new Project();
		Zip zip = new Zip();
		zip.setProject(prj);
		zip.setDestFile(zipFile);
		FileSet fileSet = new FileSet();
		fileSet.setProject(prj);
		fileSet.setDir(srcdir);
		//fileSet.setIncludes("**/*.java"); 包括哪些文件或文件夹 eg:zip.setIncludes("*.java");
		//fileSet.setExcludes(...); 排除哪些文件或文件夹
		zip.addFileset(fileSet);
		zip.execute();
		InputStream fis =null;
		try {
			if (zipFile.getTotalSpace()>0) {
				fis = new FileInputStream(zipFile);
				map.put("code", "1");
				map.put("message", "成功生成shp压缩包！");
				map.put("FileInputStream",  fis);
				map.put("FileName",  zipFile.getName());
				return map;
			}else {
				map.put("code", "0");
				map.put("message", "生成shp压缩包失败！");
				return map;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			map.put("code", "0");
			map.put("message", "生成shp压缩包发生异常！");
			return map;
		}
	}
	  private static boolean deleteDir(File dir) {
	        if (dir.isDirectory()) {
	            String[] children = dir.list();
	            //递归删除目录中的子目录下
	            for (int i=0; i<children.length; i++) {
	                boolean success = deleteDir(new File(dir, children[i]));
	                if (!success) {
	                    return false;
	                }
	            }
	        }
	        // 目录此时为空，可以删除
	        return dir.delete();
	    }
	public static void main(String[] args) {
		/*File zipFile=new File("F:/temppath/e0d166d4-c6f4-4948-8ecb-85fc8655f870/e0d166d4-c6f4-4948-8ecb-85fc8655f870.zip");
		ShpFileOpUtil.compress("F:/temppath/e0d166d4-c6f4-4948-8ecb-85fc8655f870", zipFile );*/
		//ShpFileOpUtil.createVectorFileZip("POLYGON ((30 0,60 0,60 30,30 30,30 0))");
		Map<String, Object> map = ShpFileOpUtil.getShpsPointShowPointRetuabBf("F:/temppath/hunan/hunan.shp");
		System.out.println(map);
		//ShpFileOpUtil.deleteDir(fielFile2);
		
	}
}
