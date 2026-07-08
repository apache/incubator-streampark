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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CURLBuilder {

    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> formData = new HashMap<>();
    public CURLBuilder(String url) {
        this.url = url;
    }
    public CURLBuilder addHeader(String k, String v) {
        headers.put(k, v);
        return this;
    }
    public CURLBuilder addFormData(String k, Serializable v) {
        formData.put(k, v.toString());
        return this;
    }
    public String build() {
        if (url == null)
            throw new IllegalArgumentException("[StreamPark] CURL build failed, url must not be null");
        StringBuilder cURL = new StringBuilder("curl -X POST ");
        cURL.append(String.format("'%s' \\\n", url));
        for (String h : headers.keySet()) {
            cURL.append(String.format("-H '%s: %s' \\\n", h, headers.get(h)));
        }
        for (Map.Entry<String, String> e : formData.entrySet()) {
            cURL.append(String.format("--data-urlencode '%s=%s' \\\n", e.getKey(), e.getValue()));
        }
        return cURL.toString().trim().substring(0, cURL.toString().trim().length() - 1);
    }
}
