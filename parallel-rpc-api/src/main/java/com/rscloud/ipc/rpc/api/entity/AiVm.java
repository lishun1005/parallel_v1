package com.rscloud.ipc.rpc.api.entity;

import java.io.Serializable;
import java.util.Date;

public class AiVm implements Serializable {
    private String id;

    private String ipAddr;

    private Integer maxJobs;

    private Short isDel;

    private Date ctTime;

    private Date utTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public Integer getMaxJobs() {
        return maxJobs;
    }

    public void setMaxJobs(Integer maxJobs) {
        this.maxJobs = maxJobs;
    }

    public Short getIsDel() {
        return isDel;
    }

    public void setIsDel(Short isDel) {
        this.isDel = isDel;
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
}