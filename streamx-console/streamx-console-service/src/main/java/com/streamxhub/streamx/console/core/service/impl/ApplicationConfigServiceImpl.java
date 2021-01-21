/*
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
package com.streamxhub.streamx.console.core.service.impl;

import java.io.*;
import java.util.Base64;
import java.util.Date;

import com.streamxhub.streamx.console.base.utils.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.utils.SortUtil;
import com.streamxhub.streamx.console.core.dao.ApplicationConfigMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationConfig;
import com.streamxhub.streamx.console.core.service.ApplicationConfigService;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationConfigServiceImpl
        extends ServiceImpl<ApplicationConfigMapper, ApplicationConfig>
        implements ApplicationConfigService {

    private String flinkConfTemplate = null;

    private String PROD_ENV_NAME = "prod";

    @Autowired
    private ApplicationContext context;

    @Override
    public synchronized void create(Application application) {
        String decode = new String(Base64.getDecoder().decode(application.getConfig()));
        String config = DeflaterUtils.zipString(decode);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setAppId(application.getId());
        applicationConfig.setActived(true);
        applicationConfig.setFormat(application.getFormat());
        applicationConfig.setContent(config);
        applicationConfig.setCreateTime(new Date());
        Integer version = this.baseMapper.getLastVersion(application.getId());
        applicationConfig.setVersion(version == null ? 1 : version + 1);
        // 先前的激活的配置设置为备胎....
        this.baseMapper.standby(application.getId());
        save(applicationConfig);
    }

    @Override
    public synchronized void update(Application application) {
        if (application.getConfigId() != null) {
            ApplicationConfig config = this.getById(application.getConfigId());
            String decode = new String(Base64.getDecoder().decode(application.getConfig()));
            String encode = DeflaterUtils.zipString(decode);
            // create...
            if (!config.getContent().equals(encode)) {
                this.create(application);
            } else {
                // 先前的激活的配置设置为备胎....
                this.baseMapper.standby(application.getId());
                // 将指定版本设置为激活
                this.baseMapper.active(application.getConfigId());
            }
        } else {
            ApplicationConfig config = this.getActived(application.getId());
            String decode = new String(Base64.getDecoder().decode(application.getConfig()));
            String encode = DeflaterUtils.zipString(decode);
            // create...
            if (!config.getContent().equals(encode)) {
                this.create(application);
            }
        }
    }

    private ApplicationConfig getVersion(Long id, Long configVersion) {
        QueryWrapper<ApplicationConfig> queryWrapper = new QueryWrapper();
        queryWrapper
                .lambda()
                .eq(ApplicationConfig::getAppId, id)
                .eq(ApplicationConfig::getVersion, configVersion);
        return this.getOne(queryWrapper);
    }

    @Override
    public ApplicationConfig getActived(Long id) {
        return this.baseMapper.getActived(id);
    }

    @Override
    public ApplicationConfig get(Long id) {
        ApplicationConfig config = getById(id);
        if (config.getContent() != null) {
            String unzipString = DeflaterUtils.unzipString(config.getContent());
            String encode = Base64.getEncoder().encodeToString(unzipString.getBytes());
            config.setContent(encode);
        }
        return config;
    }

    @Override
    public IPage<ApplicationConfig> page(ApplicationConfig config, RestRequest request) {
        Page<ApplicationConfig> page = new Page<>();
        SortUtil.handlePageSort(request, page, "version", Constant.ORDER_DESC, false);
        return this.baseMapper.page(page, config);
    }

    @Override
    public synchronized String readTemplate() {
        if (flinkConfTemplate == null) {
            String profiles = context.getEnvironment().getActiveProfiles()[0];
            InputStream inputStream = null;
            if (profiles.equals(PROD_ENV_NAME)) {
                //生产环境部署读取conf/flink-application.template
                try {
                    inputStream = new FileInputStream(WebUtil.getAppDir("conf").concat("/flink-application.template"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("flink-application.template");
            }
            assert inputStream != null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                this.flinkConfTemplate = builder.toString().trim();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.flinkConfTemplate;
    }
}
