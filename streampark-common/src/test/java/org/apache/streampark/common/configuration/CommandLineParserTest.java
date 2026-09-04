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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandLineParserTest {

    @Test
    void parseSeparatedInlineAndFlagValues() {
        Configuration configuration =
            CommandLineParser.parse(
                new String[]{"--name", "orders", "--parallelism=4", "--detached"});

        assertThat(configuration.toMap())
            .containsEntry("name", "orders")
            .containsEntry("parallelism", "4")
            .containsEntry("detached", "true");
    }

    @Test
    void rejectPositionalAndDuplicateArgs() {
        assertThatThrownBy(() -> CommandLineParser.parse(new String[]{"orders"}))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("Expected a --key");
        assertThatThrownBy(
            () -> CommandLineParser.parse(new String[]{"--name", "one", "--name", "two"}))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("Duplicate command-line option");
    }

    @Test
    void tokenizeQuotesAndEscapesSafely() {
        assertThat(CommandLineTokenizer.tokenize("--name 'order service' --sql select\\ 1 $HOME"))
            .containsExactly("--name", "order service", "--sql", "select 1", "$HOME");
        assertThatThrownBy(() -> CommandLineTokenizer.tokenize("--name 'unterminated"))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("unterminated");
    }

    @Test
    void requireExplicitJvmAssignments() {
        Configuration configuration =
            JvmOptionsParser.parse("-Dparallelism.default=4 -D pipeline.name='orders job'");

        assertThat(configuration.toMap())
            .containsEntry("parallelism.default", "4")
            .containsEntry("pipeline.name", "orders job");
        assertThatThrownBy(() -> JvmOptionsParser.parse("-Ddetached"))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("key=value");
    }

    @Test
    void buildImmutableGroupedOptions() {
        Map<String, Map<String, String>> grouped =
            GroupedOptionsParser.parse(
                new String[]{"--kafka", "topic=orders", "--jdbc", "url=jdbc:h2:mem:test"});

        assertThat(grouped.get("kafka")).containsEntry("topic", "orders");
        assertThat(grouped.get("jdbc")).containsEntry("url", "jdbc:h2:mem:test");
        assertThatThrownBy(() -> grouped.put("new", Map.of()))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
