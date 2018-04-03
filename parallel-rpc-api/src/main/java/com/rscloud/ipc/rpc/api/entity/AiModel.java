package com.rscloud.ipc.rpc.api.entity;

import java.io.Serializable;
import java.util.Date;

public class AiModel implements Serializable{
    private String id;

    private String name;

    private String logdir;


    private Date ctTime;

    private Date utTime;

    private Short isDel;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogdir() {
        return logdir;
    }

    public void setLogdir(String logdir) {
        this.logdir = logdir;
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

    public Short getIsDel() {
        return isDel;
    }

    public void setIsDel(Short isDel) {
        this.isDel = isDel;
    }
}