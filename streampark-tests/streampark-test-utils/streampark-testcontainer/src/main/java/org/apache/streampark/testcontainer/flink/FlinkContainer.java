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

public class FlinkContainer extends GenericContainer<FlinkContainer> {

  public static final AtomicInteger TM_COUNT = new AtomicInteger(0);

  public static final String FLINK_PROPS_KEY = "FLINK_PROPERTIES";

  private final @Nonnull FlinkComponent component;

  FlinkContainer(
      @Nonnull DockerImageName dockerImageName,
      @Nonnull FlinkComponent component,
      @Nonnull Network network,
      @Nonnull String yamlPropStr,
      @Nullable Slf4jLogConsumer slf4jLogConsumer) {
    super(dockerImageName);
    this.component = component;
    this.withCommand("/docker-entrypoint.sh", component.getName());
    this.withCreateContainerCmdModifier(
        createContainerCmd -> createContainerCmd.withName(getFlinkContainerName()));
    this.withNetwork(network);
    this.withEnv(FLINK_PROPS_KEY, yamlPropStr);
    Optional.ofNullable(slf4jLogConsumer).ifPresent(this::withLogConsumer);
  }

  protected String getFlinkContainerName() {
    if (component == JOBMANAGER) {
      return JOBMANAGER.getName();
    }
    return String.format("%s_%s", TASKMANAGER.getName(), TM_COUNT.incrementAndGet());
  }
}
