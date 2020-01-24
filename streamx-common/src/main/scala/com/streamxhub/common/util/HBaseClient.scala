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
package com.streamxhub.common.util

import java.util.Properties

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}
import org.apache.hadoop.security.UserGroupInformation
import com.streamxhub.common.conf.ConfigConst._

import scala.collection.JavaConversions._

/**
 * @author benjobs
 */
class HBaseClient(fun: () => Connection) extends Serializable {
  lazy val connection: Connection = fun()
}

object HBaseClient {
  val conf: Configuration = HBaseConfiguration.create

  def apply(prop: Properties): HBaseClient = {
    val user = prop.remove(KEY_HBASE_AUTH_USER)
    prop.foreach(x => conf.set(x._1, x._2))
    val fun = () => {
      if (user != null) {
        UserGroupInformation.setConfiguration(conf)
        val remoteUser: UserGroupInformation = UserGroupInformation.createRemoteUser(user.toString)
        UserGroupInformation.setLoginUser(remoteUser)
      }
      val connection = ConnectionFactory.createConnection(conf)
      sys.addShutdownHook {
        connection.close()
      }
      connection
    }
    new HBaseClient(fun)
  }
}
