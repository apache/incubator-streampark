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

package org.apache.streampark.console.system.service.impl;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.system.entity.Variable;
import org.apache.streampark.console.system.mapper.VariableMapper;
import org.apache.streampark.console.system.service.VariableService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class VariableServiceImpl extends ServiceImpl<VariableMapper, Variable> implements VariableService {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private CommonService commonService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVariable(Variable variable) throws Exception {
        if (this.findByVariableCode(variable.getTeamId(), variable.getVariableCode()) != null) {
            throw new ApiAlertException("Sorry, the variable code already exists.");
        }
        variable.setCreator(commonService.getCurrentUser().getUserId());
        this.save(variable);
    }

    @Override
    public IPage<Variable> findVariables(Variable variable, RestRequest request) {
        Page<Variable> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        return this.baseMapper.findVariable(page, variable);
    }

    @Override
    public Variable findByVariableCode(Long teamId, String variableCode) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Variable>()
            .eq(Variable::getVariableCode, variableCode)
            .eq(Variable::getTeamId, teamId));
    }

    @Override
    public List<Variable> findByTeamId(Long teamId) {
        return baseMapper.selectByTeamId(teamId);
    }
}
