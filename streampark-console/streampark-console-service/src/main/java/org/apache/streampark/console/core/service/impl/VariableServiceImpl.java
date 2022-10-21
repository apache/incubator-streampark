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
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Variable;
import org.apache.streampark.console.core.mapper.VariableMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.VariableService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class VariableServiceImpl extends ServiceImpl<VariableMapper, Variable> implements VariableService {

    private final Pattern placeholderPattern = Pattern.compile("\\$\\{([A-Za-z])+([A-Za-z0-9._-])+\\}");

    @Value("${streampark.variable.placeholder-start}")
    private String placeholderStart;

    @Value("${streampark.variable.placeholder-end}")
    private String placeholderEnd;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private CommonService commonService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVariable(Variable variable) {
        if (this.findByVariableCode(variable.getTeamId(), variable.getVariableCode()) != null) {
            throw new ApiAlertException("Sorry, the variable code already exists.");
        }
        variable.setCreatorId(commonService.getUserId());
        this.save(variable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVariable(Variable variable) {
        if (isDependByApplications(variable)) {
            throw new ApiAlertException("Sorry, the variable is actually used.");
        }
        this.removeById(variable);
    }

    @Override
    public IPage<Variable> page(Variable variable, RestRequest request) {
        if (variable.getTeamId() == null) {
            return null;
        }
        Page<Variable> page = new MybatisPager<Variable>().getDefaultPage(request);
        return this.baseMapper.page(page, variable);
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

    /**
     * Replace placeholders with defined variable codes.
     * @param teamId
     * @param paramWithPlaceholders Parameters with placeholders, e.g. "--cluster ${kafka.cluster}"
     * @return
     */
    @Override
    public String parseVariable(Long teamId, String paramWithPlaceholders) {
        if (StringUtils.isEmpty(paramWithPlaceholders)) {
            return paramWithPlaceholders;
        }
        List<Variable> variables = findByTeamId(teamId);
        if (CollectionUtils.isEmpty(variables)) {
            return paramWithPlaceholders;
        }
        Map<String, String> variableMap = variables.stream().collect(Collectors.toMap(Variable::getVariableCode, Variable::getVariableValue));
        String restore = paramWithPlaceholders;
        Matcher matcher = placeholderPattern.matcher(restore);
        while (matcher.find()) {
            String placeholder = matcher.group();
            String variableCode = getCodeFromPlaceholder(placeholder);
            String variableVaule = variableMap.get(variableCode);
            if (StringUtils.isNotEmpty(variableVaule)) {
                restore = restore.replace(placeholder, variableVaule);
            }
        }
        return restore;
    }

    private boolean isDependByApplications(Variable variable) {
        // Detect whether the variable is dependent on the args of the application
        List<Application> applications = applicationService.getByTeamId(variable.getTeamId());
        if (applications != null) {
            for (Application app : applications) {
                if (isDepend(variable.getVariableCode(), app.getArgs())) {
                    return true;
                }
            }
        }

        // Detect whether variables are dependent on all versions of flink sql
        List<FlinkSql> flinkSqls = flinkSqlService.getByTeamId(variable.getTeamId());
        if (flinkSqls != null) {
            for (FlinkSql flinkSql : flinkSqls) {
                if (isDepend(variable.getVariableCode(), DeflaterUtils.unzipString(flinkSql.getSql()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine whether variableCode is dependent on paramWithPlaceholders.
     * @param variableCode Variable code, e.g. "kafka.cluster"
     * @param paramWithPlaceholders Parameters with placeholders, e.g. "--cluster ${kafka.cluster}"
     * @return If paramWithPlaceholders can match the variableCode, return true, otherwise return false
     */
    private boolean isDepend(String variableCode, String paramWithPlaceholders) {
        if (StringUtils.isEmpty(paramWithPlaceholders)) {
            return false;
        }
        String placeholder = String.format("%s%s%s", placeholderStart, variableCode, placeholderEnd);
        return paramWithPlaceholders.contains(placeholder);
    }

    private String getCodeFromPlaceholder(String placeholder) {
        return placeholder.substring(placeholderStart.length(), placeholder.length() - placeholderEnd.length());
    }
}
