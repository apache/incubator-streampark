package com.streamxhub.flink.core.util

import org.apache.flink.streaming.api.functions.{AssignerWithPeriodicWatermarks, AssignerWithPunctuatedWatermarks}
import org.apache.flink.streaming.api.watermark.Watermark


object WatermarkUtils {

  def boundedOutOfOrdernessWatermark[T](maxOutOfOrderness: Long)(implicit f: T => Long): AssignerWithPeriodicWatermarks[T] = {
    new AssignerWithPeriodicWatermarks[T] {
      var currentMaxTimestamp: Long = _

      override def getCurrentWatermark: Watermark = new Watermark(currentMaxTimestamp - maxOutOfOrderness)

      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = {
        val timestamp = f(element)
        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp)
        timestamp
      }
    }
  }

  def timeLagWatermark[T](maxTimeLag: Long)(implicit f: T => Long): AssignerWithPeriodicWatermarks[T] = {
    new AssignerWithPeriodicWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = f(element)

      override def getCurrentWatermark: Watermark = new Watermark(System.currentTimeMillis() - maxTimeLag)
    }
  }

  def punctuatedWatermark[T](maxTimeLag: Long)(implicit f: T => Long, f1: T => Boolean): AssignerWithPunctuatedWatermarks[T] = {
    new AssignerWithPunctuatedWatermarks[T] {
      override def extractTimestamp(element: T, previousElementTimestamp: Long): Long = f(element)

      override def checkAndGetNextWatermark(lastElement: T, extractedTimestamp: Long): Watermark = {
        if (f1(lastElement)) new Watermark(extractedTimestamp) else null
      }
    }
  }

}