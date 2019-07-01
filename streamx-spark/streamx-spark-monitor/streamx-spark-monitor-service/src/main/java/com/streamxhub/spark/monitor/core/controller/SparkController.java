package com.streamxhub.spark.monitor.core.controller;

import com.streamxhub.spark.monitor.common.controller.BaseController;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.common.domain.RestResponse;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.domain.SparkConfRecord;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;
import com.streamxhub.spark.monitor.core.service.SparkConfRecordService;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Base64Utils;
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

    @Autowired
    private SparkConfRecordService recordService;

    @PostMapping("monitor/view")
    @RequiresPermissions("spark:monitor")
    public Map<String, Object> monitor(QueryRequest request, SparkMonitor sparkMonitor) {
        return getDataTable(this.monitorService.getPager(sparkMonitor, request));
    }

    @PostMapping("conf/view")
    @RequiresPermissions("spark:conf")
    public Map<String, Object> conf(QueryRequest request, SparkConf sparkConf) {
        return getDataTable(this.confService.getPager(sparkConf, request));
    }

    @PostMapping("conf/detail/{myId}")
    @RequiresPermissions("spark:conf")
    public RestResponse detail(@PathVariable("myId") String myId) {
        SparkConf sparkConf = this.confService.getById(myId);
        String conf = new String(Base64Utils.decodeFromString(sparkConf.getConf()));
        sparkConf.setConf(conf);
        RestResponse response = new RestResponse();
        response.put("data",sparkConf);
        return response;
    }

    @PostMapping("conf/record/{recordId}")
    @RequiresPermissions("spark:conf")
    public RestResponse record(@PathVariable("recordId") String recordId) {
        SparkConfRecord record = this.recordService.getById(recordId);
        String conf = new String(Base64Utils.decodeFromString(record.getConf()));
        record.setConf(conf);
        RestResponse response = new RestResponse();
        response.put("data",record);
        return response;
    }

    @PostMapping("monitor/start/{myId}")
    @RequiresPermissions("spark:start")
    public RestResponse start(@PathVariable("myId") String myId) {
        int code = this.monitorService.start(myId);
        RestResponse response = new RestResponse();
        response.put("code",code);
        return response;
    }

    @PostMapping("monitor/stop/{myId}")
    @RequiresPermissions("spark:stop")
    public RestResponse stop(@PathVariable("myId") String myId) {
        int code = this.monitorService.stop(myId);
        RestResponse response = new RestResponse();
        response.put("code",code);
        return response;
    }

    @DeleteMapping("monitor/delete/{myId}")
    @RequiresPermissions("spark:delete")
    public void delete(@PathVariable("myId") String myId) {
        this.monitorService.delete(myId);
    }

}
