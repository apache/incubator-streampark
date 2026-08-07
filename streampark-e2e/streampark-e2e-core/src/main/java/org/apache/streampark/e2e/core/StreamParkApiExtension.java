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

package org.apache.streampark.e2e.core;

import org.apache.streampark.e2e.core.api.ApiClient;

import com.google.common.net.HostAndPort;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Starts StreamPark via docker-compose and injects {@link ApiClient} for REST tests. */
@Slf4j
final class StreamParkApiExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final int DOCKER_PORT = 10000;
    private static final String SERVICE_NAME = "streampark";

    private final boolean localMode = Objects.equals(System.getProperty("local"), "true");
    private final int localPort = 10001;

    private ComposeContainer compose;
    private ApiClient apiClient;

    @Override
    public void beforeAll(ExtensionContext context) {
        HostAndPort address;
        if (localMode) {
            address = HostAndPort.fromParts("localhost", localPort);
        } else {
            compose = createDockerCompose(context);
            compose.start();
            address = HostAndPort.fromParts("localhost", compose.getServicePort(SERVICE_NAME, DOCKER_PORT));
        }
        apiClient = new ApiClient("http://" + address.getHost() + ":" + address.getPort());
        injectApiClient(context.getRequiredTestClass(), apiClient);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (compose != null) {
            compose.stop();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        injectApiClient(context.getRequiredTestClass(), apiClient);
        Object instance = context.getRequiredTestInstance();
        Stream.of(instance.getClass().getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .filter(field -> ApiClient.class.isAssignableFrom(field.getType()))
            .forEach(field -> setField(instance, field, apiClient));
    }

    private void injectApiClient(Class<?> clazz, ApiClient client) {
        Stream.of(clazz.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .filter(field -> ApiClient.class.isAssignableFrom(field.getType()))
            .forEach(field -> setField(null, field, client));
    }

    private void setField(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            log.error("Failed to inject ApiClient to field: {}", field.getName(), e);
        }
    }

    private ComposeContainer createDockerCompose(ExtensionContext context) {
        final Class<?> clazz = context.getRequiredTestClass();
        final StreamParkApi annotation = clazz.getAnnotation(StreamParkApi.class);
        final List<File> files =
            Stream.of(annotation.composeFiles())
                .map(path -> StreamParkApi.class.getClassLoader().getResource(path))
                .filter(Objects::nonNull)
                .map(URL::getPath)
                .map(File::new)
                .collect(Collectors.toList());

        return new ComposeContainer(files)
            .withPull(true)
            .withTailChildContainers(true)
            .withLocalCompose(true)
            .withExposedService(
                SERVICE_NAME,
                DOCKER_PORT,
                Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(300)))
            .withLogConsumer(SERVICE_NAME, outputFrame -> log.info(outputFrame.getUtf8String()))
            .waitingFor(
                SERVICE_NAME, Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(300)));
    }
}
