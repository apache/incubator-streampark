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

package org.apache.streampark.gateway.utils;

import org.apache.streampark.gateway.ConfigOption;
import org.apache.streampark.gateway.factories.SqlGatewayServiceFactory;
import org.apache.streampark.gateway.service.SqlGatewayService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Factory for {@link SqlGatewayService}. */
public class MockedSqlGatewayServiceFactory implements SqlGatewayServiceFactory {

    public static final ConfigOption<String> HOST = ConfigOption.key("host")
            .stringType()
            .noDefaultValue()
            .withDescription("The host of the Mocked SQL gateway service.");

    public static final ConfigOption<Integer> PORT = ConfigOption.key("port")
            .intType()
            .noDefaultValue()
            .withDescription("The port of the Mocked SQL gateway service.");

    public static final ConfigOption<Integer> DESCRIPTION = ConfigOption.key("description")
            .intType()
            .defaultValue(8080)
            .withDescription("The Mocked SQL gateway service.");

    @Override
    public String factoryIdentifier() {
        return "mocked";
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(HOST);
        options.add(PORT);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return Collections.singleton(DESCRIPTION);
    }

    @Override
    public SqlGatewayService createSqlGatewayService(Context context) {
        return new MockedSqlGatewayService("localhost", 8080, "The Mocked SQL gateway service");
    }
}
