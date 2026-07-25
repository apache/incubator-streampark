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

import org.apache.commons.lang3.StringUtils;

import com.typesafe.config.ConfigFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

/** Properties, YAML and HOCON configuration parsing utilities. */
public final class PropertiesUtils {

    private static final Pattern HOCON_KEY_QUOTE_PATTERN = Pattern.compile("\"");

    private PropertiesUtils() {
    }

    public static String readFile(String filename) throws IOException {
        Path path = SafePathUtils.resolveConfigPath(filename);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("[StreamPark] readFile: file " + path + " does not exist");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("[StreamPark] readFile: file " + path + " is not a normal file");
        }
        StringBuilder buffer = new StringBuilder();
        try (Scanner scanner = new Scanner(path)) {
            while (scanner.hasNextLine()) {
                buffer.append(scanner.nextLine()).append("\r\n");
            }
        }
        return buffer.toString();
    }

    public static LinkedHashMap<String, String> fromYamlText(String text) {
        try {
            Map<String, Object> map = new Yaml().load(text);
            return flatten(map);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed when loading conf error:", e);
        }
    }

    public static LinkedHashMap<String, String> fromHoconText(String conf) {
        if (conf == null) {
            throw new IllegalArgumentException("[StreamPark] fromHoconText: Hocon content must not be null");
        }
        return parseHoconByReader(new StringReader(conf));
    }

    public static LinkedHashMap<String, String> fromPropertiesText(String conf) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(conf));
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            for (String key : properties.stringPropertyNames()) {
                result.put(key, properties.getProperty(key).trim());
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading properties ", e);
        }
    }

    /** Load Yaml present in the given file. */
    public static LinkedHashMap<String, String> fromYamlFile(String filename) {
        try (InputStream inputStream = SafePathUtils.openConfigFile(filename)) {
            return fromYamlFile(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading yaml from file " + filename, e);
        }
    }

    public static LinkedHashMap<String, String> fromHoconFile(String filename) {
        try (InputStream inputStream = SafePathUtils.openConfigFile(filename)) {
            return fromHoconFile(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed when loading Hocon from file " + filename, e);
        }
    }

    /** Load properties present in the given file. */
    public static LinkedHashMap<String, String> fromPropertiesFile(String filename) {
        try (InputStream inputStream = SafePathUtils.openConfigFile(filename)) {
            return fromPropertiesFile(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "[StreamPark] Failed when loading properties from file " + filename, e);
        }
    }

    /** Load Yaml present in the given input stream. */
    public static LinkedHashMap<String, String> fromYamlFile(InputStream inputStream) {
        AssertUtils.required(
            inputStream != null,
            "[StreamPark] fromYamlFile: Properties inputStream  must not be null");
        try {
            Map<String, Object> map = new Yaml().load(inputStream);
            return flatten(map);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed when loading yaml from inputStream", e);
        }
    }

    public static LinkedHashMap<String, String> fromHoconFile(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("[StreamPark] fromHoconFile: Hocon inputStream  must not be null");
        }
        return parseHoconByReader(new InputStreamReader(inputStream));
    }

    private static LinkedHashMap<String, String> parseHoconByReader(Reader reader) {
        try {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            ConfigFactory.parseReader(reader)
                .entrySet()
                .forEach(
                    entry -> {
                        String key = HOCON_KEY_QUOTE_PATTERN.matcher(entry.getKey().trim()).replaceAll("");
                        String value = entry.getValue().unwrapped().toString().trim();
                        result.put(key, value);
                    });
            return result;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed when loading Hocon ", e);
        }
    }

    /** Load properties present in the given input stream. */
    public static LinkedHashMap<String, String> fromPropertiesFile(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException(
                "[StreamPark] fromPropertiesFile: Properties inputStream  must not be null");
        }
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            for (String key : properties.stringPropertyNames()) {
                result.put(key, properties.getProperty(key).trim());
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

    private static LinkedHashMap<String, String> flatten(Map<String, Object> map) {
        return flatten(map, "");
    }

    private static LinkedHashMap<String, String> flatten(Map<String, Object> map, String prefix) {
        if (map == null || map.isEmpty()) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                result.putAll(flatten(nested, prefix + key + "."));
            } else if (value instanceof String) {
                if (StringUtils.isNotBlank((String) value)) {
                    result.put(prefix + key, (String) value);
                }
            } else if (value instanceof Collection) {
                Collection<?> collection = (Collection<?>) value;
                if (!collection.isEmpty()) {
                    result.put(prefix + key, collection.toString());
                }
            } else if (value != null) {
                result.put(prefix + key, value.toString());
            }
        }
        return result;
    }
}
