/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.util.SortUtils;
import com.streamxhub.streamx.console.core.dao.ApplicationConfigMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationConfig;
import com.streamxhub.streamx.console.core.enums.EffectiveType;
import com.streamxhub.streamx.console.core.service.ApplicationConfigService;
import com.streamxhub.streamx.console.core.service.EffectiveService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

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

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private EffectiveService effectiveService;

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public synchronized void create(Application application, Boolean latest) {
        String decode = new String(Base64.getDecoder().decode(application.getConfig()));
        String config = DeflaterUtils.zipString(decode.trim());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setAppId(application.getId());
        applicationConfig.setFormat(application.getFormat());
        applicationConfig.setContent(config);
        applicationConfig.setCreateTime(new Date());
        Integer version = this.baseMapper.getLastVersion(application.getId());
        applicationConfig.setVersion(version == null ? 1 : version + 1);
        save(applicationConfig);
        this.setLatestOrEffective(latest, applicationConfig.getId(), application.getId());
    }

    @Transactional(rollbackFor = {Exception.class})
    public void setLatest(Long appId, Long configId) {
        LambdaUpdateWrapper<ApplicationConfig> updateWrapper = new UpdateWrapper<ApplicationConfig>().lambda();
        updateWrapper.set(ApplicationConfig::getLatest, 0)
            .eq(ApplicationConfig::getAppId, appId);
        this.update(updateWrapper);

        updateWrapper = new UpdateWrapper<ApplicationConfig>().lambda();
        updateWrapper.set(ApplicationConfig::getLatest, 1)
            .eq(ApplicationConfig::getId, configId);
        this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public synchronized void update(Application application, Boolean latest) {
        //flink sql job
        ApplicationConfig latestConfig = getLatest(application.getId());
        if (application.isFlinkSqlJob()) {
            //获取当前正在生效的配置
            ApplicationConfig effectiveConfig = getEffective(application.getId());
            //删除配置了...
            if (Utils.isEmpty(application.getConfig())) {
                if (effectiveConfig != null) {
                    //删除..
                    effectiveService.delete(application.getId(), EffectiveType.CONFIG);
                }
            } else {
                //之前没有配置,本次新增了配置...
                if (effectiveConfig == null) {
                    if (latestConfig != null) {
                        removeById(latestConfig.getId());
                    }
                    this.create(application, latest);
                } else {
                    String decode = new String(Base64.getDecoder().decode(application.getConfig()));
                    String encode = DeflaterUtils.zipString(decode.trim());
                    //需要对比两次配置是否一致,
                    if (!effectiveConfig.getContent().equals(encode)) {
                        if (latestConfig != null) {
                            removeById(latestConfig.getId());
                        }
                        this.create(application, latest);
                    }
                }
            }
        } else {
            // 可能会重新选择一个配置文件(无configId),也可能基于原有的编辑(有configId).
            Long configId = application.getConfigId();
            //基于原有的配置编辑...
            if (configId != null) {
                ApplicationConfig config = this.getById(configId);
                String decode = new String(Base64.getDecoder().decode(application.getConfig()));
                String encode = DeflaterUtils.zipString(decode.trim());
                // create...
                if (!config.getContent().equals(encode)) {
                    if (latestConfig != null) {
                        removeById(latestConfig.getId());
                    }
                    this.create(application, latest);
                } else {
                    this.setLatestOrEffective(latest, configId, application.getId());
                }
            } else {
                ApplicationConfig config = getEffective(application.getId());
                if (config != null) {
                    String decode = new String(Base64.getDecoder().decode(application.getConfig()));
                    String encode = DeflaterUtils.zipString(decode.trim());
                    // create...
                    if (!config.getContent().equals(encode)) {
                        this.create(application, latest);
                    }
                } else {
                    this.create(application, latest);
                }
            }
        }
    }

    /**
     * 未运行的任务设置为Effective
     * 正在运行的设置成Latest,"Latest"仅仅是个标记
     */
    @Override
    public void setLatestOrEffective(Boolean latest, Long configId, Long appId) {
        if (latest) {
            this.setLatest(appId, configId);
        } else {
            this.toEffective(appId, configId);
        }
    }

    @Override
    public void toEffective(Long appId, Long configId) {
        this.baseMapper.clearLatest(appId);
        effectiveService.saveOrUpdate(appId, EffectiveType.CONFIG, configId);
    }

    @Override
    public ApplicationConfig getLatest(Long appId) {
        return baseMapper.getLatest(appId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ApplicationConfig getEffective(Long appId) {
        return baseMapper.getEffective(appId);
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
        SortUtils.handlePageSort(request, page, "version", Constant.ORDER_DESC, false);
        return this.baseMapper.page(page, config.getAppId());
    }

    @Override
    public List<ApplicationConfig> history(Application application) {
        LambdaQueryWrapper<ApplicationConfig> wrapper = new QueryWrapper<ApplicationConfig>().lambda();
        wrapper.eq(ApplicationConfig::getAppId, application.getId())
            .orderByDesc(ApplicationConfig::getVersion);

        List<ApplicationConfig> configList = this.baseMapper.selectList(wrapper);
        ApplicationConfig effective = getEffective(application.getId());

        if (effective != null) {
            for (ApplicationConfig config : configList) {
                if (config.getId().equals(effective.getId())) {
                    config.setEffective(true);
                    break;
                }
            }
        }
        return configList;
    }

    @Override
    public synchronized String readTemplate() {
        if (flinkConfTemplate == null) {
            try {
                Resource resource = resourceLoader.getResource("classpath:flink-application.template");
                Scanner scanner = new Scanner(resource.getInputStream());
                StringBuilder stringBuffer = new StringBuilder();
                while (scanner.hasNextLine()) {
                    stringBuffer.append(scanner.nextLine())
                        .append(System.lineSeparator());
                }
                scanner.close();
                String template = stringBuffer.toString();
                this.flinkConfTemplate = Base64.getEncoder().encodeToString(template.getBytes());
            } catch (Exception e) {
                log.error("Read conf/flink-application.template failed, please check your deployment");
                e.printStackTrace();
            }
        }
        return this.flinkConfTemplate;
    }

    @Override
    public void removeApp(Long appId) {
        baseMapper.removeApp(appId);
    }
}
