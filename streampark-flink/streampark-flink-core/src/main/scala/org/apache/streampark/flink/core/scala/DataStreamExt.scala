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

package org.apache.streampark.flink.core.scala

import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.{AssignerWithPeriodicWatermarks, AssignerWithPunctuatedWatermarks, ProcessFunction => ProcFunc}
import org.apache.flink.streaming.api.scala.{DataStream => DStream, _}
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.runtime.operators.util.{AssignerWithPeriodicWatermarksAdapter, AssignerWithPunctuatedWatermarksAdapter}
import org.apache.flink.util.Collector

import java.time.Duration

object DataStreamExt {

  /**
   * @param dataStream
   *   DataStream extension function
   * @tparam T
   */
  class DataStream[T: TypeInformation](dataStream: DStream[T]) {

    def sideOut(fun: (T, ProcFunc[T, T]#Context) => Unit): DStream[T] =
      dataStream.process(new ProcFunc[T, T] {
        override def processElement(
            value: T,
            ctx: ProcFunc[T, T]#Context,
            out: Collector[T]): Unit = {
          fun(value, ctx)
          out.collect(value)
        }
      })

    def sideGet[R: TypeInformation](sideTag: String): DStream[R] =
      dataStream.getSideOutput(new OutputTag[R](sideTag))

    def boundedOutOfOrdernessWatermark(func: T => Long, duration: Duration): DStream[T] = {
      dataStream.assignTimestampsAndWatermarks(
        WatermarkStrategy
          .forBoundedOutOfOrderness[T](duration)
          .withTimestampAssigner(new SerializableTimestampAssigner[T]() {
            override def extractTimestamp(element: T, recordTimestamp: Long): Long = func(element)
          }))
    }

    def timeLagWatermark(fun: T => Long, maxTimeLag: Time): DStream[T] = {
      val assigner = new AssignerWithPeriodicWatermarks[T] {
        override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = fun(
          element)

        override def getCurrentWatermark: Watermark = new Watermark(
          System.currentTimeMillis() - maxTimeLag.toMilliseconds)
      }
      dataStream.assignTimestampsAndWatermarks(
        WatermarkStrategy.forGenerator[T](
          new AssignerWithPeriodicWatermarksAdapter.Strategy[T](assigner)))
    }

    def punctuatedWatermark(extractTimeFun: T => Long, checkFunc: T => Boolean): DStream[T] = {
      val assigner = new AssignerWithPunctuatedWatermarks[T] {
        override def extractTimestamp(element: T, previousElementTimestamp: Long): Long =
          extractTimeFun(element)

        override def checkAndGetNextWatermark(
            lastElement: T,
            extractedTimestamp: Long): Watermark = {
          if (checkFunc(lastElement)) new Watermark(extractedTimestamp)
          else null
        }
      }
      dataStream.assignTimestampsAndWatermarks(
        WatermarkStrategy.forGenerator[T](
          new AssignerWithPunctuatedWatermarksAdapter.Strategy[T](assigner)))
    }

    /**
     * extension process function, make be called easy
     *
     * @param processFunction
     * @param onTimerFunction
     * @tparam R
     * @return
     */
    def proc[R: TypeInformation](
        processFunction: (T, ProcFunc[T, R]#Context, Collector[R]) => Unit,
        onTimerFunction: (Long, ProcFunc[T, R]#OnTimerContext, Collector[R]) => Unit = null): DStream[R] = {

      dataStream.process(new ProcFunc[T, R] {
        override def processElement(
            value: T,
            ctx: ProcFunc[T, R]#Context,
            out: Collector[R]): Unit = processFunction(value, ctx, out)

        override def onTimer(
            timestamp: Long,
            ctx: ProcFunc[T, R]#OnTimerContext,
            out: Collector[R]): Unit = {
          if (onTimerFunction != null) {
            onTimerFunction(timestamp, ctx, out)
          } else {
            super.onTimer(timestamp, ctx, out)
          }
        }
      })
    }

  }

  /**
   * extension ProcessFunction
   *
   * @param ctx
   * @tparam IN
   * @tparam OUT
   */
  class ProcessFunction[IN, OUT](val ctx: ProcFunc[IN, OUT]#Context) {
    def sideOut[R: TypeInformation](outputTag: String, value: R): Unit = {
      val tag = new OutputTag[R](outputTag)
      ctx.output[R](tag, value)
    }
  }

}
