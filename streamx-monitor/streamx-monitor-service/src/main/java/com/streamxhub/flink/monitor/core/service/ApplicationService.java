package com.streamxhub.flink.monitor.core.service;

import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.core.entity.Application;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;


public interface ApplicationService extends IService<Application> {
    IPage<Application> list(Application app, RestRequest request);

    boolean create(Application app);

    boolean startUp(String id);

    String getYarnName(Application app);

    boolean checkExists(Application app);
}
