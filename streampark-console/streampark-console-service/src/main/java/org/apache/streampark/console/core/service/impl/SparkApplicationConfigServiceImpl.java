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
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkApplicationConfig;
import org.apache.streampark.console.core.enums.ConfigFileTypeEnum;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.mapper.SparkApplicationConfigMapper;
import org.apache.streampark.console.core.service.SparkApplicationConfigService;
import org.apache.streampark.console.core.service.SparkEffectiveService;

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
public class SparkApplicationConfigServiceImpl
    extends
        ServiceImpl<SparkApplicationConfigMapper, SparkApplicationConfig>
    implements
        SparkApplicationConfigService {

    private String sparkConfTemplate = null;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private SparkEffectiveService effectiveService;

    @Override
    public synchronized void create(SparkApplication appParam, Boolean latest) {
        String decode = new String(Base64.getDecoder().decode(appParam.getConfig()));
        String config = DeflaterUtils.zipString(decode.trim());

        SparkApplicationConfig applicationConfig = new SparkApplicationConfig();
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
        LambdaUpdateWrapper<SparkApplicationConfig> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.set(SparkApplicationConfig::getLatest, false).eq(SparkApplicationConfig::getAppId, appId);
        this.update(updateWrapper);

        updateWrapper.clear();
        updateWrapper.set(SparkApplicationConfig::getLatest, true).eq(SparkApplicationConfig::getId, configId);
        this.update(updateWrapper);
    }

    @Override
    public synchronized void update(SparkApplication appParam, Boolean latest) {
        // spark sql job
        SparkApplicationConfig latestConfig = getLatest(appParam.getId());
        if (appParam.isSparkSqlJob()) {
            updateForSparkSqlJob(appParam, latest, latestConfig);
        } else {
            updateForNonSparkSqlJob(appParam, latest, latestConfig);
        }
    }

    private void updateForNonSparkSqlJob(SparkApplication appParam, Boolean latest,
                                         SparkApplicationConfig latestConfig) {
        // may be re-selected a config file (without config id), or may be based on an original edit
        // (with config Id).
        Long configId = appParam.getConfigId();
        // an original edit
        if (configId != null) {
            SparkApplicationConfig config = this.getById(configId);
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
            SparkApplicationConfig config = getEffective(appParam.getId());
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

    private void updateForSparkSqlJob(
                                      SparkApplication appParam, Boolean latest, SparkApplicationConfig latestConfig) {
        // get effect config
        SparkApplicationConfig effectiveConfig = getEffective(appParam.getId());
        if (Utils.isEmpty(appParam.getConfig())) {
            if (effectiveConfig != null) {
                effectiveService.remove(appParam.getId(), EffectiveTypeEnum.SPARKCONFIG);
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
        LambdaUpdateWrapper<SparkApplicationConfig> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SparkApplicationConfig::getAppId, appId).set(SparkApplicationConfig::getLatest, false);
        this.update(updateWrapper);
        effectiveService.saveOrUpdate(appId, EffectiveTypeEnum.SPARKCONFIG, configId);
    }

    @Override
    public SparkApplicationConfig getLatest(Long appId) {
        return baseMapper.selectLatest(appId);
    }

    @Override
    public SparkApplicationConfig getEffective(Long appId) {
        return baseMapper.selectEffective(appId);
    }

    @Override
    public SparkApplicationConfig get(Long id) {
        SparkApplicationConfig config = getById(id);
        if (config.getContent() != null) {
            String unzipString = DeflaterUtils.unzipString(config.getContent());
            String encode = Base64.getEncoder().encodeToString(unzipString.getBytes());
            config.setContent(encode);
        }
        return config;
    }

    @Override
    public IPage<SparkApplicationConfig> getPage(SparkApplicationConfig config, RestRequest request) {
        request.setSortField("version");
        Page<SparkApplicationConfig> page = MybatisPager.getPage(request);
        IPage<SparkApplicationConfig> configList = this.baseMapper.selectPageByAppId(page, config.getAppId());
        fillEffectiveField(config.getAppId(), configList.getRecords());
        return configList;
    }

    @Override
    public List<SparkApplicationConfig> list(Long appId) {
        LambdaQueryWrapper<SparkApplicationConfig> queryWrapper = new LambdaQueryWrapper<SparkApplicationConfig>()
            .eq(SparkApplicationConfig::getAppId, appId)
            .orderByDesc(SparkApplicationConfig::getVersion);

        List<SparkApplicationConfig> configList = this.baseMapper.selectList(queryWrapper);
        fillEffectiveField(appId, configList);
        return configList;
    }

    @Override
    public synchronized String readTemplate() {
        if (sparkConfTemplate == null) {
            try {
                Resource resource = resourceLoader.getResource("classpath:spark-application.conf");
                Scanner scanner = new Scanner(resource.getInputStream());
                StringBuilder stringBuffer = new StringBuilder();
                while (scanner.hasNextLine()) {
                    stringBuffer.append(scanner.nextLine()).append(System.lineSeparator());
                }
                scanner.close();
                String template = stringBuffer.toString();
                this.sparkConfTemplate = Base64.getEncoder().encodeToString(template.getBytes());
            } catch (Exception e) {
                log.error("Read conf/spark-application.conf failed, please check your deployment");
                log.error(e.getMessage(), e);
            }
        }
        return this.sparkConfTemplate;
    }

    @Override
    public void removeByAppId(Long appId) {
        baseMapper.delete(
            new LambdaQueryWrapper<SparkApplicationConfig>().eq(SparkApplicationConfig::getAppId, appId));
    }

    private void fillEffectiveField(Long id, List<SparkApplicationConfig> configList) {
        SparkApplicationConfig effective = getEffective(id);

        if (effective == null) {
            return;
        }

        configList.stream()
            .filter(config -> config.getId().equals(effective.getId()))
            .findFirst()
            .ifPresent(config -> config.setEffective(true));
    }
}
