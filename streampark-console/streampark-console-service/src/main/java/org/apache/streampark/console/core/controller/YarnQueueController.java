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

package org.apache.streampark.console.core.controller;

import org.apache.streampark.console.base.bean.PageRequest;
import org.apache.streampark.console.base.bean.Response;
import org.apache.streampark.console.core.entity.YarnQueue;
import org.apache.streampark.console.core.service.YarnQueueService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("yarn/queue")
public class YarnQueueController {

    @Autowired
    private YarnQueueService yarnQueueService;

    /**
     * * List the queues in the specified team by the paging & optional search hint message.
     *
     * @param pageRequest page request information.
     * @param yarnQueue optional fields used to search.
     * @return Result with IPage<{@link YarnQueue}> object.
     */
    @PostMapping("list")
    public Response<IPage<YarnQueue>> list(PageRequest pageRequest, YarnQueue yarnQueue) {
        IPage<YarnQueue> queuePage = yarnQueueService.getPage(yarnQueue, pageRequest);
        return Response.success(queuePage);
    }

    @PostMapping("check")
    public Response<Integer> check(YarnQueue yarnQueue) {
        return yarnQueueService.checkYarnQueue(yarnQueue);
    }

    @PostMapping("create")
    @RequiresPermissions("yarnQueue:create")
    public Response<Boolean> create(YarnQueue yarnQueue) {
        return Response.success(yarnQueueService.createYarnQueue(yarnQueue));
    }

    @PostMapping("update")
    @RequiresPermissions("yarnQueue:update")
    public Response<Void> update(YarnQueue yarnQueue) {
        yarnQueueService.updateYarnQueue(yarnQueue);
        return Response.success();
    }

    @PostMapping("delete")
    @RequiresPermissions("yarnQueue:delete")
    public Response<Void> delete(YarnQueue yarnQueue) {
        yarnQueueService.remove(yarnQueue);
        return Response.success();
    }
}
