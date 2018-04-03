package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ParserTime {

	public static String parserOlearthTimeMilliSeconds(String filename) {
		String timeStr = null;
		int startIndexof = -1;
		int endIndexof = -1;
		if (filename.startsWith("pl_")) {
			startIndexof = 3;
			endIndexof = 11;
		}else if (filename.startsWith("GF")) {
			endIndexof = filename.lastIndexOf("_");
			if (endIndexof < 8) {
				return null;
			}
			startIndexof = endIndexof -8;
		}else {
			String dateStr = filename.substring(0, 10);
			SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date date = dataFormat.parse(dateStr);
				timeStr = "" + date.getTime();
			} catch (ParseException e) {
				return null;
			}
			return timeStr;
		}
		String dateStr = filename.substring(startIndexof, endIndexof);
		SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMdd");
		try {
			Date date = dataFormat.parse(dateStr);
			timeStr = "" + date.getTime();
		} catch (ParseException e) {
			return null;
		}
		return timeStr;
	}
	
	public static String parserTimeMilliSeconds(String filename) {
		String timeStr = null;
		int indexof = filename.lastIndexOf("_");
		if(filename.indexOf("HJ1A") != -1 || filename.indexOf("HJ1B") != -1) {
			indexof = filename.lastIndexOf("-");
		}
		if(indexof < 8) {
			return null;
		}
		String dateStr = filename.substring(indexof - 8, indexof);
		SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMdd");
		try {
			Date date = dataFormat.parse(dateStr);
			timeStr = "" + date.getTime();
		} catch (ParseException e) {
			return null;
		}
		return timeStr;
	}
	
	public static String parserPLTimeMilliSeconds(String filename) {
		String timeStr = null;
		int indexof = filename.lastIndexOf("/");
		String dateStr = filename.substring(indexof+1, indexof+9);
		SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMdd");
		try {
			Date date = dataFormat.parse(dateStr);
			timeStr = "" + date.getTime();
		} catch (ParseException e) {
			return null;
		}
		return timeStr;
	}
	public static void main(String[] args) {
		String timeStr = parserOlearthTimeMilliSeconds("GF1_PMS1_E116.4_N40.8_20150725_L1A0001094171.tiff");
		System.out.println(timeStr);
		timeStr = parserOlearthTimeMilliSeconds("pl_20151117_34453_efa.tiff");
		System.out.println(timeStr);
//		System.out.println("/users/rscloudmart/PL/20140608/20140608-HJ1A-CCD1-447-68-20150113-L20002272297.tar.gz".contains("/PL/"));
	}
	
}
