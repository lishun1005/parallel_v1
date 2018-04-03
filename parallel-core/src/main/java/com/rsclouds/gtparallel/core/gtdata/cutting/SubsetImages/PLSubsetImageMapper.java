package com.rsclouds.gtparallel.core.gtdata.cutting.SubsetImages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.common.GdalDatasetBase;
import com.rsclouds.gtparallel.core.hadoop.io.FileInfo;
import com.rsclouds.gtparallel.core.hadoop.io.PLSubsetImageInfo;
import com.rsclouds.gtparallel.core.hadoop.io.PLSubsetMapRecord;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;
import com.rsclouds.gtparallel.gtdata.utills.TransCoding;

public class PLSubsetImageMapper extends Mapper<PLSubsetMapRecord, NullWritable, Text, FileInfo>{
	public static final Logger LOG = LoggerFactory.getLogger(PLSubsetImageMapper.class);
	
	private boolean wartermarkFlag;
	private boolean saveStorageBool;
	private boolean bCover;
	private int nodataInt;
	private int widthRang;
	private int heightRang;
	private String fileSuffixStr;
	private HTable resourceTable;
	private HTable metadataTable;
	private int[] intZeroAreay;
	private BufferedImage bufferImag;
	int[] rgbArrayTemp;
	private double zeroMaxPercentage = 1.0;
	private boolean zeroFlag;
	private PLSubsetGdalDataset plSubsetDataset;
	private Text path = new Text();
	private FileInfo fileInfo = new FileInfo();
	private StringBuilder strBuilderMD5 = new StringBuilder();
	private String outputpath = null;	//瓦片输出路径
	private int pictureFormatInt;
	//瓦片行列号
	private StringBuilder colStrBuilder = new StringBuilder();
	private StringBuilder rowStrBuilder = new StringBuilder();	
	
	public void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		wartermarkFlag = conf.getBoolean(CoreConfig.KEY_WARTERMARK, true);
		saveStorageBool = conf.getBoolean(PLSubsetImageSegement.SAVE_STORAGE_BOOLEAN, false);
		nodataInt = conf.getInt("NO_DATA", -1);
		bCover = conf.getBoolean("bcover", true);
		fileSuffixStr = conf.get(PLSubsetImageSegement.KEY_FILE_FORMAT_STRING, ".png");
		zeroMaxPercentage = conf.getDouble(CoreConfig.KEY_ZERO_PERCENTAGE_INT, 1.1);
		if (zeroMaxPercentage > 1.0) {
			zeroFlag = false;
		}else {
			zeroFlag = true;
		}
		
		resourceTable = new HTable(conf, CoreConfig.MAP_RES_TABLE);
		metadataTable = new HTable(conf, CoreConfig.MAP_META_TABLE);
		resourceTable.setAutoFlushTo(false);
		widthRang = CoreConfig.WIDTH_DEFAULT;
		heightRang = CoreConfig.HEIGHT_DEFAULT;
		
		intZeroAreay = new int[widthRang*heightRang];
		rgbArrayTemp = new int[widthRang * heightRang];
		bufferImag = new BufferedImage(widthRang, heightRang, BufferedImage.TYPE_INT_ARGB_PRE);
		int[] bands = {1,2,3};
		plSubsetDataset = new PLSubsetGdalDataset(zeroMaxPercentage, widthRang, heightRang, bands, nodataInt);
		outputpath = conf.get(CoreConfig.CUTTING_OUTPUTPATH);
		while (outputpath.endsWith("/")) {
			outputpath = outputpath.substring(0, outputpath.length()-1);
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
	
	public boolean output(Context context, String outputLayerPath) throws IOException, InterruptedException{
//		String LayerName = outputLayerPath.substring(outputLayerPath.length()-4, outputLayerPath.length()) + "/";
		plSubsetDataset.resetBufferedImage();
		plSubsetDataset.setBufferedImage(0, 0, widthRang, heightRang, pictureFormatInt);
		path.set(outputLayerPath + rowStrBuilder.toString());
		if (!bCover) {
			Get getTemp = new Get(Bytes.toBytes(TransCoding.decode(path.toString() + "//" + colStrBuilder.toString()+fileSuffixStr, "UTF-8")));
//			System.out.println(path.toString() + "//" + colStrBuilder.toString()+fileSuffixStr);
			Result resultTemp = metadataTable.get(getTemp);
			if (!resultTemp.isEmpty()) {
//				System.out.println("has exit ");
				byte[] md5Byte = resultTemp.getValue(Bytes.toBytes("atts"), Bytes.toBytes("url"));
				getTemp = new Get(md5Byte);
				resultTemp = resourceTable.get(getTemp);
				ImageIcon imageIcon = new ImageIcon(resultTemp.getValue(Bytes.toBytes("img"), Bytes.toBytes("data")));
				bufferImag.setRGB(0, 0, widthRang, heightRang, intZeroAreay, 0, widthRang);
				Graphics2D gs = (Graphics2D) bufferImag.getGraphics();
				gs.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
				gs.dispose();
				
				bufferImag.getRGB(0, 0, widthRang, heightRang, rgbArrayTemp, 0, widthRang);
				if (plSubsetDataset.merge(rgbArrayTemp, widthRang, heightRang)){
					pictureFormatInt = GdalDatasetBase.PNG_FORMAT_INT;
				}else {
					pictureFormatInt = GdalDatasetBase.JPEG_FORMAT_INT;
				}
				plSubsetDataset.setBufferedImage(0, 0, widthRang, heightRang, pictureFormatInt);
			}
		}
		if(wartermarkFlag) {
			plSubsetDataset.setWarterMask("ChinaRS中科遥感", "宋体", Font.BOLD|Font.ITALIC, 13, Color.white, -1, -1, 0.4f, pictureFormatInt);
		}
		
		strBuilderMD5.append(MD5Calculate.fileByteMD5(plSubsetDataset.getBufferImagedata(pictureFormatInt)));
		System.out.println(strBuilderMD5.toString());
		fileInfo.setMD5(strBuilderMD5.toString());
		fileInfo.setLength(plSubsetDataset.getBufferImageSize());
		fileInfo.setFilename(colStrBuilder.toString()+fileSuffixStr);
		Get get = new Get(Bytes.toBytes(strBuilderMD5.toString()));
		Result result = resourceTable.get(get);
		if (result == null || result.isEmpty()) {
			Put put = new Put(Bytes.toBytes(strBuilderMD5.toString()));
			put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
					GtDataConfig.RESOURCE.LINKS.byteVal, Bytes.toBytes("1"));
			put.add(GtDataConfig.RESOURCE.FAMILY.byteVal,
					GtDataConfig.RESOURCE.DATA.byteVal, plSubsetDataset.getBufferImagedata(pictureFormatInt));
			resourceTable.put(put);
		}
		context.write(path, fileInfo);
		strBuilderMD5.delete(0, strBuilderMD5.length());
		return true;
	}
	
	public void cleanup(Context context)throws IOException, InterruptedException {
		if (resourceTable != null) {
			resourceTable.flushCommits();
			resourceTable.close();
			resourceTable = null;
		}
		if (metadataTable != null) {
			metadataTable.flushCommits();
			metadataTable.close();
			metadataTable = null;
		}
	}
	
	
	public void map(PLSubsetMapRecord key, NullWritable value, Context context) throws IOException, InterruptedException {
		List<PLSubsetImageInfo> infos = key.getPerSplitPLSubsetInfoList();
		int size = infos.size();
		if (size > 0) {
			int layerOut = infos.get(0).getLayerOut();
			int layerOrg = infos.get(0).getLayerOrg();
			int colOrg = infos.get(0).getColOrg();
			int rowOrg = infos.get(0).getRowOrg();
			String layerName = Integer.toHexString(layerOut);
			if (layerName.length() == 1) {
				layerName = "L0" + layerName;
			}else {
				layerName = "L" + layerName;
			}
			String outputLayerPath = outputpath + "/" + layerName + "/";
			boolean changRGBFlag = true;
			if (layerOut < layerOrg) {
				int colOut = infos.get(0).getColOut();
				int rowOut = infos.get(0).getRowOut();
				setCol(colOut);
				setRow(rowOut);
				int titles = (int) Math.pow(2, (layerOrg -layerOut));
				int xsize = widthRang / titles;
				int ysize = heightRang / titles;
				if (xsize == 0) {
					xsize = 1;
				}
				if (ysize == 0) {
					ysize = 1;
				}
				plSubsetDataset.resetRGB();
				for (int i = 0; i < size; i ++) {
					String filePath = infos.get(i).getImagePath().toString();
					int colRemainder = infos.get(i).getColRemainder();
					int rowRemainder = infos.get(i).getRowRemainder();
					if (plSubsetDataset.readData(filePath, xsize, ysize)) {
//						changRGBFlag = plSubsetDataset.changRGB(xsize * colRemainder, ysize * rowRemainder, xsize, ysize, zeroFlag);
						plSubsetDataset.changRGB(xsize * colRemainder, ysize * rowRemainder, xsize, ysize);
					}
					plSubsetDataset.closeDataset();
					
				}
				changRGBFlag = plSubsetDataset.compareZeroPercentage();
				if (changRGBFlag && saveStorageBool) {
					pictureFormatInt = PLSubsetGdalDataset.JPEG_FORMAT_INT;
				}else {
					pictureFormatInt = PLSubsetGdalDataset.PNG_FORMAT_INT;
				}
				output(context, outputLayerPath);
				plSubsetDataset.resetNoneZeroPercentage();
			}else {
				String filePath = infos.get(0).getImagePath().toString();
				int titles = (int) Math.pow(2, (layerOut -layerOrg));
				int colOutOrg = colOrg * titles;
				int rowOutOrg = rowOrg * titles;
				for (int displacementRow = 0; displacementRow < titles; displacementRow ++) {
					setRow(rowOutOrg + displacementRow);
					for (int displacementCol = 0; displacementCol < titles; displacementCol ++) {
						plSubsetDataset.resetRGB();
						setCol(colOutOrg + displacementCol);
						if (plSubsetDataset.readData(filePath, displacementCol, displacementRow, titles, widthRang, heightRang)) {
							changRGBFlag = plSubsetDataset.changRGB(0, 0, widthRang, heightRang, zeroFlag);
							plSubsetDataset.resetNoneZeroPercentage();
							if (changRGBFlag && saveStorageBool) {
								pictureFormatInt = PLSubsetGdalDataset.JPEG_FORMAT_INT;
							}else {
								pictureFormatInt = PLSubsetGdalDataset.PNG_FORMAT_INT;
							}
							output(context, outputLayerPath);
						}
					}
				}
				plSubsetDataset.closeDataset();
			}
		}
	}
}
