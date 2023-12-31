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

package org.apache.streampark.console.base.mybatis.pager;

import org.apache.streampark.console.base.domain.Constant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.WebUtils;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public final class MybatisPager<T> {

  public Page<T> getDefaultPage(RestRequest request) {
    return getPage(request, Constant.DEFAULT_SORT_FIELD, Constant.ORDER_DESC);
  }

  public Page<T> getPage(RestRequest request, String defaultSort, String defaultOrder) {
    Page<T> page = new Page<>();
    page.setCurrent(request.getPageNum());
    page.setSize(request.getPageSize());

    List<OrderItem> orderItems = new ArrayList<>(0);
    if (!StringUtils.isAnyBlank(request.getSortField(), request.getSortOrder())) {
      ApiAlertException.throwIfTrue(
          checkSqlInjection(request.getSortField()),
          "Illegal sql injection detected, sortField: " + request.getSortField());

      ApiAlertException.throwIfTrue(
          checkSqlInjection(request.getSortOrder()),
          "Illegal sql injection detected, sortOrder: " + request.getSortOrder());

      String sortField = WebUtils.camelToUnderscore(request.getSortField());
      if (StringUtils.equals(request.getSortOrder(), Constant.ORDER_DESC)) {
        orderItems.add(OrderItem.desc(sortField));
      } else if (StringUtils.equals(request.getSortOrder(), Constant.ORDER_ASC)) {
        orderItems.add(OrderItem.asc(sortField));
      } else {
        throw new ApiAlertException("Invalid sortOrder argument: " + request.getSortOrder());
      }
    } else if (StringUtils.isNotBlank(defaultSort)) {
      ApiAlertException.throwIfTrue(
          checkSqlInjection(defaultSort),
          "Illegal sql injection detected, defaultSort: " + defaultSort);

      if (StringUtils.equals(defaultOrder, Constant.ORDER_DESC)) {
        orderItems.add(OrderItem.desc(defaultSort));
      } else if (StringUtils.equals(defaultOrder, Constant.ORDER_ASC)) {
        orderItems.add(OrderItem.asc(defaultSort));
      } else {
        throw new ApiAlertException("Invalid sortOrder argument: " + defaultOrder);
      }
    }

    if (!orderItems.isEmpty()) {
      page.setOrders(orderItems);
    }

    return page;
  }

  private final Pattern SQL_SYNTAX_PATTERN =
      Pattern.compile(
          "(insert|delete|update|select|create|drop|truncate|grant|alter|deny|revoke|call|execute|exec|declare|show|rename|set)\\s+.*"
              + "(into|from|set|where|table|database|view|index|on|cursor|procedure|trigger|for|password|union|and|or)|"
              + "(select\\s*\\*\\s*from\\s+)|(and|or)\\s+.*(like|=|>|<|in|between|is|not|exists)",
          Pattern.CASE_INSENSITIVE);

  private final Pattern SQL_COMMENT_PATTERN =
      Pattern.compile("'.*(or|union|--|#|/\\*|;)", Pattern.CASE_INSENSITIVE);

  private boolean checkSqlInjection(String value) {
    return SQL_COMMENT_PATTERN.matcher(value).find() || SQL_SYNTAX_PATTERN.matcher(value).find();
  }
}
