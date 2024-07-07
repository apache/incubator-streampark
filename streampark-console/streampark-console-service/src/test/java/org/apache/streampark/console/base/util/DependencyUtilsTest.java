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

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.console.core.bean.FlinkConnector;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenTool;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.factories.Factory;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
class DependencyUtilsTest {

    @Disabled("Disabled due to unstable performance.")
    @Test
    public void resolveFlinkConnector() throws Exception {

        Artifact artifact = new Artifact("com.ververica", "flink-connector-mysql-cdc", "2.4.1", null);

        InternalConfigHolder.set(CommonConfig.STREAMPARK_WORKSPACE_LOCAL(), "~/tmp");

        List<File> files = MavenTool.resolveArtifacts(artifact);
        if (files.isEmpty()) {
            return;
        }

        String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
        Optional<File> jarFile = files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
        File connector = jarFile.get();

        List<String> factories = getConnectorFactory(connector);

        Class<Factory> className = Factory.class;
        URL[] array = files.stream()
                .map(
                        x -> {
                            try {
                                return x.toURI().toURL();
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .toArray(URL[]::new);

        URLClassLoader urlClassLoader = URLClassLoader.newInstance(array);
        ServiceLoader<Factory> serviceLoader = ServiceLoader.load(className, urlClassLoader);

        List<FlinkConnector> connectorResources = new ArrayList<>();
        try {
            for (Factory factory : serviceLoader) {
                String factoryClassName = factory.getClass().getName();
                if (factories.contains(factoryClassName)) {
                    FlinkConnector connectorResource = new FlinkConnector();
                    connectorResource.setClassName(factoryClassName);
                    connectorResource.setFactoryIdentifier(factory.factoryIdentifier());
                    Map<String, String> requiredOptions = new HashMap<>(0);
                    factory
                            .requiredOptions()
                            .forEach(x -> requiredOptions.put(x.key(), getOptionDefaultValue(x)));
                    connectorResource.setRequiredOptions(requiredOptions);

                    Map<String, String> optionalOptions = new HashMap<>(0);
                    factory
                            .optionalOptions()
                            .forEach(x -> optionalOptions.put(x.key(), getOptionDefaultValue(x)));
                    connectorResource.setOptionalOptions(optionalOptions);

                    connectorResources.add(connectorResource);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        urlClassLoader.close();
        System.out.println(connectorResources);
    }

    private String getOptionDefaultValue(ConfigOption<?> option) {
        if (!option.hasDefaultValue()) {
            return null;
        }
        Object value = option.defaultValue();
        if (value instanceof Duration) {
            return value.toString().replace("PT", "").toLowerCase();
        }
        return value.toString();
    }

    @Test
    public void testDuration() {
        String s = "PT30H";
        Duration duration = Duration.parse(s);
        System.out.println(duration.getSeconds());
    }

    private List<String> getConnectorFactory(File connector) throws Exception {
        String configFile = "META-INF/services/org.apache.flink.table.factories.Factory";
        JarFile jarFile = new JarFile(connector);
        JarEntry entry = jarFile.getJarEntry(configFile);
        List<String> factories = new ArrayList<>(0);
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            Scanner scanner = new Scanner(new InputStreamReader(inputStream));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    factories.add(line);
                }
            }
            scanner.close();
        }
        return factories;
    }
}
