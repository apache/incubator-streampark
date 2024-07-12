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
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.mapper.VariableMapper;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class VariableServiceImpl extends ServiceImpl<VariableMapper, Variable>
    implements
        VariableService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z])+([A-Za-z0-9._-])+\\}");

    private static final String PLACEHOLDER_START = "${";

    private static final String PLACEHOLDER_END = "}";

    @Autowired
    private ApplicationManageService applicationManageService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private ServiceHelper serviceHelper;

    @Override
    public void createVariable(Variable variable) {

        ApiAlertException.throwIfTrue(
            this.findByVariableCode(variable.getTeamId(), variable.getVariableCode()) != null,
            "The variable code already exists.");

        variable.setCreatorId(serviceHelper.getUserId());
        this.save(variable);
    }

    @Override
    public void remove(Variable variable) {
        ApiAlertException.throwIfTrue(
            isDependByApplications(variable), "The variable is actually used.");
        this.removeById(variable);
    }

    @Override
    public IPage<Variable> getPage(Variable variable, RestRequest request) {
        if (variable.getTeamId() == null) {
            return null;
        }
        Page<Variable> page = MybatisPager.getPage(request);
        return this.baseMapper.selectPage(page, variable);
    }

    @Override
    public IPage<Application> getDependAppsPage(Variable variable, RestRequest request) {
        List<Application> applications = getDependApplicationsByCode(variable);

        IPage<Application> page = new Page<>();
        if (CollectionUtils.isEmpty(applications)) {
            return page;
        }
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        page.setTotal(applications.size());
        int fromIndex = (request.getPageNum() - 1) * request.getPageSize();
        int toIndex = request.getPageNum() * request.getPageSize();
        toIndex = Math.min(toIndex, applications.size());
        page.setRecords(applications.subList(fromIndex, toIndex));
        return page;
    }

    @Override
    public void updateVariable(Variable variable) {
        // region update variable
        ApiAlertException.throwIfNull(variable.getId(), "The variable id cannot be null.");
        Variable findVariable = this.baseMapper.selectById(variable.getId());
        ApiAlertException.throwIfNull(findVariable, "The variable does not exist.");
        ApiAlertException.throwIfFalse(
            findVariable.getVariableCode().equals(variable.getVariableCode()),
            "The variable code cannot be updated.");
        this.baseMapper.updateById(variable);
        // endregion

        // set Application's field release to NEED_RESTART
        List<Application> applications = getDependApplicationsByCode(variable);
        if (CollectionUtils.isNotEmpty(applications)) {
            applicationManageService.update(
                new UpdateWrapper<Application>()
                    .lambda()
                    .in(
                        Application::getId,
                        applications.stream().map(Application::getId)
                            .collect(Collectors.toList()))
                    .set(Application::getRelease, ReleaseStateEnum.NEED_RESTART.get()));
        }
    }

    @Override
    public Variable findByVariableCode(Long teamId, String variableCode) {
        LambdaQueryWrapper<Variable> queryWrapper = new LambdaQueryWrapper<Variable>()
            .eq(Variable::getVariableCode, variableCode)
            .eq(Variable::getTeamId, teamId);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * get variables through team
     *
     * @param teamId
     * @return
     */
    @Override
    public List<Variable> listByTeamId(Long teamId) {
        return listByTeamId(teamId, null);
    }

    /**
     * Get variables through team and search keywords.
     *
     * @param teamId
     * @param keyword Fuzzy search keywords through variable code or description, Nullable.
     * @return
     */
    @Override
    public List<Variable> listByTeamId(Long teamId, String keyword) {
        return baseMapper.selectVarsByTeamId(teamId, keyword);
    }

    /**
     * Replace variable with defined variable codes.
     *
     * @param teamId
     * @param mixed Text with placeholders, e.g. "--cluster ${kafka.cluster}"
     * @return
     */
    @Override
    public String replaceVariable(Long teamId, String mixed) {
        if (StringUtils.isBlank(mixed)) {
            return mixed;
        }
        List<Variable> variables = listByTeamId(teamId);
        if (CollectionUtils.isEmpty(variables)) {
            return mixed;
        }
        Map<String, String> variableMap = variables.stream()
            .collect(Collectors.toMap(Variable::getVariableCode, Variable::getVariableValue));
        String restore = mixed;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(restore);
        while (matcher.find()) {
            String placeholder = matcher.group();
            String variableCode = getCodeFromPlaceholder(placeholder);
            String variableValue = variableMap.get(variableCode);
            if (StringUtils.isNotBlank(variableValue)) {
                restore = restore.replace(placeholder, variableValue);
            }
        }
        return restore;
    }

    private boolean isDependByApplications(Variable variable) {
        return CollectionUtils.isNotEmpty(getDependApplicationsByCode(variable));
    }

    private List<Application> getDependApplicationsByCode(Variable variable) {
        List<Application> dependApplications = new ArrayList<>();
        List<Application> applications = applicationManageService.listByTeamId(variable.getTeamId());
        Map<Long, Application> applicationMap = applications.stream()
            .collect(Collectors.toMap(Application::getId, application -> application));

        // Get applications that depend on this variable in application args
        for (Application app : applications) {
            if (isDepend(variable.getVariableCode(), app.getArgs())) {
                dependApplications.add(app);
            }
        }

        // Get the application that depends on this variable in flink sql
        List<FlinkSql> flinkSqls = flinkSqlService.listByTeamId(variable.getTeamId());
        for (FlinkSql flinkSql : flinkSqls) {
            if (isDepend(variable.getVariableCode(), DeflaterUtils.unzipString(flinkSql.getSql()))) {
                Application app = applicationMap.get(flinkSql.getAppId());
                if (!dependApplications.contains(app)) {
                    dependApplications.add(applicationMap.get(flinkSql.getAppId()));
                }
            }
        }
        return dependApplications;
    }

    /**
     * Determine whether variableCode is dependent on mixed.
     *
     * @param variableCode Variable code, e.g. "kafka.cluster"
     * @param mixed Text with placeholders, e.g. "--cluster ${kafka.cluster}"
     * @return If mixed can match the variableCode, return true, otherwise return false
     */
    private boolean isDepend(String variableCode, String mixed) {
        if (StringUtils.isBlank(mixed)) {
            return false;
        }
        String placeholder = String.format("%s%s%s", PLACEHOLDER_START, variableCode, PLACEHOLDER_END);
        return mixed.contains(placeholder);
    }

    private String getCodeFromPlaceholder(String placeholder) {
        return placeholder.substring(
            PLACEHOLDER_START.length(), placeholder.length() - PLACEHOLDER_END.length());
    }

    @Override
    public boolean existsByTeamId(Long teamId) {
        return this.baseMapper.existsByTeamId(teamId);
    }
}
