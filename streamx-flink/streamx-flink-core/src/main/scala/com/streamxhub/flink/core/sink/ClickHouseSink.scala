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
import java.util.{Base64, Collections, Date, Properties, UUID}

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.conf.FailoverStorageType
import com.streamxhub.common.conf.FailoverStorageType.FailoverStorageType
import com.streamxhub.common.util.{ConfigUtils, DateUtils, HBaseClient, Logger, MySQLUtils, ThreadUtils}
import com.streamxhub.flink.core.StreamingContext
import io.netty.handler.codec.http.HttpHeaders
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.conf.{Configuration => HConf}
import org.apache.hadoop.fs.{FileSystem => HFileSys}
import java.io.ByteArrayInputStream
import java.net.URI

import org.asynchttpclient._
import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseProperties

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.Map
import scala.collection.mutable.ListBuffer
import scala.util.Try
import com.streamxhub.common.conf.FailoverStorageType._
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.hbase.{HColumnDescriptor, HConstants, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.{BufferedMutator, BufferedMutatorParams, Put, RetriesExhaustedWithDetailsException, Connection => HBaseConn}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.io.IOUtils
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}

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

  val prop = ConfigUtils.getConf(ctx.paramMap, CLICKHOUSE_PREFIX)(instance)
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
    prop.put(KEY_CLICKHOUSE_SINK_TABLE, dbAndTable)
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
  @transient var clickHouseBuffer: ClickHouseBuffer = _
  @transient var clickHouseWriter: ClickHouseWriter = _
  @transient var clickHouseChecker: ClickHouseScheduledChecker = _
  @volatile var isClosed: Boolean = false

  override def open(config: Configuration): Unit = {
    if (!Lock.initialized) {
      Lock.lock.synchronized {
        if (!Lock.initialized) {
          Lock.initialized = true
          val table = properties.getProperty(KEY_CLICKHOUSE_SINK_TABLE)
          val bufferSize = properties.getProperty(KEY_CLICKHOUSE_SINK_BUFFER_SIZE).toInt
          clickHouseConf = new ClickHouseConfig(properties)
          clickHouseWriter = new ClickHouseWriter(clickHouseConf)
          clickHouseChecker = new ClickHouseScheduledChecker(clickHouseConf)
          clickHouseBuffer = new ClickHouseBuffer(clickHouseWriter, clickHouseConf.timeout, bufferSize, table)
          clickHouseChecker.addSinkBuffer(clickHouseBuffer)
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
      clickHouseBuffer.put(csv)
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
          if (clickHouseBuffer != null) clickHouseBuffer.close()
          if (clickHouseWriter != null) clickHouseWriter.close()
          if (clickHouseChecker != null) clickHouseChecker.close()
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

case class ClickHouseConfig(parameters: Properties) {
  val numWriters: Int = parameters(KEY_CLICKHOUSE_SINK_NUM_WRITERS).toInt
  val queueMaxCapacity: Int = parameters(KEY_CLICKHOUSE_SINK_QUEUE_CAPACITY).toInt
  val timeout: Long = parameters(KEY_CLICKHOUSE_SINK_TIMEOUT).toLong
  var currentHostId: Int = 0
  val credentials: String = (parameters.getProperty(KEY_JDBC_USER), parameters.getProperty(KEY_JDBC_PASSWORD)) match {
    case (null, null) => null
    case (u, p) => new String(Base64.getEncoder.encode(s"$u:$p".getBytes))
  }

  val maxRetries: Int = parameters(KEY_CLICKHOUSE_FAILOVER_RETRIES).toInt
  val failoverStorage: FailoverStorageType = FailoverStorageType.get(parameters.getOrElse(KEY_CLICKHOUSE_FAILOVER_STORAGE, throw new IllegalArgumentException(s"[Streamx] usage error! failover.storage muse be not null! ")))

  val jdbcUrls: util.List[String] = parameters.getOrElse(KEY_JDBC_URL, "")
    .split(SIGN_COMMA)
    .filter(_.nonEmpty)
    .map(_.replaceAll("\\s+", "").replaceFirst("^http://|^", "http://"))
    .toList

  require(jdbcUrls.nonEmpty)
  require(queueMaxCapacity > 0)
  require(numWriters > 0)
  require(timeout > 0)
  require(maxRetries > 0)

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

  def getFailoverConfig: Properties = {
    failoverStorage match {
      case Kafka => ConfigUtils.getConf(parameters.toMap.asJava, "async.failover.kafka.")
      case MySQL => ConfigUtils.getConf(parameters.toMap.asJava, "async.failover.mysql.")
      case HBase => ConfigUtils.getConf(parameters.toMap.asJava, "async.failover.hbase.", HBASE_PREFIX)
      case HDFS => ConfigUtils.getConf(parameters.toMap.asJava, "async.failover.hdfs.")
    }
  }

}

case class ClickHouseRequest(records: util.List[String], table: String) {
  var attemptCounter = 0

  def incrementCounter(): Unit = this.attemptCounter += 1

  def size: Int = records.size()
}

case class ClickHouseWriter(sinkParams: ClickHouseConfig) extends AutoCloseable with Logger {
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

  var tasks: ListBuffer[WriterTask] = ListBuffer[WriterTask]()
  var commonQueue: BlockingQueue[ClickHouseRequest] = new LinkedBlockingQueue[ClickHouseRequest](sinkParams.queueMaxCapacity)
  var asyncHttpClient: AsyncHttpClient = Dsl.asyncHttpClient
  var service: ExecutorService = Executors.newFixedThreadPool(sinkParams.numWriters, threadFactory)

  for (i <- 0 until sinkParams.numWriters) {
    val task = new WriterTask(i, asyncHttpClient, commonQueue, sinkParams, callbackService)
    tasks.add(task)
    service.submit(task)
  }

  def put(params: ClickHouseRequest): Unit = {
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
    logInfo(s"[StreamX] ${classOf[ClickHouseWriter].getSimpleName} is closed")
  }

}


case class WriterTask(id: Int,
                      asyncHttpClient: AsyncHttpClient,
                      queue: BlockingQueue[ClickHouseRequest],
                      clickHouseConf: ClickHouseConfig,
                      callbackService: ExecutorService) extends Runnable with AutoCloseable with Logger {
  val HTTP_OK = 200
  @volatile var isWorking = false

  val failoverWriter: FailoverWriter = new FailoverWriter(clickHouseConf)

  override def run(): Unit = try {
    isWorking = true
    logInfo(s"[StreamX] Start writer task, id = ${id}")
    while (isWorking || queue.size > 0) {
      val blank = queue.poll(300, TimeUnit.MILLISECONDS)
      if (blank != null) {
        send(blank)
      }
    }
  } catch {
    case e: Exception =>
      logError("[StreamX] Error while inserting data", e)
      throw new RuntimeException(e)
  } finally {
    logInfo(s"[StreamX] Task id = $id is finished")
  }

  def send(chRequest: ClickHouseRequest): Unit = {
    val request = buildRequest(chRequest)
    logger.debug(s"[StreamX] Ready to load data to ${chRequest.table}, size = ${chRequest.size}")
    val whenResponse = asyncHttpClient.executeRequest(request)
    val callback = respCallback(whenResponse, chRequest)
    whenResponse.addListener(callback, callbackService)
  }

  def buildRequest(chRequest: ClickHouseRequest): Request = {
    val query = s"INSERT INTO ${chRequest.table} VALUES ${chRequest.records.mkString(",")}"
    val host = clickHouseConf.getRandomHostUrl
    val builder = asyncHttpClient
      .preparePost(host)
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8")
      .setBody(query)

    if (clickHouseConf.credentials != null) {
      builder.setHeader(HttpHeaders.Names.AUTHORIZATION, "Basic " + clickHouseConf.credentials)
    }
    builder.build
  }

  def respCallback(whenResponse: ListenableFuture[Response], chRequest: ClickHouseRequest): Runnable = new Runnable {
    override def run(): Unit = {
      val response = whenResponse.get()
      try {
        if (response.getStatusCode != HTTP_OK) {
          handleFailedResponse(response, chRequest)
        } else {
          logInfo(s"[StreamX] Successful send data to ClickHouse, batch size = ${chRequest.size}, target table = ${chRequest.table}, current attempt = ${chRequest.attemptCounter}")
        }
      } catch {
        case e: Exception => logError(s"""[StreamX] Error while executing callback, params = $clickHouseConf,error = $e""")
          try {
            handleFailedResponse(response, chRequest)
          } catch {
            case e: Exception => logError("[StreamX] Error while handle unsuccessful response", e)
          }
      }
    }
  }

  /**
   * if send data to ClickHouse Failed, retry $maxRetries, if still failed,flush data to $failoverStorage
   *
   * @param response
   * @param chRequest
   */
  def handleFailedResponse(response: Response, chRequest: ClickHouseRequest): Unit = {
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


case class ClickHouseBuffer(writer: ClickHouseWriter,
                            timeoutMillis: Long,
                            bufferSize: Int,
                            table: String) extends AutoCloseable with Logger {

  private var timestamp = 0L

  var localValues = new CopyOnWriteArrayList[String]()

  def put(csv: String): Unit = {
    tryAddToQueue()
    localValues.add(csv)
    timestamp = System.currentTimeMillis
  }

  def tryAddToQueue(): Unit = {
    this.synchronized {
      if (flushCondition) {
        addToQueue()
      }
    }
  }

  private[this] def addToQueue(): Unit = {
    val deepCopy = buildDeepCopy(localValues)
    val params = new ClickHouseRequest(deepCopy, table)
    logger.debug(s"[StreamX] Build blank with params: buffer size = ${params.size}, target table  = ${params.table}")
    writer.put(params)
    localValues.clear()
  }

  private[this] def flushCondition: Boolean = localValues.nonEmpty && (checkSize || checkTime)

  private[this] def checkSize: Boolean = localValues.size >= bufferSize

  private[this] def checkTime: Boolean = {
    if (timestamp == 0) return false
    val current = System.currentTimeMillis
    current - timestamp > timeoutMillis
  }

  private[this] def buildDeepCopy(original: util.List[String]): util.List[String] = Collections.unmodifiableList(new util.ArrayList[String](original))

  override def close(): Unit = if (localValues.nonEmpty) addToQueue()

}

case class ClickHouseScheduledChecker(config: ClickHouseConfig) extends AutoCloseable with Logger {

  val clickHouseBuffers: ListBuffer[ClickHouseBuffer] = ListBuffer[ClickHouseBuffer]()
  val factory: ThreadFactory = ThreadUtils.threadFactory("ClickHouse-writer-checker")
  val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory)
  scheduledExecutorService.scheduleWithFixedDelay(getTask, config.timeout, config.timeout, TimeUnit.MILLISECONDS)
  logInfo(s"[StreamX] Build Sink scheduled checker, timeout (microSeconds) = ${config.timeout}")

  def addSinkBuffer(clickHouseBuffer: ClickHouseBuffer): Unit = {
    this.synchronized(clickHouseBuffers.add(clickHouseBuffer))
    logger.debug(s"[StreamX] Add clickHouseBuffer, target table = ${clickHouseBuffer.table}")
  }

  def getTask: Runnable = new Runnable {
    override def run(): Unit = {
      this synchronized {
        logger.debug(s"[StreamX] Start checking buffers. Current count of buffers = ${clickHouseBuffers.size}")
        clickHouseBuffers.foreach(_.tryAddToQueue())
      }
    }
  }

  override def close(): Unit = ThreadUtils.shutdownExecutorService(scheduledExecutorService)

}


class FailoverWriter(clickHouseConf: ClickHouseConfig) extends AutoCloseable with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new Object()
  }

  val failoverStorage: FailoverStorageType = clickHouseConf.failoverStorage
  val failoverConfig: Properties = clickHouseConf.getFailoverConfig

  private var kafkaProducer: KafkaProducer[String, String] = _
  private var hbaseConnect: HBaseConn = _
  private var mutator: BufferedMutator = _
  private var fileSystem: FileSystem = _
  private var hdfsSeparator: String = _
  private var counter: Long = 0

  def write(request: ClickHouseRequest): Unit = {

    this.synchronized {
      val table = request.table.split("\\.").last
      failoverStorage match {
        case Kafka =>
          if (!Lock.initialized) {
            Lock.lock.synchronized {
              if (!Lock.initialized) {
                Lock.initialized = true
                //failoverConfig.remove(KEY_KAFKA_TOPIC)
                failoverConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                failoverConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                kafkaProducer = new KafkaProducer[String, String](failoverConfig)
              }
            }
          }
          val topic = failoverConfig.getProperty(KEY_KAFKA_TOPIC)
          val timestamp = System.currentTimeMillis()
          val records = request.records.map(cleanUp)
          val sendData =
            s"""
               |{
               |"values":[${records.mkString(",")}],
               |"timestamp":$timestamp
               |}
               |""".stripMargin
          val record = new ProducerRecord[String, String](topic, sendData)
          kafkaProducer.send(record, new Callback() {
            override def onCompletion(recordMetadata: RecordMetadata, e: Exception): Unit = {
              logInfo(s"[StreamX] ClickHouse failover successful!! storageType:Kafka,table: $table,size:${request.size}")
            }
          }).get()

        case MySQL =>
          if (!Lock.initialized) {
            Lock.lock.synchronized {
              if (!Lock.initialized) {
                Lock.initialized = true
                failoverConfig.put(KEY_INSTANCE, s"failover-${table}")
                val mysqlConnect = MySQLUtils.getConnection(failoverConfig)
                val mysqlTable = mysqlConnect.getMetaData.getTables(null, null, table, Array("TABLE", "VIEW"))
                if (!mysqlTable.next()) {
                  MySQLUtils.execute(
                    mysqlConnect,
                    s"create table $table (`values` text, `timestamp` bigint)"
                  )
                  logWarning(s"[StreamX] ClickHouse failover storageType:MySQL,table: $table is not exist,auto created...")
                }
              }
            }
          }
          val timestamp = System.currentTimeMillis()
          val records = request.records.map(x => {
            val v = cleanUp(x)
            s""" ($v,$timestamp) """.stripMargin
          })
          val sql = s"INSERT INTO $table(`values`,`timestamp`) VALUES ${records.mkString(",")} "
          MySQLUtils.update(sql)(failoverConfig)
          logInfo(s"[StreamX] ClickHouse failover successful!! storageType:MySQL,table: $table,size:${request.size}")

        case HBase =>
          val tableName = TableName.valueOf(table)
          val familyName = "cf"
          if (!Lock.initialized) {
            Lock.lock.synchronized {
              if (!Lock.initialized) {
                Lock.initialized = true
                hbaseConnect = HBaseClient(failoverConfig).connection
                val admin = hbaseConnect.getAdmin
                if (!admin.tableExists(tableName)) {
                  val desc = new HTableDescriptor(tableName)
                  desc.addFamily(new HColumnDescriptor(familyName))
                  admin.createTable(desc)
                  logInfo(s"[StreamX] ClickHouse failover storageType:HBase,table: $table is not exist,auto created...")
                }
                val mutatorParam = new BufferedMutatorParams(tableName)
                  .listener(new BufferedMutator.ExceptionListener {
                    override def onException(exception: RetriesExhaustedWithDetailsException, mutator: BufferedMutator): Unit = {
                      for (i <- 0.until(exception.getNumExceptions)) {
                        logInfo(s"[StreamX] ClickHouse failover storageType:HBase Failed to sent put ${exception.getRow(i)},error:${exception.getLocalizedMessage}")
                      }
                    }
                  })
                mutator = hbaseConnect.getBufferedMutator(mutatorParam)
              }
            }
          }
          val timestamp = System.currentTimeMillis()
          for (i <- 0 until (request.size)) {
            val rowKey = HConstants.LATEST_TIMESTAMP - timestamp - i //you know?...
            val put = new Put(Bytes.toBytes(rowKey))
              .addColumn(familyName.getBytes, "values".getBytes, Bytes.toBytes(request.records(i)))
              .addColumn(familyName.getBytes, "timestamp".getBytes, Bytes.toBytes(timestamp))
            mutator.mutate(put)
          }
          mutator.flush()

        case HDFS =>
          counter = counter + 1
          val path = failoverConfig("path")
          val format = failoverConfig.getOrElse("format", DateUtils.dayFormat1)
          require(path != null)
          val fileName = s"$path/$table"
          val hdfsFormat = DateUtils.format(format, new Date())
          val rootPath = new Path(s"$fileName/$hdfsFormat")
          try {
            if (!Lock.initialized) {
              Lock.lock.synchronized {
                if (!Lock.initialized) {
                  Lock.initialized = true
                  fileSystem = (Option(failoverConfig("namenode")), Option(failoverConfig("user"))) match {
                    case (None, None) =>
                      HFileSys.get(new HConf())
                    case (Some(nn), Some(u)) =>
                      HFileSys.get(new URI(nn), new HConf(), u)
                    case (Some(nn), _) =>
                      HFileSys.get(new URI(nn), new HConf())
                    case _ =>
                      throw new IllegalArgumentException("[StreamX] usage error..")
                  }
                  hdfsSeparator = hdfsFormat
                  if (!fileSystem.exists(rootPath)) {
                    fileSystem.mkdirs(rootPath)
                  }
                }
              }
            }

            if (hdfsSeparator != hdfsFormat) {
              hdfsSeparator = hdfsFormat
              if (!fileSystem.exists(rootPath)) {
                fileSystem.mkdirs(rootPath)
              }
            }

            val filePath = new Path(s"$rootPath/${System.currentTimeMillis()}_${UUID.randomUUID().toString.replace("-","")}")
            var outStream = fileSystem.create(filePath)
            var record = new StringBuilder
            request.records.foreach(x => record.append(x).append("\n"))
            var inputStream = new ByteArrayInputStream(record.toString().getBytes)
            IOUtils.copyBytes(inputStream, outStream, 1024, true)
            record.clear()
            record = null
            inputStream = null
            outStream = null
          } catch {
            case e: Exception => e.printStackTrace()
          }
        case _ => throw new UnsupportedOperationException(s"[StreamX] unsupported failover storageType:${failoverStorage}")
      }
    }
  }

  private[this] def cleanUp(record: String) = s""" "${record.replace("\"", "\\\"")}" """.stripMargin


  override def close(): Unit = {
    if (kafkaProducer != null) kafkaProducer.close()
    if (mutator != null) {
      mutator.flush()
      mutator.close()
    }
  }

}
