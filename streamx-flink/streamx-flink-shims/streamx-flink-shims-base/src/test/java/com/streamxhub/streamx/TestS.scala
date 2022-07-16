/*
a
 */
package com.streamxhub.streamx

import org.junit.Test

/**
 * -
 *
 * @author ziqiang.wang
 * @date 2022-07-16 18:05
 * */
// scalastyle:off println
class TestS {

  @Test
  def test1(): Unit = {
    val array = new Array[String](1)
    array(0) = "a"
    println(array.head)
  }
}
