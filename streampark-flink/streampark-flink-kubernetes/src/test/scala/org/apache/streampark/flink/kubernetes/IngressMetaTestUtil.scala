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

import org.json4s.{DefaultFormats, JArray, JsonAST}
import org.json4s.jackson.JsonMethods.parse

import scala.util.{Failure, Success, Try}

class IngressMetaTestUtil {

  case class IngressMeta(
      addresses: List[String],
      port: Integer,
      protocol: String,
      serviceName: String,
      ingressName: String,
      hostname: String,
      path: String,
      allNodes: Boolean)

  object IngressMeta {

    @transient implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

    def as(json: String): Option[List[IngressMeta]] = {
      Try(parse(json)) match {
        case Success(ok) =>
          ok match {
            case JArray(arr) =>
              val list = arr.map(
                x => {
                  IngressMeta(
                    addresses =
                      (x \ "addresses").extractOpt[List[String]].getOrElse(List.empty[String]),
                    port = (x \ "port").extractOpt[Integer].getOrElse(0),
                    protocol = (x \ "protocol").extractOpt[String].orNull,
                    serviceName = (x \ "serviceName").extractOpt[String].orNull,
                    ingressName = (x \ "ingressName").extractOpt[String].orNull,
                    hostname = (x \ "hostname").extractOpt[String].orNull,
                    path = (x \ "path").extractOpt[String].orNull,
                    allNodes = (x \ "allNodes").extractOpt[Boolean].getOrElse(false)
                  )
                })
              Some(list)
            case _ => None
          }
        case Failure(_) => None
      }
    }
  }

}
