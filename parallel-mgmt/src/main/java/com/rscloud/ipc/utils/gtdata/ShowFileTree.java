package com.rscloud.ipc.utils.gtdata;


/**
 * 展示文件夹树的时候，用于制造相应格式的json
 * @author lijianwei
 *
 */
public class ShowFileTree implements java.io.Serializable{
	private static final long serialVersionUID = 3170076637330612113L;
	private String id; //当前目录路径
	private String pId;//父文件夹的路径
	private String name;//孩子文件夹的名字
	private String click;//点击事件
	private String open;//打开状态
	
	private String size;
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public String getpId()
	{
		return pId;
	}
	public void setpId(String pId)
	{
		this.pId = pId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getClick()
	{
		return click;
	}
	public void setClick(String click)
	{
		this.click = click;
	}
	public String getOpen()
	{
		return open;
	}
	public void setOpen(String open)
	{
		this.open = open;
	}
	
}
