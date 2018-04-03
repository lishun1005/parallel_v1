package com.rscloud.ipc.utils.gtdata;



import java.io.Serializable;
import java.util.List;

/**
 * Description:个人中心我的空间，gtdata的文件属性
 *
 * @author ljw 2014-6-19
 * 
 * @version v1.0
 *
 */
public class GtdataFile implements Serializable
{
	@Override
	public String toString() {
		return "GtdataFile [path=" + path + ", size=" + size + ", time=" + time
				+ ", filename=" + filename + ", child=" + child + "]";
	}
	private static final long serialVersionUID = -1068754518809085144L;
	private String path;//文件路径
	private String size;//文件大小，大小是-1就是文件夹
	private String time;//修改日期
	private String filename;//文件名字
	private List<GtdataFile> child;
	
	public List<GtdataFile> getChild()
	{
		return child;
	}
	public void setChild(List<GtdataFile> child)
	{
		this.child = child;
	}
	public String getPath()
	{
		return path;
	}
	public void setPath(String path)
	{
		this.path = path;
	}
	public String getSize()
	{
		return size;
	}
	public void setSize(String size)
	{
		this.size = size;
	}
	public String getTime()
	{
		return time;
	}
	public void setTime(String time)
	{
		this.time = time;
	}
	public String getFilename()
	{
		return filename;
	}
	public void setFilename(String filename)
	{
		this.filename = filename;
	}
	
}
