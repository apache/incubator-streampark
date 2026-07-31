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

package org.apache.streampark.console.core.managed.service;

import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Produces a deterministic SHA-256 fingerprint for a managed application candidate. */
@Component
public class ManagedFlinkDefinitionHasher {

    private final ObjectMapper canonicalMapper;

    public ManagedFlinkDefinitionHasher(ObjectMapper objectMapper) {
        this.canonicalMapper = objectMapper.copy()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String hash(ManagedFlinkApplicationSaveRequest request) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("jobName", trimToNull(request.getJobName()));
        definition.put("managedEnvironmentId", request.getManagedEnvironmentId());
        definition.put("jobType", request.getJobType());
        definition.put("sql", normalizeSql(request.getSql()));
        definition.put("jar", trimToNull(request.getJar()));
        definition.put("mainClass", trimToNull(request.getMainClass()));
        definition.put("args", trimToNull(request.getArgs()));
        definition.put("runtimeConfig", request.getRuntimeConfig());
        definition.put("releaseConfig", request.getReleaseConfig());
        try {
            byte[] canonical = canonicalMapper.writeValueAsBytes(definition);
            return hex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                "Managed Flink application definition cannot be serialized.", exception);
        }
    }

    static String normalizeSql(String sql) {
        if (sql == null) {
            return null;
        }
        return sql.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String hex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) {
            result.append(String.format("%02x", item & 0xff));
        }
        return result.toString();
    }
}
