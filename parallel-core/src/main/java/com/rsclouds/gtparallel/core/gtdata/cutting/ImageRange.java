package com.rsclouds.gtparallel.core.gtdata.cutting;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import com.rsclouds.gtparallel.core.common.CoreConfig;

/**
 * 存放影像的路径和范围
 * @author root
 *
 */
public class ImageRange  implements Comparable<ImageRange> {
	private String path;    //影像路径
	private double minLat;  //影像最小纬度
	private double maxLat;  //影像最大纬度
	private double minLon;  //影像最小经度
	private double maxLon; //影像最大经度
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public double getMinLat() {
		return minLat;
	}
	public void setMinLat(double minLat) {
		this.minLat = minLat;
	}
	public double getMaxLat() {
		return maxLat;
	}
	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
	}
	public double getMinLon() {
		return minLon;
	}
	public void setMinLon(double minLon) {
		this.minLon = minLon;
	}
	public double getMaxLon() {
		return maxLon;
	}
	public void setMaxLon(double maxLon) {
		this.maxLon = maxLon;
	}

	public int compareTo(ImageRange tmp) {
		if (this.maxLat - tmp.getMaxLat() > 0.000001) {
			return 1;
		}else if (tmp.getMaxLat() -this.maxLat > 0.000001) {
			return -1;
		}else {
			if (tmp.getMinLon() - this.minLon > 0.000001) {
				return 1;
			}else if (this.minLon - tmp.getMinLon()  > 0.000001) {
				return -1;
			}else {
				return 0;
			}
		}
	}
	
	public static void main(String[] args) {
//		gdal.AllRegister();
//		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
//		gdal.SetConfigOption("SHAPE_ENCODING", "");
//		String filename = "D://nanlin//image//nanyang//L15-3312E-2446N//L15-3312E-2446N.tif";
//		Dataset dataset = gdal.Open(filename, gdalconstConstants.GA_ReadOnly);
//		double[] adfGeoTransform = dataset.GetGeoTransform();
//		double tileOriginY = CoreConfig.VALUE_TILEORIGIN_Y_MERCATOR;
//		double dstResolution = 156543.033928/Math.pow(2, 12);
//		long tilerowTopLeft = (long) Math.floor((tileOriginY - adfGeoTransform[3]) / CoreConfig.HEIGHT_DEFAULT / dstResolution);
//		System.out.println(tilerowTopLeft);
		System.out.println(Math.ceil(10.000000000000001));
	}
}
