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

package org.apache.streampark.console.base.util;

import org.apache.streampark.common.util.DateUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;

/** Serialization utils */
public final class JacksonUtils {

    private JacksonUtils() {
    }

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new DefaultScalaModule());
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.setDateFormat(new SimpleDateFormat(DateUtils.fullFormat()));
    }

    public static <T> T read(String json, Class<T> clazz) throws JsonProcessingException {
        return MAPPER.readValue(json, clazz);
    }

    public static <T> T read(String json, TypeReference<T> typeReference) throws JsonProcessingException {
        return MAPPER.readValue(json, typeReference);
    }

    public static String write(Object object) throws JsonProcessingException {
        return MAPPER.writeValueAsString(object);
    }

    public static boolean isValidJson(String jsonStr) {
        try {
            JsonNode jsonNode = MAPPER.readTree(jsonStr);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Map<String, String> toMap(String jsonStr) throws JsonProcessingException {
        return (Map<String, String>) MAPPER.readValue(jsonStr, Map.class);
    }
}
