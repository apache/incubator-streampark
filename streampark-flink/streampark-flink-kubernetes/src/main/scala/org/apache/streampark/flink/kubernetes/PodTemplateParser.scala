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

package org.apache.streampark.flink.kubernetes

import org.apache.streampark.common.util.Implicits._

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.yaml.snakeyaml.Yaml

import java.util

import scala.util.Try
import scala.util.control.Breaks.{break, breakable}

object PodTemplateParser {

  val POD_TEMPLATE_INIT_CONTENT: String =
    """apiVersion: v1
      |kind: Pod
      |metadata:
      |  name: pod-template
      |""".stripMargin

  /** Get init content of pod template */
  def getInitPodTemplateContent: String =
    PodTemplateParser.POD_TEMPLATE_INIT_CONTENT.concat("spec:\n")

  /**
   * Complementary initialization pod templates
   *
   * @param podTemplateContent
   *   original pod template
   * @return
   *   complemented pod template
   */
  def completeInitPodTemplate(podTemplateContent: String): String = {
    if (podTemplateContent == null || podTemplateContent.trim.isEmpty) {
      return POD_TEMPLATE_INIT_CONTENT
    }
    val yaml = new Yaml
    val root = yaml.load(podTemplateContent).asInstanceOf[JavaMap[String, Any]]

    val res = new util.LinkedHashMap[String, Any] {
      put("apiVersion", root.getOrDefault("apiVersion", "v1"))
      put("kind", root.getOrDefault("kind", "Pod"))
      put(
        "metadata",
        root.getOrDefault(
          "metadata", {
            new util.LinkedHashMap[String, Any] {
              put("name", "pod-template")
            }
          }))
    }

    if (root.containsKey("spec")
      && Try(!root.get("spec").asInstanceOf[JavaMap[String, Any]].isEmpty)
        .getOrElse(false)) {
      res.put("spec", root.get("spec"))
    }
    yaml.dumpAsMap(res)
  }

  /**
   * Add or Merge host alias spec into pod template. When parser pod template error, it would return
   * the origin content.
   *
   * @param hosts
   *   hosts info [hostname, ip]
   * @param podTemplateContent
   *   pod template content
   * @return
   *   pod template content
   */
  def completeHostAliasSpec(hosts: JavaMap[String, String], podTemplateContent: String): String = {
    if (hosts.isEmpty) return podTemplateContent
    try {
      val content = completeInitPodTemplate(podTemplateContent)
      // convert hosts map to host alias
      val hostAlias = covertHostsMapToHostAliasNode(hosts)
      // parse yaml
      val yaml = new Yaml
      val root = yaml.load(content).asInstanceOf[JavaMap[String, Any]]
      // no exist spec
      if (!root.containsKey("spec")) {
        val spec = new util.LinkedHashMap[String, Any]()
        spec.put("hostAliases", hostAlias)
        root.put("spec", spec)
        return yaml.dumpAsMap(root)
      }
      // replace spec.hostAliases
      val spec = root.get("spec").asInstanceOf[JavaMap[String, Any]]
      spec.put("hostAliases", hostAlias)
      yaml.dumpAsMap(root)
    } catch {
      case _: Throwable => podTemplateContent
    }
  }

  /** convert hosts map to host alias */
  private[this] def covertHostsMapToHostAliasNode(
      hosts: JavaMap[String, String]): util.ArrayList[util.LinkedHashMap[String, Any]] =
    new util.ArrayList(
      hosts
        .map(e => e._1.trim -> e._2.trim)
        .groupBy(_._2)
        .mapValues(_.keys)
        .toList
        .map(e => {
          val map = new util.LinkedHashMap[String, Any]()
          map.put("ip", e._1)
          map.put("hostnames", new util.ArrayList(e._2.toList))
          map
        }))

  /**
   * Extract host-ip map from pod template. When parser pod template error, it would return empty
   * Map.
   *
   * @param podTemplateContent
   *   pod template content
   * @return
   *   hostname -> ipv4
   */
  def extractHostAliasMap(podTemplateContent: String): JavaMap[String, String] = {
    val hosts = new util.LinkedHashMap[String, String](0)
    if (podTemplateContent == null || podTemplateContent.isEmpty) {
      return hosts
    }
    try {
      val yaml = new Yaml
      val root =
        yaml.load(podTemplateContent).asInstanceOf[JavaMap[String, Any]]
      if (!root.containsKey("spec")) {
        return hosts
      }
      val spec = root.get("spec").asInstanceOf[JavaMap[String, Any]]
      if (!spec.containsKey("hostAliases")) {
        return hosts
      }
      val hostAliases =
        spec.get("hostAliases").asInstanceOf[JavaList[JavaMap[String, Any]]]
      if (CollectionUtils.isEmpty(hostAliases)) {
        return hosts
      }
      for (hostAlias <- hostAliases) {
        breakable {
          if (!hostAlias.containsKey("ip") && !hostAlias.containsKey("hostnames")) break
          val ip = hostAlias.get("ip").asInstanceOf[String]
          if (StringUtils.isBlank(ip)) break
          val hostnames =
            hostAlias.get("hostnames").asInstanceOf[JavaList[String]]
          hostnames
            .filter(StringUtils.isNotBlank(_))
            .foreach(hosts.put(_, ip))
        }
      }
    } catch {
      case _: Throwable => return new util.LinkedHashMap[String, String](0)
    }
    hosts
  }

  /**
   * Preview HostAlias pod template content
   *
   * @param hosts
   *   hostname -> ipv4
   * @return
   *   pod template content
   */
  def previewHostAliasSpec(hosts: JavaMap[String, String]): String = {
    val hostAlias = covertHostsMapToHostAliasNode(hosts)
    val root = new util.LinkedHashMap[String, Any]()
    root.put("hostAliases", hostAlias)
    val yaml = new Yaml
    yaml.dumpAsMap(root)
  }

}
