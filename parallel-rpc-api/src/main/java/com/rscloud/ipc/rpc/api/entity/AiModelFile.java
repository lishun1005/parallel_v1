package com.rscloud.ipc.rpc.api.entity;

import java.io.Serializable;
import java.util.Date;

public class AiModelFile implements Serializable {
    private String id;

    private String path;

    private Short type;

    private String aiModelId;

    private Date ctTmie;

    private Short isDel;

    private String fileName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Short getType() {
        return type;
    }

    public void setType(Short type) {
        this.type = type;
    }

    public String getAiModelId() {
        return aiModelId;
    }

    public void setAiModelId(String aiModelId) {
        this.aiModelId = aiModelId;
    }

    public Date getCtTmie() {
        return ctTmie;
    }

    public void setCtTmie(Date ctTmie) {
        this.ctTmie = ctTmie;
    }

    public Short getIsDel() {
        return isDel;
    }

    public void setIsDel(Short isDel) {
        this.isDel = isDel;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}