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

import org.apache.streampark.common.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.apache.streampark.testcontainer.flink.FlinkComponent.JOBMANAGER;
import static org.apache.streampark.testcontainer.flink.FlinkComponent.TASKMANAGER;

/**
 * Class to start a couple of flink 1-jobmanager & n-taskmanagers. The priority of
 * flinkYamlConfContent is the highest. But: The 'jobmanager.rpc.address' is always 'jobmanager'.
 * The 'rest.port' always is 8081.
 */
public class FlinkStandaloneSessionCluster implements Startable {

  public static final Logger LOG = LoggerFactory.getLogger(FlinkStandaloneSessionCluster.class);

  private static final int BLOB_SERVER_PORT = 6123;
  private static final int WEB_PORT = 8081;
  private static final Network NETWORK = Network.newNetwork();
  private static final String JM_RPC_ADDR_KEY = "jobmanager.rpc.address";
  private static final String TM_SLOT_NUM_KEY = "taskmanager.numberOfTaskSlots";
  private static final String SLOT_CONF_FORMAT = String.format("%s: %%s", TM_SLOT_NUM_KEY);

  private String yamlConfContent = String.format("%s: %s", JM_RPC_ADDR_KEY, JOBMANAGER.getName());
  private final FlinkContainer jobManagerContainer;
  private final List<FlinkContainer> taskManagerContainers = new ArrayList<>();

  private FlinkStandaloneSessionCluster(
      DockerImageName dockerImageName,
      int taskManagerNum,
      int slotsNumPerTm,
      @Nullable String yamlConfContent,
      Slf4jLogConsumer slf4jLogConsumer) {

    renderJmRpcConfIfNeeded(yamlConfContent);

    renderSlotNumIfNeeded(slotsNumPerTm);

    // Set for JM.
    this.jobManagerContainer =
        new FlinkContainer(
            dockerImageName, JOBMANAGER, NETWORK, this.yamlConfContent, slf4jLogConsumer);
    this.jobManagerContainer.addExposedPort(BLOB_SERVER_PORT);
    this.jobManagerContainer.addExposedPort(WEB_PORT);

    this.jobManagerContainer.setWaitStrategy(
        Wait.forHttp("/config")
            .forStatusCode(200)
            .forPort(WEB_PORT)
            .withStartupTimeout(Duration.ofMinutes(8)));

    // Set for TMs.
    for (int i = 0; i < taskManagerNum; i++) {
      FlinkContainer taskManager =
          new FlinkContainer(
              dockerImageName, TASKMANAGER, NETWORK, this.yamlConfContent, slf4jLogConsumer);
      this.taskManagerContainers.add(taskManager);
    }
  }

  @Nonnull
  public String getFlinkJobManagerUrl() {
    return String.format(
        "http://%s:%s", jobManagerContainer.getHost(), jobManagerContainer.getMappedPort(WEB_PORT));
  }

  @Override
  public void start() {
    Utils.notNull(jobManagerContainer);
    jobManagerContainer.start();
    Utils.notNull(taskManagerContainers);
    for (FlinkContainer taskManagerContainer : taskManagerContainers) {
      taskManagerContainer.start();
    }
  }

  @Override
  public void stop() {
    Utils.notNull(taskManagerContainers);
    for (FlinkContainer taskManagerContainer : taskManagerContainers) {
      taskManagerContainer.stop();
    }
    Utils.notNull(jobManagerContainer);
    jobManagerContainer.stop();
  }

  private void renderSlotNumIfNeeded(int slotsNumPerTm) {
    if (!this.yamlConfContent.contains(TM_SLOT_NUM_KEY)) {
      this.yamlConfContent =
          String.format(
              "%s%n%s%n", this.yamlConfContent, String.format(SLOT_CONF_FORMAT, slotsNumPerTm));
    }
  }

  private void renderJmRpcConfIfNeeded(@Nullable String yamlConfStr) {
    this.yamlConfContent =
        yamlConfStr == null
            ? this.yamlConfContent
            : (yamlConfStr.contains(JM_RPC_ADDR_KEY)
                ? yamlConfStr
                : String.format("%s%n%s%n", this.yamlConfContent, yamlConfStr));
  }

  /** A tool class to create a flink standalone session cluster quickly. */
  public static class Builder {

    private DockerImageName dockerImageName =
        DockerImageName.parse("apache/flink:1.17.1-scala_2.12");
    private int taskManagerNum = 1;
    private int slotsNumPerTm = 8;
    private @Nullable String yamlConfContent;
    private Slf4jLogConsumer slf4jLogConsumer = new Slf4jLogConsumer(LOG, false);

    private Builder() {}

    public Builder dockerImageName(DockerImageName dockerImageName) {
      this.dockerImageName = dockerImageName;
      return this;
    }

    public Builder taskManagerNum(int taskManagerNum) {
      Utils.required(taskManagerNum >= 0, "taskManagerNum must be greater than -1.");
      this.taskManagerNum = taskManagerNum;
      return this;
    }

    public Builder slotsNumPerTm(int slotsNumPerTm) {
      Utils.required(slotsNumPerTm > 0, "slotsNumPerTm must be greater than 0.");
      this.slotsNumPerTm = slotsNumPerTm;
      return this;
    }

    public Builder yamlConfContent(@Nullable String yamlConfContent) {
      this.yamlConfContent = yamlConfContent;
      return this;
    }

    public Builder slf4jLogConsumer(Slf4jLogConsumer slf4jLogConsumer) {
      this.slf4jLogConsumer = slf4jLogConsumer;
      return this;
    }

    public FlinkStandaloneSessionCluster build() {
      return new FlinkStandaloneSessionCluster(
          dockerImageName, taskManagerNum, slotsNumPerTm, yamlConfContent, slf4jLogConsumer);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
