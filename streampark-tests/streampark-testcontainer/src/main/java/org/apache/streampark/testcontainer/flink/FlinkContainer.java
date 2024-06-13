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

package org.apache.streampark.testcontainer.flink;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.streampark.testcontainer.flink.FlinkComponent.JOBMANAGER;
import static org.apache.streampark.testcontainer.flink.FlinkComponent.TASKMANAGER;

/**
 * The Flink container class. It would be created as a flink jobmanager container or a taskmanaager
 * container. Note: It's an internal class to construct a flink session cluster.
 */
class FlinkContainer extends GenericContainer<FlinkContainer> {

  private static final String FLINK_PROPS_KEY = "FLINK_PROPERTIES";
  private static final AtomicInteger TM_INDEX_SUFFIX = new AtomicInteger(0);

  private final @Nonnull FlinkComponent component;

  FlinkContainer(
      @Nonnull DockerImageName dockerImageName,
      @Nonnull FlinkComponent component,
      @Nonnull Network network,
      @Nonnull String yamlPropContent,
      @Nullable Slf4jLogConsumer slf4jLogConsumer) {
    super(dockerImageName);
    this.component = component;
    this.withCommand("/docker-entrypoint.sh", component.getName());
    this.withCreateContainerCmdModifier(
        createContainerCmd -> createContainerCmd.withName(getFlinkContainerName()));
    this.withNetwork(network);
    this.withEnv(FLINK_PROPS_KEY, yamlPropContent);
    Optional.ofNullable(slf4jLogConsumer).ifPresent(this::withLogConsumer);
  }

  @Nonnull
  protected String getFlinkContainerName() {
    if (component == JOBMANAGER) {
      return JOBMANAGER.getName();
    }
    return String.format("%s_%s", TASKMANAGER.getName(), TM_INDEX_SUFFIX.incrementAndGet());
  }
}
