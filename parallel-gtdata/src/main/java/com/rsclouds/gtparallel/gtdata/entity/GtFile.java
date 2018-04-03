package com.rsclouds.gtparallel.gtdata.entity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.client.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsclouds.gtparallel.gtdata.service.GtDataImpl;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;

public class GtFile implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2719387816443657631L;
	private static Logger logger = LoggerFactory.getLogger(GtFile.class);
	
	/**
	 * current GtFile Path in GtData
	 */
	private GtPath gtPath;
	private Metadata meta;
	
	public GtFile(String pathname){
		if (pathname == null) {
            throw new NullPointerException();
        }
		this.gtPath = new GtPath(pathname);
		getMeta();
	}
	
	private GtFile(Metadata meta){
		if (meta == null || meta.getRowKey() == null) {
            throw new NullPointerException();
        }
		this.meta = meta;
		this.gtPath = new GtPath(meta.getRowKey());
	}
	
	public GtFile(String parent, String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent != null) {
        	this.gtPath = new GtPath(parent + "/" + child);
        } else {
        	this.gtPath = new GtPath(child);
        }
        getMeta();
    }
	
	public String getName()  {
	    return this.gtPath.getDisplayFileName(); 
	}
	
	public String getPath() { 
		return this.gtPath.getDisplayPath();
	}
	
	public String getParent()  {
		return this.gtPath.getDisplayParent();		 
	}
	
	public GtFile getParentGtFile()  {
        return new GtFile(this.gtPath.getDisplayParent());
    }
	
	public boolean exists() {
		if(this.meta == null || this.meta.getRowKey() == null)
			getMeta();	
        return this.meta.getSize() == null?false:true;
    }
	
	public boolean isDirectory() {
		if(this.meta == null || this.meta.getRowKey() == null)
			getMeta();
		if(!exists())
			return false;
        return GtDataConfig.CONSTANT.NEGATIVE_ONE.strVal.equals(this.meta.getSize())?true:false;
    }
	
	public boolean isFile() {
		if(this.meta == null || this.meta.getRowKey() == null)
			getMeta();
		if(!exists())
			return false;
        return GtDataConfig.CONSTANT.NEGATIVE_ONE.strVal.equals(this.meta.getSize())?false:true;
    }
	
	public long createTime()  {
		if(this.meta == null || this.meta.getRowKey() == null)
			getMeta();
		if(this.meta.getTime() != null){
			return Long.parseLong(GtDataUtils.timeStrFillZero(this.meta.getTime()));
		}else{
			return 0;
		}		
	}
	
	public long size(){
		if(this.meta == null || this.meta.getRowKey() == null)
			getMeta();
		if(this.meta.getSize() != null){
			return Long.parseLong(this.meta.getSize());
		}else{
			return 0;
		}
	}
	
	public boolean delete() throws  IOException {
		boolean flag = GtDataImpl.getInstance().delete(this.gtPath.getGtPath());
		flush();
		return flag;
	}
	
	public List<GtFile> list() throws  IOException {
		List<Metadata> maps = GtDataImpl.getInstance().list(this.gtPath.getGtPath());
		List<GtFile> gtFiles = new ArrayList<GtFile>();
		for(Metadata m : maps){
			GtFile f = new GtFile(m);
			gtFiles.add(f);
		}	
        return gtFiles;
    }
	
	public boolean mkdir() throws  IOException {
		if(this.exists()){
			return false;
		}
		boolean flag = GtDataImpl.getInstance().mkdir(this.gtPath.getGtPath(), false);
		if(flag)
			this.flush();
        return flag;
    }
	
	public boolean mkdirs() throws IOException{
		if(this.exists()){ 
			if(this.isDirectory())
				return true;
			else				
				return false;
		}
		boolean flag = GtDataImpl.getInstance().mkdir(this.gtPath.getGtPath(), false);
		if(flag){
			GtFile parentFile = new GtFile(this.gtPath.getDisplayParent());
			if(!parentFile.isRootPath())
				flag = parentFile.mkdirs();
			this.flush();
		}
		return flag;		
	}
	
	public boolean isRootPath(){
		return "/".equals(this.gtPath.getDisplayParent());
	}
	
	public boolean renameTo(GtFile dest,boolean overwrite,boolean keepTwoFile) throws Exception { 
		if(!this.exists()){ 
			logger.error("Current GtFile is not exist.");
			return false;
		}
		if(dest.exists()){
			logger.error("Dest GtFile is allready exist.");
			return false;
		}	
		String destParent = dest.getParent();
		try{
			if(destParent.equals(this.getParent())){
				//判断是否同目录文件重命名	
				if(this.getName().equals(dest.getName())){
					return true;
				}
				return GtDataImpl.getInstance().reanme(destParent, 
						this.getName(), 
						dest.getName(),
						true);
			}else{
				//不同目录之间的文件移动
				if(!this.getName().equals(dest.getName())){
					return false;
				}
				return GtDataImpl.getInstance().copyOrMove(this.getParent(), 
						destParent, 
						this.getName(),
						true, 
						overwrite, 
						keepTwoFile);
			}
		}finally{
			this.flush();
			dest.flush();
		}
    }
	
	public boolean copyTo(GtFile dest,boolean overwrite,boolean keepTwoFile) throws Exception { 
		if(!this.exists()){ 
			logger.error("Current GtFile is not exist.");
			return false;
		}
		if(dest.exists()){
			logger.error("Dest GtFile is allready exist.");
			return false;
		}	
		String destParent = dest.getParent();
		try{
			if(destParent.startsWith(this.getParent())){
				//判断是否同目录文件重命名	
				logger.error("Can not copy to current dir path.");
				return false;
			}else{
				//不同目录之间的文件移动
				if(!this.getName().equals(dest.getName())){
					return false;
				}
				return GtDataImpl.getInstance().copyOrMove(this.getParent(), 
						destParent, 
						this.getName(),
						false, 
						overwrite, 
						keepTwoFile);
			}
		}finally{
			this.flush();
			dest.flush();
		}
    }
	
	public boolean flush(){
		return getMeta();	
	}
	
	public Map<String,String> toMap(){
		if(this.meta == null || this.meta.getRowKey() == null)
			getMeta();
		return this.meta.toStrMap();
	}
	
	public String toString() {
		Map<String, String> map = this.meta.toStrMap();
		map.put("path", getPath());
		return map.toString();
	}
	
	private boolean getMeta(){
		Result rs;
		try {
			rs = HbaseBase.selectRow(GtDataConfig.TABLE_NAME.META_TABLE.getStrVal(), gtPath.getGtPath());
			this.meta = new Metadata(rs);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return true;		
	}
	
//	public GtPath toGtPath() {      
//        return new GtPath(this.getPath());
//    }
	
	public boolean export(OutputStream out) throws IOException{
		this.flush();
		if(this.isFile()){
			return GtDataImpl.getInstance().export(this.getPath(), out);
		}else{
			return false;
		}	
	}
	
	public boolean exportFile(File outFile) throws IOException{
		return export(new FileOutputStream(outFile)) ;	
	}
	
	public boolean importFile(byte[] contents,boolean overwrite) throws IOException{
		boolean flag = false ;
		if(this.exists() && !overwrite){
			flag = false;
		}else{
			GtFile parent = new GtFile(this.getParent());
			if(!parent.isRootPath() && !parent.isDirectory()){
				parent.mkdirs();
			}
			flag = GtDataImpl.getInstance().importByByteArray(this.getPath(), contents);
		}
		return flag;	
	}
	
	public  boolean importFile(File lcFile,boolean overwrite) throws IOException{
		boolean flag = false ;
		if(this.exists() && !overwrite){
			flag = false;
		}else{
			GtFile parent = new GtFile(this.getParent());
			if(!parent.isRootPath() && !parent.isDirectory()){
				parent.mkdirs();
			}
			flag = GtDataImpl.getInstance().importByFile(this.getPath(), lcFile.getPath());
		}		
		if(flag)
			this.flush();
		return flag;
	}
	
	public boolean importFile(String lcFilePath,boolean overwrite) throws IOException{
		return importFile(new File(lcFilePath),overwrite);
	}
	

	/** 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
//		System.out.println(GtDataConfig.CONSTANT.NEGATIVE_ONE.strVal.equals("-1"));
//		System.out.println(new Metadata(null));
//		for(String str : new File("E:\\myJob\\test_1\\学习").list()){
//			System.out.println(str);
//		}
//		File from = new File("E:\\myJob\\geowebcache.xml");
//		File to = new File("E:\\myJob\\test2\\123.xml");
//		System.out.println(from.renameTo(to));
		GtFile gtfile = new GtFile("/sotcut/test1/outputmap/beijing-4.tif");
		gtfile.exportFile(new File("e://beijing.tif"));
		
	}

}
