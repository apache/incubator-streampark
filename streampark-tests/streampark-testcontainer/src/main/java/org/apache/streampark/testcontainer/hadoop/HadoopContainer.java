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

package org.apache.streampark.testcontainer.hadoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Hadoop Container for integration test. Note: It's experimental now. */
public class HadoopContainer extends GenericContainer<HadoopContainer> {

  public static final Logger LOG = LoggerFactory.getLogger(HadoopContainer.class);

  // Hadoop version is 2.7.0
  private static final DockerImageName DOCKER_IMAGE_NAME =
      DockerImageName.parse("sequenceiq/hadoop-docker:latest");

  public static final Map<Integer, Integer> MAPPED_PORTS =
      new HashMap<Integer, Integer>() {
        {
          put(50070, 50070);
          put(8088, 8088);
          put(9000, 9000);
        }
      };

  public HadoopContainer() {
    this(DOCKER_IMAGE_NAME);
  }

  public HadoopContainer(@Nonnull DockerImageName dockerImageName) {
    super(dockerImageName);
    MAPPED_PORTS.forEach(this::addFixedExposedPort);
    this.setPrivilegedMode(true);
    this.withCreateContainerCmdModifier(
        createContainerCmd -> createContainerCmd.withName("one-container-hadoop-cluster"));
    WaitAllStrategy waitAllStrategy =
        new WaitAllStrategy()
            .withStrategy(
                Wait.forHttp("/ws/v1/cluster/info")
                    .forPort(8088)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(8)))
            .withStrategy(
                Wait.forHttp("")
                    .forPort(50070)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(8)))
            .withStrategy(Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(8)))
            .withStartupTimeout(Duration.ofMinutes(8));
    this.waitingFor(waitAllStrategy);
    this.withLogConsumer(new Slf4jLogConsumer(LOG));
    this.withCommand("/etc/bootstrap.sh", "-d");
  }

  @Override
  public void start() {
    super.start();
    this.waitUntilContainerStarted();
  }
}
