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

import com.streamxhub.streamx.flink.kubernetes.watcher.{Checkpoint, FlinkRestJmConfigItem, FlinkRestOverview, JobDetails}
import org.junit.jupiter.api.Test

// scalastyle:off println
class FlinkRestJsonTest {

  @Test def flinkRestOverview(): Unit = {
    val json =
      """
        |{
        |    "taskmanagers":2,
        |    "slots-total":10,
        |    "slots-available":2,
        |    "jobs-running":2,
        |    "jobs-finished":5,
        |    "jobs-cancelled":1,
        |    "jobs-failed":1,
        |    "flink-version":"1.12.0",
        |    "flink-commit":"fc00492"
        |}
        |""".stripMargin

    val overview = FlinkRestOverview.as(json).get
    println(overview.slotsAvailable)
  }


  @Test def flinkRestJmConfigItem(): Unit = {
    val json =
      """
        |[
        |    {
        |        "key": "taskmanager.memory.process.size",
        |        "value": "1024m"
        |    },
        |    {
        |        "key": "classloader.resolve-order",
        |        "value": "parent-first"
        |    },
        |    {
        |        "key": "jobmanager.execution.failover-strategy",
        |        "value": "region"
        |    },
        |    {
        |        "key": "jobmanager.rpc.address",
        |        "value": "localhost"
        |    },
        |    {
        |        "key": "jobmanager.memory.off-heap.size",
        |        "value": "134217728b"
        |    },
        |    {
        |        "key": "jobmanager.memory.jvm-overhead.min",
        |        "value": "201326592b"
        |    },
        |    {
        |        "key": "jobmanager.memory.process.size",
        |        "value": "1024m"
        |    },
        |    {
        |        "key": "web.tmpdir",
        |        "value": "/var/folders/123456"
        |    },
        |    {
        |        "key": "jobmanager.rpc.port",
        |        "value": "6123"
        |    },
        |    {
        |        "key": "parallelism.default",
        |        "value": "1"
        |    },
        |    {
        |        "key": "taskmanager.numberOfTaskSlots",
        |        "value": "1"
        |    },
        |    {
        |        "key": "web.submit.enable",
        |        "value": "true"
        |    },
        |    {
        |        "key": "jobmanager.memory.jvm-metaspace.size",
        |        "value": "268435456b"
        |    },
        |    {
        |        "key": "jobmanager.memory.heap.size",
        |        "value": "469762048b"
        |    },
        |    {
        |        "key": "jobmanager.memory.jvm-overhead.max",
        |        "value": "201326592b"
        |    }
        |]
        |
        |""".stripMargin

    FlinkRestJmConfigItem.as(json).foreach(x => {
      println(s"${x.key}: ${x.value}")
    })

  }

  @Test def testJobDetails(): Unit = {
    val json =
      """
        |{
        |    "jobs": [
        |        {
        |            "jid": "4579b7a235f0756483da3c3618081bc2",
        |            "name": "FLink SQL",
        |            "state": "RUNNING",
        |            "start-time": 1647616038354,
        |            "end-time": -1,
        |            "duration": 43912,
        |            "last-modification": 1647616039219,
        |            "tasks": {
        |                "total": 1,
        |                "created": 0,
        |                "scheduled": 0,
        |                "deploying": 0,
        |                "running": 1,
        |                "finished": 0,
        |                "canceling": 0,
        |                "canceled": 0,
        |                "failed": 0,
        |                "reconciling": 0
        |            }
        |        }
        |    ]
        |}
        |""".stripMargin

    val jobDetails = JobDetails.as(json)
    println(jobDetails)
  }

  @Test def testCheckpoint(): Unit = {
    val json =
      """
        |{
        |    "counts":{
        |        "restored":0,
        |        "total":1914,
        |        "in_progress":0,
        |        "completed":1914,
        |        "failed":0
        |    },
        |    "summary":{
        |        "state_size":{
        |            "min":30751,
        |            "max":31096,
        |            "avg":31038
        |        },
        |        "end_to_end_duration":{
        |            "min":63,
        |            "max":6273,
        |            "avg":135
        |        },
        |        "alignment_buffered":{
        |            "min":0,
        |            "max":0,
        |            "avg":0
        |        },
        |        "processed_data":{
        |            "min":0,
        |            "max":0,
        |            "avg":0
        |        },
        |        "persisted_data":{
        |            "min":0,
        |            "max":0,
        |            "avg":0
        |        }
        |    },
        |    "latest":{
        |        "completed":{
        |            "@class":"completed",
        |            "id":1914,
        |            "status":"COMPLETED",
        |            "is_savepoint":false,
        |            "trigger_timestamp":1658138497283,
        |            "latest_ack_timestamp":1658138497372,
        |            "state_size":31096,
        |            "end_to_end_duration":89,
        |            "alignment_buffered":0,
        |            "processed_data":0,
        |            "persisted_data":0,
        |            "num_subtasks":1,
        |            "num_acknowledged_subtasks":1,
        |            "checkpoint_type":"CHECKPOINT",
        |            "tasks":{
        |            },
        |            "external_path":"oss:///streamx/prod/checkpoints/60dd003f048a5b2f92f0874e6612146c/chk-1914",
        |            "discarded":false
        |        },
        |        "savepoint":null,
        |        "failed":null,
        |        "restored":null
        |    },
        |    "history":[
        |        {
        |            "@class":"completed",
        |            "id":1914,
        |            "status":"COMPLETED",
        |            "is_savepoint":false,
        |            "trigger_timestamp":1658138497283,
        |            "latest_ack_timestamp":1658138497372,
        |            "state_size":31096,
        |            "end_to_end_duration":89,
        |            "alignment_buffered":0,
        |            "processed_data":0,
        |            "persisted_data":0,
        |            "num_subtasks":1,
        |            "num_acknowledged_subtasks":1,
        |            "checkpoint_type":"CHECKPOINT",
        |            "tasks":{
        |
        |            },
        |            "external_path":"oss:///streamx/prod/checkpoints/60dd003f048a5b2f92f0874e6612146c/chk-1914",
        |            "discarded":false
        |        }
        |    ]
        |}
        |
        |""".stripMargin
    val checkpoint = Checkpoint.as(json)
    println(checkpoint)
  }

  @Test def testIngress(): Unit = {
    val json =
      """
        |[
        |    {
        |        "addresses":[
        |            "192.168.0.1",
        |            "192.168.0.2",
        |            "192.168.0.3"
        |        ],
        |        "port":80,
        |        "protocol":"HTTP",
        |        "serviceName":"native-flink:statebackend12788-rest",
        |        "ingressName":"native-flink:statebackend12788",
        |        "hostname":"streamx.com",
        |        "path":"/native-flink/statebackend12788/",
        |        "allNodes":false
        |    },
        |    {
        |        "addresses":[
        |        ],
        |        "port":80,
        |        "protocol":"HTTP",
        |        "serviceName":"native-flink:statebackend12788-rest",
        |        "ingressName":"native-flink:statebackend12788",
        |        "hostname":"streamx.com",
        |        "path":"/native-flink/statebackend12788(/|$)(.*)",
        |        "allNodes":false
        |    }
        |]
        |""".stripMargin

    val ingressMeta = IngressMeta.as(json)
    println(ingressMeta.get)
  }

}
