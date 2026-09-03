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

import org.apache.streampark.common.configuration.ConfigException;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses YAML configuration documents with strict defaults and a narrow compatibility fallback.
 *
 * <p>The parser first evaluates the document unchanged with SnakeYAML's safe constructor,
 * duplicate-key rejection, and recursive-key rejection. Only a failed parse enables compatibility
 * normalization for editor-style multiline scalar values and unquoted scalar values beginning
 * with YAML-reserved characters. This ordering preserves standard YAML structures while accepting
 * configuration text produced by StreamPark's existing editors.
 *
 * <p>Input streams are decoded from BOM markers when present and as UTF-8 otherwise. Parse errors
 * intentionally omit source snippets because configuration lines can contain credentials.
 * Parser and dumper instances are created per invocation, making this utility safe for concurrent
 * configuration loading.
 */
public final class YamlParser {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(YamlParser.class.getName());

    private static final String LENIENT_SCALAR_PREFIXES = "!&*%@`|>";
    private static final Pattern EMPTY_VALUE_PROPERTY =
        Pattern.compile("^(\\s*)([^\\s#][^:]*):\\s*(#.*)?$");
    private static final Pattern STRUCTURED_CHILD =
        Pattern.compile("^[^#\\-][^:]*:(?:$|\\s.*)$");
    private static final Pattern BLOCK_SCALAR_HEADER =
        Pattern.compile("^[|>](?:[+-]?\\d+|\\d+[+-]?|[+-])?$");

    private YamlParser() {
    }

    /**
     * Parses a YAML file after decoding its byte-order marker, if present.
     *
     * @param path path to the YAML document
     * @return the structured root mapping, or an empty map for an empty document
     * @throws ConfigException when the file cannot be read or does not contain a valid mapping
     */
    public static Map<String, Object> parse(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        try {
            return parseBytes(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new ConfigException("Cannot read YAML configuration from " + path, e);
        }
    }

    /**
     * Parses YAML bytes from a stream without closing it.
     *
     * @param inputStream stream containing a BOM-marked or UTF-8 document
     * @return the structured root mapping, or an empty map for an empty document
     * @throws ConfigException when the stream cannot be read or does not contain a valid mapping
     */
    public static Map<String, Object> parse(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        try {
            return parseBytes(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new ConfigException("Cannot read YAML configuration stream", e);
        }
    }

    /**
     * Parses YAML characters from a reader without closing it.
     *
     * @param reader reader containing a YAML document
     * @return the structured root mapping, or an empty map for an empty document
     * @throws ConfigException when the reader cannot be consumed or the document is invalid
     */
    public static Map<String, Object> parse(Reader reader) {
        Objects.requireNonNull(reader, "reader must not be null");
        try {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                content.append(buffer, 0, count);
            }
            return parse(content.toString());
        } catch (IOException e) {
            throw new ConfigException("Cannot read YAML configuration", e);
        }
    }

    /**
     * Parses YAML text into a structured mapping.
     *
     * @param text YAML document text
     * @return the structured root mapping, or an empty map for an empty document
     * @throws ConfigException when the document is invalid or its root is not a mapping
     */
    public static Map<String, Object> parse(String text) {
        Objects.requireNonNull(text, "YAML text must not be null");
        String source = stripBom(text);
        try {
            // Valid YAML must never be rewritten by compatibility rules. Parsing the original
            // document first preserves standard mappings, sequences, tags, and block scalars.
            return parseStrict(source);
        } catch (RuntimeException originalFailure) {
            String compatible = normalize(source);
            if (!source.equals(compatible)) {
                try {
                    Map<String, Object> result = parseStrict(compatible);
                    LOG.warn(
                        "YAML parsed after compatibility normalization; quote values that begin "
                            + "with YAML-reserved characters");
                    return result;
                } catch (RuntimeException compatibilityFailure) {
                    // The original failure describes user input; the fallback failure describes
                    // generated text and would point operators at content they never supplied.
                }
            }
            throw safeError(originalFailure);
        }
    }

    /**
     * Flattens a structured YAML mapping into dot-separated keys.
     *
     * <p>Lists remain structured values so the consuming configuration domain can choose its own
     * serialization. Null leaves are omitted, and collisions between an explicit dotted key and a
     * nested mapping fail fast.
     *
     * @param document structured YAML root mapping
     * @return an immutable map containing dot-separated leaf keys
     * @throws ConfigException when a key is invalid or flattened keys collide
     */
    public static Map<String, Object> flatten(Map<String, ?> document) {
        Objects.requireNonNull(document, "YAML document must not be null");
        Map<String, Object> result = new LinkedHashMap<>();
        flatten(document, "", result);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Serializes a structured value using compact YAML flow syntax without a trailing newline.
     *
     * @param value structured value
     * @return compact YAML representation
     * @throws ConfigException when the value cannot be represented safely
     */
    public static String toFlowString(Object value) {
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
            options.setSplitLines(false);
            String output = new Yaml(options).dump(value);
            while (output.endsWith("\n") || output.endsWith("\r")) {
                output = output.substring(0, output.length() - 1);
            }
            return output;
        } catch (RuntimeException e) {
            throw safeError(e);
        }
    }

    /** Applies the two compatibility repairs supported by StreamPark's configuration editor. */
    private static String normalize(String text) {
        if (isBlank(text)) {
            return text;
        }
        return quoteValues(foldLines(text));
    }

    /**
     * Folds indented editor-style scalar continuation lines into the parent property.
     *
     * <p>Indented mappings, sequences, and block scalars are recognized as structured YAML and are
     * never folded.
     */
    private static String foldLines(String text) {
        if (isBlank(text)) {
            return text;
        }

        String lineSeparator = text.contains("\r\n") ? "\r\n" : "\n";
        String[] lines = text.split("\\r?\\n", -1);
        StringBuilder normalized = new StringBuilder(text.length());
        int index = 0;
        while (index < lines.length) {
            String currentLine = lines[index];
            Matcher matcher = EMPTY_VALUE_PROPERTY.matcher(currentLine);
            FoldBlock block =
                matcher.matches() ? findFoldBlock(lines, index, indent(currentLine)) : null;
            if (block == null) {
                appendLine(normalized, currentLine, lineSeparator, index < lines.length - 1);
                index++;
            } else {
                normalized
                    .append(matcher.group(1))
                    .append(matcher.group(2).trim())
                    .append(": ")
                    .append(String.join(" ", block.entries));
                if (block.end < lines.length) {
                    normalized.append(lineSeparator);
                }
                index = block.end;
            }
        }
        return normalized.toString();
    }

    /** Finds plain indented lines that may be folded into a scalar property. */
    private static FoldBlock findFoldBlock(String[] lines, int index, int parentIndent) {
        int next = index + 1;
        List<String> entries = new ArrayList<>();
        // A property without an inline value may own nested YAML or editor-style plain text. The
        // entire indented region is inspected before any line is modified.
        while (next < lines.length) {
            String child = lines[next];
            if (isBlank(child)) {
                next++;
            } else if (indent(child) <= parentIndent) {
                break;
            } else {
                String trimmed = child.trim();
                if (trimmed.startsWith("#")) {
                    next++;
                } else if (isStructured(trimmed)) {
                    // Nested mappings and sequences must remain untouched for the strict parser.
                    return null;
                } else {
                    entries.add(trimmed);
                    next++;
                }
            }
        }
        return entries.isEmpty() ? null : new FoldBlock(next, entries);
    }

    /** Immutable description of a scalar continuation region. */
    private static final class FoldBlock {

        private final int end;
        private final List<String> entries;

        private FoldBlock(int end, List<String> entries) {
            this.end = end;
            this.entries = entries;
        }
    }

    private static Map<String, Object> parseBytes(byte[] bytes) {
        return parse(decode(bytes));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseStrict(String text) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setAllowRecursiveKeys(false);
        Object result = new Yaml(new SafeConstructor(options)).load(text);
        if (result == null) {
            return new LinkedHashMap<>();
        }
        if (!(result instanceof Map)) {
            throw new ConfigException("YAML configuration root must be a mapping");
        }
        return (Map<String, Object>) result;
    }

    private static void flatten(Map<?, ?> source, String prefix, Map<String, Object> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new ConfigException("YAML configuration keys must be strings");
            }
            String segment = ((String) entry.getKey()).trim();
            if (segment.isEmpty()) {
                throw new ConfigException("YAML key segments must not be blank");
            }
            String key = prefix.isEmpty() ? segment : prefix + "." + segment;
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatten((Map<?, ?>) value, key, target);
            } else if (value != null && target.putIfAbsent(key, value) != null) {
                throw new ConfigException("Duplicate configuration key: " + key);
            }
        }
    }

    private static String quoteValues(String text) {
        String lineSeparator = text.contains("\r\n") ? "\r\n" : "\n";
        String[] lines = text.split("\\r?\\n", -1);
        StringBuilder normalized = new StringBuilder(text.length());
        for (int index = 0; index < lines.length; index++) {
            appendLine(
                normalized,
                quoteValue(lines[index]),
                lineSeparator,
                index < lines.length - 1);
        }
        return normalized.toString();
    }

    private static String quoteValue(String line) {
        if (isBlank(line) || line.trim().startsWith("#")) {
            return line;
        }
        int separatorIndex = line.indexOf(':');
        if (separatorIndex < 0 || separatorIndex >= line.length() - 1) {
            return line;
        }
        int valueStart = separatorIndex + 1;
        while (valueStart < line.length() && Character.isWhitespace(line.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= line.length()
            || LENIENT_SCALAR_PREFIXES.indexOf(line.charAt(valueStart)) < 0) {
            return line;
        }

        // Quote only the scalar portion. An inline comment must remain outside the generated quotes.
        int valueEnd = valueEnd(line, valueStart);
        String value = line.substring(valueStart, valueEnd).trim();
        // A standalone | or > (optionally with chomping/indent indicators) is valid block syntax.
        if (value.isEmpty() || BLOCK_SCALAR_HEADER.matcher(value).matches()) {
            return line;
        }
        return line.substring(0, valueStart)
            + '\''
            + value.replace("'", "''")
            + '\''
            + line.substring(valueEnd);
    }

    private static int valueEnd(String line, int valueStart) {
        for (int index = valueStart; index < line.length(); index++) {
            if (line.charAt(index) == '#'
                && index > valueStart
                && Character.isWhitespace(line.charAt(index - 1))) {
                int valueEnd = index;
                while (valueEnd > valueStart
                    && Character.isWhitespace(line.charAt(valueEnd - 1))) {
                    valueEnd--;
                }
                return valueEnd;
            }
        }
        return line.length();
    }

    private static boolean isStructured(String line) {
        return "-".equals(line)
            || line.startsWith("- ")
            || STRUCTURED_CHILD.matcher(line).matches();
    }

    private static int indent(String line) {
        int count = 0;
        while (count < line.length() && Character.isWhitespace(line.charAt(count))) {
            count++;
        }
        return count;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static String stripBom(String text) {
        return !text.isEmpty() && text.charAt(0) == '\ufeff' ? text.substring(1) : text;
    }

    private static String decode(byte[] bytes) {
        if (bytes.length == 0) {
            return "";
        }
        // Check four-byte markers before UTF-16 because UTF-32LE starts with the UTF-16LE marker.
        if (startsWith(bytes, 0x00, 0x00, 0xFE, 0xFF)) {
            return decode(bytes, 4, Charset.forName("UTF-32BE"));
        }
        if (startsWith(bytes, 0xFF, 0xFE, 0x00, 0x00)) {
            return decode(bytes, 4, Charset.forName("UTF-32LE"));
        }
        if (startsWith(bytes, 0xEF, 0xBB, 0xBF)) {
            return decode(bytes, 3, StandardCharsets.UTF_8);
        }
        if (startsWith(bytes, 0xFE, 0xFF)) {
            return decode(bytes, 2, StandardCharsets.UTF_16BE);
        }
        if (startsWith(bytes, 0xFF, 0xFE)) {
            return decode(bytes, 2, StandardCharsets.UTF_16LE);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String decode(byte[] bytes, int offset, Charset charset) {
        return new String(bytes, offset, bytes.length - offset, charset);
    }

    private static boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((bytes[index] & 0xFF) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static void appendLine(
                                   StringBuilder target,
                                   String line,
                                   String lineSeparator,
                                   boolean appendLineBreak) {
        target.append(line);
        if (appendLineBreak) {
            target.append(lineSeparator);
        }
    }

    private static ConfigException safeError(RuntimeException exception) {
        if (!(exception instanceof MarkedYAMLException)) {
            return exception instanceof ConfigException
                ? (ConfigException) exception
                : new ConfigException("Invalid YAML configuration");
        }
        // MarkedYAMLException#getMessage includes the offending source line. Rebuild the diagnostic
        // from problem and location fields so credentials in configuration values are never logged.
        MarkedYAMLException marked = (MarkedYAMLException) exception;
        StringBuilder message = new StringBuilder("Invalid YAML configuration");
        appendProblem(message, marked.getContext());
        appendMark(message, marked.getContextMark());
        appendProblem(message, marked.getProblem());
        appendMark(message, marked.getProblemMark());
        return new ConfigException(message.toString());
    }

    private static void appendProblem(StringBuilder message, String problem) {
        if (problem != null && !problem.trim().isEmpty()) {
            message.append(": ").append(problem.trim());
        }
    }

    private static void appendMark(StringBuilder message, Mark mark) {
        if (mark != null) {
            message
                .append(" in ")
                .append(mark.getName())
                .append(" at line ")
                .append(mark.getLine() + 1)
                .append(", column ")
                .append(mark.getColumn() + 1);
        }
    }
}
