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

package org.apache.streampark.flink.kubernetes.model

import org.apache.streampark.common.util.Utils

import scala.util.Try

/** Pod template for flink k8s cluster */
case class K8sPodTemplates(
    podTemplate: String = "",
    jmPodTemplate: String = "",
    tmPodTemplate: String = "") {

  def nonEmpty: Boolean = Option(podTemplate).exists(_.trim.nonEmpty) ||
    Option(jmPodTemplate).exists(_.trim.nonEmpty) ||
    Option(tmPodTemplate).exists(_.trim.nonEmpty)

  def isEmpty: Boolean = !nonEmpty

  override def hashCode(): Int = Utils.hashCode(podTemplate, jmPodTemplate, tmPodTemplate)

  override def equals(obj: Any): Boolean = {
    obj match {
      case that: K8sPodTemplates =>
        Try(podTemplate.trim).getOrElse("") == Try(that.podTemplate.trim).getOrElse("") &&
        Try(jmPodTemplate.trim).getOrElse("") == Try(that.jmPodTemplate.trim).getOrElse("") &&
        Try(tmPodTemplate.trim).getOrElse("") == Try(that.tmPodTemplate.trim).getOrElse("")
      case _ => false
    }
  }

}

object K8sPodTemplates {

  def empty: K8sPodTemplates = new K8sPodTemplates()

  def of(podTemplate: String, jmPodTemplate: String, tmPodTemplate: String): K8sPodTemplates =
    K8sPodTemplates(safeGet(podTemplate), safeGet(jmPodTemplate), safeGet(tmPodTemplate))

  private[this] def safeGet(content: String): String = {
    content match {
      case null => ""
      case x if x.trim.isEmpty => ""
      case x => x
    }
  }

}
