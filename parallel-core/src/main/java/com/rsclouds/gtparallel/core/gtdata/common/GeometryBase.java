package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.osr.SpatialReference;

import com.rsclouds.gtparallel.core.common.CoreConfig;


public class GeometryBase {

	private Geometry geometry;
	public static final int WGS84 = 0;
	public static final int MERCATOR = 1;
	public static final int LOADMUTILGEO = 1;
	public static final int LOADGEO = 2;
	/**
	 * 打开矢量文件，返回一个多多边形矢量对象
	 * @param filename
	 * @return
	 */
	public GeometryBase() {
		geometry = null;
	}
	
	public GeometryBase(String filename) {
		geometry = loadMutilGeometry(filename);
	}
	
	public GeometryBase(Geometry geo) {
		this.geometry = geo;
	}
	
	/**
	 * 读取适量文件时，不遍历多边形矢量对象，直接返回图层中的所有矢量对象合并后的矢量对象
	 * @param filename
	 * @return
	 */
	public Geometry loadGeometry(String filename) {
		ogr.RegisterAll();		
		DataSource ds =  ogr.Open(filename, gdalconstConstants.GA_ReadOnly);
		if(ds == null) {
			return null;
		}
		Layer oLayer = ds.GetLayer(0);
		if(oLayer == null) {
			ds.delete();
			return null;
		}
		Feature oFeature = null;
		Geometry oGeometryMutil = null;
		while( (oFeature = oLayer.GetNextFeature()) != null ) {
			Geometry oGeometryTemp = oFeature.GetGeometryRef(); 
			if(oGeometryTemp == null) {
				oGeometryMutil = null;
				break;
			}
			if(oGeometryMutil == null) {
				oGeometryMutil = oGeometryTemp;
				if(oLayer.GetSpatialRef() != null) {
					oGeometryMutil.AssignSpatialReference(oLayer.GetSpatialRef());
				}
			}else {
				oGeometryMutil = oGeometryMutil.Union(oGeometryTemp);
			}	
		}
		
		if(oGeometryMutil != null && oGeometryMutil.GetGeometryCount() == 0) {
			oGeometryMutil = null;
		}
		if(oLayer.GetSpatialRef() != null) {
			oGeometryMutil.AssignSpatialReference(oLayer.GetSpatialRef());
		}
		ds.delete();
		return oGeometryMutil;
	}
	
	public  Geometry loadMutilGeometry(String filename) {
//		gdal.SetConfigOption("GDAL_DATA", "D://ProgramFiles//lib//gdal-1.9.2//data");
		ogr.RegisterAll();
		//filename = "F://test.geojson";
//		CPLSetConfigOption("GDAL_DATA","d:\\gdal-1.9.0\\data");
		
		DataSource ds =  ogr.Open(filename, gdalconstConstants.GA_ReadOnly);
		if(ds == null) {
			return null;
		}
		Layer oLayer = ds.GetLayer(0);
		if(oLayer == null) {
			ds.delete();
			return null;
		}
		Feature oFeature = null;
		Geometry oGeometryMutil = null;
		while( (oFeature = oLayer.GetNextFeature()) != null ) {
			Geometry oGeometryTemp = oFeature.GetGeometryRef(); 
			if(oGeometryTemp == null) {
				oGeometryMutil = null;
				break;
			}
			int eType = oGeometryTemp.GetGeometryType();
			if(eType == ogr.wkbMultiPolygon) {
				int count = oGeometryTemp.GetGeometryCount();
				for(int i = 0; i < count; i ++) {
					if(oGeometryMutil == null) {
						oGeometryMutil = oGeometryTemp.GetGeometryRef(i);
						if(oLayer.GetSpatialRef() != null) {
							oGeometryMutil.AssignSpatialReference(oLayer.GetSpatialRef());
						}
					}else {
						oGeometryMutil = oGeometryMutil.Union(oGeometryTemp.GetGeometryRef(i));
					}
				}
				
			}else if (eType == ogr.wkbPolygon) {
				if(oGeometryMutil == null) {
					oGeometryMutil = oGeometryTemp;
					if(oLayer.GetSpatialRef() != null) {
						oGeometryMutil.AssignSpatialReference(oLayer.GetSpatialRef());
					}
				}else {
					oGeometryMutil = oGeometryMutil.Union(oGeometryTemp);
				}
			}else if (eType == ogr.wkbGeometryCollection) {
				if(oGeometryMutil == null) {
					int count = oGeometryTemp.GetGeometryCount();
					for(int i = 0; i < count; i ++) {
						System.out.println(oGeometryTemp.GetGeometryRef(i).GetGeometryName());
						if(oGeometryMutil == null) {
							oGeometryMutil = oGeometryTemp.GetGeometryRef(i);
							if(oLayer.GetSpatialRef() != null) {
								oGeometryMutil.AssignSpatialReference(oLayer.GetSpatialRef());
							}
						}else {
							oGeometryMutil = oGeometryMutil.Union(oGeometryTemp.GetGeometryRef(i));
						}
					}
				}else {
					oGeometryMutil = oGeometryMutil.Union(oGeometryTemp.GetGeometryRef(0));
				}
			}else {
				oGeometryMutil = oGeometryTemp;
				if (eType == ogr.wkbPolygon25D) {
					System.out.println("wkbPolygon25D");
					oGeometryMutil.FlattenTo2D();
				}else if (eType == ogr.wkbMultiPolygon25D) {
					oGeometryMutil.FlattenTo2D();
					System.out.println("wkbMultiPolygon25D");
				}else {
					oGeometryMutil = null;
					break;
				}
				
			}
		}
		
		if(oGeometryMutil != null && oGeometryMutil.GetGeometryCount() == 0) {
			oGeometryMutil = null;
		}
		if(oLayer.GetSpatialRef() != null) {
			oGeometryMutil.AssignSpatialReference(oLayer.GetSpatialRef());
		}
		ds.delete();
		return oGeometryMutil;
	}
	
	
	public void ImageRowCol2Projection(double[] adfGeoTransform, int[] col, int[]row, double[] x, double[] y) {
		int length = row.length > col.length ? col.length : row.length;
		for(int i = 0; i < length; i ++) {
			x[i] = adfGeoTransform[0] + adfGeoTransform[1] * col[i] + adfGeoTransform[2] * row[i];
			y[i] = adfGeoTransform[3] + adfGeoTransform[4] * col[i] + adfGeoTransform[5] * row[i];
		}
	}
	
	public Geometry createPolygon(double[] x, double[]y, int projectType) {
		Geometry polygon = null;
		StringBuilder wkt = new StringBuilder("POLYGON ((");
		int length = x.length > y.length ? y.length : x.length;
		for(int i = 0; i < length; i ++) {
			wkt.append(x[i]);
			wkt.append(" ");
			wkt.append(y[i]);
			wkt.append(",");
		}
		wkt.append(x[0] + " " + y[0] + "))");
		polygon = Geometry.CreateFromWkt(wkt.toString());
		if (projectType == WGS84) {
			SpatialReference reference = new SpatialReference("");
			reference.SetWellKnownGeogCS("WGS84");
			polygon.AssignSpatialReference(reference);
		}else if (projectType == MERCATOR) {
			SpatialReference reference = new SpatialReference("");
//			reference.SetWellKnownGeogCS(CoreConfig.MERCATOR_PROJECT);
			reference.ImportFromEPSG(3857);
			polygon.AssignSpatialReference(reference);
		}	
		return polygon;
	}
	
	public boolean Union(double[] adfGeoTransform, int[] col, int[]row, int projectType) {
		double[] x = new double[col.length];
		double[] y = new double[row.length];
		ImageRowCol2Projection(adfGeoTransform, col, row, x, y);
		Geometry geo = createPolygon(x, y, projectType);
		if (this.geometry == null) {
			this.geometry = geo;
		}else {
			this.geometry = this.geometry.Union(geo);
		}
		if(this.geometry == null) {
			return false;
		}else {
			return true;
		}
	}
	
	public boolean uniou(Geometry geo) {
		if (this.geometry == null) {
			this.geometry = geo;
		}else {
			this.geometry = this.geometry.Union(geo);
		}
		if(this.geometry == null) {
			return false;
		}else {
			return true;
		}
	}
	

	/**
	 * 根据瓦片的行列号计算瓦片的矢量范围并进行合并
	 * @param adfGeoTransform
	 * @param colNum 
	 * @param rowNum
	 * @return
	 */
	public boolean Union(int layers, long colNum, long rowNum, double key_tileOrigin_x, double key_tileOrigin_y, int projectType) {
		double dstResolution = 156543.033928/(Math.pow(2, layers));
		double[] x = new double[4];
		double[] y = new double[4];
		x[0] = colNum * dstResolution * CoreConfig.WIDTH_DEFAULT - key_tileOrigin_x;
		y[0] = key_tileOrigin_y - rowNum * dstResolution * CoreConfig.HEIGHT_DEFAULT;
		
		x[1] = (colNum+1) * dstResolution * CoreConfig.WIDTH_DEFAULT - key_tileOrigin_x;
		y[1] = y[0];
		
		x[2] = x[1];
		y[2] = key_tileOrigin_y - (rowNum+1) * dstResolution * CoreConfig.HEIGHT_DEFAULT;
			
		x[3] = x[0];
		y[3] = y[2];
		
		Geometry geo = createPolygon(x, y, projectType);
		if (this.geometry == null) {
			this.geometry = geo;
		}else {
			this.geometry = this.geometry.Union(geo);
		}
		if(this.geometry == null) {
			return false;
		}else {
			return true;
		}
	}
	
	/**
	 * 根据瓦片的行列号计算瓦片的矢量范围并进行合并
	 * @param adfGeoTransform
	 * @param colNum 
	 * @param rowNum
	 * @return
	 */
	public Geometry createPolygon(double dstResolution, long colNum, long rowNum, 
			double key_tileOrigin_x, double key_tileOrigin_y, int projectType) {
		double[] x = new double[4];
		double[] y = new double[4];
		x[0] = colNum * dstResolution * CoreConfig.WIDTH_DEFAULT - key_tileOrigin_x;
		y[0] = key_tileOrigin_y - rowNum * dstResolution * CoreConfig.HEIGHT_DEFAULT;
		
		x[1] = x[0] + dstResolution * 256;
		y[1] = y[0];
		
		x[2] = x[1];
		y[2] = y[0] - dstResolution * 256;
		
		x[3] = x[0];
		y[3] = y[2];
		Geometry geo = createPolygon(x, y, projectType);
		return geo;
	}	
	
	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	public String getJSON(){
		if(geometry != null)
			return this.geometry.ExportToJson();
		else {
			return null;
		}
	}
	
	public String getWkt() {
		if(geometry != null) {
			return this.geometry.ExportToWkt();
		}else {
			return null;
		}
	}
	
	public void Union(Geometry geo) {
		if (this.geometry == null) {
			this.geometry = geo;
		}else {
			this.geometry = this.geometry.Union(geo);
		}
	}
	
	public void Union(String geometryLocalPathTemp) {
		Geometry geo = loadMutilGeometry(geometryLocalPathTemp);
		if (geo == null)
		{
			System.out.println("read file error" + geometryLocalPathTemp);
		}
		this.geometry = this.geometry.Union(geo);
	}
	
	/**
	 * 读取适量文件时
	 * @param geometryLocalPath
	 * @param type 若果  type==GeometryBase.LOADGEO 读取适量文件时，不遍历多边形矢量对象，直接返回图层中的所有矢量对象合并后的矢量对象
	 * @return
	 */
	public boolean union(String geometryLocalPath, int type) {
		Geometry geo = null;
		if (type == GeometryBase.LOADGEO) {
			geo = loadGeometry(geometryLocalPath);
		}else if (type == GeometryBase.LOADMUTILGEO) {
			geo = loadMutilGeometry(geometryLocalPath);
		}
		if (geo == null) {
			return false;
		}
		if (this.geometry == null) {
			this.geometry = geo;
		}else {
			this.geometry = this.geometry.Union(geo);
		}
		return true;
	}
	
	public boolean writeToLocalFile(String outputPath) {
		if(this.geometry == null) {
			return false;
		}
		
		File file = new File(outputPath);
		OutputStream out = null;
		boolean bresult = false;
		try {
			out = new FileOutputStream(file);
			out.write(this.geometry.ExportToJson().getBytes());
			bresult = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				bresult = false;
			}
		}
		return bresult;
	}
	
	public static void main(String[] args) throws IOException {}
	
}
