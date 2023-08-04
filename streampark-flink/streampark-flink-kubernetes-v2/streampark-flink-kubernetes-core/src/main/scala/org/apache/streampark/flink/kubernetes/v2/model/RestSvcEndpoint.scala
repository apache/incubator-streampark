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

package org.apache.streampark.flink.kubernetes.v2.model

import org.apache.streampark.flink.kubernetes.v2.observer.{reachFlinkRestType, AccessFlinkRestType}

/**
 * Flink rest service endpoint info on kubernetes.
 *
 * @param namespace k8s resource namespace
 * @param name      k8s resource name
 * @param port      Flink rest service port
 * @param clusterIP fFink rest server clusterIP
 */
case class RestSvcEndpoint(namespace: String, name: String, port: Int, clusterIP: String) {

  /** k8s dns for flink rest service. */
  lazy val dns: String = s"$name.$namespace"

  lazy val dnsRest: String = s"http://$dns:$port"

  lazy val ipRest: String = s"http://$clusterIP:$port"

  /** Choose rest api http address according to [[reachFlinkRestType]]. */
  def chooseRest: String = reachFlinkRestType match {
    case AccessFlinkRestType.DNS => dnsRest
    case AccessFlinkRestType.IP  => ipRest
  }

  /** Choose rest api host according to [[reachFlinkRestType]]. */
  def chooseHost: String = reachFlinkRestType match {
    case AccessFlinkRestType.DNS => dns
    case AccessFlinkRestType.IP  => clusterIP
  }

}
