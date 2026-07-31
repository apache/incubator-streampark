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

package org.apache.streampark.console.core.managed.provider.volcengine;

import java.util.Arrays;

/** Short-lived plaintext credential material that can be cleared after request signing. */
final class VolcengineCredentials implements AutoCloseable {

    private final char[] accessKey;

    private final char[] secretKey;

    VolcengineCredentials(String accessKey, String secretKey) {
        this.accessKey = accessKey.toCharArray();
        this.secretKey = secretKey.toCharArray();
    }

    char[] accessKey() {
        return accessKey;
    }

    char[] secretKey() {
        return secretKey;
    }

    @Override
    public void close() {
        Arrays.fill(accessKey, '\0');
        Arrays.fill(secretKey, '\0');
    }
}
