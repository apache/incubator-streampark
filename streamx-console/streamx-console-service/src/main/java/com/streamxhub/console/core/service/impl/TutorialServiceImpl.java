package com.streamxhub.console.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.console.core.dao.TutorialMapper;
import com.streamxhub.console.core.entity.Tutorial;
import com.streamxhub.console.core.service.TutorialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TutorialServiceImpl extends ServiceImpl<TutorialMapper, Tutorial> implements TutorialService {
    @Override
    public Tutorial getByName(String name) {
        return this.baseMapper.getByName(name);
    }
}
