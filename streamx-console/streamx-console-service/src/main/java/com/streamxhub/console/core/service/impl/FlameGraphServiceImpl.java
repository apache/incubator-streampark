package com.streamxhub.console.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.console.core.dao.FlameGraphMapper;
import com.streamxhub.console.core.entity.FlameGraph;
import com.streamxhub.console.core.service.FlameGraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlameGraphServiceImpl extends ServiceImpl<FlameGraphMapper, FlameGraph> implements FlameGraphService {

}
