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

package com.streamxhub.spark.monitor.ext

import scala.collection.{Iterable => ScalaIter, SortedMap}
import scala.reflect.ClassTag

/**
  * Created by guoning on 2018/10/24.
  * 升级版迭代器
  * 支持reduceBuyKey
  *
  * 使用方法
  * import Iterable._
  */
case class Iterable[K: ClassTag, V: ClassTag](iterable: ScalaIter[(K, V)]) {
  def reduceByKey(f: (V, V) => V): Map[K, V] = {
    val reduceByKey = iterable.groupBy(_._1).map {
      case (key, iter) => key -> iter.map(_._2).reduce(f)
    }
    reduceByKey
  }
}

object Iterable {

  implicit def foreach[K: ClassTag, V: ClassTag](iter: ScalaIter[(K, V)]): Iterable[K, V] = Iterable[K, V](iter)

  def main(args: Array[String]): Unit = {
    val arr = List(
      "2018-10-27 02:00:00" -> "a",
      "2018-10-27 02:00:00" -> "a",
      "2018-10-27 01:00:00" -> "a",
      "2018-10-27 04:00:00" -> "a",
      "2018-10-27 10:00:00" -> "b",
      "2018-10-27 00:00:00" -> "b",
      "2018-10-27 09:00:00" -> "b",
      "2018-10-27 07:00:00" -> "c",
      "2018-10-27 08:00:00" -> "c",
      "2018-10-27 05:00:00" -> "c",
      "2018-10-27 03:00:00" -> "c",
      "2018-10-27 11:00:00" -> "d")

    val map = arr.reduceByKey(_ + _)
    val sorted = SortedMap[String, String](map.toSeq: _*)
    for (elem <- sorted) {
      println(elem)
    }
  }

}
