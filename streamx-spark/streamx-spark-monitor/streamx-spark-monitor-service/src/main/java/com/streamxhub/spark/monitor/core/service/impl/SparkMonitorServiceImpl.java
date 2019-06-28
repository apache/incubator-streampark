package com.streamxhub.spark.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;
import com.streamxhub.spark.monitor.common.domain.Constant;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.common.utils.SortUtil;
import com.streamxhub.spark.monitor.core.dao.SparkMonitorMapper;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;
import com.streamxhub.spark.monitor.core.service.SparkConfRecordService;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.streamxhub.spark.monitor.api.Const.*;

import java.util.Date;
import java.util.Map;

/**
 * @author benjobs
 */
@Slf4j
@Service("sparkMonitorService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkMonitorServiceImpl extends ServiceImpl<SparkMonitorMapper, SparkMonitor> implements SparkMonitorService {


    @Autowired
    private WatcherService watcherService;

    @Autowired
    private SparkConfService sparkConfService;

    @Autowired
    private SparkConfRecordService recordService;

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
        String proxyUri = confMap.get(SPARK_PARAM_APP_PROXY_URI_BASES());
        String startUp = confMap.get(SPARK_PARAM_DEPLOY_STARTUP());
        SparkMonitor monitor = new SparkMonitor(id, appId, appName, confVersion, status,startUp);

        if (!CommonUtils.isBlank(proxyUri)) {
            monitor.setTrackUrl(proxyUri.split(",")[0]);
        }

        SparkMonitor exist = baseMapper.selectById(id);
        if (exist == null) {
            monitor.setCreateTime(new Date());
            baseMapper.insert(monitor);
        } else {
            monitor.setModifyTime(new Date());
            baseMapper.updateById(monitor);
        }
    }

    @Override
    public IPage<SparkMonitor> getPager(SparkMonitor sparkMonitor, QueryRequest request) {
        try {
            Page<SparkMonitor> page = new Page<>();
            SortUtil.handlePageSort(request, page, "CREATE_TIME", Constant.ORDER_ASC, false);
            QueryWrapper<SparkMonitor> wrapper = new QueryWrapper<>();
            if (sparkMonitor.getAppId() != null) {
                wrapper.eq("APP_ID", sparkMonitor.getAppId().trim());
            }
            if (sparkMonitor.getAppName() != null) {
                wrapper.like("APP_NAME", sparkMonitor.getAppName().trim());
            }
            return this.baseMapper.selectPage(page, wrapper);
        } catch (Exception e) {
            log.error("查询Spark监控异常", e);
            return null;
        }
    }

    @Override
    public void delete(String myId) {
        this.baseMapper.deleteById(myId);
        sparkConfService.delete(myId);
        recordService.delete(myId);
        watcherService.delete(myId);
    }

}
