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

package org.apache.streampark.console.core.managed;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedFlinkSchemaContractTest {

    private static final Path ASSEMBLY_SCRIPT = Path.of("src/main/assembly/script");
    private static final Path H2_SCHEMA = Path.of("src/main/resources/db/schema-h2.sql");

    private static final Pattern POSTGRES_CREATE_TABLE =
        Pattern.compile("(?i)create\\s+table\\s+\"public\"\\.\"(t_[a-z0-9_]+)\"");
    private static final Pattern POSTGRES_DROP_TABLE =
        Pattern.compile("(?i)drop\\s+table\\s+if\\s+exists\\s+\"public\"\\.\"(t_[a-z0-9_]+)\"");
    private static final Pattern POSTGRES_CREATE_SEQUENCE =
        Pattern.compile("(?i)create\\s+sequence(?:\\s+if\\s+not\\s+exists)?\\s+\"public\"\\.\"([a-z0-9_]+)\"");
    private static final Pattern POSTGRES_DROP_SEQUENCE =
        Pattern.compile("(?i)drop\\s+sequence\\s+if\\s+exists\\s+\"public\"\\.\"([a-z0-9_]+)\"");

    @Test
    void shouldKeepPostgresCreateAndDropListsSymmetric() throws IOException {
        String postgres = read(ASSEMBLY_SCRIPT.resolve("schema/pgsql-schema.sql"));

        assertThat(extract(postgres, POSTGRES_DROP_TABLE))
            .containsAll(extract(postgres, POSTGRES_CREATE_TABLE));
        assertThat(extract(postgres, POSTGRES_DROP_SEQUENCE))
            .containsAll(extract(postgres, POSTGRES_CREATE_SEQUENCE));
    }

    @Test
    void shouldDefineManagedFoundationInEverySchema() throws IOException {
        String mysql = read(ASSEMBLY_SCRIPT.resolve("schema/mysql-schema.sql"));
        String postgres = read(ASSEMBLY_SCRIPT.resolve("schema/pgsql-schema.sql"));
        String h2 = read(H2_SCHEMA);

        for (String table : Set.of(
            "t_cloud_account",
            "t_cloud_account_team",
            "t_managed_flink_env",
            "t_managed_flink_app",
            "t_managed_flink_artifact",
            "t_managed_flink_operation")) {
            assertThat(mysql).contains("`" + table + "`");
            assertThat(postgres).contains("\"public\".\"" + table + "\"");
            assertThat(h2).contains("`" + table + "`");
        }

        assertThat(mysql)
            .contains(
                "un_flink_cluster_id",
                "fk_managed_flink_env_cluster",
                "fk_managed_flink_app_application",
                "un_managed_flink_artifact_env_checksum",
                "fk_managed_flink_artifact_resource",
                "un_managed_flink_operation_intent",
                "fk_managed_flink_operation_parent",
                "`version_id` bigint default null comment 'flink version id; null for managed environment'");
        assertThat(postgres)
            .contains(
                "un_flink_cluster_id",
                "fk_managed_flink_env_cluster",
                "fk_managed_flink_app_application",
                "un_managed_flink_artifact_env_checksum",
                "fk_managed_flink_artifact_resource",
                "un_managed_flink_operation_intent",
                "fk_managed_flink_operation_parent",
                "\"version_id\" int8,");
        assertThat(h2)
            .contains(
                "unique (`id`)",
                "fk_managed_flink_env_cluster",
                "fk_managed_flink_app_application",
                "un_managed_flink_artifact_env_checksum",
                "fk_managed_flink_artifact_resource",
                "un_managed_flink_operation_intent",
                "fk_managed_flink_operation_parent",
                "`version_id` bigint default null comment 'flink version id; null for managed environment'");
    }

    @Test
    void shouldProvideManagedFoundationUpgradeForBothDialects() throws IOException {
        String mysql = read(ASSEMBLY_SCRIPT.resolve("upgrade/mysql/3.0.0.sql"));
        String postgres = read(ASSEMBLY_SCRIPT.resolve("upgrade/pgsql/3.0.0.sql"));

        for (String table : Set.of(
            "t_cloud_account",
            "t_cloud_account_team",
            "t_managed_flink_env",
            "t_managed_flink_app",
            "t_managed_flink_artifact",
            "t_managed_flink_operation")) {
            assertThat(mysql).contains("`" + table + "`");
            assertThat(postgres).contains("\"public\".\"" + table + "\"");
        }

        assertThat(mysql).contains("un_flink_cluster_id", "modify column `version_id` bigint null");
        assertThat(postgres).contains(
            "un_flink_cluster_id", "alter column \"version_id\" drop not null");
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private static Set<String> extract(String sql, Pattern pattern) {
        Set<String> values = new HashSet<>();
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }
}
