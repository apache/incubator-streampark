package com.streamxhub.spark.monitor.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.spark.monitor.core.dao.SparkMonitorMapper;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;
import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.streamxhub.spark.monitor.api.Const.*;

import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class SparkMonitorServiceImpl extends ServiceImpl<SparkMonitorMapper, SparkMonitor> implements SparkMonitorService {

    @Override
    public void publish(String id, Map<String, String> confMap) {
        doAction(id, 1, confMap);
    }

    @Override
    public void shutdown(String id, Map<String, String> confMap) {
        doAction(id, 0, confMap);
    }

    private void doAction(String id, Integer status, Map<String, String> confMap) {
        String appName = confMap.get(SPARK_PARAM_APP_NAME());
        String confVersion = confMap.get(SPARK_PARAM_APP_CONF_LOCAL_VERSION());
        String appId = confMap.get(SPARK_PARAM_APP_ID());
        SparkMonitor monitor = new SparkMonitor(id, appId, appName, confVersion, status);
        SparkMonitor exist = baseMapper.selectById(id);
        if (exist == null) {
            monitor.setCreateTime(new Date());
            baseMapper.insert(monitor);
        } else {
            monitor.setModifyTime(new Date());
            baseMapper.updateById(monitor);
        }
    }

}
