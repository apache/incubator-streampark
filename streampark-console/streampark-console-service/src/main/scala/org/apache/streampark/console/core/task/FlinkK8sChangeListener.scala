package org.apache.streampark.console.core.task

import org.apache.streampark.common.enums.ExecutionMode
import org.apache.streampark.common.util.ThreadUtils
import org.apache.streampark.common.zio.ZIOContainerSubscription.{ConcurrentMapExtension, RefMapExtension}
import org.apache.streampark.common.zio.ZIOExt.IOOps
import org.apache.streampark.console.core.entity.{Application, FlinkCluster}
import org.apache.streampark.console.core.enums.{FlinkAppState, OptionState}
import org.apache.streampark.console.core.service.{ApplicationService, FlinkClusterService}
import org.apache.streampark.console.core.service.alert.AlertService
import org.apache.streampark.console.core.utils.FlinkAppStateConverter
import org.apache.streampark.flink.kubernetes.v2.model._
import org.apache.streampark.flink.kubernetes.v2.model.EvalJobState.EvalJobState
import org.apache.streampark.flink.kubernetes.v2.observer.{FlinkK8sObserver, Name, Namespace}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import zio.{UIO, ZIO}

import java.util.Date
import java.util.concurrent.{ExecutorService, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

@Component
class FlinkK8sChangeListener {
  @Lazy @Autowired
  private var applicationService: ApplicationService = _

  @Lazy @Autowired
  private var flinkClusterService: FlinkClusterService = _
  @Lazy @Autowired
  private var alertService: AlertService = _

  private val executor: ExecutorService = new ThreadPoolExecutor(
    Runtime.getRuntime.availableProcessors * 5,
    Runtime.getRuntime.availableProcessors * 10,
    20L,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable](1024),
    ThreadUtils.threadFactory("streampark-notify-executor"),
    new ThreadPoolExecutor.AbortPolicy)

  subscribeJobStatusChange.forkDaemon.runIO
  subscribeMetricsChange.forkDaemon.runIO
  subscribeRestSvcEndpointChange.forkDaemon.runIO

  def subscribeJobStatusChange: UIO[Unit] = {
    FlinkK8sObserver.evaluatedJobSnaps
      .flatSubscribeValues()
      .foreach {
        jobSnap =>
          ZIO
            .attempt {
              // update application recrod
              val appId = jobSnap.appId
              // get pre application record
              val app: Application = applicationService.getById(appId)
              if (app == null || jobSnap.evalState != null) return ZIO.unit
              applicationService.persistMetrics(app)
              // update application record
              setByJobStatusCV(app, jobSnap)

              // send alert
              val state: FlinkAppState = FlinkAppState.of(app.getState)
              if (
                FlinkAppState.FAILED == state || FlinkAppState.LOST == state || FlinkAppState.RESTARTING == state || FlinkAppState.FINISHED == state
              ) {
                executor.execute(() => alertService.alert(app, state))
              }

            }
            .retryN(3)
            .ignore
      }
  }

  def subscribeMetricsChange: UIO[Unit] = {
    FlinkK8sObserver.clusterMetricsSnaps
      .flatSubscribe()
      .foreach {
        metricsSnap =>
          ZIO
            .attempt {
              val key: (Namespace, Name) = metricsSnap._1
              // 未完成，通过Namespace和Name查询
              val app: Application = applicationService.getById(key)

              // discard session mode change
              if (app == null || ExecutionMode.isKubernetesSessionMode(app.getExecutionMode))
                return ZIO.unit

              val clusterMetrics: ClusterMetrics = metricsSnap._2
              app.setJmMemory(clusterMetrics.totalJmMemory)
              app.setTmMemory(clusterMetrics.totalTmMemory)
              app.setTotalTM(clusterMetrics.totalTm)
              app.setTotalSlot(clusterMetrics.totalSlot)
              app.setAvailableSlot(clusterMetrics.availableSlot)
              applicationService.persistMetrics(app)
            }
            .retryN(3)
            .ignore
      }
  }

  def subscribeRestSvcEndpointChange: UIO[Unit] = {
    FlinkK8sObserver.restSvcEndpointSnaps
      .flatSubscribe()
      .foreach {
        restSvcEndpointSnap =>
          ZIO
            .attempt {

              val key: (Namespace, Name) = restSvcEndpointSnap._1
              val restSvcEndpoint: RestSvcEndpoint = restSvcEndpointSnap._2

              // 未完成，通过Namespace和Name查询
              val app: Application = applicationService.getById(key)

              // 未完成，通过Namespace和Name查询
              val flinkCluster: FlinkCluster = flinkClusterService.getById(key)

              if (restSvcEndpoint == null || restSvcEndpoint.chooseRest == null) return ZIO.unit
              val url = restSvcEndpoint.chooseRest
              app.setFlinkRestUrl(url)
              applicationService.persistMetrics(app)

              flinkCluster.setAddress(url)
              flinkClusterService.update(flinkCluster)

            }
            .retryN(3)
            .ignore
      }
  }

  private def setByJobStatusCV(app: Application, jobSnapshot: JobSnapshot): Unit = { // infer the final flink job state
    val evalJobState: EvalJobState = inferEvalJobStateFromPersist(
      jobSnapshot.evalState,
      FlinkAppStateConverter.flinkAppStateToK8sEvalJobState(FlinkAppState.of(app.getState)))

    val state: FlinkAppState = FlinkAppStateConverter.k8sEvalJobStateToFlinkAppState(evalJobState)
    val jobStatusOption: Option[JobStatus] = jobSnapshot.jobStatus

    if (jobStatusOption.nonEmpty) {
      val jobStatus: JobStatus = jobStatusOption.get
      // corrective start-time / end-time / duration
      val preStartTime: Long =
        if (app.getStartTime != null) app.getStartTime.getTime
        else 0

      val startTime: Long = Math.max(jobStatus.startTs, preStartTime)
      val preEndTime: Long =
        if (app.getEndTime != null) app.getEndTime.getTime
        else 0
      var endTime: Long = Math.max(jobStatus.endTs.getOrElse(-1L), preEndTime)
      var duration: Long = if (app.getDuration != null) app.getDuration else 0
      if (FlinkAppState.isEndState(state.getValue)) {
        if (endTime < startTime) endTime = System.currentTimeMillis
        if (duration <= 0) duration = endTime - startTime
      }
      app.setJobId(jobStatus.jobId)
      val totalTask = if (jobStatus.tasks.nonEmpty) jobStatus.tasks.get.total else 0
      app.setTotalTask(totalTask)
      app.setStartTime(
        new Date(
          if (startTime > 0) startTime
          else 0))
      app.setEndTime(
        if (endTime > 0 && endTime >= startTime) new Date(endTime)
        else null)
      app.setDuration(
        if (duration > 0) duration
        else 0)
    }

    app.setState(state.getValue)
    // when a flink job status change event can be received, it means
    // that the operation command sent by streampark has been completed.
    app.setOptionState(OptionState.NONE.getValue)
  }

  def inferEvalJobStateFromPersist(current: EvalJobState, previous: EvalJobState): EvalJobState = {
    current match {
      case EvalJobState.LOST =>
        if (EvalJobState.effectEndStates.contains(current)) previous else EvalJobState.TERMINATED
      case EvalJobState.TERMINATED =>
        previous match {
          case EvalJobState.CANCELLING => EvalJobState.CANCELED
          case EvalJobState.FAILING => EvalJobState.FAILED
          case _ =>
            EvalJobState.TERMINATED
        }
      case _ => current
    }
  }
}
