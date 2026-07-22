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
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.common.util.TypeCastUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe configuration storage containers. */
public final class InternalConfigHolder {

    private static final Logger log = LoggerFactory.getLogger(InternalConfigHolder.class);

    private static final int INITIAL_CAPACITY = 45;

    private static final ConcurrentHashMap<String, Object> confData =
        new ConcurrentHashMap<>(INITIAL_CAPACITY);

    private static final ConcurrentHashMap<String, InternalOption<?>> confOptions =
        new ConcurrentHashMap<>(INITIAL_CAPACITY);

    private InternalConfigHolder() {
    }

    public static void initConfigHub() {
        CommonConfig.STREAMPARK_WORKSPACE_LOCAL();
        K8sFlinkConfig.INGRESS_CLASS();
    }

    static void register(@Nonnull InternalOption<?> conf) {
        confOptions.put(conf.getKey(), conf);
        if (conf.getDefaultValue() != null) {
            confData.put(conf.getKey(), conf.getDefaultValue());
        }
    }

    @Nonnull
    public static <T> T get(@Nonnull InternalOption<T> conf) {
        Object value = confData.get(conf.getKey());
        if (value == null || value.equals(conf.getDefaultValue())) {
            String v = SystemPropertyUtils.get(conf.getKey());
            if (v != null) {
                if (!v.equals(value)) {
                    set(conf, v);
                }
                return TypeCastUtils.cast(v, conf.getClassType());
            }
            return conf.getDefaultValue();
        }
        return TypeCastUtils.cast(value.toString(), conf.getClassType());
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> T get(@Nonnull String key) {
        Object v = confData.get(key);
        if (v != null) {
            return (T) v;
        }
        InternalOption<?> conf = confOptions.get(key);
        if (conf != null) {
            return (T) conf.getDefaultValue();
        }
        InternalOptionSpec config = getConfig(key);
        String sysProp = SystemPropertyUtils.get(key);
        if (sysProp != null) {
            return TypeCastUtils.cast(sysProp, config.getClassType());
        }
        throw new IllegalArgumentException("Config key has not been registered: " + key);
    }

    @Nullable
    public static InternalOptionSpec getConfig(String key) {
        return confOptions.get(key);
    }

    @Nonnull
    public static Set<String> keys() {
        Map<String, InternalOption<?>> copy = new HashMap<>(confOptions);
        return copy.keySet();
    }

    public static void set(@Nonnull InternalOptionSpec conf, Object value) {
        if (!confOptions.containsKey(conf.getKey())) {
            throw new IllegalArgumentException("config key has not been registered: " + conf.getKey());
        }
        if (value == null) {
            confData.remove(conf.getKey());
            return;
        }
        if (conf.getClassType() != value.getClass()) {
            throw new IllegalArgumentException(
                "config value type is not match of "
                    + conf.getKey()
                    + ", required: "
                    + conf.getClassType()
                    + ", actual: "
                    + value.getClass());
        }
        SystemPropertyUtils.set(conf.getKey(), value.toString());
        confData.put(conf.getKey(), value);
    }

    public static void log() {
        Set<String> configKeys = keys();
        StringBuilder sb = new StringBuilder();
        sb.append("Registered configs:\n");
        sb.append("ConfigHub collected configs: ").append(configKeys.size()).append("\n");
        for (String key : configKeys) {
            String value =
                key.contains("password") ? Constants.DEFAULT_DATAMASK_STRING : String.valueOf(get(key));
            sb.append("  ").append(key).append(" = ").append(value).append("\n");
        }
        log.info(sb.toString());
    }
}
