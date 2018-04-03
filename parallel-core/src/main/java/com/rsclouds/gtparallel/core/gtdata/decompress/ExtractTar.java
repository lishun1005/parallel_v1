package com.rsclouds.gtparallel.core.gtdata.decompress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * 解压tar.gz文件的核心算法
 *
 */
public class ExtractTar {

	private BufferedOutputStream bufferedOutputStream;
	private String zipfileName = null;

	public ExtractTar() {

	}

	public boolean unTargzFile(String targzFileName) {
		// 创建解压的文件目录
		this.zipfileName = targzFileName;
		int index = targzFileName.lastIndexOf("/");
		String name = targzFileName.substring(index + 1);
		name = trimExtension(name); // 去除.gz
		name = trimExtension(name);// 去除.tar
		String outputDirectory = targzFileName.substring(0, index)
				+ File.separator + name;
		File file = new File(outputDirectory);
		if (!file.exists()) {
			file.mkdir();
		}
		return unFile(outputDirectory);
	}

	// 解压文件到解压目录
	public boolean unFile(String outputDirectory) {
		FileInputStream fis = null;
		TarArchiveInputStream in = null;
		BufferedInputStream bufferedInputStream = null;
		try {
			fis = new FileInputStream(zipfileName);
			GZIPInputStream is = new GZIPInputStream(new BufferedInputStream(
					fis));
			in = (TarArchiveInputStream) new ArchiveStreamFactory()
					.createArchiveInputStream("tar", is);
			bufferedInputStream = new BufferedInputStream(in);
			TarArchiveEntry entry = in.getNextTarEntry();

			while (entry != null) {
				String name = entry.getName();
				String[] names = name.split("/");
				String fileName = outputDirectory;
				for (int i = 0; i < names.length; i++) {
					String str = names[i];
					fileName = fileName + File.separator + str;
				}
				if (name.endsWith("/")) {
					mkFolder(fileName);
				} else {
					File file = mkFile(fileName);
					bufferedOutputStream = new BufferedOutputStream(
							new FileOutputStream(file));
					int b;
					while ((b = bufferedInputStream.read()) != -1) {
						bufferedOutputStream.write(b);
					}
					bufferedOutputStream.flush();
					bufferedOutputStream.close();
				}
				entry = (TarArchiveEntry) in.getNextEntry();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ArchiveException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (bufferedInputStream != null) {
					bufferedInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	// 创建文件夹
	private void mkFolder(String fileName) {
		File f = new File(fileName);
		if (!f.exists()) {
			f.mkdir();
		}
	}

	// 创建文件
	private File mkFile(String fileName) {
		File f = new File(fileName);
		try {
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	}

	// 去除文件的后缀名
	public String trimExtension(String filename) {
		if ((filename != null) && (filename.length() > 0)) {
			int i = filename.lastIndexOf('.');
			if ((i > -1) && (i < (filename.length()))) {
				return filename.substring(0, i);
			}
		}
		return filename;
	}

	public static void main(String[] args) {
		ExtractTar extractTar = new ExtractTar();
		extractTar.unTargzFile("/Users/chenshang/Downloads/test.tar.gz");
	}
}
