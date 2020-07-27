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
package com.streamxhub.flink.core.ext

import java.lang.reflect.Method

import com.streamxhub.flink.core.sink.EchoSink
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.{AssignerWithPeriodicWatermarks, AssignerWithPunctuatedWatermarks, ProcessFunction}
import org.apache.flink.streaming.api.functions.timestamps.{AscendingTimestampExtractor, BoundedOutOfOrdernessTimestampExtractor}
import org.apache.flink.streaming.api.scala.{OutputTag, DataStream => DStream}
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

/**
 *
 * @param dataStream DataStream 扩展方法.
 * @tparam T
 */
class DataStreamExt[T: TypeInformation](val dataStream: DStream[T]) {

  private[this] val assignerWithPeriodicMethod: Method = dataStream.getClass.getMethod("assignTimestampsAndWatermarks", classOf[AssignerWithPeriodicWatermarks[T]])
  assignerWithPeriodicMethod.setAccessible(true)

  private[this] val assignerWithPunctuatedMethod: Method = dataStream.getClass.getMethod("assignTimestampsAndWatermarks", classOf[AssignerWithPunctuatedWatermarks[T]])
  assignerWithPunctuatedMethod.setAccessible(true)

  /**
   *
   * @param fun
   * @return
   */
  def sideOut(fun: (T, ProcessFunction[T, T]#Context) => Unit): DStream[T] = dataStream.process(new ProcessFunction[T, T] {
    override def processElement(value: T, ctx: ProcessFunction[T, T]#Context, out: Collector[T]): Unit = {
      fun(value, ctx)
      out.collect(value)
    }
  })

  def sideGet[R: TypeInformation](sideTag: String): DStream[R] = dataStream.getSideOutput(new OutputTag[R](sideTag))

  def echo(name: String = null): Unit = EchoSink(dataStream, name)

  /**
   * 基于最大延迟时间的Watermark生成
   *
   * @param fun
   * @param maxOutOfOrderness
   * @return
   **/

  def boundedOutOfOrdernessWatermark(fun: T => Long)(implicit maxOutOfOrderness: Time): DStream[T] = {
    val assigner = new BoundedOutOfOrdernessTimestampExtractor[T](maxOutOfOrderness) {
      override def extractTimestamp(element: T): Long = fun(element)
    }
    assignerWithPeriodicMethod.invoke(dataStream, assigner)
    dataStream.asInstanceOf[DStream[T]]
  }

  /**
   * 基于最大延迟时间的Watermark生成,直接用系统时间戳做比较
   *
   * @param fun
   * @param maxTimeLag
   * @return
   */
  def timeLagWatermarkWatermark(fun: T => Long)(implicit maxTimeLag: Time): DStream[T] = {
    val assigner = new AssignerWithPeriodicWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = fun(element)

      override def getCurrentWatermark: Watermark = new Watermark(System.currentTimeMillis() - maxTimeLag.toMilliseconds)
    }
    assignerWithPeriodicMethod.invoke(dataStream, assigner)
    dataStream.asInstanceOf[DStream[T]]
  }

  def punctuatedWatermark(extractTimeFun: T => Long, checkFun: T => Boolean): DStream[T] = {
    val assigner = new AssignerWithPunctuatedWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = extractTimeFun(element)

      override def checkAndGetNextWatermark(lastElement: T, extractedTimestamp: Long): Watermark = {
        if (checkFun(lastElement)) new Watermark(extractedTimestamp) else null
      }
    }
    assignerWithPunctuatedMethod.invoke(dataStream, assigner)
    dataStream.asInstanceOf[DStream[T]]
  }

  def ascendingTimestampWatermark(fun: T => Long): DStream[T] = {
    val assigner = new AscendingTimestampExtractor[T] {
      def extractAscendingTimestamp(element: T): Long = fun(element)
    }
    assignerWithPeriodicMethod.invoke(dataStream, assigner)
    dataStream.asInstanceOf[DStream[T]]
  }

}
