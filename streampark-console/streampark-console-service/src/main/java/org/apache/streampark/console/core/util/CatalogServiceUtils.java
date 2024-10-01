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

package org.apache.streampark.console.core.util;

import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.WebUtils;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.factories.FactoryUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CatalogServiceUtils {

    private static final Pattern PATTERN_FLINK_CONNECTOR_PLUGIN =
        Pattern.compile(
            "^streampark-flink-connector-plugin-(\\d+)\\.(\\d+)\\.(\\d+)(?:-([^-]+))?\\.jar$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static Catalog getCatalog(
                                     String catalogName, Map<String, String> options,
                                     Map<String, String> configurations) {
        ClassLoader classLoader = getCatalogClassLoader(Thread.currentThread().getContextClassLoader());
        return FactoryUtil.createCatalog(
            catalogName, options, Configuration.fromMap(configurations), classLoader);
    }

    /** get catalog classloader, add streampark-flink-connector-plugins */
    protected static ClassLoader getCatalogClassLoader(ClassLoader classLoader) {
        File pluginDir = WebUtils.getPluginDir();
        File[] pluginFiles =
            pluginDir.listFiles(
                pathname -> pathname.getName().matches(PATTERN_FLINK_CONNECTOR_PLUGIN.pattern()));
        if (pluginFiles == null) {
            return classLoader;
        }
        List<URL> pluginUrls = new ArrayList<URL>();
        for (File file : pluginFiles) {
            try {
                URL pluginUrl = file.toURI().toURL();
                pluginUrls.add(pluginUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return new URLClassLoader(pluginUrls.toArray(new URL[0]), classLoader);
    }

    public static Map<String, String> getOptions(String configuration) {
        try {
            return JacksonUtils.toMap(configuration);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
