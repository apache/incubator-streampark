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
package org.apache.streampark.gateway.service;

import org.apache.streampark.gateway.exception.ValidationException;
import org.apache.streampark.gateway.factories.SqlGatewayServiceFactory;
import org.apache.streampark.gateway.factories.SqlGatewayServiceFactoryUtils;
import org.apache.streampark.gateway.utils.FakeSqlGatewayService;
import org.apache.streampark.gateway.utils.MockedSqlGatewayService;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.AssertFactory;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.apache.streampark.gateway.factories.SqlGatewayServiceFactoryUtils.createSqlGatewayService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/** Test {@link SqlGatewayServiceFactoryUtils}. */
public class SqlGatewayServiceFactoryUtilsTest {

    public static final InstanceOfAssertFactory<Stream, ListAssert<Throwable>> STREAM_THROWABLE =
        new InstanceOfAssertFactory<>(
            Stream.class, Assertions::<Throwable>assertThat);

    @Test
    public void testCreateServices() {
        String id = UUID.randomUUID().toString();
        Map<String, String> config = getDefaultConfig(id);
        config.put("streampark.sql-gateway.service", "mocked;fake");
        List<SqlGatewayService> actual = SqlGatewayServiceFactoryUtils.createSqlGatewayService(config);
        MockedSqlGatewayService expectedMocked = new MockedSqlGatewayService("localhost", 8080,
            "The Mocked SQL gateway service");
        assertThat(actual).isEqualTo(Arrays.asList(expectedMocked, FakeSqlGatewayService.INSTANCE));
    }

    @Test
    public void testCreateServiceWithDuplicateIdentifier() {
        Map<String, String> config = getDefaultConfig();
        config.put("streampark.sql-gateway.service", "mocked;mocked");
        validateException(
            config,
            "Get the duplicate service identifier 'mocked' for the option 'streampark.sql-gateway.service'. "
                + "Please keep the specified service identifier unique.");
    }

    @Test
    public void testCreateServiceWithoutType() {
        Map<String, String> config = getDefaultConfig();
        config.remove("streampark.sql-gateway.service");
        validateException(
            config,
            "Service options do not contain an option key 'streampark.sql-gateway.service' for discovering an service.");
    }

    @Test
    public void testCreateUnknownService() {
        Map<String, String> config = getDefaultConfig();
        config.put("streampark.sql-gateway.service", "mocked;unknown");
        validateException(
            config,
            String.format(
                "Could not find any factory for identifier 'unknown' "
                    + "that implements '%s' in the classpath.",
                SqlGatewayServiceFactory.class.getCanonicalName()));
    }

    /*
     * @Test public void testCreateServiceWithMissingOptions() { Map<String, String> config = getDefaultConfig();
     * config.remove("sql-gateway.Service.mocked.host");
     * 
     * validateException( config, "One or more required options are missing.\n\n" + "Missing required options are:\n\n"
     * + "host"); }
     */

    /*
     * @Test public void testCreateServiceWithUnconsumedOptions() { Map<String, String> config = getDefaultConfig();
     * config.put("sql-gateway.Service.mocked.unconsumed-option", "error");
     * 
     * validateException( config, "Unsupported options found for 'mocked'.\n\n" + "Unsupported options:\n\n" +
     * "unconsumed-option\n\n" + "Supported options:\n\n" + "description\n" + "host\n" + "id\n" + "port"); }
     */

    // --------------------------------------------------------------------------------------------

    private void validateException(Map<String, String> config, String errorMessage) {
        assertThatThrownBy(() -> createSqlGatewayService(config))
            .satisfies(anyCauseMatches(ValidationException.class, errorMessage));
    }

    private Map<String, String> getDefaultConfig() {
        return getDefaultConfig(UUID.randomUUID().toString());
    }

    private Map<String, String> getDefaultConfig(String id) {
        Map<String, String> config = new HashMap<>();
        config.put("sql-gateway.Service.mocked.id", id);
        config.put("streampark.sql-gateway.service", "mocked");
        config.put("sql-gateway.Service.mocked.host", "localhost");
        config.put("sql-gateway.Service.mocked.port", "9999");
        return config;
    }

    /**
     * Shorthand to assert chain of causes. Same as:
     *
     * <pre>{@code
     * assertThat(throwable)
     *     .extracting(FlinkAssertions::chainOfCauses, FlinkAssertions.STREAM_THROWABLE)
     * }</pre>
     */
    public static ListAssert<Throwable> assertThatChainOfCauses(Throwable root) {
        return Assertions.assertThat(root)
            .extracting(SqlGatewayServiceFactoryUtilsTest::chainOfCauses, STREAM_THROWABLE);
    }

    public static ThrowingConsumer<? super Throwable> anyCauseMatches(
                                                                      Class<? extends Throwable> clazz,
                                                                      String containsMessage) {
        return t -> assertThatChainOfCauses(t)
            .as(
                "Any cause is instance of class '%s' and contains message '%s'",
                clazz, containsMessage)
            .anySatisfy(
                cause -> Assertions.assertThat(cause)
                    .isInstanceOf(clazz)
                    .hasMessageContaining(containsMessage));
    }

    /**
     * You can use this method in combination with {@link AbstractThrowableAssert#extracting(Function,
     * AssertFactory)} to perform assertions on a chain of causes. For example:
     *
     * <pre>{@code
     * assertThat(throwable)
     *     .extracting(FlinkAssertions::chainOfCauses, FlinkAssertions.STREAM_THROWABLE)
     * }</pre>
     *
     * @return the list is ordered from the current {@link Throwable} up to the root cause.
     */
    public static Stream<Throwable> chainOfCauses(Throwable throwable) {
        if (throwable == null) {
            return Stream.empty();
        }
        if (throwable.getCause() == null) {
            return Stream.of(throwable);
        }
        return Stream.concat(Stream.of(throwable), chainOfCauses(throwable.getCause()));
    }
}
