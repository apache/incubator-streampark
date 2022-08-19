/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.spark.core

import com.streamxhub.streamx.common.conf.ConfigConst._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.annotation.meta.getter

/**
 * <b><code>SparkBatch</code></b>
 * <p/>
 * Spark 流处理 入口封装
 * <p/>
 * <b>Creation Time:</b> 2022/8/8 20:44.
 *
 * @author gn
 * @since streamx ${PROJECT_VERSION}
 */
trait SparkStreaming extends Spark {

  @(transient@getter)
  protected lazy val context: StreamingContext = {

    /**
     * 构建 StreamingContext
     *
     * @return
     */
    def _context(): StreamingContext = {
      // 时间间隔
      val duration = sparkConf.get(KEY_SPARK_BATCH_DURATION).toInt
      new StreamingContext(sparkSession.sparkContext, Seconds(duration))
    }

    checkpoint match {
      case "" => _context()
      case checkpointPath =>
        val tmpContext = StreamingContext.getOrCreate(checkpointPath, _context, createOnError = createOnError)
        tmpContext.checkpoint(checkpointPath)
        tmpContext
    }
  }

  /**
   * 启动
   */
  final override def start(): Unit = {
    context.start()
    context.awaitTermination()
  }

  /**
   * config 阶段的目的是让开发者可以通过钩子的方式设置更多的参数(约定的配置文件以外的其他参数)
   * 用户设置sparkConf参数,如,spark序列化:
   * conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
   * // 注册要序列化的自定义类型。
   * conf.registerKryoClasses(Array(classOf[User], classOf[Order],...))
   *
   * @param sparkConf
   */
  override def config(sparkConf: SparkConf): Unit = {}

  /**
   * 阶段是在参数都设置完毕了 给开发者提供的一个用于做其他动作的入口, 该阶段是在初始化完成之后在程序启动之前进行
   */
  override def ready(): Unit = {}

  /**
   * destroy 阶段,是程序运行完毕了,在jvm退出之前的最后一个阶段,一般用于收尾的工作
   */
  override def destroy(): Unit = {}

}

