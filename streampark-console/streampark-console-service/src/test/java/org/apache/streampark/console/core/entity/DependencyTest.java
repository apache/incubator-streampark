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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.util.DependencyUtils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import scala.collection.JavaConversions;

@Slf4j
class DependencyTest {

    @Test
    void resolveMavenDependencies() {
        /**
         * <dependency>
         *      <groupId>org.apache.flink</groupId>
         *      <artifactId>flink-table-common</artifactId>
         *      <version>${flink.version}</version>
         * </dependency>
         *
         * <dependency>
         *     <groupId>org.apache.flink</groupId>
         *     <artifactId>flink-java</artifactId>
         *     <version>${flink.version}</version>
         * </dependency>
         *
         */

        List<Application.Pom> dependency = new ArrayList<>();

        Application.Pom dept = new Application.Pom();
        dept.setGroupId("org.apache.flink");
        dept.setArtifactId("flink-java");
        dept.setVersion("1.11.x");
        dependency.add(dept);

        StringBuilder builder = new StringBuilder();
        dependency.forEach(x -> {
            String info = String.format("%s:%s:%s,", x.getGroupId(), x.getArtifactId(), x.getVersion());
            builder.append(info);
        });
        String packages = builder.deleteCharAt(builder.length() - 1).toString();

        builder.setLength(0);
        builder.append("org.apache.flink:force-shading,")
            .append("com.google.code.findbugs:jsr305,")
            .append("org.slf4j:*,")
            .append("org.apache.logging.log4j:*");

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info(">>>>> running....");
            }
        }, 0, 3000);

        try {
            Collection<String> jars = JavaConversions.asJavaCollection(
                DependencyUtils.resolveMavenDependencies(
                    builder.toString(),
                    packages,
                    null,
                    null,
                    null,
                    out -> {
                        System.err.println("---------->" + out);
                    }
                )
            );
            System.out.println();
            System.out.println("----------------------------------------------------------------");
            jars.forEach(System.out::println);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
