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
package com.streamxhub.flink.core.util

import org.apache.flink.streaming.api.functions.timestamps.{AscendingTimestampExtractor, BoundedOutOfOrdernessTimestampExtractor}
import org.apache.flink.streaming.api.functions.{AssignerWithPeriodicWatermarks, AssignerWithPunctuatedWatermarks}
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time


object WatermarkUtils {

  /**
   * 基于最大延迟时间的Watermark生成
   * @param fun
   * @param maxOutOfOrderness
   * @tparam T
   * @return
   */
  def boundedOutOfOrdernessWatermark[T](fun: T => Long)(implicit maxOutOfOrderness: Time): AssignerWithPeriodicWatermarks[T] = {
    new BoundedOutOfOrdernessTimestampExtractor[T](maxOutOfOrderness) {
      override def extractTimestamp(element: T): Long = fun(element)
    }
  }

  /**
   * 基于最大延迟时间的Watermark生成,直接用系统时间戳做比较
   * @param fun
   * @param maxTimeLag
   * @tparam T
   * @return
   */
  def timeLagWatermarkWatermark[T](fun: T => Long)(implicit maxTimeLag: Long): AssignerWithPeriodicWatermarks[T] = {
    new AssignerWithPeriodicWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long =  fun(element)
      override def getCurrentWatermark: Watermark = new Watermark(System.currentTimeMillis() - maxTimeLag)
    }
  }

  def punctuatedWatermark[T](implicit fun: T => Long, f1: T => Boolean): AssignerWithPunctuatedWatermarks[T] = {
    new AssignerWithPunctuatedWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = fun(element)
      override def checkAndGetNextWatermark(lastElement: T, extractedTimestamp: Long): Watermark = {
        if (f1(lastElement)) new Watermark(extractedTimestamp) else null
      }
    }
  }

  def ascendingTimestamp[T](fun: T => Long): AscendingTimestampExtractor[T] = {
    new AscendingTimestampExtractor[T] {
      def extractAscendingTimestamp(element: T): Long = fun(element)
    }
  }



}