package com.streamxhub.spark.monitor.core.service.impl;


import com.streamxhub.spark.monitor.api.util.PropertiesUtil;
import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static com.streamxhub.spark.monitor.api.Const.*;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class WatcherServiceImpl implements WatcherService {

    @Value("${spark.app.monitor.zookeeper}")
    private String zookeeperConnect;

    @Autowired
    private SparkConfService sparkConfService;

    @Autowired
    private SparkMonitorService sparkMonitorService;

    @Override
    public void config(String id, String conf) {
        Map<String, String> confMap = getConfigMap(conf);
        String appName = confMap.get(SPARK_PARAM_APP_NAME());
        Integer confVersion = Integer.parseInt(confMap.get(SPARK_PARAM_APP_CONF_VERSION()));
        SparkConf sparkConf = new SparkConf(id, appName, confVersion, Base64.getEncoder().encodeToString(conf.getBytes(StandardCharsets.UTF_8)));
        boolean configFlag = sparkConfService.config(sparkConf);
        System.out.println(id + ":config");
    }

    @Override
    public void publish(String id, String conf) {
        Map<String, String> confMap = PropertiesUtil.getPropertiesFromText(conf);
        sparkMonitorService.publish(id, confMap);
        System.out.println(id + ":publish");
    }

    @Override
    public void shutdown(String id, String conf) {
        Map<String, String> confMap = PropertiesUtil.getPropertiesFromText(conf);
        sparkMonitorService.shutdown(id, confMap);
        System.out.println(id + ":shutdown");
    }

    @Override
    public void delete(String myId) {
        String confPath = SPARK_CONF_PATH_PREFIX() + "/" + myId;
        String monitorPath = SPARK_MONITOR_PATH_PREFIX() + "/" + myId;
        ZooKeeperUtil.delete(confPath, zookeeperConnect);
        ZooKeeperUtil.delete(monitorPath, zookeeperConnect);
    }

    private Map<String, String> getConfigMap(String conf) {
        if (Pattern.compile(SPARK_CONF_TYPE_REGEXP()).matcher(conf).find()) {
            return PropertiesUtil.getPropertiesFromText(conf);
        } else {
            return PropertiesUtil.getPropertiesFromYamlText(conf);
        }
    }

}
