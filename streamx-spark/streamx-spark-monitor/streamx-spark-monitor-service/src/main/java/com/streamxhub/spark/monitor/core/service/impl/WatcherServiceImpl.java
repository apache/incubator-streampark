package com.streamxhub.spark.monitor.core.service.impl;


import com.streamxhub.spark.monitor.api.util.PropertiesUtil;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;

import static com.streamxhub.spark.monitor.api.Const.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class WatcherServiceImpl implements WatcherService {

    @Autowired
    private SparkConfService sparkConfService;

    @Autowired
    private SparkMonitorService sparkMonitorService;

    @Override
    public void config(String id, String conf) {
        Map<String, String> confMap = getConfigMap(conf);
        String appName = confMap.get(SPARK_PARAM_APP_NAME());
        String confVersion = confMap.get(SPARK_PARAM_APP_CONF_LOCAL_VERSION());
        SparkConf sparkConf = new SparkConf(id, appName, confVersion, Base64Utils.encodeToString(conf.getBytes()));
        boolean configFlag = sparkConfService.config(sparkConf);
        System.out.println(id + ":config");
    }

    @Override
    public void publish(String id, String conf) {
        Map<String, String> confMap = getConfigMapFromDebugString(conf);
        sparkMonitorService.publish(id, confMap);
        System.out.println(id + ":publish");
    }

    @Override
    public void shutdown(String id, String conf) {
        Map<String, String> confMap = getConfigMapFromDebugString(conf);
        sparkMonitorService.shutdown(id, confMap);
        System.out.println(id + ":shutdown");
    }

    private Map<String, String> getConfigMap(String conf) {
        if (!conf.matches(SPARK_CONF_REGEXP())) {
            return PropertiesUtil.getPropertiesFromYamlText(conf);
        } else {
            return getConfigMapFromDebugString(conf);
        }
    }

    private Map<String, String> getConfigMapFromDebugString(String conf) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(conf));
            Set<String> set = properties.stringPropertyNames();
            Map<String, String> map = new HashMap<>(0);
            for (String k : set) {
                map.put(k, properties.getProperty(k));
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

}
