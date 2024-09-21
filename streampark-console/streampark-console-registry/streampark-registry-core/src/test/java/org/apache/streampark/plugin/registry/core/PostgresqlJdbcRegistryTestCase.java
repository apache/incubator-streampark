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

package org.apache.streampark.plugin.registry.core;

import org.apache.streampark.registry.api.sql.SqlScriptRunner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Stream;

@ActiveProfiles("postgresql")
public class PostgresqlJdbcRegistryTestCase extends JdbcRegistryTestCase {

    private static GenericContainer<?> postgresqlContainer;

    @SneakyThrows
    @BeforeAll
    public static void setUpTestingServer() {
        postgresqlContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:16.0"))
            .withUsername("root")
            .withPassword("root")
            .withDatabaseName("streampark")
            .withNetwork(Network.newNetwork())
            .withExposedPorts(5432);

        Startables.deepStart(Stream.of(postgresqlContainer)).join();

        String jdbcUrl = "jdbc:postgresql://localhost:" + postgresqlContainer.getMappedPort(5432) + "/streampark";
        System.clearProperty("spring.datasource.url");
        System.setProperty("spring.datasource.url", jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername("root");
        config.setPassword("root");

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            SqlScriptRunner sqlScriptRunner = new SqlScriptRunner(dataSource, "postgresql_registry_init.sql");
            sqlScriptRunner.execute();
        }
    }

    @SneakyThrows
    @AfterAll
    public static void tearDownTestingServer() {
        postgresqlContainer.close();
    }
}
