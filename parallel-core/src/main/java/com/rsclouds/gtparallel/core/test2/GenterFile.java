package com.rsclouds.gtparallel.core.test2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.gtdata.utills.MD5Calculate;

public class GenterFile {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		byte[] uuid = null;
		byte[] data = null;
		int size = 1024 * 1024; 
		for(int i=0;i<490;i++){
			uuid = Bytes.toBytes(UUID.randomUUID().toString());
			ByteArrayOutputStream bais = new ByteArrayOutputStream(size);
			for(int j=0;j<size/uuid.length;j++){
				IOUtils.write(uuid, bais);	
			}
			bais.write(uuid, 0, size%uuid.length);
			data = bais.toByteArray();
			String md5 = MD5Calculate.fileByteMD5(data);			
			File file = new File("E:/myJob/md5files",md5);
			FileUtils.writeByteArrayToFile(file, data);
			System.out.println("built md5 file :" + md5 +",size :" + data.length);
		}
		
//		for(int i=0;i<1;i++){
//			byte[] data = Bytes.toBytes(UUID.randomUUID().toString());
//			String md5 = MD5Calculate.fileByteMD5(data);
//			System.out.println("data - md5 : " + md5);
//			File file = new File("E:/myJob/md5files",md5);
//			for(int j = 0; j < 1024/36 ;j++){
//				FileUtils.writeByteArrayToFile(file, data,true);
//			}		
//		}
		

	}

}
