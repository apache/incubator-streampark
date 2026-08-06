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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.request.flink.FlinkAppCreateRequest;
import org.apache.streampark.console.core.response.flink.FlinkAppResponse;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DtoAssemblerTest {

    @Test
    void shouldCopyPropertiesBetweenCompatibleTypes() {
        FlinkApplication app = new FlinkApplication();
        app.setId(1L);
        app.setJobName("demo");

        FlinkAppResponse response = DtoAssembler.toDto(app, FlinkAppResponse.class);

        Assertions.assertEquals(1L, response.getId());
        Assertions.assertEquals("demo", response.getJobName());
    }

    @Test
    void shouldMapPageRecords() {
        FlinkApplication app = new FlinkApplication();
        app.setId(2L);

        Page<FlinkApplication> page = new Page<>(1, 10, 1);
        page.setRecords(java.util.Collections.singletonList(app));

        var responsePage = DtoAssembler.toPage(page, a -> DtoAssembler.toDto(a, FlinkAppResponse.class));

        Assertions.assertEquals(1, responsePage.getRecords().size());
        Assertions.assertEquals(2L, responsePage.getRecords().get(0).getId());
    }

    @Test
    void shouldCopyRequestOntoEntity() {
        FlinkAppCreateRequest request = new FlinkAppCreateRequest();
        request.setJobName("sql-job");
        request.setTeamId(100L);

        FlinkApplication app = new FlinkApplication();
        DtoAssembler.copy(request, app);

        Assertions.assertEquals("sql-job", app.getJobName());
        Assertions.assertEquals(100L, app.getTeamId());
    }
}
