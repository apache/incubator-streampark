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
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.FlinkEnvCheckEnum;
import org.apache.streampark.console.core.mapper.FlinkEnvMapper;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Date;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkEnvServiceImpl extends ServiceImpl<FlinkEnvMapper, FlinkEnv>
    implements
        FlinkEnvService {

    @Autowired
    private FlinkClusterService flinkClusterService;
    @Autowired
    private ApplicationInfoService applicationInfoService;

    /**
     * two places will be checked: <br>
     * 1) name repeated <br>
     * 2) flink-dist repeated <br>
     */
    @Override
    public FlinkEnvCheckEnum check(FlinkEnv version) {
        // 1) check name
        LambdaQueryWrapper<FlinkEnv> queryWrapper = new LambdaQueryWrapper<FlinkEnv>().eq(FlinkEnv::getFlinkName,
            version.getFlinkName());
        if (version.getId() != null) {
            queryWrapper.ne(FlinkEnv::getId, version.getId());
        }
        if (this.count(queryWrapper) > 0) {
            return FlinkEnvCheckEnum.NAME_REPEATED;
        }

        String lib = version.getFlinkHome().concat("/lib");
        File flinkLib = new File(lib);
        // 2) flink/lib path exists and is a directory
        if (!flinkLib.exists() || !flinkLib.isDirectory()) {
            return FlinkEnvCheckEnum.INVALID_PATH;
        }

        // 3) check flink-dist
        File[] files = flinkLib.listFiles(f -> f.getName().matches("flink-dist.*\\.jar"));
        if (files == null || files.length == 0) {
            return FlinkEnvCheckEnum.FLINK_DIST_NOT_FOUND;
        }

        if (files.length > 1) {
            return FlinkEnvCheckEnum.FLINK_DIST_REPEATED;
        }

        return FlinkEnvCheckEnum.OK;
    }

    @Override
    public boolean create(FlinkEnv version) {
        long count = this.baseMapper.selectCount(null);
        version.setIsDefault(count == 0);
        version.setCreateTime(new Date());
        version.doSetVersion();
        version.doSetFlinkConf();
        return save(version);
    }

    @Override
    public void removeById(Long id) {
        FlinkEnv flinkEnv = getById(id);
        checkOrElseAlert(flinkEnv);
        Long count = this.baseMapper.selectCount(null);
        ApiAlertException.throwIfFalse(
            !(count > 1 && flinkEnv.getIsDefault()),
            "The flink home is set as default, please change it first.");

        this.baseMapper.deleteById(id);
    }

    @Override
    public void update(FlinkEnv version) {
        FlinkEnv flinkEnv = getById(version.getId());
        checkOrElseAlert(flinkEnv);
        flinkEnv.setDescription(version.getDescription());
        flinkEnv.setFlinkName(version.getFlinkName());
        if (!version.getFlinkHome().equals(flinkEnv.getFlinkHome())) {
            flinkEnv.setFlinkHome(version.getFlinkHome());
            flinkEnv.doSetFlinkConf();
            flinkEnv.doSetVersion();
        }
        updateById(flinkEnv);
    }

    @Override
    public void setDefault(Long id) {
        this.baseMapper.setDefault(id);
    }

    @Override
    public FlinkEnv getByAppId(Long appId) {
        return this.baseMapper.selectByAppId(appId);
    }

    @Override
    public FlinkEnv getDefault() {
        return this.baseMapper.selectOne(
            new LambdaQueryWrapper<FlinkEnv>().eq(FlinkEnv::getIsDefault, true));
    }

    @Override
    public FlinkEnv getByIdOrDefault(Long id) {
        if (id == null) {
            return getDefault();
        }
        FlinkEnv flinkEnv = getById(id);
        return flinkEnv == null ? getDefault() : flinkEnv;
    }

    @Override
    public void syncConf(Long id) {
        FlinkEnv flinkEnv = getById(id);
        flinkEnv.doSetFlinkConf();
        updateById(flinkEnv);
    }

    @Override
    public void validity(Long id) {
        FlinkEnv flinkEnv = getById(id);
        checkOrElseAlert(flinkEnv);
    }

    private void checkOrElseAlert(FlinkEnv flinkEnv) {

        // 1.check exists
        ApiAlertException.throwIfNull(flinkEnv, "The flink home does not exist, please check.");

        // 2.check if it is being used by any flink cluster
        ApiAlertException.throwIfTrue(
            flinkClusterService.existsByFlinkEnvId(flinkEnv.getId()),
            "The flink home is still in use by some flink cluster, please check.");

        // 3.check if it is being used by any application
        ApiAlertException.throwIfTrue(
            applicationInfoService.existsByFlinkEnvId(flinkEnv.getId()),
            "The flink home is still in use by some application, please check.");
    }
}
