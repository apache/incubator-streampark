package com.streamxhub.spark.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import static com.streamxhub.spark.monitor.api.Const.*;
import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.core.dao.SparkConfMapper;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.domain.SparkConfRecord;
import com.streamxhub.spark.monitor.core.service.SparkConfRecordService;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;

import java.util.Date;
import java.util.List;


/**
 * @author benjobs
 */
@Slf4j
@Service("sparkConfService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkConfServiceImpl extends ServiceImpl<SparkConfMapper, SparkConf> implements SparkConfService {

    @Value("${spark.app.monitor.zookeeper}")
    private String zookeeperConnect;

    @Autowired
    private SparkConfRecordService confRecordService;

    @Override
    public boolean config(SparkConf sparkConf) {
        SparkConf existConf = baseMapper.selectById(sparkConf.getMyId());
        if (existConf == null) {
            sparkConf.setCreateTime(new Date());
            baseMapper.insert(sparkConf);
            return true;
        } else {
            if (sparkConf.getConfVersion().compareTo(existConf.getConfVersion()) > 0) {
                confRecordService.addRecord(sparkConf);
                sparkConf.setModifyTime(new Date());
                baseMapper.updateById(sparkConf);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public IPage<SparkConf> getPager(SparkConf sparkConf, QueryRequest request) {
        Page<SparkConf> page = new Page<>();
        QueryWrapper<SparkConf> wrapper = new QueryWrapper<>();
        if (sparkConf.getAppName() != null) {
            wrapper.like("APP_NAME", sparkConf.getAppName().trim());
        }
        wrapper.groupBy("MY_ID").orderByDesc("CONF_VERSION").orderByAsc("CREATE_TIME");
        IPage<SparkConf> pager = this.baseMapper.selectPage(page, wrapper);
        List<SparkConf> sparkConfList = pager.getRecords();

        for (SparkConf conf : sparkConfList) {
            List<SparkConfRecord> records = this.confRecordService.getRecords(conf.getMyId());
            conf.setHistory(records);
        }
        return pager;
    }

    @Override
    public Integer delete(String myId) {
        return this.baseMapper.deleteById(myId);
    }

    @Override
    public void update(String myId,String conf) {
        SparkConf existConf = getById(myId);
        //保存修改之前的记录
        confRecordService.addRecord(existConf);

        //保存配置
        existConf.setModifyTime(new Date());
        //版本号加1
        existConf.setConfVersion(existConf.getConfVersion() + 1);
        existConf.setConf(Base64Utils.encodeToString(conf.getBytes()));
        updateById(existConf);

        String path = SPARK_CONF_PATH_PREFIX().concat("/").concat(myId);
        //持久保存...
        ZooKeeperUtil.update(path,conf,zookeeperConnect,true);

    }
}
