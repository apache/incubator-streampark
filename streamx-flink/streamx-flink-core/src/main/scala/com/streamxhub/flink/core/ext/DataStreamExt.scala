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

import java.time.Duration

import com.streamxhub.flink.core.sink.scala.EchoSink
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions._
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.runtime.operators.util.{AssignerWithPeriodicWatermarksAdapter, AssignerWithPunctuatedWatermarksAdapter}
import org.apache.flink.util.Collector

/**
 *
 * @param dataStream DataStream 扩展方法.
 * @tparam T
 */
class DataStreamExt[T: TypeInformation](val dataStream: DataStream[T]) {

  /**
   *
   * @param fun
   * @return
   */
  def sideOut(fun: (T, ProcessFunction[T, T]#Context) => Unit): DataStream[T] = dataStream.process(new ProcessFunction[T, T] {
    override def processElement(value: T, ctx: ProcessFunction[T, T]#Context, out: Collector[T]): Unit = {
      fun(value, ctx)
      out.collect(value)
    }
  })

  def sideGet[R: TypeInformation](sideTag: String): DataStream[R] = dataStream.getSideOutput(new OutputTag[R](sideTag))

  /**
   * ¬
   * 两阶段精准一次的print...
   *
   * @param sinkIdentifier
   */
  def echo(sinkIdentifier: String = null): Unit = EchoSink(dataStream, sinkIdentifier)

  /**
   * 基于最大延迟时间的Watermark生成
   *
   * @return
   * */

  def boundedOutOfOrdernessWatermark(func: T => Long, duration: Duration): DataStream[T] = {
    dataStream.assignTimestampsAndWatermarks(WatermarkStrategy.forBoundedOutOfOrderness[T](duration).withTimestampAssigner(new SerializableTimestampAssigner[T]() {
      override def extractTimestamp(element: T, recordTimestamp: Long): Long = func(element)
    }))
  }

  /**
   * 基于最大延迟时间的Watermark生成,直接用系统时间戳做比较
   *
   * @param fun
   * @param maxTimeLag
   * @return
   */
  def timeLagWatermarkWatermark(fun: T => Long, maxTimeLag: Time): DataStream[T] = {
    val assigner = new AssignerWithPeriodicWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = fun(element)

      override def getCurrentWatermark: Watermark = new Watermark(System.currentTimeMillis() - maxTimeLag.toMilliseconds)
    }
    dataStream.assignTimestampsAndWatermarks(WatermarkStrategy.forGenerator[T](new AssignerWithPeriodicWatermarksAdapter.Strategy[T](assigner)))
  }

  def punctuatedWatermark(extractTimeFun: T => Long, checkFunc: T => Boolean): DataStream[T] = {
    val assigner = new AssignerWithPunctuatedWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = extractTimeFun(element)

      override def checkAndGetNextWatermark(lastElement: T, extractedTimestamp: Long): Watermark = {
        if (checkFunc(lastElement)) new Watermark(extractedTimestamp) else null
      }
    }
    dataStream.assignTimestampsAndWatermarks(WatermarkStrategy.forGenerator[T](new AssignerWithPunctuatedWatermarksAdapter.Strategy[T](assigner)))
  }

}
