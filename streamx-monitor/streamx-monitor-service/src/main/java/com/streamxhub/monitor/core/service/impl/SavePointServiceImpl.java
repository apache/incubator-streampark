package com.streamxhub.monitor.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.monitor.core.dao.SavePointMapper;
import com.streamxhub.monitor.core.entity.SavePoint;
import com.streamxhub.monitor.core.service.SavePointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("savePointService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SavePointServiceImpl extends ServiceImpl<SavePointMapper, SavePoint> implements SavePointService {

    @Override
    public void obsolete(Long appId) {
        this.baseMapper.obsolete(appId);
    }

    @Override
    public SavePoint getLastest(Long id) {
       return this.baseMapper.getLastest(id);
    }
}
