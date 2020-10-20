/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.common.util.HdfsUtils;
import com.streamxhub.console.base.exception.ServiceException;
import com.streamxhub.console.core.dao.ApplicationBackUpMapper;
import com.streamxhub.console.core.entity.ApplicationBackUp;
import com.streamxhub.console.core.entity.SavePoint;
import com.streamxhub.console.core.service.ApplicationBackUpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author benjobs
 */
@Slf4j
@Service("applicationBackUpService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationBackUpServiceImpl extends ServiceImpl<ApplicationBackUpMapper, ApplicationBackUp> implements ApplicationBackUpService {

    @Override
    public List<ApplicationBackUp> getBackups(ApplicationBackUp backUp) {
        QueryWrapper<ApplicationBackUp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",backUp.getAppId());
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public Boolean delete(Long id) throws ServiceException {
        ApplicationBackUp backUp = getById(id);
        try {
            if (HdfsUtils.exists(backUp.getPath())) {
                HdfsUtils.deleteFile(backUp.getPath());
            }
            removeById(id);
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
