/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.common.util

import com.streamxhub.streamx.common.conf.{CommonConfig, InternalConfigHolder}
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.fs.CommonConfigurationKeys
import org.apache.hadoop.net.NetUtils
import org.apache.hadoop.yarn.api.records.YarnApplicationState._
import org.apache.hadoop.yarn.api.records._
import org.apache.hadoop.yarn.conf.{HAUtil, YarnConfiguration}
import org.apache.hadoop.yarn.util.{ConverterUtils, RMHAUtils}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.HttpClients

import java.net.InetAddress
import java.security.PrivilegedExceptionAction
import java.util
import java.util.{HashMap => JavaHashMap, List => JavaList}
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.{break, breakable}
import scala.util.{Failure, Success, Try}

object YarnUtils extends Logger {

  private[this] var rmHttpURL: String = _

  lazy val PROXY_YARN_URL = InternalConfigHolder.get[String](CommonConfig.STREAMX_PROXY_YARN_URL)

  /**
   * hadoop.http.authentication.type<br>
   * 获取 yarn http 认证方式.<br> ex: sample, kerberos
   *
   * @return
   */
  lazy val hasYarnHttpKerberosAuth: Boolean = {
    val yarnHttpAuth: String = InternalConfigHolder.get[String](CommonConfig.STREAM_YARN_AUTH)
    "kerberos".equalsIgnoreCase(yarnHttpAuth)
  }

  /**
   *
   * @param appName
   * @return
   */
  def getAppId(appName: String): JavaList[ApplicationId] = {
    val appStates = util.EnumSet.of(RUNNING, ACCEPTED, SUBMITTED)
    val appIds = try {
      HadoopUtils.yarnClient.getApplications(appStates).filter(_.getName == appName).map(_.getApplicationId)
    } catch {
      case e: Exception => e.printStackTrace()
        ArrayBuffer.empty[ApplicationId]
    }
    appIds.toList
  }

  /**
   * 查询 state
   *
   * @param appId
   * @return
   */
  def getState(appId: String): YarnApplicationState = {
    val applicationId = ConverterUtils.toApplicationId(appId)
    val state = try {
      val applicationReport = HadoopUtils.yarnClient.getApplicationReport(applicationId)
      applicationReport.getYarnApplicationState
    } catch {
      case e: Exception => e.printStackTrace()
        null
    }
    state
  }

  /**
   * 判断任务名为appName的任务，是否在yarn中运行，状态为RUNNING
   *
   * @return boolean
   * @param appName
   * @return
   */
  def isContains(appName: String): Boolean = {
    val runningApps = HadoopUtils.yarnClient.getApplications(util.EnumSet.of(RUNNING))
    if (runningApps != null) {
      runningApps.exists(_.getName == appName)
    } else {
      false
    }
  }

  def getRMWebAppProxyURL: String = {
    if (StringUtils.isNotBlank(PROXY_YARN_URL)) PROXY_YARN_URL else getRMWebAppURL()
  }

  /**
   * <pre>
   *
   * @return
   * </pre>
   */
  def getRMWebAppURL(): String = {

    if (rmHttpURL == null) {
      synchronized {
        val conf = HadoopUtils.hadoopConf
        val useHttps = YarnConfiguration.useHttps(conf)
        val (addressPrefix, defaultPort, protocol) = useHttps match {
          case x if x => (YarnConfiguration.RM_WEBAPP_HTTPS_ADDRESS, "8090", "https://")
          case _ => (YarnConfiguration.RM_WEBAPP_ADDRESS, "8088", "http://")
        }

        rmHttpURL = Option(conf.get("yarn.web-proxy.address", null)) match {
          case Some(proxy) => s"$protocol$proxy"
          case _ =>
            val name = if (!HAUtil.isHAEnabled(conf)) addressPrefix else {
              val yarnConf = new YarnConfiguration(conf)
              val activeRMId = {
                Option(RMHAUtils.findActiveRMHAId(yarnConf)) match {
                  case Some(x) =>
                    logInfo("findActiveRMHAId successful")
                    x
                  case None =>
                    //If you don't know why, don't modify it
                    logWarn(s"findActiveRMHAId is null,config yarn.acl.enable:${yarnConf.get("yarn.acl.enable")},now http try it.")
                    // url ==> rmId
                    val idUrlMap = new JavaHashMap[String, String]
                    val rmIds = HAUtil.getRMHAIds(conf)
                    rmIds.foreach(id => {
                      val address = conf.get(HAUtil.addSuffix(addressPrefix, id)) match {
                        case null =>
                          val hostname = conf.get(HAUtil.addSuffix("yarn.resourcemanager.hostname", id))
                          s"$hostname:$defaultPort"
                        case x => x
                      }
                      idUrlMap.put(s"$protocol$address", id)
                    })
                    var rmId: String = null
                    val rpcTimeoutForChecks = yarnConf.getInt(
                      CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_KEY,
                      CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_DEFAULT
                    )
                    breakable(idUrlMap.foreach(x => {
                      //test yarn url
                      val activeUrl = httpTestYarnRMUrl(x._1, rpcTimeoutForChecks)
                      if (activeUrl != null) {
                        rmId = idUrlMap(activeUrl)
                        break
                      }
                    }))
                    rmId
                }
              }
              require(activeRMId != null, "[StreamX] YarnUtils.getRMWebAppURL: can not found yarn active node")
              logInfo(s"current activeRMHAId: $activeRMId")
              val appActiveRMKey = HAUtil.addSuffix(addressPrefix, activeRMId)
              val hostnameActiveRMKey = HAUtil.addSuffix(YarnConfiguration.RM_HOSTNAME, activeRMId)
              if (null == HAUtil.getConfValueForRMInstance(appActiveRMKey, yarnConf) && null != HAUtil.getConfValueForRMInstance(hostnameActiveRMKey, yarnConf)) {
                logInfo(s"Find rm web address by : $hostnameActiveRMKey")
                hostnameActiveRMKey
              } else {
                logInfo(s"Find rm web address by : $appActiveRMKey")
                appActiveRMKey
              }
            }

            val inetSocketAddress = conf.getSocketAddr(name, s"0.0.0.0:$defaultPort", defaultPort.toInt)

            val address = NetUtils.getConnectAddress(inetSocketAddress)

            val buffer = new StringBuilder(protocol)
            val resolved = address.getAddress
            if (resolved != null && !resolved.isAnyLocalAddress && !resolved.isLoopbackAddress) {
              buffer.append(address.getHostName)
            } else {
              Try(InetAddress.getLocalHost.getCanonicalHostName) match {
                case Success(value) => buffer.append(value)
                case _ => buffer.append(address.getHostName)
              }
            }
            buffer
              .append(":")
              .append(address.getPort)
              .toString()
        }
        logInfo(s"yarn resourceManager webapp url:$rmHttpURL")
      }
    }
    rmHttpURL
  }

  private[this] def httpTestYarnRMUrl(url: String, timeout: Int): String = {
    val httpClient = HttpClients.createDefault();
    val context = HttpClientContext.create()
    val httpGet = new HttpGet(url)
    val requestConfig = RequestConfig
      .custom()
      .setSocketTimeout(timeout)
      .setConnectTimeout(timeout)
      .build()
    httpGet.setConfig(requestConfig)
    Try(httpClient.execute(httpGet, context)) match {
      case Success(_) => context.getTargetHost.toString
      case _ => null
    }
  }

  def getYarnAppTrackingUrl(applicationId: ApplicationId): String = HadoopUtils.yarnClient.getApplicationReport(applicationId).getTrackingUrl

  /**
   *
   * @param url url
   * @return
   */
  def restRequest(url: String): String = {
    if (url == null) return null

    def request(url: String): String = {
      val config = RequestConfig.custom.setConnectTimeout(5000).build
      if (hasYarnHttpKerberosAuth) {
        HadoopUtils.getUgi().doAs(new PrivilegedExceptionAction[String] {
          override def run(): String = {
            Try(HttpClientUtils.httpAuthGetRequest(url, config)) match {
              case Success(v) => v
              case Failure(e) =>
                logError("yarnUtils authRestRequest error, detail: ", e)
                null
            }
          }
        })
      } else {
        Try(HttpClientUtils.httpGetRequest(url, config)) match {
          case Success(v) => v
          case Failure(e) =>
            logError("yarnUtils restRequest error, detail: ", e)
            null
        }
      }
    }

    if (url.startsWith("http://") || url.startsWith("https://")) request(url) else {
      request(s"${getRMWebAppURL()}/$url")
    }

  }

}
