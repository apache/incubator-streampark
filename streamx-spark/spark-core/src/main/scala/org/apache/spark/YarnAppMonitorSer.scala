package org.apache.spark

import java.util.concurrent.{ConcurrentHashMap, ScheduledFuture, TimeUnit}

import org.apache.spark.internal.Logging
import org.apache.spark.rpc._
import org.apache.spark.util.{IntParam, ThreadUtils, Utils}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.concurrent.duration.FiniteDuration
import scala.sys.process._
import scala.util.Try

class YarnAppMonitorSer(
                         override val rpcEnv: RpcEnv,
                         val conf: SparkConf
                       ) extends ThreadSafeRpcEndpoint with Logging {

  val schedulerThread = ThreadUtils.newDaemonSingleThreadScheduledExecutor("app-monitor")
  var schedulerThreadFuture: ScheduledFuture[_] = _

  val appKillCommand = conf.get("spark.yarn.app.kill.command", "yarn")
  val startCommand = conf.get("spark.yarn.app.start.command", "run.sh")
  val schedulerStartTime = conf.getLong("spark.yarn.app.scheduled.start.time", 3000L)
  val schedulerIntervalTime = conf.getLong("spark.yarn.app.scheduled.interval.time", 9000L)
  val appNameMap = new ConcurrentHashMap[String, String]()

  logInfo("Init YarnAppMonitorSer...")
  logInfo(s"get config startCommand $startCommand .")
  logInfo(s"get config appKillCommand $appKillCommand .")
  logInfo("Init YarnAppMonitorSer success.")

  override def onStart(): Unit = {
    schedulerThreadFuture = schedulerThread.scheduleWithFixedDelay(new Runnable {
      override def run(): Unit = {
        val appInfo = getAppInfo.toMap
        if (appInfo.nonEmpty) {
          appNameMap.foreach {
            case (name, config) =>
              if (!checkApp(name, appInfo)) {
                logInfo(s"restart app $name")
                startApp(name, config)
              }
          }
        }
      }
    }, schedulerStartTime, schedulerIntervalTime, TimeUnit.MILLISECONDS)
  }

  override def receive: PartialFunction[Any, Unit] = {
    case YarnAppStopRequest(appName) => logInfo(s"kill $appName result value " + killApp(appName))
    case YarnAppStartRequest(appName, config) => logInfo(s"start $appName result value " + startApp(appName, config))
    case _ => logWarning("unknown request")
  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case YarnAppMonitorRequest(appName, config) =>
      if (!appNameMap.containsKey(appName)) appNameMap += appName -> config
      context.reply(YarnAppResponse("success", 200))

    case YarnAppCancelRequest(appName) =>
      if (appNameMap.containsKey(appName)) appNameMap -= appName
      context.reply(YarnAppResponse(appName, killApp(appName)))

    case YarnAppRequest(appName) =>
      context.reply(YarnAppResponse(appNameMap.map(_._1).mkString(","), 200))

    case _ => context.reply(YarnAppResponse("unknown request", 404))
  }

  override def onStop(): Unit = {
    if (schedulerThreadFuture != null) {
      schedulerThreadFuture.cancel(true)
    }
    schedulerThread.shutdownNow()
  }

  private def getAppInfo = {
    val appInfo = new ConcurrentHashMap[String, String]()
    val res = Try {
      s"$appKillCommand application -list" !!
    }.getOrElse("")
    res.split("\n").foreach(s => {
      val i = s.split("\\s")
      appInfo += s"${i(1)}" -> s"${i(0)}"
    })
    appInfo
  }

  private def startApp(appName: String, conf: String): Int = {
    s"$startCommand $conf" #> new java.io.File("/dev/null") !
  }

  private def getKillCmd = {
    val r = "(.*yarn$)".r
    appKillCommand match {
      case r(n) => s"$n application -kill"
      case _ => appKillCommand
    }
  }

  private def checkApp(appName: String, appInfo: Map[String, String]) = {
    appInfo.contains(appName)
  }

  private def getAppId(appName: String): String = {
    val res = Try {
      s"$appKillCommand application -list" #| s"grep $appName" !!
    }.getOrElse("")
    res.split("\\s")(0)
  }

  private def killApp(appName: String): Int = {
    val appId = getAppId(appName)
    if (appId != "") s"$getKillCmd $appId" ! else 0
  }

}

trait YarnAppMonitor extends Logging {
  val SYS_NAME = "yarn-application-monitor"
  val EN_NAME = "yarn-app-monitor-ser"
  var host = Utils.localHostName()
  var port = 23456
  var propertiesFile: String = _
  var sparkConf: SparkConf = _
  val className = "YarnAppMonitor"

  private lazy val properties: HashMap[String, String] = {
    val p = new HashMap[String, String]()
    Option(propertiesFile).foreach(fileName => {
      val proper = Utils.getPropertiesFromFile(fileName)
      proper.foreach { case (k, v) =>
        p(k) = v
      }
    })
    p
  }


  def main(args: Array[String]): Unit = {
    parse(args.toList)
    properties.foreach { case (k, v) =>
      System.setProperty(k, v)
    }
    sparkConf = new SparkConf()
    handle()
  }

  def handle(): Unit = {}

  @tailrec
  private def parse(args: List[String]): Unit = args match {
    case ("--host" | "-h") :: value :: tail =>
      Utils.checkHost(value, "Please use hostname " + value)
      host = value
      parse(tail)

    case ("--ip" | "-i") :: value :: tail =>
      Utils.checkHost(value, "Please use hostname " + value)
      host = value
      parse(tail)

    case ("--port" | "-p") :: IntParam(value) :: tail =>
      port = value
      parse(tail)

    case ("--properties-file") :: value :: tail =>
      propertiesFile = value
      parse(tail)

    case ("--help" | "-H") :: tail => printUsageAndExit(0)
    case Nil => //No-op
    case _ => printUsageAndExit(1)

  }

  private def printUsageAndExit(exitCode: Int): Unit = {
    System.err.println(
      s"Usage: $className [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i HOST, --ip HOST     Hostname to listen on (default: localHostName) \n" +
        "  -h HOST, --host HOST   Hostname to listen on\n" +
        "  -p PORT, --port PORT   Port to listen on (default: 23456)\n" +
        "  --properties-file FILE Path to a custom Spark properties file.\n" +
        "                         Default is conf/spark-defaults.conf.\n" +
        "  -H, --help             Print help info.")
    System.exit(exitCode)
  }
}

object YarnAppMonitorSer extends YarnAppMonitor {
  override val className: String = "YarnAppMonitorSer"

  override def handle(): Unit = {
    val securityManager = new SecurityManager(sparkConf)
    val rpcEnv = RpcEnv.create(SYS_NAME, host, port, sparkConf, securityManager)
    rpcEnv.setupEndpoint(EN_NAME, new YarnAppMonitorSer(rpcEnv, sparkConf))
    rpcEnv.awaitTermination()
  }
}

object YarnAppMonitorCli extends YarnAppMonitor {
  override val className: String = "YarnAppMonitorCli"

  def createYarnAppMonitorRef(sparkConf: SparkConf, host: String, port: Int): RpcEndpointRef = {
    val securityManager = new SecurityManager(sparkConf)
    val rpcEnv = RpcEnv.create(SYS_NAME, host, host, port, sparkConf, securityManager, true)
    rpcEnv.setupEndpointRef(RpcAddress(host, port), EN_NAME)

  }

  override def handle(): Unit = {
    val rpcEnvRef = createYarnAppMonitorRef(sparkConf, host, port)
    val timeoutProper = "spark.application.monitor.timeout"

    val request = sparkConf.get("spark.application.monitor.request", "YarnAppRequest")
    val appName = sparkConf.get("spark.application.monitor.name", "")
    val appConf = sparkConf.get("spark.application.monitor.config", "")
    val timeout = sparkConf.getLong(timeoutProper, 30000L)
    val yarnAppReq = request match {
      case "YarnAppRequest" => YarnAppRequest(appName)
      case "YarnAppMonitorRequest" => YarnAppMonitorRequest(appName, appConf)
      case "YarnAppCancelRequest" => YarnAppCancelRequest(appName)
      case "YarnAppStartRequest" => YarnAppStartRequest(appName, appConf)
      case "YarnAppStopRequest" => YarnAppStopRequest(appName)
      case _ => YarnAppRequest(appName)
    }

    val reqType = yarnAppReq match {
      case YarnAppStopRequest(name) => 1
      case YarnAppStartRequest(name, conf) => 1
      case _ => 0
    }

    if (reqType == 1) {
      rpcEnvRef.send(yarnAppReq)
      System.exit(0)
    }

    val rpcTimeout = new RpcTimeout(FiniteDuration(timeout, TimeUnit.MILLISECONDS), timeoutProper)
    val res = rpcEnvRef.askSync[YarnAppResponse](yarnAppReq, rpcTimeout)
    logInfo(res.info)
    System.exit(res.code)
  }
}

trait YarnAppReq

trait YarnAppRes

case class YarnAppRequest(appName: String) extends YarnAppReq

case class YarnAppMonitorRequest(appName: String, conf: String) extends YarnAppReq

case class YarnAppStartRequest(appName: String, conf: String) extends YarnAppReq

case class YarnAppStopRequest(appName: String) extends YarnAppReq

case class YarnAppCancelRequest(appName: String) extends YarnAppReq

case class YarnAppResponse(info: String, code: Int) extends YarnAppRes

