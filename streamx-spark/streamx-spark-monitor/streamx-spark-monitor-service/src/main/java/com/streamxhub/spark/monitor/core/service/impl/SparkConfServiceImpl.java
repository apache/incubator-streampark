package com.streamxhub.spark.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.spark.monitor.common.domain.Constant;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.common.utils.SortUtil;
import com.streamxhub.spark.monitor.core.dao.SparkConfMapper;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.domain.SparkConfRecord;
import com.streamxhub.spark.monitor.core.service.SparkConfRecordService;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


/**
 * @author benjobs
 */
@Slf4j
@Service("sparkConfService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkConfServiceImpl extends ServiceImpl<SparkConfMapper, SparkConf> implements SparkConfService {

    @Autowired
    private SparkConfRecordService confRecordService;

    @Override
    public boolean config(SparkConf sparkConf) {
        SparkConf existConf = baseMapper.selectById(sparkConf.getMyId());
        SparkConfRecord record = new SparkConfRecord(sparkConf.getMyId(), sparkConf.getAppName(), sparkConf.getConfVersion(), sparkConf.getConf());
        if (existConf == null) {
            sparkConf.setCreateTime(new Date());
            baseMapper.insert(sparkConf);
            confRecordService.save(record);
            return true;
        } else {
            if (sparkConf.getConfVersion().compareTo(existConf.getConfVersion()) > 0) {
                sparkConf.setModifyTime(new Date());
                baseMapper.updateById(sparkConf);
                confRecordService.save(record);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public IPage<SparkConf> getPager(SparkConf sparkConf, QueryRequest request) {
        Page<SparkConf> page = new Page<>();
        SortUtil.handlePageSort(request, page, "CREATE_TIME", Constant.ORDER_ASC, false);
        QueryWrapper<SparkConf> wrapper = new QueryWrapper<>();
        if (sparkConf.getAppName() != null) {
            wrapper.like("APP_NAME", sparkConf.getAppName().trim());
        }
        return this.baseMapper.selectPage(page, wrapper);
    }

    @Override
    public Integer delete(String myId) {
        return this.baseMapper.deleteById(myId);
    }
}
