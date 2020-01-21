/**
 * Copyright (c) 2019 The StreamX Project
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

package com.streamxhub.flink.core.sink

import java.io.PrintWriter
import java.nio.file.{Files, Paths}
import java.sql.{Connection, PreparedStatement, Statement}
import java.util
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong
import java.util.{Base64, Collections, Properties}

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.{ConfigUtils, Logger, MySQLUtils, ThreadUtils}
import com.streamxhub.flink.core.StreamingContext
import io.netty.handler.codec.http.HttpHeaders
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.asynchttpclient._
import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseProperties

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object ClickHouseSink {

  /**
   * @param ctx      : StreamingContext
   * @param instance : ClickHouse实例名称(用于区分多个不同的ClickHouse实例...)
   * @return
   */
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): ClickHouseSink = new ClickHouseSink(ctx, overwriteParams, parallelism, name, uid)

}

class ClickHouseSink(@transient ctx: StreamingContext,
                     overwriteParams: Map[String, String] = Map.empty[String, String],
                     parallelism: Int = 0,
                     name: String = null,
                     uid: String = null)(implicit instance: String = "") extends Sink with Logger {

  val prop = ConfigUtils.getConf(ctx.paramMap, CLICKHOUSE_PREFIX)(instance)
  overwriteParams.foreach { case (k, v) => prop.put(k, v) }

  def sink[T](stream: DataStream[T])(implicit toCSVFun: T => String): DataStreamSink[T] = {
    val sinkFun = new AsyncClickHouseSinkFunction[T](prop, toCSVFun)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  def syncSink[T](stream: DataStream[T])(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val sinkFun = new ClickHouseSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class AsyncClickHouseSinkFunction[T](properties: Properties, toCSVFun: T => String) extends RichSinkFunction[T] with Logger {
  val LOCK = new Object()

  @volatile
  @transient var sinkManager: SinkManager = _
  @transient var sinkBuffer: SinkBuffer = _

  override def open(config: Configuration): Unit = {
    if (sinkManager == null) {
      LOCK.synchronized {
        if (sinkManager == null) {
          sinkManager = new SinkManager(properties)
        }
      }
    }
    sinkBuffer = sinkManager.getBuffer(properties)
  }

  override def invoke(value: T): Unit = {
    val csv = toCSVFun(value)
    try {
      sinkBuffer.put(csv)
    } catch {
      case e: Exception =>
        logger.error(s"""[StreamX] Error while sending data to Clickhouse, record = $csv,error:$e""")
        throw new RuntimeException(e)
    }
  }

  @throws[Exception]
  override def close(): Unit = {
    if (sinkBuffer != null) sinkBuffer.close()
    if (sinkManager != null && !sinkManager.isClosed) {
      LOCK.synchronized {
        if (!sinkManager.isClosed) sinkManager.close()
      }
    }
    super.close()
  }

}


class AsyncClickHouseOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new AsyncClickHouseSinkFunction[T](prop, toSQlFun)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}


class ClickHouseSinkFunction[T](config: Properties, toSQLFn: T => String) extends RichSinkFunction[T] with Logger {
  private var connection: Connection = _
  private var statement: Statement = _
  private val batchSize = config.remove(KEY_JDBC_INSERT_BATCH) match {
    case null => DEFAULT_JDBC_INSERT_BATCH
    case batch => batch.toString.toInt
  }
  private val offset: AtomicLong = new AtomicLong(0L)
  private var timestamp = 0L

  override def open(parameters: Configuration): Unit = {
    val url: String = Try(config.remove(KEY_JDBC_URL).toString).getOrElse(null)
    val user: String = Try(config.remove(KEY_JDBC_USER).toString).getOrElse(null)
    val driver: String = Try(config.remove(KEY_JDBC_DRIVER).toString).getOrElse(null)

    val properties = new ClickHouseProperties()
    (user, driver) match {
      case (u, d) if (u != null && d != null) =>
        Class.forName(d)
        properties.setUser(u)
      case (null, null) =>
      case (_, d) if d != null => Class.forName(d)
      case _ => properties.setUser(user)
    }
    //reflect set all properties...
    config.foreach(x => {
      val field = Try(Option(properties.getClass.getDeclaredField(x._1))).getOrElse(None) match {
        case None =>
          val boolField = s"is${x._1.substring(0, 1).toUpperCase}${x._1.substring(1)}"
          Try(Option(properties.getClass.getDeclaredField(boolField))).getOrElse(None) match {
            case Some(x) => x
            case None => throw new IllegalArgumentException(s"ClickHouseProperties config error,property:${x._1} invalid,please see ru.yandex.clickhouse.settings.ClickHouseProperties")
          }
        case Some(x) => x
      }
      field.setAccessible(true)
      field.getType.getSimpleName match {
        case "String" => field.set(properties, x._2)
        case "int" | "Integer" => field.set(properties, x._2.toInt)
        case "long" | "Long" => field.set(properties, x._2.toLong)
        case "boolean" | "Boolean" => field.set(properties, x._2.toBoolean)
        case _ =>
      }
    })
    val dataSource = new ClickHouseDataSource(url, properties)
    connection = dataSource.getConnection
    if (batchSize > 1) {
      statement = connection.createStatement()
    }
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    require(connection != null)
    val sql = toSQLFn(value)
    batchSize match {
      case 1 =>
        try {
          statement = connection.prepareStatement(sql)
          statement.asInstanceOf[PreparedStatement].executeUpdate
        } catch {
          case e: Exception =>
            logError(s"[StreamX] ClickHouseSink invoke error:${sql}")
            throw e
          case _ =>
        }
      case batch =>
        try {
          statement.addBatch(sql)
          (offset.incrementAndGet() % batch, System.currentTimeMillis()) match {
            case (0, _) => execBatch()
            case (_, current) if current - timestamp > 1000 => execBatch()
            case _ =>
          }
        } catch {
          case e: Exception =>
            logError(s"[StreamX] ClickHouseSink batch invoke error:${sql}")
            throw e
          case _ =>
        }
    }
  }

  override def close(): Unit = {
    execBatch()
    MySQLUtils.close(statement, connection)
  }

  private[this] def execBatch(): Unit = {
    if (offset.get() > 0) {
      offset.set(0)
      val count = statement.executeBatch().sum
      statement.clearBatch()
      logInfo(s"[StreamX] ClickHouseSink batch $count successful..")
      timestamp = System.currentTimeMillis()
    }
  }

}


class ClickHouseOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new ClickHouseSinkFunction[T](prop, toSQlFun)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}


//----------------------------...async...-----------------------------------------------------------
class ClickHouseConfig(parameters: Properties) {
  var currentHostId: Int = 0
  var authorizationRequired: Boolean = false
  val credentials: String = (parameters.getProperty(KEY_JDBC_USER), parameters.getProperty(KEY_JDBC_PASSWORD)) match {
    case (null, null) => ""
    case (u, p) =>
      val credentials = String.join(":", u, p)
      new String(Base64.getEncoder.encode(credentials.getBytes))
  }

  val failedRecordsPath: String = parameters(KEY_CLICKHOUSE_SINK_FAILED_PATH)
  val numWriters: Int = parameters.getProperty(KEY_CLICKHOUSE_SINK_NUM_WRITERS).toInt
  val queueMaxCapacity: Int = parameters.getProperty(KEY_CLICKHOUSE_SINK_QUEUE_CAPACITY).toInt
  val timeout: Int = parameters.getProperty(KEY_CLICKHOUSE_SINK_TIMEOUT).toInt
  val maxRetries: Int = parameters.getProperty(KEY_CLICKHOUSE_SINK_RETRIES).toInt

  require(failedRecordsPath != null)
  require(queueMaxCapacity > 0)
  require(numWriters > 0)
  require(timeout > 0)
  require(maxRetries > 0)

  val hostsString: String = parameters.getProperty(KEY_HOST)
  require(hostsString != null)
  val hostsWithPorts: util.List[String] = buildHosts(hostsString)
  require(hostsWithPorts.isEmpty)

  def buildHosts(hostsString: String): util.List[String] = hostsString.split(SIGN_COMMA).map(checkHttpAndAdd).toList

  def checkHttpAndAdd(host: String): String = {
    val newHost = host.replace(" ", "")
    if (!newHost.contains("http")) {
      "http://" + newHost
    } else {
      newHost
    }
  }

  def getRandomHostUrl: String = {
    currentHostId = ThreadLocalRandom.current.nextInt(hostsWithPorts.size)
    hostsWithPorts.get(currentHostId)
  }

  def getNextHost: String = {
    if (currentHostId >= hostsWithPorts.size - 1) {
      currentHostId = 0
    } else {
      currentHostId += 1
    }
    hostsWithPorts.get(currentHostId)
  }
}

class ClickHouseRequest(val values: util.List[String], val table: String) {
  var attemptCounter = 0

  def incrementCounter(): Unit = this.attemptCounter += 1
}

class SinkWriter() extends AutoCloseable with Logger {

  var sinkParams: ClickHouseConfig = _
  var service: ExecutorService = _
  var callbackService: ExecutorService = _
  var tasks = new ArrayBuffer[WriterTask]
  var commonQueue: BlockingQueue[ClickHouseRequest] = _
  var asyncHttpClient: AsyncHttpClient = _

  def this(sinkParams: ClickHouseConfig) = {
    this()
    this.sinkParams = sinkParams
    asyncHttpClient = Dsl.asyncHttpClient
    val numWriters = sinkParams.numWriters
    commonQueue = new LinkedBlockingQueue[ClickHouseRequest](sinkParams.queueMaxCapacity)
    val threadFactory = ThreadUtils.threadFactory("ClickHouse-writer")
    service = Executors.newFixedThreadPool(sinkParams.numWriters, threadFactory)
    val callbackServiceFactory = ThreadUtils.threadFactory("ClickHouse-writer-callback-executor")
    val cores = Runtime.getRuntime.availableProcessors
    val coreThreadsNum = Math.max(cores / 4, 2)
    callbackService = new ThreadPoolExecutor(coreThreadsNum, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue[Runnable], callbackServiceFactory)

    for (i <- 0 until numWriters) {
      val task = new WriterTask(i, asyncHttpClient, commonQueue, sinkParams, callbackService)
      tasks.add(task)
      service.submit(task)
    }
    try {
      val path = Paths.get(sinkParams.failedRecordsPath)
      Files.createDirectories(path)
    } catch {
      case e: Exception =>
        logger.error("[StreamX] Error while starting CH writer", e)
        throw new RuntimeException(e)
    }
  }

  def put(params: ClickHouseRequest): Unit = {
    try {
      commonQueue.put(params)
    } catch {
      case e: InterruptedException =>
        logger.error("[StreamX] Interrupted error while putting data to queue", e)
        Thread.currentThread.interrupt()
        throw new RuntimeException(e)
    }
  }

  private def stopWriters(): Unit = tasks.foreach(_.setStopWorking())

  @throws[Exception]
  override def close(): Unit = {
    logger.info("[StreamX] Closing ClickHouse-writer...")
    stopWriters()
    ThreadUtils.shutdownExecutorService(service)
    ThreadUtils.shutdownExecutorService(callbackService)
    asyncHttpClient.close()
    logger.info("[StreamX] {} is closed", classOf[SinkWriter].getSimpleName)
  }

}


class WriterTask(val id: Int,
                 val asyncHttpClient: AsyncHttpClient,
                 val queue: BlockingQueue[ClickHouseRequest],
                 val sinkSettings: ClickHouseConfig,
                 val callbackService: ExecutorService) extends Runnable with Logger {
  val HTTP_OK = 200
  @volatile var isWorking = false

  override def run(): Unit = try {
    isWorking = true
    logger.info("[StreamX] Start writer task, id = {}", id)
    while (isWorking || queue.size > 0) {
      val blank = queue.poll(300, TimeUnit.MILLISECONDS)
      if (blank != null) {
        send(blank)
      }
    }
  } catch {
    case e: Exception =>
      logger.error("[StreamX] Error while inserting data", e)
      throw new RuntimeException(e)
  } finally {
    logger.info("[StreamX] Task id = {} is finished", id)
  }

  private def send(chRequest: ClickHouseRequest): Unit = {
    val request = buildRequest(chRequest)
    logger.debug("[StreamX] Ready to load data to {}, size = {}", chRequest.table, chRequest.values.size)
    val whenResponse = asyncHttpClient.executeRequest(request)
    val callback = respCallback(whenResponse, chRequest)
    whenResponse.addListener(callback, callbackService)
  }

  def buildRequest(chRequest: ClickHouseRequest): Request = {
    val resultCSV = String.join(" , ", chRequest.values)
    val query = s"INSERT INTO ${chRequest.table} VALUES ${resultCSV}"
    val host = sinkSettings.getRandomHostUrl
    val builder = asyncHttpClient
      .preparePost(host)
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8")
      .setBody(query)

    if (sinkSettings.authorizationRequired) {
      builder.setHeader(HttpHeaders.Names.AUTHORIZATION, "Basic " + sinkSettings.credentials)
    }
    builder.build
  }

  def respCallback(whenResponse: ListenableFuture[Response], chRequest: ClickHouseRequest): Runnable = new Runnable {
    override def run(): Unit = {
      val response = whenResponse.get()
      try {
        if (response.getStatusCode != HTTP_OK) {
          handleUnsuccessfulResponse(response, chRequest)
        } else {
          logger.info(s"[StreamX] Successful send data to ClickHouse, batch size = ${chRequest.values.size()}, target table = ${chRequest.table}, current attempt = ${chRequest.attemptCounter}")
        }
      } catch {
        case e: Exception => logger.error(s"""[StreamX] Error while executing callback, params = $sinkSettings,error = $e""")
          try {
            handleUnsuccessfulResponse(response, chRequest);
          } catch {
            case e: Exception => logger.error("[StreamX] Error while handle unsuccessful response", e);
          }
      }
    }
  }

  @throws[Exception]
  def handleUnsuccessfulResponse(response: Response, chRequest: ClickHouseRequest): Unit = {
    if (chRequest.attemptCounter > sinkSettings.maxRetries) {
      logger.warn(s"[StreamX] Failed to send data to ClickHouse, cause: limit of attempts is exceeded. ClickHouse response = ${response}. Ready to flush data on disk")
      logFailedRecords(chRequest)
    } else {
      chRequest.incrementCounter()
      logger.warn(s"[StreamX] Next attempt to send data to ClickHouse, table = ${chRequest.table}, buffer size = ${chRequest.values.size}, current attempt num = ${chRequest.attemptCounter}, max attempt num = ${sinkSettings.maxRetries}, response = ${response}")
      queue.put(chRequest)
    }
  }

  @throws[Exception]
  def logFailedRecords(chRequest: ClickHouseRequest): Unit = {
    val filePath = s"${sinkSettings.failedRecordsPath}/${chRequest}_${System.currentTimeMillis}"
    val writer = new PrintWriter(filePath)
    try {
      chRequest.values.foreach(writer.println)
      writer.flush()
    } finally {
      if (writer != null) writer.close()
    }
    logger.info("[StreamX] Successful send data on disk, path = {}, size = {} ", filePath, chRequest.values.size)
  }

  def setStopWorking(): Unit = isWorking = false
}


class SinkBuffer(val writer: SinkWriter,
                 val timeoutMillis: Long,
                 val bufferSize: Int,
                 val table: String) extends AutoCloseable with Logger {

  private var lastAddTimeMillis = 0L

  var localValues: ArrayBuffer[String] = new ArrayBuffer[String]

  def put(recordAsCSV: String): Unit = {
    tryAddToQueue()
    localValues.add(recordAsCSV)
    lastAddTimeMillis = System.currentTimeMillis
  }

  def tryAddToQueue(): Unit = {
    if (flushCondition) {
      addToQueue()
    }
  }

  private[this] def addToQueue(): Unit = {
    val deepCopy = buildDeepCopy(localValues.toList)
    val params = new ClickHouseRequest(deepCopy, table)
    logger.debug("[StreamX] Build blank with params: buffer size = {}, target table  = {}", params.values.size, params.table)
    writer.put(params)
    localValues.clear()
  }

  private[this] def flushCondition: Boolean = localValues.nonEmpty && (checkSize || checkTime)

  private[this] def checkSize: Boolean = localValues.size >= bufferSize

  private[this] def checkTime: Boolean = {
    if (lastAddTimeMillis == 0) return false
    val current = System.currentTimeMillis
    current - lastAddTimeMillis > timeoutMillis
  }

  private[this] def buildDeepCopy(original: util.List[String]): util.List[String] = Collections.unmodifiableList(new util.ArrayList[String](original))

  override def close(): Unit = if (localValues.nonEmpty) addToQueue()

}

class SinkScheduledChecker(params: ClickHouseConfig) extends AutoCloseable with Logger {

  val sinkBuffers: ArrayBuffer[SinkBuffer] = new ArrayBuffer[SinkBuffer]()
  val factory: ThreadFactory = ThreadUtils.threadFactory("ClickHouse-writer-checker")
  val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory)
  scheduledExecutorService.scheduleWithFixedDelay(getTask, params.timeout, params.timeout, TimeUnit.SECONDS)
  logger.info("[StreamX] Build Sink scheduled checker, timeout (sec) = {}", params.timeout)

  def addSinkBuffer(sinkBuffer: SinkBuffer): Unit = {
    this.synchronized(sinkBuffers.add(sinkBuffer))
    logger.debug("[StreamX] Add sinkBuffer, target table = {}", sinkBuffer.table)
  }

  private def getTask: Runnable = new Runnable {
    override def run(): Unit = {
      this synchronized {
        logger.debug("[StreamX] Start checking buffers. Current count of buffers = {}", sinkBuffers.size)
        sinkBuffers.foreach(_.tryAddToQueue())
      }
    }
  }

  @throws[Exception]
  override def close(): Unit = ThreadUtils.shutdownExecutorService(scheduledExecutorService)
}

class SinkManager(properties: Properties) extends AutoCloseable with Logger {
  val sinkParams: ClickHouseConfig = new ClickHouseConfig(properties)
  val writer = new SinkWriter(sinkParams)
  val checker = new SinkScheduledChecker(sinkParams)
  @volatile var isClosed: Boolean = false

  def getBuffer(properties: Properties): SinkBuffer = {
    val table = properties.getProperty(KEY_CLICKHOUSE_SINK_TABLE)
    val bufferSize = properties.getProperty(KEY_CLICKHOUSE_SINK_BUFFER_SIZE).toInt
    val sinkBuffer = new SinkBuffer(writer, sinkParams.timeout, bufferSize, table)
    checker.addSinkBuffer(sinkBuffer)
    sinkBuffer
  }

  @throws[Exception]
  override def close(): Unit = {
    writer.close()
    checker.close()
    isClosed = true
  }

}