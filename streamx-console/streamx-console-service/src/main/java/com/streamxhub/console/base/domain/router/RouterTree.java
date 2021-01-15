package com.streamxhub.console.base.domain.router;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterTree<T> {

    private String id;

    private String key;

    private String icon;

    private String title;

    private String value;

    private String text;

    private String permission;

    private String type;

    private String display;

    private Double order;

    private String path;

    private String component;

    private List<RouterTree<T>> children;

    private String parentId;

    private boolean hasParent = false;

    private boolean hasChildren = false;

    private Date createTime;

    private Date modifyTime;

    public void initChildren() {
        this.children = new ArrayList<>();
    }
}
