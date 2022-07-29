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
import com.streamxhub.streamx.flink.kubernetes.{ChangeEventBus, FlinkTrackController, KubernetesRetriever, MetricWatcherConfig}
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.util.Timeout
import org.json4s.JsonAST.JNothing
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, JNull}

import java.nio.charset.StandardCharsets
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.concurrent.ThreadSafe
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
 * author: benjobs
 */
@ThreadSafe
class FlinkCheckpointWatcher(conf: MetricWatcherConfig = MetricWatcherConfig.defaultConf)
                            (implicit val trackController: FlinkTrackController,
                             implicit val eventBus: ChangeEventBus) extends Logger with FlinkWatcher {

  private val trackTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trackTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trackTaskExecPool)

  private val timerExec = Executors.newSingleThreadScheduledExecutor()
  private var timerSchedule: ScheduledFuture[_] = _

  /**
   * start watcher process
   */
  override def doStart(): Unit = {
    timerSchedule = timerExec.scheduleAtFixedRate(() => doWatch(), 0, conf.requestIntervalSec, TimeUnit.SECONDS)
    logInfo("[flink-k8s] FlinkCheckpointWatcher started.")
  }

  /**
   * stop watcher process
   */
  override def doStop(): Unit = {
    timerSchedule.cancel(true)
    logInfo("[flink-k8s] FlinkCheckpointWatcher stopped.")
  }

  /**
   * closes resource, relinquishing any underlying resources.
   */
  override def doClose(): Unit = {
    timerExec.shutdownNow()
    trackTaskExecutor.shutdownNow()
    logInfo("[flink-k8s] FlinkCheckpointWatcher closed.")
  }

  /**
   * single flink metrics tracking task
   */
  override def doWatch(): Unit = {
    // get all legal tracking cluster key
    val trackIds: Set[TrackId] = Try(trackController.collectTracks()).filter(_.nonEmpty).getOrElse(return)
    // retrieve flink metrics in thread pool
    val futures: Set[Future[Option[CheckpointCV]]] =
      trackIds.map(id => {
        val future = Future(collect(id))
        future onComplete (_.getOrElse(None) match {
          case Some(cp) => eventBus.postAsync(FlinkJobCheckpointChangeEvent(id, cp))
          case _ =>
        })
        future
      })

    // blocking until all future are completed or timeout is reached
    Try(Await.ready(Future.sequence(futures), conf.requestTimeoutSec seconds))
      .failed.map { _ =>
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
  def collect(trackId: TrackId): Option[CheckpointCV] = {
    if (trackId.jobId != null) {
      val flinkJmRestUrl = trackController.getClusterRestUrl(ClusterKey.of(trackId)).filter(_.nonEmpty).getOrElse(return None)
      // call flink rest overview api
      val checkpoint: Checkpoint = Try(
        Checkpoint.as(
          Request.get(s"$flinkJmRestUrl/jobs/${trackId.jobId}/checkpoints")
            .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
            .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
            .execute.returnContent.asString(StandardCharsets.UTF_8)
        ) match {
          case Some(c) => c
          case _ => return None
        }
      ).getOrElse(return None)

      val checkpointCV = CheckpointCV(
        id = checkpoint.id,
        externalPath = checkpoint.externalPath,
        isSavepoint = checkpoint.isSavepoint,
        checkpointType = checkpoint.checkpointType,
        status = checkpoint.status,
        triggerTimestamp = checkpoint.triggerTimestamp
      )
      Some(checkpointCV)
    } else None
  }

}

private[kubernetes] case class Checkpoint(id: Long,
                                          status: String,
                                          externalPath: String,
                                          isSavepoint: Boolean,
                                          checkpointType: String,
                                          triggerTimestamp: Long)


object Checkpoint {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  def as(json: String): Option[Checkpoint] = {
    Try(parse(json)) match {
      case Success(ok) =>
        val completed = ok \ "latest" \ "completed"
        completed match {
          case JNull | JNothing => None
          case _ =>
            val cp = Checkpoint(
              id = (completed \ "id").extractOpt[Long].getOrElse(0L),
              status = (completed \ "status").extractOpt[String].getOrElse(null),
              externalPath = (completed \ "external_path").extractOpt[String].getOrElse(null),
              isSavepoint = (completed \ "is_savepoint").extractOpt[Boolean].getOrElse(false),
              checkpointType = (completed \ "checkpoint_type").extractOpt[String].getOrElse(null),
              triggerTimestamp = (completed \ "trigger_timestamp").extractOpt[Long].getOrElse(0L)
            )
            Some(cp)
        }
      case Failure(_) => None
    }
  }

}
