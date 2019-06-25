package com.streamxhub.spark.monitor.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.spark.monitor.core.dao.SparkConfMapper;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.service.SparkConfService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service("sparkConfService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkConfServiceImpl extends ServiceImpl<SparkConfMapper, SparkConf> implements SparkConfService {

    @Override
    public boolean config(SparkConf sparkConf) {
        SparkConf existConf = baseMapper.selectById(sparkConf.getConfId());
        if (existConf == null) {
            baseMapper.insert(sparkConf);
            baseMapper.saveRecord(sparkConf);
            return true;
        } else {
            if (sparkConf.getConfVersion().compareTo(existConf.getConfVersion()) > 0) {
                baseMapper.updateById(sparkConf);
                baseMapper.saveRecord(sparkConf);
                return true;
            } else {
                return false;
            }
        }
    }
}
