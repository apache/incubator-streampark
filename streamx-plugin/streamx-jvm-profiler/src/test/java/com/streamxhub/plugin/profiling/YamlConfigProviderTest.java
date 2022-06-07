/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.plugin.profiling;

import com.streamxhub.streamx.plugin.profiling.YamlConfigProvider;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class YamlConfigProviderTest {
    @Test
    public void getConfig() throws IOException {
        {
            YamlConfigProvider provider = new YamlConfigProvider("not_exiting_file");
            Assert.assertEquals(0, provider.getConfig().size());
        }

        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            YamlConfigProvider provider = new YamlConfigProvider(file.getAbsolutePath());
            Assert.assertEquals(0, provider.getConfig().size());
        }

        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "key1: value1\n"
                    + "key2:\n"
                    + "- value2a\n"
                    + "- value2b\n"
                    + "key3:\n"
                    + "    key3a: value3a\n"
                    + "    key3b:\n"
                    + "    - value3b\n"
                    + "    - value3c\n"
                    + "override:\n"
                    + "  override1: \n"
                    + "    key1: value11\n"
                    + "    key2:\n"
                    + "    - value22a\n"
                    + "    - value22b\n"
                    + "    key3:\n"
                    + "      key3a: value33a\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            YamlConfigProvider provider = new YamlConfigProvider(file.getAbsolutePath());
            Map<String, Map<String, List<String>>> config = provider.getConfig();
            Assert.assertEquals(2, config.size());

            Map<String, List<String>> rootConfig = config.get("");
            Assert.assertEquals(4, rootConfig.size());
            Assert.assertEquals(Arrays.asList("value1"), rootConfig.get("key1"));
            Assert.assertEquals(Arrays.asList("value2a", "value2b"), rootConfig.get("key2"));
            Assert.assertEquals(Arrays.asList("value3a"), rootConfig.get("key3.key3a"));
            Assert.assertEquals(Arrays.asList("value3b", "value3c"), rootConfig.get("key3.key3b"));

            Map<String, List<String>> override1Config = config.get("override1");
            Assert.assertEquals(3, override1Config.size());
            Assert.assertEquals(Arrays.asList("value11"), override1Config.get("key1"));
            Assert.assertEquals(Arrays.asList("value22a", "value22b"), override1Config.get("key2"));
            Assert.assertEquals(Arrays.asList("value33a"), override1Config.get("key3.key3a"));
        }
    }

    @Test
    public void getConfigFromBadHttpUrl() throws IOException {
        YamlConfigProvider provider = new YamlConfigProvider("http://localhost/bad_url");
        Map<String, Map<String, List<String>>> config = provider.getConfig();
        Assert.assertEquals(0, config.size());
    }

    @Test
    public void getConfigFromFile() throws IOException {
        String content =
            "key1: value1\n"
                + "key2:\n"
                + "- value2a\n"
                + "- value2b\n"
                + "key3:\n"
                + "    key3a: value3a\n"
                + "    key3b:\n"
                + "    - value3b\n"
                + "    - value3c\n"
                + "override:\n"
                + "  override1: \n"
                + "    key1: value11\n"
                + "    key2:\n"
                + "    - value22a\n"
                + "    - value22b\n"
                + "    key3:\n"
                + "      key3a: value33a\n"
                + "";

        Path path = Files.createTempFile("jvm-profiler_", ".yaml");
        Files.write(path, content.getBytes());
        path.toFile().deleteOnExit();

        YamlConfigProvider provider = new YamlConfigProvider(path.toString());
        Map<String, Map<String, List<String>>> config = provider.getConfig();
        Assert.assertEquals(2, config.size());

        Map<String, List<String>> rootConfig = config.get("");
        Assert.assertEquals(4, rootConfig.size());
        Assert.assertEquals(Arrays.asList("value1"), rootConfig.get("key1"));
        Assert.assertEquals(Arrays.asList("value2a", "value2b"), rootConfig.get("key2"));
        Assert.assertEquals(Arrays.asList("value3b", "value3c"), rootConfig.get("key3.key3b"));

        Map<String, List<String>> override1Config = config.get("override1");
        Assert.assertEquals(3, override1Config.size());
        Assert.assertEquals(Arrays.asList("value11"), override1Config.get("key1"));
        Assert.assertEquals(Arrays.asList("value22a", "value22b"), override1Config.get("key2"));
        Assert.assertEquals(Arrays.asList("value33a"), override1Config.get("key3.key3a"));
    }
}
