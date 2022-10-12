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

package org.apache.streampark.console.core.metrics.flink;

import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.console.base.util.CommonUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

@Data
public class JvmProfiler implements Serializable {

    private transient ObjectMapper mapper = new ObjectMapper();

    private String metric;
    private Long id;
    private String token;
    private String type;
    private String profiler;

    @JsonIgnore
    public Map getMetricsAsMap() throws IOException {
        if (CommonUtils.notEmpty(metric)) {
            String content = DeflaterUtils.unzipString(metric);
            return mapper.readValue(content, Map.class);
        }
        return Collections.EMPTY_MAP;
    }
}
