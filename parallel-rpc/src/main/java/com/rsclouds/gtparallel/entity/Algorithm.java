package com.rsclouds.gtparallel.entity;

import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "rsipc_algorithm")
public class Algorithm {

    private String id;

    private String name;

    private String displayName;


    private Integer isDel;

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

    public Integer getIsDel() {
        return isDel;
    }

    public void setIsDel(Integer isDel) {
        this.isDel = isDel;
    }

}