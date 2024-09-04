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

package org.apache.streampark.common.util

import org.apache.streampark.common.conf.{CommonConfig, InternalConfigHolder}
import org.apache.streampark.common.constants.Constants
import org.apache.streampark.common.util.Implicits._

import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.fs.CommonConfigurationKeys
import org.apache.hadoop.net.NetUtils
import org.apache.hadoop.yarn.api.records._
import org.apache.hadoop.yarn.api.records.YarnApplicationState._
import org.apache.hadoop.yarn.conf.{HAUtil, YarnConfiguration}
import org.apache.hadoop.yarn.util.RMHAUtils
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.core5.util.Timeout

import java.io.IOException
import java.net.InetAddress
import java.security.PrivilegedExceptionAction
import java.util
import java.util.concurrent.TimeUnit

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}
import scala.util.control.Breaks.{break, breakable}

object YarnUtils extends Logger {

  private[this] var rmHttpURL: String = _

  lazy val PROXY_YARN_URL =
    InternalConfigHolder.get[String](CommonConfig.STREAMPARK_PROXY_YARN_URL)

  /**
   * hadoop.http.authentication.type<br> get yarn http authentication mode.<br> ex: simple, kerberos
   *
   * @return
   */
  lazy val hasYarnHttpKerberosAuth: Boolean = {
    val yarnHttpAuth: String =
      InternalConfigHolder.get[String](CommonConfig.STREAMPARK_YARN_AUTH)
    "kerberos".equalsIgnoreCase(yarnHttpAuth)
  }

  lazy val hasYarnHttpSimpleAuth: Boolean = {
    val yarnHttpAuth: String =
      InternalConfigHolder.get[String](CommonConfig.STREAMPARK_YARN_AUTH)
    "simple".equalsIgnoreCase(yarnHttpAuth)
  }

  /**
   * @param appName
   * @return
   */
  def getAppId(appName: String): JavaList[ApplicationId] = {
    val appStates = util.EnumSet.of(RUNNING, ACCEPTED, SUBMITTED)
    val appIds =
      try {
        HadoopUtils.yarnClient
          .getApplications(appStates)
          .filter(_.getName == appName)
          .map(_.getApplicationId)
      } catch {
        case e: Exception =>
          e.printStackTrace()
          ArrayBuffer.empty[ApplicationId]
      }
    appIds.toList
  }

  /**
   * get yarn application state
   *
   * @param appId
   * @return
   */
  def getState(appId: String): YarnApplicationState = {
    val applicationId = ApplicationId.fromString(appId)
    val state =
      try {
        val applicationReport =
          HadoopUtils.yarnClient.getApplicationReport(applicationId)
        applicationReport.getYarnApplicationState
      } catch {
        case e: Exception =>
          e.printStackTrace()
          null
      }
    state
  }

  /**
   * Determine if the task named appName is running in yarn
   *
   * @return
   *   boolean
   * @param appName
   * @return
   */
  def isContains(appName: String): Boolean = {
    val runningApps =
      HadoopUtils.yarnClient.getApplications(util.EnumSet.of(RUNNING))
    if (runningApps != null) {
      runningApps.exists(_.getName == appName)
    } else {
      false
    }
  }

  def getRMWebAppProxyURL: String = {
    if (StringUtils.isNotBlank(PROXY_YARN_URL)) PROXY_YARN_URL
    else getRMWebAppURL()
  }

  def getRMWebAppURL(getLatest: Boolean = false): String = {
    if (rmHttpURL == null || getLatest) {
      synchronized {
        val conf = HadoopUtils.hadoopConf
        val useHttps = YarnConfiguration.useHttps(conf)
        val (addressPrefix, defaultPort, protocol) = useHttps match {
          case x if x =>
            (YarnConfiguration.RM_WEBAPP_HTTPS_ADDRESS, "8090", Constants.HTTPS_SCHEMA)
          case _ =>
            (YarnConfiguration.RM_WEBAPP_ADDRESS, "8088", Constants.HTTP_SCHEMA)
        }

        rmHttpURL = Option(conf.get("yarn.web-proxy.address", null)) match {
          case Some(proxy) => s"$protocol$proxy"
          case _ =>
            val name =
              if (!HAUtil.isHAEnabled(conf)) addressPrefix
              else {
                val yarnConf = new YarnConfiguration(conf)
                val activeRMId = {
                  Option(RMHAUtils.findActiveRMHAId(yarnConf)) match {
                    case Some(x) =>
                      logInfo("'findActiveRMHAId' successful")
                      x
                    case None =>
                      // if you don't know why, don't modify it
                      logWarn(s"'findActiveRMHAId' is null,config yarn.acl.enable:${yarnConf
                          .get("yarn.acl.enable")},now http try it.")
                      // url ==> rmId
                      val idUrlMap = new JavaHashMap[String, String]
                      val rmIds = HAUtil.getRMHAIds(conf)
                      rmIds.foreach(id => {
                        val address = conf.get(HAUtil.addSuffix(addressPrefix, id)) match {
                          case null =>
                            val hostname =
                              conf.get(HAUtil.addSuffix("yarn.resourcemanager.hostname", id))
                            s"$hostname:$defaultPort"
                          case x => x
                        }
                        idUrlMap.put(s"$protocol$address", id)
                      })
                      var rmId: String = null
                      val rpcTimeoutForChecks = yarnConf.getInt(
                        CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_KEY,
                        CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_DEFAULT)
                      breakable(
                        idUrlMap.foreach(x => {
                          // test yarn url
                          val activeUrl =
                            httpTestYarnRMUrl(x._1, rpcTimeoutForChecks)
                          if (activeUrl != null) {
                            rmId = idUrlMap(activeUrl)
                            break
                          }
                        }))
                      rmId
                  }
                }
                require(
                  activeRMId != null,
                  "[StreamPark] YarnUtils.getRMWebAppURL: can not found yarn active node")
                logInfo(s"Current activeRMHAId: $activeRMId")
                val appActiveRMKey = HAUtil.addSuffix(addressPrefix, activeRMId)
                val hostnameActiveRMKey =
                  HAUtil.addSuffix(YarnConfiguration.RM_HOSTNAME, activeRMId)
                if (null == HAUtil.getConfValueForRMInstance(
                    appActiveRMKey,
                    yarnConf) && null != HAUtil.getConfValueForRMInstance(
                    hostnameActiveRMKey,
                    yarnConf)) {
                  logInfo(s"Find rm web address by : $hostnameActiveRMKey")
                  hostnameActiveRMKey
                } else {
                  logInfo(s"Find rm web address by : $appActiveRMKey")
                  appActiveRMKey
                }
              }

            val inetSocketAddress =
              conf.getSocketAddr(name, s"0.0.0.0:$defaultPort", defaultPort.toInt)

            val address = NetUtils.getConnectAddress(inetSocketAddress)

            val buffer = new mutable.StringBuilder(protocol)
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
        logInfo(s"Yarn resourceManager webapp url:$rmHttpURL")
      }
    }
    rmHttpURL
  }

  private[this] def httpTestYarnRMUrl(url: String, timeout: Int): String = {
    val config = RequestConfig
      .custom()
      .setConnectTimeout(timeout, TimeUnit.MILLISECONDS)
      .build()
    HttpClientUtils.httpGetRequest(url, config)
  }

  def getYarnAppTrackingUrl(applicationId: ApplicationId): String =
    HadoopUtils.yarnClient.getApplicationReport(applicationId).getTrackingUrl

  /**
   * @param url
   *   url
   * @return
   */
  @throws[IOException]
  def restRequest(url: String, timeout: Timeout): String = {
    if (url == null) return null
    url match {
      case u if u.matches("^http(|s)://.*") =>
        Try(request(url, timeout)) match {
          case Success(v) => v
          case Failure(e) =>
            if (hasYarnHttpKerberosAuth) {
              throw new IOException(s"yarnUtils authRestRequest error, url: $u, detail: $e")
            } else {
              throw new IOException(s"yarnUtils restRequest error, url: $u, detail: $e")
            }
        }
      case _ =>
        Try(request(s"${getRMWebAppURL()}/$url", timeout)) match {
          case Success(v) => v
          case Failure(_) =>
            Utils.retry[String](5)(request(s"${getRMWebAppURL(true)}/$url", timeout)) match {
              case Success(v) => v
              case Failure(e) =>
                throw new IOException(s"yarnUtils restRequest retry 5 times all failed. detail: $e")
            }
        }
    }
  }

  private[this] def request(reqUrl: String, timeout: Timeout): String = {
    val config = RequestConfig.custom.setConnectTimeout(timeout).build
    if (hasYarnHttpKerberosAuth) {
      HadoopUtils
        .getUgi()
        .doAs(new PrivilegedExceptionAction[String] {
          override def run(): String = {
            HttpClientUtils.httpAuthGetRequest(reqUrl, config)
          }
        })
    } else {
      val url =
        if (!hasYarnHttpSimpleAuth) reqUrl
        else s"$reqUrl?user.name=${HadoopConfigUtils.hadoopUserName}"
      HttpClientUtils.httpGetRequest(url, config)
    }
  }

}
