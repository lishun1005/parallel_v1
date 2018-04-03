package com.rsclouds.gtparallel.gtdata.utills;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * 计算文件md5值工具类
 * @author root
 *
 */
public class MD5Calculate {

	private static String byteArrayToHex(byte[] byteArray) {
		StringBuilder hs = new StringBuilder();
		String stmp = "";
		for (int n = 0; n < byteArray.length; n++) {
			stmp = (Integer.toHexString(byteArray[n] & 0XFF));
			if (stmp.length() == 1) {
				hs.append("0" + stmp);
			} else {
				hs.append(stmp);
			}
			if (n < byteArray.length - 1) {
				hs.append("");
			}
		}
		return hs.toString();
	}

	public static String LocalfileMD5(String inputFile) throws IOException {
		int bufferSize = 256 * 1024;
		FileInputStream fileInputStream = null;
		DigestInputStream digestInputStream = null;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			fileInputStream = new FileInputStream(inputFile);

			digestInputStream = new DigestInputStream(fileInputStream,
					messageDigest);
			byte[] buffer = new byte[bufferSize];
			while (digestInputStream.read(buffer) > 0)
				;
			messageDigest = digestInputStream.getMessageDigest();
			byte[] resultByteArray = messageDigest.digest();
			return byteArrayToHex(resultByteArray);
		} catch (NoSuchAlgorithmException e) {
			return null;
		} finally {
			try {
				digestInputStream.close();
			} catch (Exception e) {
			}
			try {
				fileInputStream.close();
			} catch (Exception e) {
			}
		}

	}

	public static String HDFSfileMD5(FileSystem fs, Path path)
			throws IOException {
		int bufferSize = 256 * 1024;
		FSDataInputStream in = null;
		DigestInputStream digestInputStream = null;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");

			in = fs.open(path);
			digestInputStream = new DigestInputStream(in, messageDigest);

			byte[] buffer = new byte[bufferSize];
			while (digestInputStream.read(buffer) > 0)
				;
			messageDigest = digestInputStream.getMessageDigest();
			byte[] resultByteArray = messageDigest.digest();
			return byteArrayToHex(resultByteArray);
		} catch (NoSuchAlgorithmException e) {
			return null;
		} finally {
			try {
				digestInputStream.close();
			} catch (Exception e) {
			}
			try {
				in.close();
			} catch (Exception e) {
			}
		}
	}

	public static String fileByteMD5(byte[] filebyte) {
		MessageDigest messageDigest;
		DigestInputStream digestInputStream = null;
		InputStream in = null;
		int bufferSize = 256 * 1024;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			in = new ByteArrayInputStream(filebyte);
			digestInputStream = new DigestInputStream(in, messageDigest);

			byte[] buffer = new byte[bufferSize];
			while (digestInputStream.read(buffer) > 0)
				;
			messageDigest = digestInputStream.getMessageDigest();
			byte[] resultByteArray = messageDigest.digest();
			return byteArrayToHex(resultByteArray);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				digestInputStream.close();
			} catch (Exception e) {
			}
			try {
				in.close();
			} catch (Exception e) {
			}
		}

	}
	
	public static void main(String[] args) throws Exception {
//		System.out.println(System.currentTimeMillis());
//		System.out.println(LocalfileMD5("D://VMware-workstation-full-7.1.1-282343.zip"));
//		System.out.println(System.currentTimeMillis());
//		String filePath = "/test/_alllayers/L16/C4564321646";
//		if(filePath.contains("_alllayers/L")){
//			int index = filePath.indexOf("_alllayers/L")+11;
//			if(index + 3 < filePath.length()){
//				int x = Integer.valueOf(filePath.substring(index+1, index+3));
//				if(x >= 10){
//					String s = Integer.toHexString(x);
//					if(s.length()<2){
//						s = "0" + s;
//					}
//					filePath = filePath.replace("/L"+x, "/L"+s);
//				}					
//			}					
//		}
//		System.out.println(filePath);
		System.out.println(LocalfileMD5("E:/myJob/bbbb.tiff"));
	}
}
