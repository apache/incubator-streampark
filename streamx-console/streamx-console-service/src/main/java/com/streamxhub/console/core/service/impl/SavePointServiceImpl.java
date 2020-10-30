package com.streamxhub.console.core.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.common.util.HdfsUtils;
import com.streamxhub.console.base.domain.Constant;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.base.exception.ServiceException;
import com.streamxhub.console.base.utils.SortUtil;
import com.streamxhub.console.core.dao.SavePointMapper;
import com.streamxhub.console.core.entity.SavePoint;
import com.streamxhub.console.core.service.SavePointService;
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

    @Override
    public IPage<SavePoint> page(SavePoint savePoint, RestRequest request) {
        Page<SavePoint> page = new Page<>();
        SortUtil.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        return this.baseMapper.page(page, savePoint);
    }
}
