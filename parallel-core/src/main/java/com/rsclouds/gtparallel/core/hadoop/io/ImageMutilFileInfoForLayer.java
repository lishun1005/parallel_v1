package com.rsclouds.gtparallel.core.hadoop.io;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;

/**
 * 存储切片层级所需要的文件信息，主要用于分幅影像数据切片
 * @author root
 *
 */
public class ImageMutilFileInfoForLayer {
	//影像的起始位置未必是瓦片的起始位置，需要计算偏移量
	private int tileColOffset;                           //瓦片左上角列偏移位置
	private int tileRowOffset;                           //瓦片左上角行偏移位置
	private int curentLayer = 0;                         //当前切片层级
	private long xreadOrigin;                            //从原始影像x坐标开始读取
	private long yreadOrigin;                            //从原始影像的y坐标开始读取
	private int  xreadOriginActual;                      //从原始影像x坐标开始读取
	private int  yreadOriginActual;                      //从原始影像的y坐标开始读取
	private long xreadLen;                               //实际读取宽度
	private long yreadLen;                               //实际读取长度
	private long colNum;                                 //瓦片列号
	private long rowNum;                                 //瓦片行号
	private double dstResolution;                        //切片层级对应分辨率
	private double oriResolution;	                     //原始影像分辨率
	private int[] bands;                                 //存放波段
	private Text gtdataOutputPath = new Text();          //存放瓦片输出路径
	private List<String>paths = new ArrayList<String>(); //存放分幅影像数据路径
	
	
}
