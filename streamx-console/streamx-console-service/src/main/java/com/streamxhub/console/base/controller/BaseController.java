package com.streamxhub.console.base.controller;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * @author benjobs
 */
public class BaseController {

    protected Map<String, Object> getDataTable(IPage<?> pageInfo) {
        Map<String, Object> rspData = new HashMap<>();
        rspData.put("rows", pageInfo.getRecords());
        rspData.put("total", pageInfo.getTotal());
        return rspData;
    }
}
