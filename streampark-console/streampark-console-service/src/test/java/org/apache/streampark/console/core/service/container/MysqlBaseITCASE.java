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

package org.apache.streampark.console.core.service.container;

import org.apache.streampark.console.SpringUnitTestBase;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.util.stream.Stream;

public class MysqlBaseITCASE extends SpringUnitTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(MysqlBaseITCASE.class);
    protected static final MySqlContainer MYSQL_CONTAINER =
        createMySqlContainer("docker/server/my.cnf");

    @BeforeAll
    public static void startContainers() {
        LOG.info("Starting containers...");
        Startables.deepStart(Stream.of(MYSQL_CONTAINER)).join();
        LOG.info("Containers are started.");
    }

    @AfterAll
    public static void stopContainers() {
        LOG.info("Stopping containers...");
        if (MYSQL_CONTAINER != null) {
            MYSQL_CONTAINER.stop();
        }
        LOG.info("Containers are stopped.");
    }

    protected static MySqlContainer createMySqlContainer(String configPath) {
        return (MySqlContainer) new MySqlContainer(MySqlContainer.MYSQL_VERSION)
            .withConfigurationOverride(configPath)
            .withSetupSQL("docker/setup.sql")
            .withDatabaseName("flink-test")
            .withUsername("flinkuser")
            .withPassword("flinkpw")
            .withLogConsumer(new Slf4jLogConsumer(LOG));
    }
}
