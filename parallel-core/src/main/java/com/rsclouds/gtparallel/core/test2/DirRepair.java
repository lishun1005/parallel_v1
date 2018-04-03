package com.rsclouds.gtparallel.core.test2;

import java.io.IOException;

import com.rsclouds.gtparallel.core.gtdata.api.GtDataImpl;

public class DirRepair {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("创建/users目录: " + GtDataImpl.mkdir("/users", false));
		System.out.println("创建/projects目录: " + GtDataImpl.mkdir("/projects", false));
		System.out.println("创建/projects/rscloudmart目录: " + GtDataImpl.mkdir("/projects/rscloudmart", false));
	}

}
