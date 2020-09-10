package com.streamxhub.monitor.core.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.monitor.core.entity.Application;
import com.streamxhub.monitor.core.entity.ApplicationConfig;

public interface ApplicationConfigService extends IService<ApplicationConfig> {

    void create(Application application);

    void update(Application application);

    ApplicationConfig getActived(Long id);
}
