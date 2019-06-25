package com.streamxhub.spark.monitor.core.ext

import scala.collection.{Iterable => ScalaIter}
import scala.reflect.ClassTag

/**
  * Created by benjobs on 2019/05/05.
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
}
