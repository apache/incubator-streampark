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

package org.apache.streampark.common.util;

import org.apache.streampark.shaded.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public final class JsonUtils implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.setDateFormat(new SimpleDateFormat(DateUtils.FULL_FORMAT));
    }

    private JsonUtils() {
    }

    public static <T> T read(Object obj, Class<T> clazz) throws Exception {
        if (obj instanceof String) {
            return MAPPER.readValue((String) obj, clazz);
        }
        return MAPPER.readValue(write(obj), clazz);
    }

    public static String write(Object obj) throws Exception {
        return MAPPER.writeValueAsString(obj);
    }
}
