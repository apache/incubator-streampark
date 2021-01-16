package com.streamxhub.streamx.console.base.domain;

import java.io.Serializable;

import lombok.Data;

/**
 * @author benjobs
 */
@Data
public class RestRequest implements Serializable {

    private static final long serialVersionUID = -4869594085374385813L;

    private int pageSize = 10;
    private int pageNum = 1;

    private String sortField;
    private String sortOrder;
}
