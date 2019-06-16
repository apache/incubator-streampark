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

package com.streamxhub.spark.monitor.support.actor

import MySQLActor._
import com.streamxhub.spark.monitor.support.mysql.MySQLClient

/**
  * Created by benjobs on 2019/01/09.
  *
  */

class MySQLActor extends BaseActor {
  override def receive: Receive = {
    case Select(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive Select($db),$sql")
      val result = MySQLClient.select(sql, db)
      currentSender ! result
    case SelectOne(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive SelectOne($db),$sql")
      val result = MySQLClient.selectOne(sql, db)
      currentSender ! result
    case ExecuteUpdate(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive ExecuteUpdate($db),$sql")
      val result = MySQLClient.executeUpdate(sql, db)
      currentSender ! result
    case ExecuteBatch(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive ExecuteBatch($db),$sql")
      val result = MySQLClient.executeBatch(sql, db)
      currentSender ! result
  }
}

object MySQLActor {

  case class Select(sql: String, db: String = "")

  case class SelectOne(sql: String, db: String = "")

  case class ExecuteUpdate(sql: String, db: String = "")

  case class ExecuteBatch(sql: Iterable[String], db: String = "")

}
