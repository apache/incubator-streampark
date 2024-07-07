/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.enums.ConfigFileTypeEnum;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.mapper.ApplicationConfigMapper;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.EffectiveService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationConfigServiceImpl
        extends
            ServiceImpl<ApplicationConfigMapper, ApplicationConfig>
        implements
            ApplicationConfigService {

    private String flinkConfTemplate = null;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private EffectiveService effectiveService;

    @Override
    public synchronized void create(Application appParam, Boolean latest) {
        String decode = new String(Base64.getDecoder().decode(appParam.getConfig()));
        String config = DeflaterUtils.zipString(decode.trim());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setAppId(appParam.getId());

        if (appParam.getFormat() != null) {
            ConfigFileTypeEnum fileType = ConfigFileTypeEnum.of(appParam.getFormat());
            ApiAlertException.throwIfTrue(
                    fileType == null || ConfigFileTypeEnum.UNKNOWN == fileType,
                    "application' config error. must be (.properties|.yaml|.yml |.conf)");

            applicationConfig.setFormat(fileType.getValue());
        }

        applicationConfig.setContent(config);
        applicationConfig.setCreateTime(new Date());
        Integer version = this.baseMapper.selectLastVersion(appParam.getId());
        applicationConfig.setVersion(version == null ? 1 : version + 1);
        save(applicationConfig);
        this.setLatestOrEffective(latest, applicationConfig.getId(), appParam.getId());
    }

    public void setLatest(Long appId, Long configId) {
        LambdaUpdateWrapper<ApplicationConfig> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.set(ApplicationConfig::getLatest, false).eq(ApplicationConfig::getAppId, appId);
        this.update(updateWrapper);

        updateWrapper.clear();
        updateWrapper.set(ApplicationConfig::getLatest, true).eq(ApplicationConfig::getId, configId);
        this.update(updateWrapper);
    }

    @Override
    public synchronized void update(Application appParam, Boolean latest) {
        // flink sql job
        ApplicationConfig latestConfig = getLatest(appParam.getId());
        if (appParam.isFlinkSqlJob()) {
            updateForFlinkSqlJob(appParam, latest, latestConfig);
        } else {
            updateForNonFlinkSqlJob(appParam, latest, latestConfig);
        }
    }

    private void updateForNonFlinkSqlJob(Application appParam, Boolean latest, ApplicationConfig latestConfig) {
        // may be re-selected a config file (without config id), or may be based on an original edit
        // (with config Id).
        Long configId = appParam.getConfigId();
        // an original edit
        if (configId != null) {
            ApplicationConfig config = this.getById(configId);
            String decode = new String(Base64.getDecoder().decode(appParam.getConfig()));
            String encode = DeflaterUtils.zipString(decode.trim());
            // create...
            if (!config.getContent().equals(encode)) {
                if (latestConfig != null) {
                    removeById(latestConfig.getId());
                }
                this.create(appParam, latest);
            } else {
                this.setLatestOrEffective(latest, configId, appParam.getId());
            }
        } else {
            ApplicationConfig config = getEffective(appParam.getId());
            if (config != null) {
                String decode = new String(Base64.getDecoder().decode(appParam.getConfig()));
                String encode = DeflaterUtils.zipString(decode.trim());
                // create...
                if (!config.getContent().equals(encode)) {
                    this.create(appParam, latest);
                }
            } else {
                this.create(appParam, latest);
            }
        }
    }

    private void updateForFlinkSqlJob(
                                      Application appParam, Boolean latest, ApplicationConfig latestConfig) {
        // get effect config
        ApplicationConfig effectiveConfig = getEffective(appParam.getId());
        if (Utils.isEmpty(appParam.getConfig())) {
            if (effectiveConfig != null) {
                effectiveService.remove(appParam.getId(), EffectiveTypeEnum.CONFIG);
            }
        } else {
            // there was no configuration before, is a new configuration
            if (effectiveConfig == null) {
                if (latestConfig != null) {
                    removeById(latestConfig.getId());
                }
                this.create(appParam, latest);
            } else {
                String decode = new String(Base64.getDecoder().decode(appParam.getConfig()));
                String encode = DeflaterUtils.zipString(decode.trim());
                // need to diff the two configs are consistent
                if (!effectiveConfig.getContent().equals(encode)) {
                    if (latestConfig != null) {
                        removeById(latestConfig.getId());
                    }
                    this.create(appParam, latest);
                }
            }
        }
    }

    /** Not running tasks are set to Effective, running tasks are set to Latest */
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
        LambdaUpdateWrapper<ApplicationConfig> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(ApplicationConfig::getAppId, appId).set(ApplicationConfig::getLatest, false);
        this.update(updateWrapper);
        effectiveService.saveOrUpdate(appId, EffectiveTypeEnum.CONFIG, configId);
    }

    @Override
    public ApplicationConfig getLatest(Long appId) {
        return baseMapper.selectLatest(appId);
    }

    @Override
    public ApplicationConfig getEffective(Long appId) {
        return baseMapper.selectEffective(appId);
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
    public IPage<ApplicationConfig> getPage(ApplicationConfig config, RestRequest request) {
        request.setSortField("version");
        Page<ApplicationConfig> page = MybatisPager.getPage(request);
        IPage<ApplicationConfig> configList = this.baseMapper.selectPageByAppId(page, config.getAppId());
        fillEffectiveField(config.getAppId(), configList.getRecords());
        return configList;
    }

    @Override
    public List<ApplicationConfig> list(Long appId) {
        LambdaQueryWrapper<ApplicationConfig> queryWrapper = new LambdaQueryWrapper<ApplicationConfig>()
                .eq(ApplicationConfig::getAppId, appId)
                .orderByDesc(ApplicationConfig::getVersion);

        List<ApplicationConfig> configList = this.baseMapper.selectList(queryWrapper);
        fillEffectiveField(appId, configList);
        return configList;
    }

    @Override
    public synchronized String readTemplate() {
        if (flinkConfTemplate == null) {
            try {
                Resource resource = resourceLoader.getResource("classpath:flink-application.conf");
                Scanner scanner = new Scanner(resource.getInputStream());
                StringBuilder stringBuffer = new StringBuilder();
                while (scanner.hasNextLine()) {
                    stringBuffer.append(scanner.nextLine()).append(System.lineSeparator());
                }
                scanner.close();
                String template = stringBuffer.toString();
                this.flinkConfTemplate = Base64.getEncoder().encodeToString(template.getBytes());
            } catch (Exception e) {
                log.error("Read conf/flink-application.conf failed, please check your deployment");
                log.error(e.getMessage(), e);
            }
        }
        return this.flinkConfTemplate;
    }

    @Override
    public void removeByAppId(Long appId) {
        baseMapper.delete(
                new LambdaQueryWrapper<ApplicationConfig>().eq(ApplicationConfig::getAppId, appId));
    }

    private void fillEffectiveField(Long id, List<ApplicationConfig> configList) {
        ApplicationConfig effective = getEffective(id);

        if (effective == null) {
            return;
        }

        configList.stream()
                .filter(config -> config.getId().equals(effective.getId()))
                .findFirst()
                .ifPresent(config -> config.setEffective(true));
    }
}
