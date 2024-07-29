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

import org.apache.streampark.common.util.Implicits._

import io.netty.resolver.HostsFileParser

import java.net.InetAddress

import scala.collection.immutable.ListMap

object HostsUtils {

  /**
   * Get hosts info from system, entry of return Map [hostname -> ipv4]. The elements are sorted in
   * reverse order by the length of the hostname.
   */
  def getSortSystemHosts: ListMap[String, String] = {
    val ipMap =
      HostsFileParser.parse.inet4Entries.map(e => e._1 -> e._2.getHostAddress)
    ListMap(ipMap.toSeq.sortWith(_._1.length > _._1.length): _*)
  }

  /** Get hosts info from system, entry of return Map [hostname -> ipv4]. Scala api. */
  def getSystemHosts(excludeLocalHost: Boolean = false): Map[String, String] = {
    var map = HostsFileParser.parse.inet4Entries
      .map(e => e._1 -> e._2.getHostAddress)
      .toMap
    if (excludeLocalHost) {
      val localHostName = InetAddress.getLocalHost.getHostName
      map = map
        .filter(!_._1.equals("localhost"))
        .filter(!_._1.equals(localHostName))
        .filter(!_._2.equals("127.0.0.1"))
    }
    map
  }

  /** Get hosts info from system, entry of return Map [hostname -> ipv4]. Java api. */
  def getSystemHostsAsJava(excludeLocalHost: Boolean): JavaMap[String, String] =
    getSystemHosts(excludeLocalHost)

}
