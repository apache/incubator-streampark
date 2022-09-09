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

package org.apache.streampark.flink.core

import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.enums.{ApiType, CheckpointStorage, RestartStrategy, StateBackend => XStateBackend}
import org.apache.streampark.common.util._
import org.apache.flink.api.common.RuntimeExecutionMode
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.configuration.{Configuration, CoreOptions}
import org.apache.flink.contrib.streaming.state.{DefaultConfigurableOptionsFactory, EmbeddedRocksDBStateBackend}
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend
import org.apache.flink.runtime.state.storage.{FileSystemCheckpointStorage, JobManagerCheckpointStorage}
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.TableConfig

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.{HashMap => JavaHashMap}
import scala.collection.JavaConversions._
import scala.collection.Map
import scala.util.{Failure, Success, Try}

private[flink] object FlinkStreamingInitializer {

  private[this] var flinkInitializer: FlinkStreamingInitializer = _

  def initStream(args: Array[String], config: (StreamExecutionEnvironment, ParameterTool) => Unit = null): (ParameterTool, StreamExecutionEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkStreamingInitializer(args, ApiType.scala)
          flinkInitializer.streamEnvConfFunc = config
          flinkInitializer.initStreamEnv()
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment)
  }

  def initJavaStream(args: StreamEnvConfig): (ParameterTool, StreamExecutionEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkStreamingInitializer(args.args, ApiType.java)
          flinkInitializer.javaStreamEnvConfFunc = args.conf
          flinkInitializer.initStreamEnv()
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment)
  }
}

private[flink] class FlinkStreamingInitializer(args: Array[String], apiType: ApiType) extends Logger {

  var streamEnvConfFunc: (StreamExecutionEnvironment, ParameterTool) => Unit = _

  var tableConfFunc: (TableConfig, ParameterTool) => Unit = _

  var javaStreamEnvConfFunc: StreamEnvConfigFunction = _

  var javaTableEnvConfFunc: TableEnvConfigFunction = _

  lazy val parameter: ParameterTool = initParameter()

  private[this] var localStreamEnv: StreamExecutionEnvironment = _

  private[this] lazy val defaultFlinkConf: Map[String, String] = {
    parameter.get(KEY_FLINK_CONF(), null) match {
      case null =>
        val flinkHome = System.getenv("FLINK_HOME")
        require(flinkHome != null, "FLINK_HOME not found.")
        logInfo(s"flinkHome: $flinkHome")
        val yaml = new File(s"$flinkHome/conf/flink-conf.yaml")
        PropertiesUtils.loadFlinkConfYaml(yaml)
      case flinkConf =>
        PropertiesUtils.loadFlinkConfYaml(DeflaterUtils.unzipString(flinkConf))
    }
  }

  def readFlinkConf(config: String): Map[String, String] = {
    val extension = config.split("\\.").last.toLowerCase

    val map = config match {
      case x if x.startsWith("yaml://") =>
        PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("prop://") =>
        PropertiesUtils.fromPropertiesText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("hdfs://") =>
        /**
         * If the config file is hdfs mode, needs copy the hdfs related configuration file to `resources` dir
         */
        val text = HdfsUtils.read(x)
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesText(text)
          case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
          case _ => throw new IllegalArgumentException("[StreamPark] Usage:flink.conf file error,must be properties or yml")
        }
      case _ =>
        val configFile = new File(config)
        require(configFile.exists(), s"[StreamPark] Usage:flink.conf file $configFile is not found!!!")
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
          case "yml" | "yaml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
          case _ => throw new IllegalArgumentException("[StreamPark] Usage:flink.conf file error,must be properties or yml")
        }
    }

    map
      .filter(!_._1.startsWith(KEY_FLINK_DEPLOYMENT_OPTION_PREFIX))
      .map(x => x._1.replace(KEY_FLINK_DEPLOYMENT_PROPERTY_PREFIX, "") -> x._2)
  }

  def initParameter(): ParameterTool = {
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(KEY_APP_CONF(), null) match {
      // scalastyle:off throwerror
      case null | "" => throw new ExceptionInInitializerError("[StreamPark] Usage:can't fond config,please set \"--conf $path \" in main arguments")
      // scalastyle:on throwerror
      case file => file
    }
    val configArgs = readFlinkConf(config)
    // config priority: explicitly specified priority > project profiles > system profiles
    ParameterTool.fromSystemProperties().mergeWith(ParameterTool.fromMap(configArgs))
  }

  def streamEnvironment: StreamExecutionEnvironment = {
    if (localStreamEnv == null) {
      this.synchronized {
        if (localStreamEnv == null) {
          initStreamEnv()
        }
      }
    }
    localStreamEnv
  }

  def initStreamEnv(): Unit = {
    localStreamEnv = StreamExecutionEnvironment.getExecutionEnvironment
    Try(parameter.get(KEY_FLINK_PARALLELISM()).toInt).getOrElse {
      Try(parameter.get(CoreOptions.DEFAULT_PARALLELISM.key()).toInt).getOrElse(CoreOptions.DEFAULT_PARALLELISM.defaultValue().toInt)
    } match {
      case p if p > 0 => localStreamEnv.setParallelism(p)
      case _ => throw new IllegalArgumentException("[StreamPark] parallelism must be > 0. ")
    }
    val interval = Try(parameter.get(KEY_FLINK_WATERMARK_INTERVAL).toInt).getOrElse(0)
    if (interval > 0) {
      localStreamEnv.getConfig.setAutoWatermarkInterval(interval)
    }

    val executionMode = Try(RuntimeExecutionMode.valueOf(parameter.get(KEY_EXECUTION_RUNTIME_MODE))).getOrElse(RuntimeExecutionMode.STREAMING)
    localStreamEnv.setRuntimeMode(executionMode)

    restartStrategy()

    checkpoint()

    apiType match {
      case ApiType.java if javaStreamEnvConfFunc != null => javaStreamEnvConfFunc.configuration(localStreamEnv.getJavaEnv, parameter)
      case ApiType.scala if streamEnvConfFunc != null => streamEnvConfFunc(localStreamEnv, parameter)
      case _ =>
    }
    localStreamEnv.getConfig.setGlobalJobParameters(parameter)
  }

  private[this] def restartStrategy(): Unit = {
    /**
     * Priority to the current project configuration to find the configuration, if can not find,
     * then take $FLINK_HOME/conf/flink-conf.yml as the configuration
     */
    val prefixLen = "flink.".length
    val strategy = Try(RestartStrategy.byName(parameter.get(KEY_FLINK_RESTART_STRATEGY)))
      .getOrElse(
        Try(RestartStrategy.byName(defaultFlinkConf("restart-strategy"))).getOrElse(null)
      )
    strategy match {
      case RestartStrategy.`failure-rate` =>
        /**
         * restart-strategy.failure-rate.max-failures-per-interval: maximum number of restarts before a Job is deemed to have failed
         * restart-strategy.failure-rate.failure-rate-interval: time interval for calculating the failure rate
         * restart-strategy.failure-rate.delay: time interval between two consecutive reboot attempts
         * e.g:
         * >>>
         * max-failures-per-interval: 10
         * failure-rate-interval: 5 min
         * delay: 2 s
         * <<<
         * That is:
         * the time interval of each abnormal restart is "2 seconds",
         * if the total number of failures reaches "10" within "5 minutes", the task will fail.
         */
        val interval = Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_PER_INTERVAL).toInt)
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_PER_INTERVAL.drop(prefixLen)).toInt).getOrElse(3)
          )

        val rateInterval = DateUtils.getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_RATE_INTERVAL))
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_RATE_INTERVAL.drop(prefixLen))).getOrElse(null)
          ), (5, TimeUnit.MINUTES))

        val delay = DateUtils.getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_DELAY))
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_DELAY.drop(prefixLen))).getOrElse(null)
          ))

        streamEnvironment.getConfig.setRestartStrategy(RestartStrategies.failureRateRestart(
          interval,
          Time.of(rateInterval._1, rateInterval._2),
          Time.of(delay._1, delay._2)
        ))

      case RestartStrategy.`fixed-delay` =>
        /**
         * restart-strategy.fixed-delay.attempts: the number of times Flink tries to execute a Job before it finally failure
         * restart-strategy.fixed-delay.delay: specific how long the restart interval
         * e.g:
         * attempts: 5,delay: 3 s
         * That is:
         * The maximum number of failed retries for a task is 5, and the time interval for each task restart is 3 seconds,
         * if the number of failed attempts reaches 5, the task will fail and exit
         */
        val attempts = Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS).toInt)
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS.drop(prefixLen)).toInt).getOrElse(3)
          )

        val delay = DateUtils.getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_DELAY))
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_DELAY.drop(prefixLen))).getOrElse(null)
          ))

        /**
         * Total `restartAttempts` after task execution failure, each restart interval `delayBetweenAttempts`
         */
        streamEnvironment.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(attempts, Time.of(delay._1, delay._2)))

      case RestartStrategy.`none` => streamEnvironment.getConfig.setRestartStrategy(RestartStrategies.noRestart())

      case null => logInfo("RestartStrategy not set,use default from $flink_conf")
    }
  }

  private[this] def checkpoint(): Unit = {
    // read from the configuration file whether to enable checkpoint, default is disabled.
    val enableCheckpoint = Try(parameter.get(KEY_FLINK_CHECKPOINTS_ENABLE).toBoolean).getOrElse(false)
    if(!enableCheckpoint) return

    val cpInterval = Try(parameter.get(KEY_FLINK_CHECKPOINTS_INTERVAL).toInt).getOrElse(1000)
    val cpMode = Try(CheckpointingMode.valueOf(parameter.get(KEY_FLINK_CHECKPOINTS_MODE))).getOrElse(CheckpointingMode.EXACTLY_ONCE)
    val cpCleanUp = Try(ExternalizedCheckpointCleanup.valueOf(parameter.get(KEY_FLINK_CHECKPOINTS_CLEANUP))).getOrElse(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    val cpTimeout = Try(parameter.get(KEY_FLINK_CHECKPOINTS_TIMEOUT).toLong).getOrElse(CheckpointConfig.DEFAULT_TIMEOUT)
    val cpMaxConcurrent = Try(parameter.get(KEY_FLINK_CHECKPOINTS_MAX_CONCURRENT).toInt).getOrElse(CheckpointConfig.DEFAULT_MAX_CONCURRENT_CHECKPOINTS)
    val cpMinPauseBetween = Try(parameter.get(KEY_FLINK_CHECKPOINTS_MIN_PAUSEBETWEEN).toLong).getOrElse(CheckpointConfig.DEFAULT_MIN_PAUSE_BETWEEN_CHECKPOINTS)
    val unaligned = Try(parameter.get(KEY_FLINK_CHECKPOINTS_UNALIGNED).toBoolean).getOrElse(false)

    // default: enable checkpoint, interval 1s to start a checkpoint
    streamEnvironment.enableCheckpointing(cpInterval)

    val cpConfig = streamEnvironment.getCheckpointConfig

    cpConfig.setCheckpointingMode(cpMode)
    // default: min pause interval between checkpoints
    cpConfig.setMinPauseBetweenCheckpoints(cpMinPauseBetween)
    // default: checkpoints must complete within $cpTimeout minutes or be discarded
    cpConfig.setCheckpointTimeout(cpTimeout)
    // default: allow ? times checkpoint at the same time, default one.
    cpConfig.setMaxConcurrentCheckpoints(cpMaxConcurrent)
    // default: checkpoint data is retained when cancelled
    cpConfig.enableExternalizedCheckpoints(cpCleanUp)
    // unaligned checkpoint (flink 1.11.1 +=)
    cpConfig.enableUnalignedCheckpoints(unaligned)

    val stateBackend = XStateBackend.withName(parameter.get(KEY_FLINK_STATE_BACKEND, null))
    if (stateBackend != null) {
      require(
        stateBackend == XStateBackend.hashmap || stateBackend == XStateBackend.rocksdb,
        "state.backend must be [hashmap|rocksdb] in flink 1.13 and above"
      )
      val storage = {
        val storage = parameter.get(KEY_FLINK_STATE_CHECKPOINT_STORAGE, null) match {
          // read from flink-conf.yaml
          case null =>
            logWarn("can't found flink.state.checkpoint-storage from properties,now try found from flink-conf.yaml")
            val storage = defaultFlinkConf("state.checkpoint-storage")
            require(storage != null, s"[StreamPark] can't found state.checkpoint-storage from default FlinkConf ")
            logInfo(s"state.checkpoint-storage: state.checkpoint-storage found in flink-conf.yaml,$storage")
            storage
          case storage =>
            logInfo(s"state.checkpoint-storage: flink.checkpoints.dir found in properties,$storage")
            storage
        }

        Try(CheckpointStorage.withName(storage)) match {
          case Success(value) => value
          case Failure(e) => throw new IllegalArgumentException(e)
        }
      }

      lazy val cpDir = parameter.get(KEY_FLINK_STATE_CHECKPOINTS_DIR, null) match {
        // read from flink-conf.yaml
        case null =>
          logWarn("can't found flink.state.checkpoints.dir from properties,now try found from flink-conf.yaml")
          // read `state.checkpoints.dir` key from flink-conf.yaml
          val dir = defaultFlinkConf("state.checkpoints.dir")
          require(dir != null, s"[StreamPark] can't found state.checkpoints.dir from Default FlinkConf ")
          logInfo(s"state.backend: state.checkpoints.dir found in flink-conf.yaml,$dir")
          dir
        case dir =>
          logInfo(s"state.backend: flink.checkpoints.dir found in properties,$dir")
          dir
      }

      stateBackend match {
        case XStateBackend.hashmap =>
          logInfo("state.backend: hashmap...")
          streamEnvironment.setStateBackend(new HashMapStateBackend())
          storage match {
            case CheckpointStorage.jobmanager =>
              logInfo("state.checkpoint-storage: jobmanager...")
              val maxMemorySize = Try(parameter.get(KEY_FLINK_STATE_BACKEND_MEMORY).toInt).getOrElse(JobManagerCheckpointStorage.DEFAULT_MAX_STATE_SIZE)
              val jobManagerCheckpointStorage = new JobManagerCheckpointStorage(maxMemorySize)
              cpConfig.setCheckpointStorage(jobManagerCheckpointStorage)
            case CheckpointStorage.filesystem =>
              logInfo("state.checkpoint-storage: filesystem...")
              cpConfig.setCheckpointStorage(new FileSystemCheckpointStorage(cpDir))
          }
        case XStateBackend.rocksdb =>
          logInfo("state.backend: rocksdb...")
          val rock = new EmbeddedRocksDBStateBackend()
          val map = new JavaHashMap[String, Object]()
          val skipKey = List(KEY_FLINK_STATE_BACKEND_ASYNC, KEY_FLINK_STATE_BACKEND_INCREMENTAL, KEY_FLINK_STATE_BACKEND_MEMORY, KEY_FLINK_STATE_ROCKSDB)
          parameter.getProperties.filter(_._1.startsWith(KEY_FLINK_STATE_ROCKSDB)).filterNot(x => skipKey.contains(x._1)).foreach(x => map.put(x._1, x._2))
          if (map.nonEmpty) {
            val optionsFactory = new DefaultConfigurableOptionsFactory
            val config = new Configuration()
            val confData = classOf[Configuration].getDeclaredField("confData")
            confData.setAccessible(true)
            confData.set(map, config)
            optionsFactory.configure(config)
            rock.setRocksDBOptions(optionsFactory)
          }
          streamEnvironment.setStateBackend(rock)
          storage match {
            case CheckpointStorage.filesystem =>
              logInfo("state.checkpoint-storage: filesystem...")
              cpConfig.setCheckpointStorage(new FileSystemCheckpointStorage(cpDir))
            case _ =>
              throw new IllegalArgumentException("[StreamPark] state.backend is  rocksdb, state.checkpoint-storage must be filesystem...")
          }
      }
    }
  }

}
