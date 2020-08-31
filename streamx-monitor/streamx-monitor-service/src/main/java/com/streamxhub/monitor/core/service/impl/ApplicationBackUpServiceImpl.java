package com.streamxhub.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.monitor.base.domain.RestRequest;
import com.streamxhub.monitor.core.dao.ApplicationBackUpMapper;
import com.streamxhub.monitor.core.entity.ApplicationBackUp;
import com.streamxhub.monitor.core.service.ApplicationBackUpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service("applicationBackUpService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationBackUpServiceImpl extends ServiceImpl<ApplicationBackUpMapper, ApplicationBackUp> implements ApplicationBackUpService {

    @Override
    public IPage<ApplicationBackUp> query(ApplicationBackUp backUp, RestRequest request) {
        return null;
    }
}
