package com.streamxhub.spark.monitor.core.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.spark.monitor.core.dao.SparkConfRecordMapper;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.domain.SparkConfRecord;
import com.streamxhub.spark.monitor.core.service.SparkConfRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author benjobs
 */
@Service("sparkConfRecordService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkConfRecordServiceImpl extends ServiceImpl<SparkConfRecordMapper, SparkConfRecord> implements SparkConfRecordService {

    @Override
    public void delete(String myId) {
        UpdateWrapper<SparkConfRecord> wrapper = new UpdateWrapper<>();
        wrapper.eq("MY_ID", myId);
        baseMapper.delete(wrapper);
    }

    @Override
    public List<SparkConfRecord> getRecords(String myId) {
        QueryWrapper<SparkConfRecord> historyWrapper = new QueryWrapper<>();
        historyWrapper.eq("MY_ID", myId).orderByDesc("CONF_VERSION");
        return this.baseMapper.selectList(historyWrapper);
    }

    @Override
    public void addRecord(SparkConf sparkConf) {
        SparkConfRecord record = new SparkConfRecord(sparkConf.getMyId(), sparkConf.getAppName(), sparkConf.getConfVersion(), sparkConf.getConf());
        record.setCreateTime(new Date());
        record.setConfOwner(sparkConf.getConfOwner());
        save(record);

        QueryWrapper<SparkConfRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MY_ID", sparkConf.getMyId());
        queryWrapper.ne("RECORD_ID", record.getRecordId());
        queryWrapper.eq("PARENT_ID", 0);

        SparkConfRecord existRecord = baseMapper.selectOne(queryWrapper);
        if (existRecord != null) {
            existRecord.setParentId(record.getRecordId());
            baseMapper.updateById(existRecord);
        }
    }
}
