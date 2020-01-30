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
import scala.collection.mutable.ListBuffer
import scala.util.Try
import com.streamxhub.flink.core.failover._

/**
 * @author benjobs
 */
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

  val prop = ConfigUtils.getConf(ctx.paramMap, CLICKHOUSE_SINK_PREFIX)(instance)
  overwriteParams.foreach { case (k, v) => prop.put(k, v) }

  /**
   *
   * @param stream
   * @param dbAndTable database and table ,e.g test.user....
   * @param toCSVFun
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T], dbAndTable: String)(implicit toCSVFun: T => String = null): DataStreamSink[T] = {
    prop.put(KEY_SINK_FAILOVER_TABLE, dbAndTable)
    val sinkFun = new AsyncClickHouseSinkFunction[T](prop)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  def syncSink[T](stream: DataStream[T])(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val sinkFun = new ClickHouseSinkFunction[T](prop)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class AsyncClickHouseSinkFunction[T](properties: Properties)(implicit toCSVFun: T => String = null) extends RichSinkFunction[T] with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new Object()
  }

  @transient var clickHouseConf: ClickHouseConfig = _
  @transient var sinkBuffer: SinkBuffer = _
  @transient var clickHouseWriter: ClickHouseSinkWriter = _
  @transient var failoverChecker: FailoverChecker = _
  @volatile var isClosed: Boolean = false

  override def open(config: Configuration): Unit = {
    if (!Lock.initialized) {
      Lock.lock.synchronized {
        if (!Lock.initialized) {
          Lock.initialized = true
          val table = properties(KEY_SINK_FAILOVER_TABLE)
          val bufferSize = properties(KEY_SINK_THRESHOLD_BUFFER_SIZE).toInt
          val requestTimeout = properties.getOrElse(KEY_SINK_THRESHOLD_REQ_TIMEOUT, DEFAULT_SINK_REQUEST_TIMEOUT).toString.toInt
          clickHouseConf = new ClickHouseConfig(properties)
          clickHouseWriter = ClickHouseSinkWriter(clickHouseConf, requestTimeout)
          failoverChecker = FailoverChecker(clickHouseConf.checkTimeout)
          sinkBuffer = SinkBuffer(clickHouseWriter, clickHouseConf.checkTimeout, bufferSize, table)
          failoverChecker.addSinkBuffer(sinkBuffer)
          logInfo("[StreamX] AsyncClickHouseSink initialize... ")
        }
      }
    }
  }

  override def invoke(value: T): Unit = {
    val csv = toCSVFun match {
      case null => //啧啧啧...
        val buffer = new StringBuilder("(")
        val fields = value.getClass.getDeclaredFields
        fields.foreach(f => {
          f.setAccessible(true)
          val v = f.get(value)
          f.getType.getSimpleName match {
            case "String" => buffer.append(s""""$v",""".stripMargin)
            case _ => buffer.append(s"""$v,""".stripMargin)
          }
        })
        buffer.toString().replaceFirst(",$", ")")
      case _ => toCSVFun(value)
    }
    try {
      sinkBuffer.put(csv)
    } catch {
      case e: Exception =>
        logError(s"""[StreamX] Error while sending data to Clickhouse, record = $csv,error:$e""")
        throw new RuntimeException(e)
    }
  }

  override def close(): Unit = {
    if (!isClosed) {
      Lock.lock.synchronized {
        if (!isClosed) {
          if (sinkBuffer != null) sinkBuffer.close()
          if (clickHouseWriter != null) clickHouseWriter.close()
          if (failoverChecker != null) failoverChecker.close()
          isClosed = true
          super.close()
        }
      }
    }
  }
}


class AsyncClickHouseOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String = null) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new AsyncClickHouseSinkFunction[T](prop)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}


class ClickHouseSinkFunction[T](config: Properties)(implicit toSQLFn: T => String = null) extends RichSinkFunction[T] with Logger {
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
            logError(s"""[StreamX] ClickHouseSink invoke error:$sql""")
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
            logError(s"""[StreamX] ClickHouseSink batch invoke error:$sql""")
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

  val sinkFunction = new ClickHouseSinkFunction[T](prop)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}


/**
 *
 * Flink sink for Clickhouse database. Powered by Async Http Client.
 *
 * High-performance library for loading data to Clickhouse.
 *
 * It has two triggers for loading data: by timeout and by buffer size.
 *
 */
//---------------------------------------------------------------------------------------

class ClickHouseConfig(parameters: Properties) extends FailoverConf(parameters) {
  var currentHostId: Int = 0
  val credentials: String = (parameters.getProperty(KEY_JDBC_USER), parameters.getProperty(KEY_JDBC_PASSWORD)) match {
    case (null, null) => null
    case (u, p) => new String(Base64.getEncoder.encode(s"$u:$p".getBytes))
  }
  val jdbcUrls: util.List[String] = parameters.getOrElse(KEY_JDBC_URL, "")
    .split(SIGN_COMMA)
    .filter(_.nonEmpty)
    .map(_.replaceAll("\\s+", "").replaceFirst("^http://|^", "http://"))
    .toList

  require(jdbcUrls.nonEmpty)

  def getRandomHostUrl: String = {
    currentHostId = ThreadLocalRandom.current.nextInt(jdbcUrls.size)
    jdbcUrls.get(currentHostId)
  }

  def getNextHost: String = {
    if (currentHostId >= jdbcUrls.size - 1) {
      currentHostId = 0
    } else {
      currentHostId += 1
    }
    jdbcUrls.get(currentHostId)
  }

}

case class ClickHouseSinkWriter(sinkParams: ClickHouseConfig, requestTimeout: Int) extends SinkWriter with Logger {
  private val callbackServiceFactory = ThreadUtils.threadFactory("ClickHouse-writer-callback-executor")
  private val threadFactory: ThreadFactory = ThreadUtils.threadFactory("ClickHouse-writer")

  var callbackService: ExecutorService = new ThreadPoolExecutor(
    math.max(Runtime.getRuntime.availableProcessors / 4, 2),
    Integer.MAX_VALUE,
    60L,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable],
    callbackServiceFactory
  )

  var tasks: ListBuffer[ClickHouseWriterTask] = ListBuffer[ClickHouseWriterTask]()
  var commonQueue: BlockingQueue[SinkRequest] = new LinkedBlockingQueue[SinkRequest](sinkParams.queueMaxCapacity)
  var asyncHttpClient: AsyncHttpClient = Dsl.asyncHttpClient
  var service: ExecutorService = Executors.newFixedThreadPool(sinkParams.numWriters, threadFactory)

  for (i <- 0 until sinkParams.numWriters) {
    val task = ClickHouseWriterTask(i, asyncHttpClient, requestTimeout, commonQueue, sinkParams, callbackService)
    tasks.add(task)
    service.submit(task)
  }

  def write(params: SinkRequest): Unit = {
    try {
      commonQueue.put(params)
    } catch {
      case e: InterruptedException =>
        logError(s"[StreamX] Interrupted error while putting data to queue,error:$e")
        Thread.currentThread.interrupt()
        throw new RuntimeException(e)
    }
  }

  override def close(): Unit = {
    logInfo("[StreamX] Closing ClickHouse-writer...")
    tasks.foreach(_.close())
    ThreadUtils.shutdownExecutorService(service)
    ThreadUtils.shutdownExecutorService(callbackService)
    asyncHttpClient.close()
    commonQueue.clear()
    tasks.clear()
    logInfo(s"[StreamX] ${classOf[ClickHouseSinkWriter].getSimpleName} is closed")
  }

}


case class ClickHouseWriterTask(id: Int,
                                asyncHttpClient: AsyncHttpClient,
                                requestTimeout: Int,
                                queue: BlockingQueue[SinkRequest],
                                clickHouseConf: ClickHouseConfig,
                                callbackService: ExecutorService) extends Runnable with AutoCloseable with Logger {
  val HTTP_OK = 200
  @volatile var isWorking = false

  val failoverWriter: FailoverWriter = new FailoverWriter(clickHouseConf.failoverStorage, clickHouseConf.getFailoverConfig)

  override def run(): Unit = try {
    isWorking = true
    logInfo(s"[StreamX] Start writer task, id = ${id}")
    while (isWorking || queue.size > 0) {
      val req = queue.poll(300, TimeUnit.MILLISECONDS)
      if (req != null) {
        send(req)
      }
    }
  } catch {
    case e: Exception =>
      logError("[StreamX] Error while inserting data", e)
      throw new RuntimeException(e)
  } finally {
    logInfo(s"[StreamX] Task id = $id is finished")
  }

  def send(chRequest: SinkRequest): Unit = {
    val request = buildRequest(chRequest)
    logger.debug(s"[StreamX] Ready to load data to ${chRequest.table}, size = ${chRequest.size}")
    val whenResponse = asyncHttpClient.executeRequest(request)
    val callback = respCallback(whenResponse, chRequest)
    whenResponse.addListener(callback, callbackService)
  }

  def buildRequest(chRequest: SinkRequest): Request = {
    val query = s"INSERT INTO ${chRequest.table} VALUES ${chRequest.records.mkString(",")}"
    val host = clickHouseConf.getRandomHostUrl
    val builder = asyncHttpClient
      .preparePost(host)
      .setRequestTimeout(requestTimeout)
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8")
      .setBody(query)

    if (clickHouseConf.credentials != null) {
      builder.setHeader(HttpHeaders.Names.AUTHORIZATION, "Basic " + clickHouseConf.credentials)
    }
    builder.build
  }

  def respCallback(whenResponse: ListenableFuture[Response], chRequest: SinkRequest): Runnable = new Runnable {
    override def run(): Unit = {
      Try(whenResponse.get()).getOrElse(null) match {
        case null =>
          logError(s"""[StreamX] Error ClickHouseSink executing callback, params = $clickHouseConf,can not get Response. """)
          handleFailedResponse(null, chRequest)
        case resp if resp.getStatusCode != HTTP_OK =>
          logError(s"""[StreamX] Error ClickHouseSink executing callback, params = $clickHouseConf, StatusCode = ${resp.getStatusCode} """)
          handleFailedResponse(resp, chRequest)
        case _ =>
      }
    }
  }

  /**
   * if send data to ClickHouse Failed, retry $maxRetries, if still failed,flush data to $failoverStorage
   *
   * @param response
   * @param chRequest
   */
  def handleFailedResponse(response: Response, chRequest: SinkRequest): Unit = {
    if (chRequest.attemptCounter > clickHouseConf.maxRetries) {
      logWarning(s"""[StreamX] Failed to send data to ClickHouse, cause: limit of attempts is exceeded. ClickHouse response = $response. Ready to flush data to ${clickHouseConf.failoverStorage}""")
      failoverWriter.write(chRequest)
      logInfo(s"[StreamX] failover Successful, StorageType = ${clickHouseConf.failoverStorage}, size = ${chRequest.size}")
    } else {
      chRequest.incrementCounter()
      logWarning(s"[StreamX] Next attempt to send data to ClickHouse, table = ${chRequest.table}, buffer size = ${chRequest.size}, current attempt num = ${chRequest.attemptCounter}, max attempt num = ${clickHouseConf.maxRetries}, response = $response")
      queue.put(chRequest)
    }
  }


  override def close(): Unit = {
    isWorking = false
    failoverWriter.close()
  }
}

