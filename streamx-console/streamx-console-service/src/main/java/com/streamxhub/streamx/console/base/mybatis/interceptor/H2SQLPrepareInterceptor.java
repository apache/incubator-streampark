/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.base.mybatis.interceptor;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * the mybatis interceptor for update/insert/delete in h2
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class H2SQLPrepareInterceptor extends JsqlParserSupport implements Interceptor {

    @Override
    public Object intercept(final Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(statementHandler);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.UPDATE) {
            PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
            mpBs.sql(parserMulti(mpBs.sql(), boundSql));
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(final Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(final Properties properties) {

    }

    /**
     * 更新
     *
     * @param update 更新对象
     * @param index  下标
     * @param sql    sql
     * @param obj    额外参数
     */
    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        if (obj instanceof BoundSql) {
            BoundSql boundSql = (BoundSql) obj;
            Object parameterObject = boundSql.getParameterObject();
            if (parameterObject instanceof MapperMethod.ParamMap<?>) {
                MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) parameterObject;
                if (!paramMap.containsKey(Constants.ENTITY)) {
                    return;
                }
                Object entity = paramMap.get(Constants.ENTITY);
                if (Objects.nonNull(entity) && paramMap.containsKey(Constants.WRAPPER)) {
                    TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
                    Map<String, String> columnPropertyMap = tableInfo.getFieldList().stream().collect(Collectors.toMap(TableFieldInfo::getColumn, TableFieldInfo::getProperty));

                    // 去重set片段中的列
                    Map<String, Long> columnNameCountMap = update.getUpdateSets().stream().flatMap(set -> set.getColumns().stream())
                        .collect(Collectors.groupingBy(Column::getColumnName, Collectors.counting()));

                    columnNameCountMap.forEach((column, times) -> {
                        for (long i = 1L; i < times; ) {
                            Iterator<UpdateSet> updateSetIterator = update.getUpdateSets().iterator();
                            while (((Iterator<?>) updateSetIterator).hasNext()) {
                                UpdateSet updateSet = updateSetIterator.next();
                                if (updateSet.getColumns().stream().anyMatch(c -> c.getColumnName().equals(column))) {
                                    updateSetIterator.remove();
                                    break;
                                }
                            }
                            Iterator<ParameterMapping> parameterMappingIterator = boundSql.getParameterMappings().iterator();
                            while (parameterMappingIterator.hasNext()) {
                                ParameterMapping parameterMapping = parameterMappingIterator.next();
                                String property = columnPropertyMap.get(column);
                                if (Objects.nonNull(property) && parameterMapping.getProperty().equals(Constants.ENTITY_DOT + property)) {
                                    parameterMappingIterator.remove();
                                    break;
                                }
                            }
                            columnNameCountMap.put(column, times--);
                        }
                    });
                }
            }
        }
    }
}
