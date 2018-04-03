package com.rscloud.ipc.rpc.api.entity;

import java.io.Serializable;
import java.util.Date;

public class AiVmInstancePort  implements Serializable {
    private String id;

    private String vmId;

    private Integer vmPort;

    private Short isUse;

    private Date ctTime;

    private Short isDel;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public Integer getVmPort() {
        return vmPort;
    }

    public void setVmPort(Integer vmPort) {
        this.vmPort = vmPort;
    }

    public Short getIsUse() {
        return isUse;
    }

    public void setIsUse(Short isUse) {
        this.isUse = isUse;
    }

    public Date getCtTime() {
        return ctTime;
    }

    public void setCtTime(Date ctTime) {
        this.ctTime = ctTime;
    }

    public Short getIsDel() {
        return isDel;
    }

    public void setIsDel(Short isDel) {
        this.isDel = isDel;
    }
}