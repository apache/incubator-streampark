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

package org.apache.streampark.common.conf;

import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.common.util.StringCastUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;

import org.apache.streampark.shaded.org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe configuration storage containers. All configurations will be automatically
 * initialized from the spring configuration items of the same name.
 */
public final class InternalConfigHolder {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(InternalConfigHolder.class.getName());

    private static final int INITIAL_CAPACITY = 45;

    private static final Map<String, Object> CONF_DATA = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    private static final Map<String, InternalOption> CONF_OPTIONS =
        new ConcurrentHashMap<>(INITIAL_CAPACITY);

    static {
        initConfigHub();
    }

    private InternalConfigHolder() {
    }

    /** Initialize the ConfigHub. */
    public static void initConfigHub() {
        CommonConfig.STREAMPARK_WORKSPACE_LOCAL();
        InternalOption unused = K8sFlinkConfig.jobStatusTrackTaskTimeoutSec;
    }

    static void register(@Nonnull InternalOption conf) {
        CONF_OPTIONS.put(conf.getKey(), conf);
        if (conf.getDefaultValue() != null) {
            CONF_DATA.put(conf.getKey(), conf.getDefaultValue());
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> T get(@Nonnull InternalOption conf) {
        Object value = CONF_DATA.get(conf.getKey());
        if (value == null || value == conf.getDefaultValue()) {
            String v = SystemPropertyUtils.get(conf.getKey());
            if (v != null) {
                if (!v.equals(value)) {
                    set(conf, v);
                }
                return StringCastUtils.cast(v, conf.getClassType());
            } else {
                return (T) conf.getDefaultValue();
            }
        } else {
            return StringCastUtils.cast(value.toString(), conf.getClassType());
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> T get(@Nonnull String key) {
        Object v = CONF_DATA.get(key);
        if (v == null) {
            InternalOption conf = CONF_OPTIONS.get(key);
            if (conf == null) {
                InternalOption config = getConfig(key);
                String sysVal = SystemPropertyUtils.get(key);
                if (sysVal != null) {
                    return StringCastUtils.cast(sysVal, config.getClassType());
                }
                throw new IllegalArgumentException("Config key has not been registered: " + key);
            }
            return (T) conf.getDefaultValue();
        }
        return (T) v;
    }

    @Nullable
    public static InternalOption getConfig(String key) {
        return CONF_OPTIONS.get(key);
    }

    @Nonnull
    public static Set<String> keys() {
        return CONF_OPTIONS.keySet();
    }

    public static void set(@Nonnull InternalOption conf, Object value) {
        if (!CONF_OPTIONS.containsKey(conf.getKey())) {
            throw new IllegalArgumentException("config key has not been registered: " + conf);
        }
        if (value == null) {
            CONF_DATA.remove(conf.getKey());
        } else if (conf.getClassType() != value.getClass()) {
            throw new IllegalArgumentException(
                "config value type is not match of "
                    + conf.getKey()
                    + ", required: "
                    + conf.getClassType()
                    + ", actual: "
                    + value.getClass());
        } else {
            SystemPropertyUtils.set(conf.getKey(), value.toString());
            CONF_DATA.put(conf.getKey(), value);
        }
    }

    public static void log() {
        Set<String> configKeys = keys();
        String details =
            configKeys.stream()
                .map(
                    key -> key
                        + " = "
                        + (key.contains("password")
                            ? Constants.DEFAULT_DATAMASK_STRING
                            : get(key)))
                .collect(Collectors.joining("\n  "));
        LOG.info(
            "[StreamPark] Registered configs:\nConfigHub collected configs: {}\n  {}",
            configKeys.size(),
            details);
    }
}
