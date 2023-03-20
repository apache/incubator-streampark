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

import org.apache.streampark.console.base.domain.Constant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.mapper.ApplicationLogMapper;
import org.apache.streampark.console.core.service.ApplicationLogService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationLogServiceImpl extends ServiceImpl<ApplicationLogMapper, ApplicationLog>
    implements ApplicationLogService {

  @Override
  public IPage<ApplicationLog> page(ApplicationLog applicationLog, RestRequest request) {
    Page<ApplicationLog> page =
        new MybatisPager<ApplicationLog>().getPage(request, "option_time", Constant.ORDER_DESC);
    LambdaQueryWrapper<ApplicationLog> queryWrapper =
        new LambdaQueryWrapper<ApplicationLog>()
            .eq(ApplicationLog::getAppId, applicationLog.getAppId());
    return this.page(page, queryWrapper);
  }

  @Override
  public void removeApp(Long appId) {
    LambdaQueryWrapper<ApplicationLog> queryWrapper =
        new LambdaQueryWrapper<ApplicationLog>().eq(ApplicationLog::getAppId, appId);
    this.remove(queryWrapper);
  }

  @Override
  public Boolean delete(ApplicationLog applicationLog) {
    return removeById(applicationLog.getId());
  }
}
