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

import org.apache.streampark.common.CommonConfiguration;
import org.apache.streampark.plugin.registry.RegistryTestCase;
import org.apache.streampark.plugin.registry.core.server.IJdbcRegistryServer;
import org.apache.streampark.registry.api.ConnectionState;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = {CommonConfiguration.class, JdbcRegistryProperties.class})
@SpringBootApplication(scanBasePackageClasses = {CommonConfiguration.class, JdbcRegistryProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class JdbcRegistryTestCase extends RegistryTestCase<JdbcRegistry> {

    @Autowired
    private JdbcRegistryProperties jdbcRegistryProperties;

    @Autowired
    private IJdbcRegistryServer jdbcRegistryServer;

    @SneakyThrows
    @Test
    public void testAddConnectionStateListener() {

        AtomicReference<ConnectionState> connectionState = new AtomicReference<>();
        registry.addConnectionStateListener(connectionState::set);

        // todo: since the jdbc registry is started at the auto configuration, the stateListener is added after the
        // registry is started.
        assertThat(connectionState.get()).isEqualTo(null);
    }

    @Override
    public JdbcRegistry createRegistry() {
        return new JdbcRegistry(jdbcRegistryProperties, jdbcRegistryServer);
    }

}
