package com.rscloud.ipc.rpc.api.dto;

import java.io.Serializable;
import java.util.Date;

public class MapManageDto  implements Serializable {
	
    private String id;

    private String mapName;

    private String showMaxLayers;

    private String project;

    private String usage;

    private Short waterMark;

    private String range;
    
    private String ownerUserId;

    private Date ctTime;
    
    private Date utTime; 

	private String outPath;
	
	private Integer isDel;
	
	private String serviceType;
	
	private String geoRange;
	
	private String format;
	
	private Integer isPublish;

	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public Integer getIsDel() {
		return isDel;
	}

	public void setIsDel(Integer isDel) {
		this.isDel = isDel;
	}

    public String getShowMaxLayers() {
		return showMaxLayers;
	}

	public void setShowMaxLayers(String showMaxLayers) {
		this.showMaxLayers = showMaxLayers;
	}


    public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public Short getWaterMark() {
        return waterMark;
    }

    public void setWaterMark(Short waterMark) {
        this.waterMark = waterMark;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getOutPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }
    public String getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}
	public Date getCtTime() {
		return ctTime;
	}

	public void setCtTime(Date ctTime) {
		this.ctTime = ctTime;
	}

	public Date getUtTime() {
		return utTime;
	}

	public void setUtTime(Date utTime) {
		this.utTime = utTime;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getGeoRange() {
		return geoRange;
	}

	public void setGeoRange(String geoRange) {
		this.geoRange = geoRange;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Integer getIsPublish() {
		return isPublish;
	}

	public void setIsPublish(Integer isPublish) {
		this.isPublish = isPublish;
	}
	

}