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

package org.apache.streampark.console.core.utils

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper
import com.baomidou.mybatisplus.core.toolkit.support.SFunction

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

/** MyBatis scala extension. */
object MybatisScalaExt {

  def lambdaQuery[E]: LambdaQueryWrapper[E]   = new LambdaQueryWrapper[E]()
  def lambdaUpdate[E]: LambdaUpdateWrapper[E] = new LambdaUpdateWrapper[E]()

  // noinspection DuplicatedCode
  implicit class LambdaQueryOps[E](wrapper: LambdaQueryWrapper[E]) {
    def typedIn[V](column: E => V, values: Iterable[V]): LambdaQueryWrapper[E] = {
      wrapper.in(new SFunction[E, V] { override def apply(t: E): V = column(t) }, values.asJavaCollection)
    }

    def typedEq[V](column: E => V, value: V): LambdaQueryWrapper[E] = {
      wrapper.eq(new SFunction[E, V] { override def apply(t: E): V = column(t) }, value)
    }
  }

  // noinspection DuplicatedCode
  implicit class LambdaUpdateOps[E](wrapper: LambdaUpdateWrapper[E]) {
    def typedSet[V](column: E => V, value: V): LambdaUpdateWrapper[E] = {
      wrapper.set((e: E) => column(e), value)
    }

    def typedSet[V](cond: Boolean, column: E => V, value: V): LambdaUpdateWrapper[E] = {
      wrapper.set(cond, new SFunction[E, V] { override def apply(t: E): V = column(t) }, value)
    }

    def typedEq[V](column: E => V, value: V): LambdaUpdateWrapper[E] = {
      wrapper.eq(new SFunction[E, V] { override def apply(t: E): V = column(t) }, value)
    }
  }

}
