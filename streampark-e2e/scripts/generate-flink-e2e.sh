#!/usr/bin/env bash
# Generates Flink version-specific E2E tests and docker resources.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CASE_DIR="$ROOT/streampark-e2e-case/src/test/java/org/apache/streampark/e2e/cases"
DOCKER_DIR="$ROOT/streampark-e2e-case/src/test/resources/docker"

APACHE_LICENSE='/*
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
 */'

YAML_LICENSE='#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#'

generate_remote_compose() {
  local dir="$1" patch="$2"
  cat >"$dir/docker-compose.yaml" <<EOF
${YAML_LICENSE}

services:
  jobmanager:
    image: flink:${patch}
    command: jobmanager
    ports:
      - "8081:8081"
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
    networks:
      - e2e
    volumes:
      - flink_data:/opt/flink
      - /var/run/docker.sock:/var/run/docker.sock
    healthcheck:
      test: [ "CMD", "curl", "http://localhost:8081" ]
      interval: 5s
      timeout: 5s
      retries: 120

  taskmanager:
    image: flink:${patch}
    depends_on:
      - jobmanager
    command: taskmanager
    scale: 1
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 2
    networks:
      - e2e
    volumes:
      - flink_data:/opt/flink
      - /var/run/docker.sock:/var/run/docker.sock
    healthcheck:
      test: [ "CMD", "curl", "http://localhost:8081" ]
      interval: 5s
      timeout: 5s
      retries: 120

  streampark:
    image: apache/streampark:ci
    command: bash bin/streampark.sh start_docker
    build:
      context: ./
      dockerfile: ./Dockerfile
    ports:
      - 10000:10000
      - 10030:10030
    environment:
      - SPRING_PROFILES_ACTIVE=h2
      - TZ=Asia/Shanghai
      - FLINK_JOBMANAGER_URL=http://jobmanager:8081
    privileged: true
    restart: unless-stopped
    networks:
      - e2e
    volumes:
      - flink_data:/opt/flink
      - \${HOME}/streampark_build_logs:/tmp/streampark/logs/build_logs/
      - /var/run/docker.sock:/var/run/docker.sock
    healthcheck:
      test: [ "CMD", "curl", "http://localhost:10000" ]
      interval: 5s
      timeout: 5s
      retries: 120
networks:
  e2e:
volumes:
  flink_data:
EOF
  cp "$DOCKER_DIR/flink-1.20-on-remote/Dockerfile" "$dir/Dockerfile"
}

generate_yarn_assets() {
  local dir="$1" patch="$2" java_tag="$3"
  local image="apache/streampark-flink-${patch}-on-yarn:ci"
  cp "$DOCKER_DIR/flink-1.20-on-yarn/docker-compose.config" "$dir/docker-compose.config"
  cat >"$dir/Dockerfile" <<EOF
${YAML_LICENSE}

FROM apache/streampark:ci as base-image
FROM flink:${patch}-scala_2.12-${java_tag} as flink-image
FROM eclipse-temurin:11-jdk-jammy as jdk11-image

FROM sbloodys/hadoop:3.3.6
COPY --from=base-image /streampark /streampark
RUN sudo chown -R hadoop.hadoop /streampark \\
    && sed -i "s/hadoop-user-name: hdfs\$/hadoop-user-name: hadoop/g" /streampark/conf/config.yaml

COPY --from=flink-image /opt/flink /flink-${patch}
RUN sudo chown -R hadoop.hadoop /flink-${patch}

COPY --from=jdk11-image /opt/java/openjdk /usr/lib/jvm/jdk11
ENV JAVA_HOME=/usr/lib/jvm/jdk11
ENV PATH="\${JAVA_HOME}/bin:\${PATH}"
EOF
  sed "s/apache\\/streampark-flink-1.20.1-on-yarn:ci/${image//\//\\/}/g" \
    "$DOCKER_DIR/flink-1.20-on-yarn/docker-compose.yaml" >"$dir/docker-compose.yaml"
}

generate_remote_test() {
  local class_prefix="$1" docker_key="$2" patch="$3"
  cat >"$CASE_DIR/${class_prefix}OnRemoteClusterDeployTest.java" <<EOF
${APACHE_LICENSE}

package org.apache.streampark.e2e.cases;

import org.apache.streampark.e2e.core.StreamPark;
import org.apache.streampark.e2e.pages.LoginPage;
import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.flink.ApacheFlinkPage;
import org.apache.streampark.e2e.pages.flink.FlinkHomePage;
import org.apache.streampark.e2e.pages.flink.clusters.ClusterDetailForm;
import org.apache.streampark.e2e.pages.flink.clusters.FlinkClustersPage;
import org.apache.streampark.e2e.pages.flink.clusters.RemoteForm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/flink-${docker_key}-on-remote/docker-compose.yaml")
public class ${class_prefix}OnRemoteClusterDeployTest {

    public static RemoteWebDriver browser;

    private static final String flinkName = "flink-${patch}";

    private static final String flinkHome = "/opt/flink/";

    private static final String flinkDescription = "description test";

    private static final String flinkClusterName = "flink_${patch}_cluster_e2e";

    private static final String flinkJobManagerUrl = "http://jobmanager:8081";

    private static final ClusterDetailForm.DeployMode deployMode = ClusterDetailForm.DeployMode.STANDALONE;

    @BeforeAll
    public static void setUp() {
        FlinkHomePage flinkHomePage = new LoginPage(browser)
            .login()
            .goToNav(ApacheFlinkPage.class)
            .goToTab(FlinkHomePage.class);

        flinkHomePage.createFlinkHome(flinkName, flinkHome, flinkDescription);

        flinkHomePage.goToNav(ApacheFlinkPage.class)
            .goToTab(FlinkClustersPage.class);
    }

    @Test
    @Order(1)
    public void testCreateFlinkCluster() {
        FlinkClustersPage flinkClustersPage = new FlinkClustersPage(browser);

        flinkClustersPage.createFlinkCluster()
            .<RemoteForm>addCluster(deployMode)
            .jobManagerURL(flinkJobManagerUrl)
            .clusterName(flinkClusterName)
            .flinkVersion(flinkName)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(flinkClustersPage.flinkClusterList)
                    .as("Flink clusters list should contain newly-created application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(flinkClusterName)));
    }

    @Test
    @Order(5)
    public void testDeleteFlinkCluster() {
        final FlinkClustersPage flinkClustersPage = new FlinkClustersPage(browser);

        flinkClustersPage.deleteFlinkCluster(flinkClusterName);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();
                    Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
                    assertThat(flinkClustersPage.flinkClusterList)
                        .noneMatch(it -> it.getText().contains(flinkClusterName));
                });
    }
}
EOF
}

generate_yarn_cluster_test() {
  local class_prefix="$1" docker_key="$2" patch="$3"
  cat >"$CASE_DIR/${class_prefix}OnYarnClusterDeployTest.java" <<EOF
${APACHE_LICENSE}

package org.apache.streampark.e2e.cases;

import org.apache.streampark.e2e.core.StreamPark;
import org.apache.streampark.e2e.pages.LoginPage;
import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.flink.ApacheFlinkPage;
import org.apache.streampark.e2e.pages.flink.FlinkHomePage;
import org.apache.streampark.e2e.pages.flink.clusters.ClusterDetailForm;
import org.apache.streampark.e2e.pages.flink.clusters.FlinkClustersPage;
import org.apache.streampark.e2e.pages.flink.clusters.YarnSessionForm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/flink-${docker_key}-on-yarn/docker-compose.yaml")
public class ${class_prefix}OnYarnClusterDeployTest {

    public static RemoteWebDriver browser;

    private static final String flinkName = "flink-${patch}";

    private static final String flinkHome = "/flink-${patch}";

    private static final String flinkDescription = "description test";

    private static final String flinkClusterName = "flink_${patch}_cluster_e2e";

    private static final String flinkClusterNameEdited = "flink_${patch}_cluster_e2e_edited";

    private static final ClusterDetailForm.DeployMode deployMode = ClusterDetailForm.DeployMode.YARN_SESSION;

    @BeforeAll
    public static void setup() {
        FlinkHomePage flinkHomePage = new LoginPage(browser)
            .login()
            .goToNav(ApacheFlinkPage.class)
            .goToTab(FlinkHomePage.class);

        flinkHomePage.createFlinkHome(flinkName, flinkHome, flinkDescription);

        flinkHomePage.goToNav(ApacheFlinkPage.class)
            .goToTab(FlinkClustersPage.class);
    }

    @Test
    @Order(1)
    public void testCreateFlinkCluster() {
        final FlinkClustersPage flinkClustersPage = new FlinkClustersPage(browser);

        flinkClustersPage.createFlinkCluster()
            .<YarnSessionForm>addCluster(deployMode)
            .resolveOrder(YarnSessionForm.ResolveOrder.CHILD_FIRST)
            .clusterName(flinkClusterName)
            .flinkVersion(flinkName)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(flinkClustersPage.flinkClusterList)
                    .as("Flink clusters list should contain newly-created application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(flinkClusterName)));
    }

    @Test
    @Order(2)
    public void testEditFlinkCluster() {
        final FlinkClustersPage flinkClustersPage = new FlinkClustersPage(browser);

        flinkClustersPage.editFlinkCluster(flinkClusterName)
            .<YarnSessionForm>addCluster(deployMode)
            .clusterName(flinkClusterNameEdited)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(flinkClustersPage.flinkClusterList)
                    .as("Flink clusters list should contain edited application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(flinkClusterNameEdited)));
    }

    @Test
    @Order(3)
    public void testStartFlinkCluster() {
        final FlinkClustersPage flinkClustersPage = new FlinkClustersPage(browser);

        flinkClustersPage.startFlinkCluster(flinkClusterNameEdited);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(flinkClustersPage.flinkClusterList)
                    .as("Flink clusters list should contain running application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("RUNNING")));
    }

    @Test
    @Order(4)
    public void testStopFlinkCluster() {
        final FlinkClustersPage flinkClustersPage = new FlinkClustersPage(browser);

        flinkClustersPage.stopFlinkCluster(flinkClusterNameEdited);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(flinkClustersPage.flinkClusterList)
                    .as("Flink clusters list should contain canceled application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("CANCELED")));
    }

    @Test
    @Order(5)
    public void testDeleteFlinkCluster() {
        final FlinkClustersPage flinkClustersPage = new FlinkClustersPage(browser);

        flinkClustersPage.deleteFlinkCluster(flinkClusterNameEdited);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();
                    Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
                    assertThat(flinkClustersPage.flinkClusterList)
                        .noneMatch(it -> it.getText().contains(flinkClusterNameEdited));
                });
    }
}
EOF
}

generate_sql_test() {
  local class_prefix="$1" docker_key="$2" patch="$3" app_suffix="$4"
  sed \
    -e "s/FlinkSQL120OnYarnTest/FlinkSQL${class_prefix#Flink}OnYarnTest/g" \
    -e "s/flink-1.20-on-yarn/flink-${docker_key}-on-yarn/g" \
    -e "s/flink-1.20.1/flink-${patch}/g" \
    -e "s/flink-120-e2e-test/flink-${app_suffix}-e2e-test/g" \
    -e "s/flink_1.20.1_cluster_e2e/flink_${patch}_cluster_e2e/g" \
    "$CASE_DIR/FlinkSQL120OnYarnTest.java" >"$CASE_DIR/FlinkSQL${class_prefix#Flink}OnYarnTest.java"
}

generate_k8s_assets() {
  local dir="$1" patch="$2" java_tag="$3"
  local image="apache/streampark-flink-${patch}-on-k8s:ci"
  cat >"$dir/Dockerfile" <<EOF
${YAML_LICENSE}

FROM apache/streampark:ci as base-image
FROM flink:${patch}-scala_2.12-${java_tag} as flink-image
FROM apache/streampark:ci
COPY --from=flink-image /opt/flink /flink-${patch}
EOF
  sed "s/apache\\/streampark-flink-1.20.1-on-k8s:ci/${image//\//\\/}/g" \
    "$DOCKER_DIR/flink-1.20-on-k8s/docker-compose.yaml" >"$dir/docker-compose.yaml"
}

generate_k8s_cluster_test() {
  local class_prefix="$1" docker_key="$2" patch="$3" java_tag="$4"
  sed \
    -e "s/Flink120OnK8sClusterDeployTest/${class_prefix}OnK8sClusterDeployTest/g" \
    -e "s/flink-1.20-on-k8s/flink-${docker_key}-on-k8s/g" \
    -e "s/flink-1.20.1/flink-${patch}/g" \
    -e "s/flink_1.20.1_k8s_cluster_e2e/flink_${patch}_k8s_cluster_e2e/g" \
    -e "s/flink:1.20.1-scala_2.12-java8/flink:${patch}-scala_2.12-${java_tag}/g" \
    "$CASE_DIR/Flink120OnK8sClusterDeployTest.java" >"$CASE_DIR/${class_prefix}OnK8sClusterDeployTest.java"
}

generate_k8s_sql_test() {
  local class_prefix="$1" docker_key="$2" patch="$3" java_tag="$4" app_suffix="$5"
  sed \
    -e "s/FlinkSQL120OnK8sTest/FlinkSQL${class_prefix#Flink}OnK8sTest/g" \
    -e "s/flink-1.20-on-k8s/flink-${docker_key}-on-k8s/g" \
    -e "s/flink-1.20.1/flink-${patch}/g" \
    -e "s/flink_1.20.1_k8s_cluster_e2e/flink_${patch}_k8s_cluster_e2e/g" \
    -e "s/flink120e2etest/flink${app_suffix}e2etest/g" \
    -e "s/flink120session/flink${app_suffix}session/g" \
    -e "s/flink:1.20.1-scala_2.12-java8/flink:${patch}-scala_2.12-${java_tag}/g" \
    "$CASE_DIR/FlinkSQL120OnK8sTest.java" >"$CASE_DIR/FlinkSQL${class_prefix#Flink}OnK8sTest.java"
}

# docker_key|patch|class_prefix|java_tag|app_suffix
VERSIONS=(
  "1.19|1.19.0|Flink119|java8|119"
  "2.0|2.0.2|Flink200|java11|200"
  "2.1|2.1.2|Flink210|java11|210"
  "2.2|2.2.1|Flink220|java11|220"
  "2.3|2.3.0|Flink230|java11|230"
)

for entry in "${VERSIONS[@]}"; do
  IFS='|' read -r docker_key patch class_prefix java_tag app_suffix <<<"$entry"
  remote_dir="$DOCKER_DIR/flink-${docker_key}-on-remote"
  yarn_dir="$DOCKER_DIR/flink-${docker_key}-on-yarn"
  k8s_dir="$DOCKER_DIR/flink-${docker_key}-on-k8s"
  mkdir -p "$remote_dir" "$yarn_dir" "$k8s_dir"
  generate_remote_compose "$remote_dir" "$patch"
  generate_yarn_assets "$yarn_dir" "$patch" "$java_tag"
  generate_k8s_assets "$k8s_dir" "$patch" "$java_tag"
  generate_remote_test "$class_prefix" "$docker_key" "$patch"
  generate_yarn_cluster_test "$class_prefix" "$docker_key" "$patch"
  generate_k8s_cluster_test "$class_prefix" "$docker_key" "$patch" "$java_tag"
  generate_sql_test "$class_prefix" "$docker_key" "$patch" "$app_suffix"
  generate_k8s_sql_test "$class_prefix" "$docker_key" "$patch" "$java_tag" "$app_suffix"
  echo "Generated Flink ${patch} (${class_prefix})"
done

# Generate K8s assets for baseline versions managed outside VERSIONS loop
BASELINE_VERSIONS=(
  "1.17|1.17.2|Flink117|java8|117"
  "1.18|1.18.1|Flink118|java8|118"
)
for entry in "${BASELINE_VERSIONS[@]}"; do
  IFS='|' read -r docker_key patch class_prefix java_tag app_suffix <<<"$entry"
  k8s_dir="$DOCKER_DIR/flink-${docker_key}-on-k8s"
  mkdir -p "$k8s_dir"
  generate_k8s_assets "$k8s_dir" "$patch" "$java_tag"
  generate_k8s_cluster_test "$class_prefix" "$docker_key" "$patch" "$java_tag"
  generate_k8s_sql_test "$class_prefix" "$docker_key" "$patch" "$java_tag" "$app_suffix"
  echo "Generated K8s baseline Flink ${patch} (${class_prefix})"
done

# Remove Flink 1.16 assets
rm -rf "$DOCKER_DIR/flink-1.16-on-remote" "$DOCKER_DIR/flink-1.16-on-yarn"
rm -f "$CASE_DIR"/Flink116*.java "$CASE_DIR"/FlinkSQL116*.java
echo "Removed Flink 1.16 E2E assets"
