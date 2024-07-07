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

package org.apache.streampark.gateway.session;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/** Environment to initialize the {@code Session}. */
public class SessionEnvironment implements Serializable {

    private final @Nullable String sessionName;
    private final @Nullable String defaultCatalog;
    private final Map<String, String> sessionConfig;

    public SessionEnvironment(
                              @Nullable String sessionName,
                              @Nullable String defaultCatalog,
                              Map<String, String> sessionConfig) {
        this.sessionName = sessionName;
        this.defaultCatalog = defaultCatalog;
        this.sessionConfig = sessionConfig;
    }

    @Nullable
    public String getSessionName() {
        return sessionName;
    }

    @Nullable
    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public Map<String, String> getSessionConfig() {
        return sessionConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SessionEnvironment that = (SessionEnvironment) o;
        return Objects.equals(sessionName, that.sessionName)
                && Objects.equals(defaultCatalog, that.defaultCatalog)
                && Objects.equals(sessionConfig, that.sessionConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionName, defaultCatalog, sessionConfig);
    }

    @Override
    public String toString() {
        return "SessionEnvironment{"
                + "sessionName='"
                + sessionName
                + '\''
                + ", defaultCatalog='"
                + defaultCatalog
                + '\''
                + ", sessionConfig="
                + sessionConfig
                + '}';
    }
}
