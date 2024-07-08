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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.enums.FlinkEnvCheckEnum;
import org.apache.streampark.console.core.mapper.SparkEnvMapper;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.SparkEnvService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkEnvServiceImpl extends ServiceImpl<SparkEnvMapper, SparkEnv>
        implements
            SparkEnvService {

    @Autowired
    private FlinkClusterService flinkClusterService;
    @Autowired
    private ApplicationInfoService applicationInfoService;

    /**
     * two places will be checked: <br>
     * 1) name repeated <br>
     * 2) spark jars repeated <br>
     */
    @Override
    public FlinkEnvCheckEnum check(SparkEnv version) {
        // 1) check name
        LambdaQueryWrapper<SparkEnv> queryWrapper = new LambdaQueryWrapper<SparkEnv>().eq(SparkEnv::getSparkName,
                version.getSparkName());
        if (version.getId() != null) {
            queryWrapper.ne(SparkEnv::getId, version.getId());
        }
        if (this.count(queryWrapper) > 0) {
            return FlinkEnvCheckEnum.NAME_REPEATED;
        }

        String lib = version.getSparkHome().concat("/jars");
        File sparkLib = new File(lib);
        // 2) spark/jars path exists and is a directory
        if (!sparkLib.exists() || !sparkLib.isDirectory()) {
            return FlinkEnvCheckEnum.INVALID_PATH;
        }

        return FlinkEnvCheckEnum.OK;
    }

    @Override
    public boolean create(SparkEnv version) throws Exception {
        long count = this.baseMapper.selectCount(null);
        version.setIsDefault(count == 0);
        version.setCreateTime(new Date());
        version.doSetSparkConf();
        version.doSetVersion();
        return save(version);
    }

    @Override
    public void removeById(Long id) {
        SparkEnv sparkEnv = getById(id);
        checkOrElseAlert(sparkEnv);
        Long count = this.baseMapper.selectCount(null);
        ApiAlertException.throwIfFalse(
                !(count > 1 && sparkEnv.getIsDefault()),
                "The spark home is set as default, please change it first.");

        this.baseMapper.deleteById(id);
    }

    @Override
    public void update(SparkEnv version) throws IOException {
        SparkEnv sparkEnv = getById(version.getId());
        checkOrElseAlert(sparkEnv);
        sparkEnv.setDescription(version.getDescription());
        sparkEnv.setSparkName(version.getSparkName());
        if (!version.getSparkHome().equals(sparkEnv.getSparkHome())) {
            sparkEnv.setSparkHome(version.getSparkHome());
            sparkEnv.doSetSparkConf();
            sparkEnv.doSetVersion();
        }
        updateById(sparkEnv);
    }

    @Override
    public void setDefault(Long id) {
        this.baseMapper.setDefault(id);
    }

    @Override
    public SparkEnv getByAppId(Long appId) {
        return this.baseMapper.selectByAppId(appId);
    }

    @Override
    public SparkEnv getDefault() {
        return this.baseMapper.selectOne(
                new LambdaQueryWrapper<SparkEnv>().eq(SparkEnv::getIsDefault, true));
    }

    @Override
    public SparkEnv getByIdOrDefault(Long id) {
        SparkEnv sparkEnv = getById(id);
        return sparkEnv == null ? getDefault() : sparkEnv;
    }

    @Override
    public void syncConf(Long id) {
        SparkEnv sparkEnv = getById(id);
        sparkEnv.doSetSparkConf();
        updateById(sparkEnv);
    }

    @Override
    public void validity(Long id) {
        SparkEnv sparkEnv = getById(id);
        checkOrElseAlert(sparkEnv);
    }

    private void checkOrElseAlert(SparkEnv sparkEnv) {

        // 1.check exists
        ApiAlertException.throwIfNull(sparkEnv, "The spark home does not exist, please check.");

        // todo : To be developed
        // 2.check if it is being used by any spark cluster
        // ApiAlertException.throwIfTrue(
        // flinkClusterService.existsByFlinkEnvId(sparkEnv.getId()),
        // "The spark home is still in use by some spark cluster, please check.");
        //
        // // 3.check if it is being used by any application
        // ApiAlertException.throwIfTrue(
        // applicationInfoService.existsBySparkEnvId(sparkEnv.getId()),
        // "The spark home is still in use by some application, please check.");
    }
}
