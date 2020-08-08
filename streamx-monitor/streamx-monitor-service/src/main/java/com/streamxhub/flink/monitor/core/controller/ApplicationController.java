package com.streamxhub.flink.monitor.core.controller;

import com.streamxhub.flink.monitor.base.controller.BaseController;
import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.domain.RestResponse;
import com.streamxhub.flink.monitor.core.entity.Application;
import com.streamxhub.flink.monitor.core.service.ApplicationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/app")
public class ApplicationController extends BaseController {

    @Autowired
    private ApplicationService applicationService;

    @RequestMapping("list")
    public RestResponse list(Application app, RestRequest request) {
        IPage<Application> applicationList = applicationService.list(app, request);
        return RestResponse.create().data(applicationList);
    }

    @RequestMapping("name")
    public RestResponse yarnName(Application app) {
        String yarnName = applicationService.getYarnName(app);
        return RestResponse.create().data(yarnName);
    }

    @RequestMapping("exists")
    public RestResponse exists(Application app) {
        boolean exists = applicationService.checkExists(app);
        return RestResponse.create().data(exists);
    }

    @RequestMapping("create")
    public RestResponse create(Application app) throws IOException {
      boolean saved = applicationService.create(app);
      return RestResponse.create().data(saved);
    }

    @RequestMapping("startUp")
    public RestResponse startUp(String id) {
        boolean started = applicationService.startUp(id);
        return RestResponse.create().data(started);
    }

}
