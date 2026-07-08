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

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;

import com.typesafe.config.ConfigFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public final class PropertiesUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(PropertiesUtils.class.getName());

    private PropertiesUtils() {
    }

    public static String readFile(String filename) {
        Path path = SafePathUtils.resolveConfigPath(filename);
        try (Scanner scanner = new Scanner(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            StringBuilder buffer = new StringBuilder();
            while (scanner.hasNextLine()) {
                buffer.append(scanner.nextLine()).append("\r\n");
            }
            return buffer.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("[StreamPark] readFile: failed to read " + path, e);
        }
    }

    public static Map<String, String> fromYamlText(String text) {
        try {
            Map<String, Object> map = new Yaml().load(text);
            return flatten(map);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed when loading conf error:", e);
        }
    }

    public static Map<String, String> fromHoconText(String conf) {
        if (conf == null) {
            throw new IllegalArgumentException("[StreamPark] fromHoconText: Hocon content must not be null");
        }
        try {
            return parseHoconByReader(new StringReader(conf));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading Hocon ", e);
        }
    }

    public static Map<String, String> fromPropertiesText(String conf) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(conf));
            Map<String, String> result = new HashMap<>();
            for (String k : properties.stringPropertyNames()) {
                result.put(k, properties.getProperty(k).trim());
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading properties ", e);
        }
    }

    public static Map<String, String> fromYamlFile(String filename) {
        try (InputStream inputStream = SafePathUtils.openConfigFile(filename)) {
            return fromYamlFile(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading yaml from file", e);
        }
    }

    public static Map<String, String> fromHoconFile(String filename) {
        try (InputStream inputStream = SafePathUtils.openConfigFile(filename)) {
            return fromHoconFile(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading Hocon ", e);
        }
    }

    public static Map<String, String> fromPropertiesFile(String filename) {
        try (InputStream inputStream = SafePathUtils.openConfigFile(filename)) {
            return fromPropertiesFile(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading properties from file", e);
        }
    }

    public static Map<String, String> fromYamlFile(InputStream inputStream) {
        AssertUtils.required(
            inputStream != null,
            "[StreamPark] fromYamlFile: Properties inputStream  must not be null");
        try {
            Map<String, Object> map = new Yaml().load(inputStream);
            return flatten(map);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed when loading yaml from inputStream", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static Map<String, String> fromHoconFile(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("[StreamPark] fromHoconFile: Hocon inputStream  must not be null");
        }
        try {
            return parseHoconByReader(new InputStreamReader(inputStream));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading Hocon ", e);
        }
    }

    private static Map<String, String> parseHoconByReader(java.io.Reader reader) throws IOException {
        Map<String, String> result = new HashMap<>();
        ConfigFactory.parseReader(reader)
            .entrySet()
            .forEach(
                x -> {
                    String k = x.getKey().trim().replaceAll("\"", "");
                    String v = x.getValue().unwrapped().toString().trim();
                    result.put(k, v);
                });
        return result;
    }

    public static Map<String, String> fromPropertiesFile(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException(
                "[StreamPark] fromPropertiesFile: Properties inputStream  must not be null");
        }
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            Map<String, String> result = new HashMap<>();
            for (String k : properties.stringPropertyNames()) {
                result.put(k, properties.getProperty(k).trim());
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "[StreamPark] Failed when loading properties from inputStream", e);
        }
    }

    public static Map<String, String> fromYamlTextAsJava(String text) {
        return new HashMap<>(fromYamlText(text));
    }

    public static Map<String, String> fromHoconTextAsJava(String text) {
        return new HashMap<>(fromHoconText(text));
    }

    public static Map<String, String> fromPropertiesTextAsJava(String text) {
        return new HashMap<>(fromPropertiesText(text));
    }

    public static Map<String, String> fromYamlFileAsJava(String filename) {
        return new HashMap<>(fromYamlFile(filename));
    }

    public static Map<String, String> fromHoconFileAsJava(String filename) {
        return new HashMap<>(fromHoconFile(filename));
    }

    public static Map<String, String> fromPropertiesFileAsJava(String filename) {
        return new HashMap<>(fromPropertiesFile(filename));
    }

    public static Map<String, String> fromYamlFileAsJava(InputStream inputStream) {
        return new HashMap<>(fromYamlFile(inputStream));
    }

    public static Map<String, String> fromHoconFileAsJava(InputStream inputStream) {
        return new HashMap<>(fromHoconFile(inputStream));
    }

    public static Map<String, String> fromPropertiesFileAsJava(InputStream inputStream) {
        return new HashMap<>(fromPropertiesFile(inputStream));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> flatten(Map<String, Object> map) {
        return flatten(map, "");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> flatten(Map<String, Object> map, String prefix) {
        Map<String, String> result = new HashMap<>();
        if (map == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix + entry.getKey();
            Object v = entry.getValue();
            if (v instanceof Map) {
                result.putAll(flatten((Map<String, Object>) v, key + "."));
            } else if (v instanceof String) {
                if (StringUtils.isNotBlank((String) v)) {
                    result.put(key, (String) v);
                }
            } else if (v instanceof java.util.Collection) {
                if (!((java.util.Collection<?>) v).isEmpty()) {
                    result.put(key, v.toString());
                }
            } else if (v != null) {
                result.put(key, v.toString());
            }
        }
        return result;
    }
}
