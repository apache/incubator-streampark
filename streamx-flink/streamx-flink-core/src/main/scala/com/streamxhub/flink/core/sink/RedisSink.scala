package com.streamxhub.flink.core.sink

import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.redis.{RedisSink => RSink}
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}
import com.streamxhub.flink.core.conf.ConfigConst.SINK_REDIS_PREFIX
import com.streamxhub.flink.core.StreamingContext

import scala.collection.JavaConversions._
import scala.collection.Map
import com.streamxhub.flink.core.conf.ConfigConst._

object RedisSink {
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null): RedisSink = new RedisSink(ctx, overwriteParams, parallelism, name, uid)
}

class RedisSink(@transient ctx: StreamingContext,
                overwriteParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null
               ) extends Sink {

  @Override
  def sink[T](stream: DataStream[T])(implicit mapper: RedisMapper[T]): DataStreamSink[T] = {
    val builder = new FlinkJedisPoolConfig.Builder()
    val config = ctx.parameter.toMap ++ overwriteParams
    config.filter(_._1.startsWith(SINK_REDIS_PREFIX)).filter(_._2.nonEmpty).map(x => x._1.drop(SINK_REDIS_PREFIX.length) -> x._2).map {
      case (KEY_HOST, host) => builder.setHost(host)
      case (KEY_PORT, port) => builder.setPort(port.toInt)
      case (KEY_DB, db) => builder.setDatabase(db.toInt)
      case (KEY_PASSWORD, password) => builder.setPassword(password)
      case _ =>
    }
    val sink = stream.addSink(new RSink[T](builder.build(), mapper))
    afterSink(sink, parallelism, name, uid)
  }

}

case class Mapper[T](cmd: RedisCommand, key: String, k: T => String, v: T => String) extends RedisMapper[T] {
  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(cmd, key)

  override def getKeyFromData(r: T): String = k(r)

  override def getValueFromData(r: T): String = v(r)
}