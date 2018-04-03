package com.rsclouds.gtparallel.core.test2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class FileCheckTest {

	public static void main(String[] args) {
		int len = 20;
		
		File file = new File("F:\\data\\sdm\\ftp\\test");
		File[] files = file.listFiles();
		for(int j = 0; j < files.length; j ++) {
			files[j].setLastModified(System.currentTimeMillis() + 100000);
			try {
				InputStream in = new FileInputStream(files[j]);
				GZIPInputStream zip = new GZIPInputStream(in);
				TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
						.createArchiveInputStream("tar", zip);
				tarInputStream.close();
				zip.close();
				in.close();
			} catch (ArchiveException e) {
				e.printStackTrace();
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(files[j].getPath());
				continue;
			}
		}
	}
}
