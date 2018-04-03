package com.rsclouds.gtparallel.entity;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "rsipc_product_line")
public class ProductLine {
    private String id;

    private String name;

    private String displayName;

    private String remark;

    private String effectPicPath;

    private String modelId;

    private Integer isDel;

    private Date createTime;

    private OptimalModel optimalModel;

    public OptimalModel getOptimalModel() {
        return optimalModel;
    }

    public void setOptimalModel(OptimalModel optimalModel) {
        this.optimalModel = optimalModel;
    }

    @Id
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getEffectPicPath() {
        return effectPicPath;
    }

    public void setEffectPicPath(String effectPicPath) {
        this.effectPicPath = effectPicPath;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Integer getIsDel() {
        return isDel;
    }

    public void setIsDel(Integer isDel) {
        this.isDel = isDel;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}