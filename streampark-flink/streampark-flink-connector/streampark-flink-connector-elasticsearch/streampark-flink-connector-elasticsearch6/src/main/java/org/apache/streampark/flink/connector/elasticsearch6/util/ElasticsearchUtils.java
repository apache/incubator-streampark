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

package org.apache.streampark.flink.connector.elasticsearch6.util;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;

import java.lang.reflect.Field;

public final class ElasticsearchUtils {
    private ElasticsearchUtils() {}

    public static IndexRequest indexRequest(String index, String id, String source) {
        return indexRequest(index, id, source, XContentType.JSON);
    }

    public static IndexRequest indexRequest(String index, String id, String source, XContentType xContentType) {
        if (source == null) throw new IllegalArgumentException("IndexRequest error:source can not be null...");
        if (xContentType == null) throw new IllegalArgumentException("IndexRequest error:xContentType can not be null...");
        IndexRequest indexReq = new IndexRequest(index).id(id);
        setField(indexReq, "source", new BytesArray(source));
        setField(indexReq, "contentType", xContentType);
        return indexReq;
    }

    private static void setField(IndexRequest indexReq, String name, Object value) {
        try {
            Field field = indexReq.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(indexReq, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set IndexRequest field: " + name, e);
        }
    }
}
