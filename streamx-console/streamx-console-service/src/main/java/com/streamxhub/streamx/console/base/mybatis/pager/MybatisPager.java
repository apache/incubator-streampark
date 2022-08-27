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

package com.streamxhub.streamx.console.base.mybatis.pager;

import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.util.WebUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author benjobs
 */
@SuppressWarnings("unchecked")
public final class MybatisPager<T> {

    public Page<T> getDefaultPage(RestRequest request) {
        return getPage(request, "create_time", Constant.ORDER_DESC);
    }

    /**
     *
     * @param request
     * @param defaultSort
     * @param defaultOrder
     * @return
     */
    public Page<T> getPage(
        RestRequest request,
        String defaultSort,
        String defaultOrder) {

        Page<T> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        String sortField = WebUtils.camelToUnderscore(request.getSortField());

        List<OrderItem> orderItems = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getSortField())
            && StringUtils.isNotBlank(request.getSortOrder())
            && !StringUtils.equalsIgnoreCase(request.getSortField(), "undefined")
            && !StringUtils.equalsIgnoreCase(request.getSortOrder(), "undefined")) {
            if (StringUtils.equals(request.getSortOrder(), Constant.ORDER_DESC)) {
                orderItems.add(OrderItem.desc(sortField));
            } else {
                orderItems.add(OrderItem.asc(sortField));
            }
        } else {
            if (StringUtils.isNotBlank(defaultSort)) {
                if (StringUtils.equals(defaultOrder, Constant.ORDER_DESC)) {
                    orderItems.add(OrderItem.desc(defaultSort));
                } else {
                    orderItems.add(OrderItem.asc(defaultSort));
                }
            }
        }
        if (orderItems.size() > 0) {
            page.setOrders(orderItems);
        }
        return page;
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

}
