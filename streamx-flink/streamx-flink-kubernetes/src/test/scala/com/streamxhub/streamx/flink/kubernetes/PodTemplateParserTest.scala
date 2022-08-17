/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.kubernetes

import com.google.common.collect.ImmutableMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import scala.collection.JavaConverters._

/**
 * @author Al-assad
 */
// scalastyle:off println
class PodTemplateParserTest {


  @Test
  def testCompleteInitPodTemplate(): Unit = {
    val podTemplateExpect = Map(
      "" ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |""".stripMargin,
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: jm-pod-template
        |""".stripMargin
        ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: jm-pod-template
          |""".stripMargin,
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |""".stripMargin
        ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |""".stripMargin,
      """apiVersion: v1
        |spec:
        |""".stripMargin
        ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |""".stripMargin,
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |  containers:
        |    - name: flink-main-container
        |      volumeMounts:
        |        - name: checkpoint-pvc
        |          mountPath: /opt/flink/checkpoints
        |        - name: savepoint-pvc
        |          mountPath: /opt/flink/savepoints
        |  volumes:
        |    - name: checkpoint-pvc
        |      persistentVolumeClaim:
        |        claimName: flink-checkpoint
        |    - name: savepoint-pvc
        |      persistentVolumeClaim:
        |        claimName: flink-savepoint
        |""".stripMargin
        ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |spec:
          |  containers:
          |  - name: flink-main-container
          |    volumeMounts:
          |    - name: checkpoint-pvc
          |      mountPath: /opt/flink/checkpoints
          |    - name: savepoint-pvc
          |      mountPath: /opt/flink/savepoints
          |  volumes:
          |  - name: checkpoint-pvc
          |    persistentVolumeClaim:
          |      claimName: flink-checkpoint
          |  - name: savepoint-pvc
          |    persistentVolumeClaim:
          |      claimName: flink-savepoint
          |""".stripMargin)
    for (expect <- podTemplateExpect) {
      val res = PodTemplateParser.completeInitPodTemplate(expect._1)
      assertEquals(expect._2, res)
    }
  }


  @Test
  def testHostAliasSpecToPodTemplate(): Unit = {
    val hostMap = ImmutableMap.of(
      "hdp01.assad.site", "192.168.3.114",
      "hdp02.assad.site", "192.168.3.115",
      "hdp03.assad.site", "192.168.3.116",
      "hdp01", "192.168.3.114",
      "hdp02", "192.168.3.115"
    )
    val expected = Map(
      "" ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |spec:
          |  hostAliases:
          |  - ip: 192.168.3.114
          |    hostnames:
          |    - hdp01.assad.site
          |    - hdp01
          |  - ip: 192.168.3.116
          |    hostnames:
          |    - hdp03.assad.site
          |  - ip: 192.168.3.115
          |    hostnames:
          |    - hdp02.assad.site
          |    - hdp02
          |""".stripMargin,

      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |""".stripMargin ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |spec:
          |  hostAliases:
          |  - ip: 192.168.3.114
          |    hostnames:
          |    - hdp01.assad.site
          |    - hdp01
          |  - ip: 192.168.3.116
          |    hostnames:
          |    - hdp03.assad.site
          |  - ip: 192.168.3.115
          |    hostnames:
          |    - hdp02.assad.site
          |    - hdp02
          |""".stripMargin,
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |  hostAliases:
        |  - ip: 192.168.3.114
        |    hostnames:
        |    - hdp01.assad.site
        |    - hdp01
        |""".stripMargin ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |spec:
          |  hostAliases:
          |  - ip: 192.168.3.114
          |    hostnames:
          |    - hdp01.assad.site
          |    - hdp01
          |  - ip: 192.168.3.116
          |    hostnames:
          |    - hdp03.assad.site
          |  - ip: 192.168.3.115
          |    hostnames:
          |    - hdp02.assad.site
          |    - hdp02
          |""".stripMargin,
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |  containers:
        |    - name: flink-main-container
        |      volumeMounts:
        |        - name: checkpoint-pvc
        |          mountPath: /opt/flink/checkpoints
        |        - name: savepoint-pvc
        |          mountPath: /opt/flink/savepoints
        |  volumes:
        |    - name: checkpoint-pvc
        |      persistentVolumeClaim:
        |        claimName: flink-checkpoint
        |    - name: savepoint-pvc
        |      persistentVolumeClaim:
        |        claimName: flink-savepoint
        |""".stripMargin ->
        """apiVersion: v1
          |kind: Pod
          |metadata:
          |  name: pod-template
          |spec:
          |  containers:
          |  - name: flink-main-container
          |    volumeMounts:
          |    - name: checkpoint-pvc
          |      mountPath: /opt/flink/checkpoints
          |    - name: savepoint-pvc
          |      mountPath: /opt/flink/savepoints
          |  volumes:
          |  - name: checkpoint-pvc
          |    persistentVolumeClaim:
          |      claimName: flink-checkpoint
          |  - name: savepoint-pvc
          |    persistentVolumeClaim:
          |      claimName: flink-savepoint
          |  hostAliases:
          |  - ip: 192.168.3.114
          |    hostnames:
          |    - hdp01.assad.site
          |    - hdp01
          |  - ip: 192.168.3.116
          |    hostnames:
          |    - hdp03.assad.site
          |  - ip: 192.168.3.115
          |    hostnames:
          |    - hdp02.assad.site
          |    - hdp02
          |""".stripMargin
    )
    for (expect <- expected) {
      val result = PodTemplateParser.completeHostAliasSpec(hostMap, expect._1)
      assertEquals(result.trim, expect._2.trim)
    }
  }


  @Test
  def testExtractHostAliasMapFromPodTemplate(): Unit = {
    val expected = Map(
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |  containers:
        |  - name: flink-main-container
        |    volumeMounts:
        |    - name: checkpoint-pvc
        |      mountPath: /opt/flink/checkpoints
        |    - name: savepoint-pvc
        |      mountPath: /opt/flink/savepoints
        |  volumes:
        |  - name: checkpoint-pvc
        |    persistentVolumeClaim:
        |      claimName: flink-checkpoint
        |  - name: savepoint-pvc
        |    persistentVolumeClaim:
        |      claimName: flink-savepoint
        |  hostAliases:
        |  - ip: 192.168.3.114
        |    hostnames:
        |    - hdp01.assad.site
        |    - hdp01
        |  - ip: 192.168.3.116
        |    hostnames:
        |    - hdp03.assad.site
        |  - ip: 192.168.3.115
        |    hostnames:
        |    - hdp02.assad.site
        |    - hdp02
        |""".stripMargin
        ->
        Map("hdp01.assad.site" -> "192.168.3.114",
          "hdp01" -> "192.168.3.114",
          "hdp03.assad.site" -> "192.168.3.116",
          "hdp02.assad.site" -> "192.168.3.115",
          "hdp02" -> "192.168.3.115"
        ),
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |  containers:
        |  - name: flink-main-container
        |    volumeMounts:
        |    - name: checkpoint-pvc
        |      mountPath: /opt/flink/checkpoints
        |    - name: savepoint-pvc
        |      mountPath: /opt/flink/savepoints
        |  volumes:
        |  - name: checkpoint-pvc
        |    persistentVolumeClaim:
        |      claimName: flink-checkpoint
        |  - name: savepoint-pvc
        |    persistentVolumeClaim:
        |      claimName: flink-savepoint
        |  hostAliases:
        |""".stripMargin
        ->
        Map(),
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec:
        |  hostAliases:
        |  - ip:
        |    hostnames:
        |    - hdp01.assad.site
        |    - hdp01
        |  - ip: 192.168.3.116
        |    hostname:
        |    - hdp03.assad.site
        |  - ip: 192.168.3.115
        |    hostnames:
        |    - hdp02.assad.site
        |  - ip: 192.168.3.115
        |    hostname: hdp02.assad.site
        |""".stripMargin
        -> Map(),
      """apiVersion: v1
        |kind: Pod
        |metadata:
        |  name: pod-template
        |spec: 2333
        |""".stripMargin
        -> Map()
    )

    for (expect <- expected) {
      val hostsMap = PodTemplateParser.extractHostAliasMap(expect._1).asScala
      assertEquals(hostsMap, expect._2)
    }
  }

  @Test
  def testPreviewHostAliasSpec(): Unit = {
    val hosts = Map("hdp01.assad.site" -> "192.168.3.114",
      "hdp01" -> "192.168.3.114",
      "hdp03.assad.site" -> "192.168.3.116",
      "hdp02.assad.site" -> "192.168.3.115",
      "hdp02" -> "192.168.3.115"
    )
    val hostAlias = PodTemplateParser.previewHostAliasSpec(hosts.asJava)
    println(hostAlias)
  }


}
