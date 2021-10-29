/*
 * Copyright (c) 2021 The StreamX Project
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

package com.streamxhub.streamx.common.util

import io.netty.resolver.HostsFileParser

import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._

/**
 * @author Al-assad
 */
object HostsUtils {

  /**
   * Get hosts info from system, entry of return Map [hostanme -> ipv4].
   * The elements are sorted in reverse order by the length of the hostname.
   */
  def getSystemHosts: ListMap[String, String] = {
    val systemHosts = HostsFileParser.parse
    val ipMap = systemHosts.inet4Entries().asScala.map(e => e._1 -> e._2.getHostAddress)
    ListMap(ipMap.toSeq.sortWith(_._1.length > _._1.length): _*)
  }

}
