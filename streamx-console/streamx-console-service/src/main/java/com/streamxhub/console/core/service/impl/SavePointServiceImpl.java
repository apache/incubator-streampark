package com.streamxhub.console.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.common.util.HdfsUtils;
import com.streamxhub.console.base.exception.ServiceException;
import com.streamxhub.console.core.dao.SavePointMapper;
import com.streamxhub.console.core.entity.SavePoint;
import com.streamxhub.console.core.service.SavePointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author benjobs
 */
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

    @Override
    public List<SavePoint> getHistory(Long appId) {
        return this.baseMapper.getHistory(appId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) throws ServiceException {
        SavePoint savePoint = getById(id);
        try {
            if (HdfsUtils.exists(savePoint.getSavePoint())) {
                HdfsUtils.deleteFile(savePoint.getSavePoint());
            }
            removeById(id);
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
