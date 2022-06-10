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

package com.streamxhub.streamx.console.base.util;

import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;

/**
 * 处理排序工具类
 * @author benjobs
 */
@SuppressWarnings("unchecked")
public final class SortUtils {

    private SortUtils() {

    }

    /**
     * 处理排序（分页情况下） for mybatis-plus
     *
     * @param request           QueryRequest
     * @param page              Page
     * @param defaultSort       默认排序的字段
     * @param defaultOrder      默认排序规则
     * @param camelToUnderscore 是否开启驼峰转下划线
     */
    public static void handlePageSort(
        RestRequest request,
        Page page,
        String defaultSort,
        String defaultOrder,
        boolean camelToUnderscore) {
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        String sortField = WebUtils.camelToUnderscore(request.getSortField());
        if (camelToUnderscore) {
            sortField = WebUtils.camelToUnderscore(sortField);
            defaultSort = WebUtils.camelToUnderscore(defaultSort);
        }
        if (StringUtils.isNotBlank(request.getSortField())
            && StringUtils.isNotBlank(request.getSortOrder())
            && !StringUtils.equalsIgnoreCase(request.getSortField(), "undefined")
            && !StringUtils.equalsIgnoreCase(request.getSortOrder(), "undefined")) {
            if (StringUtils.equals(request.getSortOrder(), Constant.ORDER_DESC)) {
                page.setDesc(sortField);
            } else {
                page.setAsc(sortField);
            }
        } else {
            if (StringUtils.isNotBlank(defaultSort)) {
                if (StringUtils.equals(defaultOrder, Constant.ORDER_DESC)) {
                    page.setDesc(defaultSort);
                } else {
                    page.setAsc(defaultSort);
                }
            }
        }
    }

    /**
     * 处理排序 for mybatis-plus
     *
     * @param request QueryRequest
     * @param page    Page
     */
    public static void handlePageSort(RestRequest request, Page page) {
        handlePageSort(request, page, null, null, false);
    }

    /**
     * 处理排序 for mybatis-plus
     *
     * @param request           QueryRequest
     * @param page              Page
     * @param camelToUnderscore 是否开启驼峰转下划线
     */
    public static void handlePageSort(RestRequest request, Page page, boolean camelToUnderscore) {
        handlePageSort(request, page, null, null, camelToUnderscore);
    }

    /**
     * 处理排序 for mybatis-plus
     *
     * @param request           QueryRequest
     * @param wrapper           wrapper
     * @param defaultSort       默认排序的字段
     * @param defaultOrder      默认排序规则
     * @param camelToUnderscore 是否开启驼峰转下划线
     */
    public static void handleWrapperSort(
        RestRequest request,
        QueryWrapper wrapper,
        String defaultSort,
        String defaultOrder,
        boolean camelToUnderscore) {
        String sortField = request.getSortField();
        if (camelToUnderscore) {
            sortField = WebUtils.camelToUnderscore(sortField);
            defaultSort = WebUtils.camelToUnderscore(defaultSort);
        }
        if (StringUtils.isNotBlank(request.getSortField())
            && StringUtils.isNotBlank(request.getSortOrder())
            && !StringUtils.equalsIgnoreCase(request.getSortField(), "undefined")
            && !StringUtils.equalsIgnoreCase(request.getSortOrder(), "undefined")) {
            if (StringUtils.equals(request.getSortOrder(), Constant.ORDER_DESC)) {
                wrapper.orderByDesc(sortField);
            } else {
                wrapper.orderByAsc(sortField);
            }
        } else {
            if (StringUtils.isNotBlank(defaultSort)) {
                if (StringUtils.equals(defaultOrder, Constant.ORDER_DESC)) {
                    wrapper.orderByDesc(defaultSort);
                } else {
                    wrapper.orderByAsc(defaultSort);
                }
            }
        }
    }

    /**
     * 处理排序 for mybatis-plus
     *
     * @param request QueryRequest
     * @param wrapper wrapper
     */
    public static void handleWrapperSort(RestRequest request, QueryWrapper wrapper) {
        handleWrapperSort(request, wrapper, null, null, false);
    }

    /**
     * 处理排序 for mybatis-plus
     *
     * @param request           QueryRequest
     * @param wrapper           wrapper
     * @param camelToUnderscore 是否开启驼峰转下划线
     */
    public static void handleWrapperSort(
        RestRequest request, QueryWrapper wrapper, boolean camelToUnderscore) {
        handleWrapperSort(request, wrapper, null, null, camelToUnderscore);
    }
}
