package com.streamxhub.spark.monitor.core.controller;

import com.streamxhub.spark.monitor.common.controller.BaseController;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("spark")
public class SparkController extends BaseController {

    @Autowired
    private SparkMonitorService monitorService;

    @Autowired
    private SparkConfService confService;

    @PostMapping("monitor")
    @RequiresPermissions("spark:monitor")
    public Map<String, Object> monitor(QueryRequest request, SparkMonitor sparkMonitor) {
        return getDataTable(this.monitorService.getMonitor(sparkMonitor,request));
    }

    @PostMapping("conf")
    @RequiresPermissions("spark:conf")
    public Map<String, Object> conf(QueryRequest request, SparkConf sparkConf) {
        return getDataTable(this.confService.getConf(sparkConf,request));
    }


}
