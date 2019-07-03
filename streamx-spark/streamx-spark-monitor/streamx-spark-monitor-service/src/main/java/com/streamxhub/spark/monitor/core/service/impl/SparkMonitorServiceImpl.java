package com.streamxhub.spark.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;
import com.streamxhub.spark.monitor.common.domain.Constant;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.common.utils.CommandUtils;
import com.streamxhub.spark.monitor.common.utils.SortUtil;
import com.streamxhub.spark.monitor.core.dao.SparkMonitorMapper;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;
import com.streamxhub.spark.monitor.core.service.SparkConfRecordService;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.streamxhub.spark.monitor.api.Const.*;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author benjobs
 */
@Slf4j
@Service("sparkMonitorService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkMonitorServiceImpl extends ServiceImpl<SparkMonitorMapper, SparkMonitor> implements SparkMonitorService {

    @Value("${spark.app.hadoop.user}")
    private String hadoopUser;

    @Autowired
    private WatcherService watcherService;

    @Autowired
    private SparkConfService sparkConfService;

    @Autowired
    private SparkConfRecordService recordService;

    private ScheduledExecutorService checkExecutorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().daemon(true).build());

    @Override
    public void publish(String id, Map<String, String> confMap) {
        SparkMonitor monitor = getSparkMonitor(confMap);
        monitor.setMyId(id);
        monitor.setStatusValue(SparkMonitor.Status.RUNNING);
        SparkMonitor exist = baseMapper.selectById(id);
        if (exist == null) {
            monitor.setCreateTime(new Date());
            baseMapper.insert(monitor);
        } else {
            monitor.setModifyTime(new Date());
            baseMapper.updateById(monitor);
        }
    }

    private SparkMonitor getSparkMonitor(Map<String, String> confMap) {
        String appName = confMap.get(SPARK_PARAM_APP_NAME());
        String confVersion = confMap.get(SPARK_PARAM_APP_CONF_VERSION());
        String appId = confMap.get(SPARK_PARAM_APP_ID());
        String proxyUri = confMap.get(SPARK_PARAM_APP_PROXY_URI_BASES());
        String startUp = confMap.get(SPARK_PARAM_DEPLOY_STARTUP());

        SparkMonitor monitor = new SparkMonitor(appId, appName, confVersion, startUp);
        if (!CommonUtils.isBlank(proxyUri)) {
            monitor.setTrackUrl(proxyUri.split(",")[0]);
        }
        return monitor;
    }

    /**
     * 该监控不存在,则算"失联"
     * 如存在:
     * 上次的状态如果为停止中,则本次记作已停止
     * 反之,都算意外终止,"失联"
     *
     * @param id
     * @param confMap
     */
    @Override
    public void shutdown(String id, Map<String, String> confMap) {
        SparkMonitor monitor = getSparkMonitor(confMap);
        monitor.setMyId(id);
        SparkMonitor exist = baseMapper.selectById(id);
        if (exist == null) {
            monitor.setStatusValue(SparkMonitor.Status.LOST);
            monitor.setCreateTime(new Date());
            baseMapper.insert(monitor);
        } else {
            SparkMonitor.Status status = SparkMonitor.Status.getByStatus(exist.getStatus());
            assert status != null;
            switch (status) {
                case KILLING:
                case KILL_FAILURE:
                case KILLED:
                    monitor.setStatusValue(SparkMonitor.Status.KILLED);
                    break;
                default:
                    monitor.setStatusValue(SparkMonitor.Status.LOST);
                    break;
            }
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

    /**
     * 返回 -1,没启动文件
     * 0:执行启动成功
     * 其他:启动失败.
     *
     * @param myId
     * @return
     */
    @Override
    public int start(String myId) {
        SparkMonitor monitor = this.getById(myId);
        String startUp = monitor.getStartUp();
        int exitCode = 1;
        if (StringUtils.isNotBlank(startUp)) {
            exitCode = CommandUtils.executeScript(CommandUtils.BASH_RUN_SCHEAM.concat(startUp));
            if (exitCode == 0) {
                //启动中..
                monitor.setStatusValue(SparkMonitor.Status.STARTING);
                this.updateById(monitor);
                this.checkStart(myId);
            } else {
                //启动失败
                monitor.setStatusValue(SparkMonitor.Status.START_FAILURE);
                this.updateById(monitor);
            }
        }
        return exitCode;
    }

    @Override
    public int stop(String myId) {
        SparkMonitor monitor = this.getById(myId);
        String cmd = String.format("yarn application -kill %s", monitor.getAppId());
        int exitCode = 1;
        try {
            String home = System.getProperty("streamx.home");
            exitCode = CommandUtils.runAsExecUser(home.concat("/bin/executor.so"), hadoopUser, cmd);
            if (exitCode == 0) {
                //停止中..
                monitor.setStatusValue(SparkMonitor.Status.KILLING);
                this.checkStop(myId);
            } else {
                //停止失败..
                monitor.setStatusValue(SparkMonitor.Status.KILL_FAILURE);
            }
            this.updateById(monitor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exitCode;
    }

    /**
     * 1分钟之后如果状态还是一直启动中,则认为启动超时,如启动2分钟还未检测到则认为启动失败..
     *
     * @param myId
     */
    @Override
    public void checkStart(String myId) {
        //启动超时检查
        checkExecutorService.schedule(() -> {
            SparkMonitor monitor = getById(myId);
            if (monitor.getStatus().equals(SparkMonitor.Status.STARTING.getValue())) {
                monitor.setStatusValue(SparkMonitor.Status.START_TIMEOUT);
                this.updateById(monitor);
            }
        }, 60, TimeUnit.SECONDS);
        //启动失败...
        checkExecutorService.schedule(() -> {
            SparkMonitor monitor = getById(myId);
            if (monitor.getStatus().equals(SparkMonitor.Status.START_TIMEOUT.getValue())) {
                monitor.setStatusValue(SparkMonitor.Status.START_FAILURE);
                this.updateById(monitor);
            }
        }, 120, TimeUnit.SECONDS);
    }

    /**
     * 30 秒之后如果状态还是一直停止中,则认为停止失败..
     *
     * @param myId
     */
    @Override
    public void checkStop(String myId) {
        checkExecutorService.schedule(() -> {
            SparkMonitor monitor = getById(myId);
            if (monitor.getStatus().equals(SparkMonitor.Status.KILLING.getValue())) {
                monitor.setStatusValue(SparkMonitor.Status.KILL_FAILURE);
                this.updateById(monitor);
            }
        }, 30, TimeUnit.SECONDS);
    }


}
