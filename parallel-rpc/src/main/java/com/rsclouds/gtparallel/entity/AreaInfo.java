package com.rsclouds.gtparallel.entity;


/**
 * 
 * Copyright zkyg 
 */

import org.postgresql.util.PGobject;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 
 * Description:地区表
 *
 * @author JhYao 2014年7月30日
 * 
 * @version v1.0
 *
 */
@Table(name = "rscipc_area_info")
public class AreaInfo implements Serializable 
{
	private String admincode;//编码
	private String name;//行政区划名称
	private String proadcode;//父编号
	private String cityadcode;//城市编号
	private String cityname;//城市名字
	private String pinyin;   //中文拼音
	private String pysx;   //拼音缩写
	private String proname;//
	private BigDecimal shapeLeng;
	private BigDecimal shapeArea;
	private PGobject geom;//面
	private PGobject pointGeom;//中心点
	private int isHandleCoverRate;
	
	@Column(name = "is_handle_cover_rate")
	public int getIsHandleCoverRate() {
		return isHandleCoverRate;
	}
	public void setIsHandleCoverRate(int isHandleCoverRate) {
		this.isHandleCoverRate = isHandleCoverRate;
	}
	@Id
	public String getAdmincode()
	{
		return admincode;
	}
	public void setAdmincode(String admincode)
	{
		this.admincode = admincode;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getProadcode()
	{
		return proadcode;
	}
	public void setProadcode(String proadcode)
	{
		this.proadcode = proadcode;
	}
	public String getCityadcode()
	{
		return cityadcode;
	}
	public void setCityadcode(String cityadcode)
	{
		this.cityadcode = cityadcode;
	}
	public String getCityname()
	{
		return cityname;
	}
	public void setCityname(String cityname)
	{
		this.cityname = cityname;
	}
	public String getPinyin() {
		return pinyin;
	}
	public void setPinyin(String pinyin) {
		this.pinyin = pinyin;
	}
	public String getPysx() {
		return pysx;
	}
	public void setPysx(String pysx) {
		this.pysx = pysx;
	}
	public String getProname()
	{
		return proname;
	}
	public void setProname(String proname)
	{
		this.proname = proname;
	}
	@Column(name = "shape_leng")
	public BigDecimal getShapeLeng()
	{
		return shapeLeng;
	}
	public void setShapeLeng(BigDecimal shapeLeng)
	{
		this.shapeLeng = shapeLeng;
	}
	@Column(name = "shape_area")
	public BigDecimal getShapeArea()
	{
		return shapeArea;
	}
	public void setShapeArea(BigDecimal shapeArea)
	{
		this.shapeArea = shapeArea;
	}
	public PGobject getGeom()
	{
		return geom;
	}
	public void setGeom(PGobject geom)
	{
		this.geom = geom;
	}
	@Column(name = "point_geom")
	public PGobject getPointGeom()
	{
		return pointGeom;
	}
	public void setPointGeom(PGobject pointGeom)
	{
		this.pointGeom = pointGeom;
	}
	
}
