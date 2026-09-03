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

package org.apache.streampark.common.configuration;

import org.apache.streampark.common.util.PathUtils;
import org.apache.streampark.common.util.YamlParser;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Parses YAML, HOCON, and Java properties into origin-aware configuration snapshots.
 *
 * <p>YAML is loaded with SnakeYAML's safe constructor and duplicate-key rejection. Nested mappings
 * are flattened with dot-separated keys; scalar lists are retained as structured values. HOCON is
 * resolved before extraction. A document with a non-mapping root or ambiguous flattened keys is
 * rejected instead of being interpreted heuristically.
 */
public final class ConfigurationParser {

    private ConfigurationParser() {
    }

    /**
     * Parses a file and infers its format from the filename.
     *
     * @param path configuration file path
     * @return immutable file-sourced configuration snapshot
     * @throws ConfigException when the file cannot be read or its extension or content is invalid
     */
    public static Configuration parse(Path path) {
        return parse(path, ConfigurationFormat.fromPath(path));
    }

    /**
     * Parses a file using an explicit document format.
     *
     * @param path configuration file path
     * @param format syntax used by the document
     * @return immutable file-sourced configuration snapshot
     * @throws ConfigException when the file cannot be read or parsed
     */
    public static Configuration parse(Path path, ConfigurationFormat format) {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream inputStream = PathUtils.openFile(path.toString())) {
            return parse(inputStream, format, path.toAbsolutePath().normalize().toString());
        } catch (IOException e) {
            throw new ConfigException("Cannot read configuration file " + path, e);
        }
    }

    /**
     * Parses in-memory document content with a diagnostic origin name.
     *
     * @param content configuration document text
     * @param format syntax used by the document
     * @param originName source name retained in value origins and diagnostics
     * @return immutable file-sourced configuration snapshot
     * @throws ConfigException when the content cannot be parsed
     */
    public static Configuration parse(String content, ConfigurationFormat format, String originName) {
        Objects.requireNonNull(content, "configuration content must not be null");
        return parse(new StringReader(content), format, originName);
    }

    /**
     * Parses document content from a stream without closing the supplied stream.
     *
     * <p>YAML delegates byte decoding to {@link YamlParser} so byte-order markers are honored.
     * HOCON and properties streams are decoded as UTF-8.
     *
     * @param inputStream document input stream
     * @param format syntax used by the document
     * @param originName source name retained in value origins and diagnostics
     * @return immutable file-sourced configuration snapshot
     * @throws ConfigException when the stream cannot be read or parsed
     */
    public static Configuration parse(
                                      InputStream inputStream,
                                      ConfigurationFormat format,
                                      String originName) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        if (format == ConfigurationFormat.YAML) {
            return buildConfiguration(YamlParser.flatten(YamlParser.parse(inputStream)), originName);
        }
        return parse(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8),
            format,
            originName);
    }

    private static Configuration parse(Reader reader, ConfigurationFormat format, String originName) {
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(format, "format must not be null");
        try {
            // Each syntax-specific parser ends at the same raw map boundary. Option conversion is
            // deferred until typed access so this module does not require a global option registry.
            Map<String, Object> values;
            switch (format) {
                case YAML:
                    values = parseYaml(reader);
                    break;
                case HOCON:
                    values = parseHocon(reader);
                    break;
                case PROPERTIES:
                    values = parseProperties(reader);
                    break;
                default:
                    throw new ConfigException("Unsupported configuration format: " + format);
            }
            return buildConfiguration(values, originName);
        } catch (ConfigException e) {
            throw e;
        } catch (RuntimeException | IOException e) {
            throw new ConfigException("Cannot parse " + format + " configuration from " + originName, e);
        }
    }

    private static Map<String, Object> parseYaml(Reader reader) {
        return YamlParser.flatten(YamlParser.parse(reader));
    }

    private static Map<String, Object> parseHocon(Reader reader) {
        // Resolve substitutions before unwrapping to prevent unresolved ConfigValue instances from
        // leaking into the engine-neutral configuration model.
        Config config = ConfigFactory.parseReader(reader).resolve();
        Map<String, Object> result = new LinkedHashMap<>();
        config.entrySet().forEach(entry -> putUnique(result, entry.getKey(), entry.getValue().unwrapped()));
        return result;
    }

    private static Map<String, Object> parseProperties(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            putUnique(result, key, properties.getProperty(key));
        }
        return result;
    }

    private static void putUnique(Map<String, Object> target, String key, Object value) {
        if (target.putIfAbsent(key, value) != null) {
            throw new ConfigException("Duplicate configuration key: " + key);
        }
    }

    private static Configuration buildConfiguration(
                                                    Map<String, Object> values,
                                                    String originName) {
        // Parsed documents are file layers even when their content came from memory. originName
        // distinguishes the physical file, compressed payload, or logical resource in diagnostics.
        return Configuration.builder().add(originName, ConfigSource.FILE, values).build();
    }
}
