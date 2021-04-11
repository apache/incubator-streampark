package com.streamxhub.streamx.flink.udf

import org.apache.flink.table.functions.ScalarFunction

import java.text.SimpleDateFormat
import java.util.Date

class HashCode(val factor: Int = 12) extends ScalarFunction {
  def eval(s: String): Int = s match {
    case null => 0
    case x => x.hashCode() * factor
  }
}

class DateFormat(val format: String = "yyyy-MM-dd HH:mm:ss") extends ScalarFunction {
  var sf: SimpleDateFormat = _

  def eval(time: Long): String = {
    if (sf == null) {
      sf = new SimpleDateFormat(format)
    }
    sf.format(new Date(time))
  }
}

class Length extends ScalarFunction {
  def eval(str: String): Int = {
    str match {
      case null => 0
      case x =>  x.length
    }
  }
}
