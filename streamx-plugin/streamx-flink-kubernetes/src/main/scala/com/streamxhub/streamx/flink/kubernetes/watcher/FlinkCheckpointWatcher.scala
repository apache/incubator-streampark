/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes.watcher

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.event.FlinkJobCheckpointChangeEvent
import com.streamxhub.streamx.flink.kubernetes.model.{CheckpointCV, ClusterKey, TrackId}
import com.streamxhub.streamx.flink.kubernetes.{ChangeEventBus, FlinkTrackCachePool, KubernetesRetriever, MetricWatcherConfig}
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.util.Timeout
import org.json4s.jackson.JsonMethods.parse
import org.json4s.DefaultFormats

import java.nio.charset.StandardCharsets
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.concurrent.ThreadSafe
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
 * auther: benjobs
 */

@ThreadSafe
class FlinkCheckpointWatcher(conf: MetricWatcherConfig = MetricWatcherConfig.defaultConf)
                            (implicit val cachePool: FlinkTrackCachePool,
                             implicit val eventBus: ChangeEventBus) extends Logger with FlinkWatcher {

  private val trackTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trackTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trackTaskExecPool)

  private val timerExec = Executors.newSingleThreadScheduledExecutor()
  private var timerSchedule: ScheduledFuture[_] = _

  // status of whether FlinkJobWatcher has already started
  @volatile private var isStarted = false

  /**
   * start watcher process
   */
  // noinspection DuplicatedCode
  override def start(): Unit = this.synchronized {
    if (!isStarted) {
      timerSchedule = timerExec.scheduleAtFixedRate(() => watch(), 0, conf.requestIntervalSec, TimeUnit.SECONDS)
      isStarted = true
      logInfo("[flink-k8s] FlinkCheckpointWatcher started.")
    }
  }

  /**
   * stop watcher process
   */
  override def stop(): Unit = this.synchronized {
    if (isStarted) {
      timerSchedule.cancel(true)
      isStarted = false
      logInfo("[flink-k8s] FlinkCheckpointWatcher stopped.")
    }
  }

  /**
   * closes resource, relinquishing any underlying resources.
   */
  // noinspection DuplicatedCode
  override def close(): Unit = this.synchronized {
    if (isStarted) {
      timerSchedule.cancel(true)
      isStarted = false
    }
    Try(timerExec.shutdownNow())
    Try(trackTaskExecutor.shutdownNow())
    logInfo("[flink-k8s] FlinkCheckpointWatcher closed.")
  }

  /**
   * single flink metrics tracking task
   */
  override def watch(): Unit = {
    // get all legal tracking cluster key
    val trackIds: Set[TrackId] = Try(cachePool.collectTracks()).filter(_.nonEmpty).getOrElse(return)
    // retrieve flink metrics in thread pool
    val futures: Set[Future[Option[CheckpointCV]]] =
      trackIds.map(id => {
        val future = Future(collect(id))
        future.filter(_.nonEmpty).foreach {
          result => eventBus.postAsync(FlinkJobCheckpointChangeEvent(id, result.get))
        }
        future
      })
    // blocking until all future are completed or timeout is reached
    Try {
      val futureHold = Future.sequence(futures)
      Await.ready(futureHold, conf.requestTimeoutSec seconds)
    }.failed.map { _ =>
      logError(s"[FlinkCheckpointWatcher] tracking flink-job checkpoint on kubernetes mode timeout," +
        s" limitSeconds=${conf.requestTimeoutSec}," +
        s" trackingClusterKeys=${trackIds.mkString(",")}")
    }
  }

  /**
   * Collect flink-job checkpoint from kubernetes-native cluster.
   * Returns None when the flink-cluster-client request fails (or
   * in case of the relevant flink rest api require failure).
   *
   */
  def collect(id: TrackId): Option[CheckpointCV] = {
    val clusterKey: ClusterKey = ClusterKey.of(id)
    val flinkJmRestUrl = cachePool.getClusterRestUrl(clusterKey).filter(_.nonEmpty).getOrElse(return None)
    // call flink rest overview api
    val checkpoint: Checkpoint = Try(
      Checkpoint.as(
        Request.get(s"$flinkJmRestUrl/jobs/${id.jobId}/checkpoints")
          .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
          .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
          .execute.returnContent.asString(StandardCharsets.UTF_8)
      )
    ).getOrElse(return None)
    val ackTime = System.currentTimeMillis
    val checkpointCV = CheckpointCV(
      id = checkpoint.id,
      checkpointPath = checkpoint.externalPath,
      isSavepoint = checkpoint.isSavepoint,
      checkpointType = checkpoint.checkpointType,
      pollAckTime = ackTime)
    Some(checkpointCV)
  }

}

private[kubernetes] case class Checkpoint(id: Long,
                                          status: String,
                                          externalPath: String,
                                          isSavepoint: Boolean,
                                          checkpointType: String)


object Checkpoint {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  def as(json: String): Checkpoint = {
    Try(parse(json)) match {
      case Success(ok) =>
        val completed = ok \ "latest" \ "completed"
        Checkpoint(
          id = (completed \ "id").extractOpt[Long].getOrElse(0L),
          status = (completed \ "status").extractOpt[String].getOrElse(null),
          externalPath = (completed \ "external_path").extractOpt[String].getOrElse(null),
          isSavepoint = (completed \ "is_savepoint").extractOpt[Boolean].getOrElse(false),
          checkpointType = (completed \ "checkpoint_type").extractOpt[String].getOrElse(null)
        )
      case Failure(_) => null
    }
  }

}


