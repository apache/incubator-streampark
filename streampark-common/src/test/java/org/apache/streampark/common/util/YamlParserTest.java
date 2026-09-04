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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies strict YAML parsing and the bounded compatibility fallback. */
class YamlParserTest {

    @Test
    void parseEditorMultilineScalar() {
        String yaml =
            "source:\n"
                + "  tables:\n"
                + "    db.table_a,\n"
                + "    # db.table_b,\n"
                + "    db.table_c\n"
                + "transforms:\n"
                + "  - source-table: db.table_a\n"
                + "    projection: |\n"
                + "      id,\n"
                + "      name\n";

        Map<String, Object> document = YamlParser.parse(yaml);

        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) document.get("source");
        assertThat(source)
            .containsEntry("tables", "db.table_a, db.table_c");
        assertThat(document.get("transforms")).isInstanceOf(java.util.List.class);
    }

    @Test
    void quoteUnsafeScalarsOnRetry() {
        Map<String, Object> document =
            YamlParser.parse(
                "security:\n"
                    + "  keytab: !unquoted-keytab\n"
                    + "state:\n"
                    + "  directory: >hdfs:///checkpoints\n");

        assertThat(YamlParser.flatten(document))
            .containsEntry("security.keytab", "!unquoted-keytab")
            .containsEntry("state.directory", ">hdfs:///checkpoints");
    }

    @Test
    void decodeUtf16LittleEndianBom() {
        byte[] content = "pipeline:\n  name: demo\n".getBytes(StandardCharsets.UTF_16LE);
        byte[] bytes = new byte[content.length + 2];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xFE;
        System.arraycopy(content, 0, bytes, 2, content.length);

        Map<String, Object> document =
            YamlParser.parse(new ByteArrayInputStream(bytes));

        assertThat(YamlParser.flatten(document)).containsEntry("pipeline.name", "demo");
    }

    @Test
    void serializeListsUsingYamlFlowSyntax() {
        assertThat(YamlParser.toFlowString(Arrays.asList("host-a", "host-b")))
            .isEqualTo("[host-a, host-b]");
    }

    @Test
    void rejectDuplicateKeysSafely() {
        assertThatThrownBy(() -> YamlParser.parse("password: first\npassword: second\n"))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("duplicate key password")
            .hasMessageNotContaining("first")
            .hasMessageNotContaining("second");
    }
}
