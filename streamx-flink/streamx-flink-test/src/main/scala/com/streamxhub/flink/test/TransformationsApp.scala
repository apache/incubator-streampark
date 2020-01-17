package com.streamxhub.flink.test


import com.streamxhub.flink.core.{DataSetContext, FlinkDataSet}
import org.apache.flink.api.common.accumulators.LongCounter
import org.apache.flink.api.common.functions.{GroupCombineFunction, GroupReduceFunction, RichMapFunction}
import org.apache.flink.api.common.operators.Order
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector

import scala.collection.JavaConversions._


/**
 * 学习flink的各类算子的流转操作......
 */
object TransformationsApp extends FlinkDataSet {

  override def handler(context: DataSetContext): Unit = {

    /**
     * https://ci.apache.org/projects/flink/flink-docs-release-1.9/dev/batch/dataset_transformations.html
     */
    val stream = context.env.readTextFile("data/in/num.txt")

    /**
     *
     * map FlatMap
     *
     * https://ci.apache.org/projects/flink/flink-docs-release-1.9/dev/batch/dataset_transformations.html
     */
    println(" map & flatMap)--------------------------------------")
    val dataSet = stream.flatMap(_.split("\\s+")).map(_.toInt).map(x => 1 -> x)
    dataSet.max(1).print()


    /**
     * MapPartition
     * https://ci.apache.org/projects/flink/flink-docs-release-1.9/dev/batch/dataset_transformations.html#mappartition
     */
    println(" MapPartition)--------------------------------------")
    dataSet.mapPartition { in => Some(in.size) }.print()

    /**
     * Filter
     * https://ci.apache.org/projects/flink/flink-docs-release-1.9/dev/batch/dataset_transformations.html#filter
     */
    println(" Filter)--------------------------------------")
    dataSet.filter(_._2 > 10).print()


    /**
     * transformations-on-grouped-datase
     * https://ci.apache.org/projects/flink/flink-docs-release-1.9/dev/batch/dataset_transformations.html#transformations-on-grouped-dataset
     */
    println(" Reduce on DataSet Grouped by Key Expression --------------------------------------")
    case class WC(word: String, count: Int) {
      override def toString: String = s"word:${word},count:${count}"
    }

    val words: DataSet[WC] = context.env.readTextFile("data/in/wordcount.txt")
      .flatMap(_.split("\\s+"))
      .map(x => WC(x, 1))

    /**
     * 直接写对应bean中的字段名
     */
    val wc1 = words.groupBy("word").reduce {
      (w1, w2) => WC(w1.word, w1.count + w2.count)
    }
    /**
     * KeySelector Function
     */
    val wc2 = words.groupBy(_.word).reduce {
      (w1, w2) => WC(w1.word, w1.count + w2.count)
    }
    /**
     * 下标模式
     */
    val wc3 = words.groupBy(0).reduce {
      (w1, w2) => WC(w1.word, w1.count + w2.count)
    }
    println(wc3)

    /**
     * Reduce on DataSet Grouped by Field Position Keys (Tuple DataSets only)
     * 元祖类型的数据,根据数据的字段位置进行分组(必须是元祖类型的数据)
     * https://ci.apache.org/projects/flink/flink-docs-release-1.9/dev/batch/dataset_transformations.html#reduce-on-dataset-grouped-by-field-position-keys-tuple-datasets-only
     */
    val tuples: DataSet[(String, Int, Double)] = context.env.fromCollection(
      List(
        ("a", 1, 1.001),
        ("a", 1, 1.002),
        ("b", 2, 1.002),
        ("b", 2, 1.004),
        ("b", 3, 1.004)
      )
    )
    // group on the first and second Tuple field
    //根据元祖的第一和第二个字段分组
    tuples.groupBy(0, 1).reduce((a, b) => (a._1, a._2, a._3 + b._3)).print()

    /**
     * Reduce on DataSet grouped by Case Class Fields
     * 根据case class的字段进行分组,reduce
     */
    case class MyClass(val a: String, b: Int, c: Double)
    val tuples2: DataSet[MyClass] = context.env.fromCollection(List(
      MyClass("a", 1, 1.001),
      MyClass("a", 1, 1.002),
      MyClass("b", 2, 1.002),
      MyClass("b", 2, 1.004),
      MyClass("b", 3, 1.004)
    ))
    // group on the first and second field
    tuples2.groupBy("a", "b").reduce((a, b) => MyClass(a.a, a.b, a.c + b.c)).print()


    /**
     *
     *
     */
    println(" GroupReduce on Grouped DataSet)--------------------------------------")

    val input: DataSet[(Int, String)] = context.env.fromCollection(List(
      1 -> "g",
      1 -> "x",
      1 -> "i",
      1 -> "a",
      2 -> "b",
      2 -> "c",
      2 -> "q",
      2 -> "a",
      3 -> "c",
      3 -> "n"
    ))

    input.groupBy(0).reduceGroup {
      (in, out: Collector[(Int, String)]) =>
        //out.collect是一个方法,将每次in里的循环的元素作用到Collector的collect方法上
        in.foreach(out.collect)
    }.print()

    /**
     * 将同一个组的元素重新组成一个List返回...
     */
    input.groupBy(0).reduceGroup(a => {
      a.map(_._2).toList
    }).print()

    /**
     * 将同一个组的元素,重新组成一个有序的List返回,desc...
     */
    input.groupBy(0).sortGroup(1, Order.ASCENDING).reduceGroup {
      (in, out: Collector[(Int, String)]) =>
        var prev: (Int, String) = null
        for (t <- in) {
          if (prev == null || prev != t)
            out.collect(t)
          prev = t
        }
    }.print()

    //将同一个组的元素,重新组成一个有序的List返回
    input.groupBy(0).sortGroup(1, Order.ASCENDING).reduceGroup(a => {
      a.map(_._2).toList
    }).print()

    input.groupBy(0).reduceGroup(a => {
      a.map(_._2).toList.sortWith((a, b) => a.compareTo(b) <= 0)
    }).printOnTaskManager("reduceGroup")


    /**
     * Combinable GroupReduceFunctions
     */
    println(" Combinable GroupReduceFunctions)--------------------------------------")
    class MyCombinableGroupReducer extends GroupReduceFunction[(String, Int), String] with GroupCombineFunction[(String, Int), (String, Int)] {
      override def reduce(in: java.lang.Iterable[(String, Int)], out: Collector[String]): Unit = {
        val r: (String, Int) =
          in.iterator().reduce((a, b) => (a._1, a._2 + b._2))
        // concat key and sum and emit
        out.collect(r._1 + "-" + r._2)
      }

      override def combine(in: java.lang.Iterable[(String, Int)],
                           out: Collector[(String, Int)]): Unit = {
        val r: (String, Int) =
          in.iterator.reduce((a, b) => (a._1, a._2 + b._2))
        // emit tuple with key and sum
        out.collect(r)
      }
    }
    val input2: DataSet[(String, Int)] = context.env.fromCollection(List(
      "a" -> 12,
      "a" -> 16,
      "a" -> 33,
      "b" -> 15
    ))
    input2.groupBy(0).reduceGroup(new MyCombinableGroupReducer).print()


    //4) groupBy & first-N
    println(" groupBy & firstN)--------------------------------------")
    val listData = List(
      1 -> "Hadoop",
      1 -> "HDFS",
      1 -> "HBase",
      1 -> "Spark",
      1 -> "Flink",
      2 -> "J2SE",
      2 -> "spring",
      3 -> "Linux",
      3 -> "c++",
      4 -> "VUE",
      4 -> "jQuery",
      4 -> "react"
    )
    //分组取指定条数据
    val groupData = context.env.fromCollection(listData)

    groupData.groupBy(0).first(2).print()

    println(" groupBy & sortGroup & firstN --------------------------------------")
    //分组排序
    groupData.groupBy(0).sortGroup(1, Order.ASCENDING).first(2).print()


    /**
     *
     * counter...
     *
     */

    stream.map(new RichMapFunction[String, String] {
      //1)定义计数器
      val counter = new LongCounter()

      override def open(parameters: Configuration): Unit = {
        //注册计数器
        getRuntimeContext.addAccumulator("counter", counter)
      }

      override def map(value: String): String = {
        //递增计数器
        counter.add(1L)
        value
      }
    }).print()

    /**
     * GroupCombine on a Grouped DataSet
     */
    println(" GroupCombine on a Grouped DataSet --------------------------------------")
    val input4 = context.env.readTextFile("data/in/wordcount.txt").flatMap(_.split("\\s+")).map(Tuple1(_))
    val combinedWords: DataSet[(String, Int)] = input4
      .groupBy(0)
      .combineGroup {
        (x, out: Collector[(String, Int)]) =>
          var key: String = null
          var count = 0
          for (word <- x) {
            key = word._1
            count += 1
          }
          out.collect((key, count))
      }

    combinedWords
      .groupBy(0)
      .reduceGroup {
        (words, out: Collector[(String, Int)]) =>
          var key: String = null
          var sum = 0
          for ((word, count) <- words) {
            key = word
            sum += count
          }
          out.collect((key, sum))
      }.print()

  }

}
