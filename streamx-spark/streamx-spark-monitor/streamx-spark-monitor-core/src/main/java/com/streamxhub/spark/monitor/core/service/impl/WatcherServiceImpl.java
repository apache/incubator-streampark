package com.streamxhub.spark.monitor.core.service.impl;

import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("watcherService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class WatcherServiceImpl implements WatcherService {


    @Override
    public void config(String currentPath, String conf) {

    }

    @Override
    public void publish(String currentPath, String conf) {

    }

    @Override
    public void shutdown(String currentPath, String conf) {

    }
}

