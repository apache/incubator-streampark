package com.streamxhub.streamx.flink.core

import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.proxy.FlinkShimsProxy

object FlinkSqlHelper extends Logger {

  def verifySql(flinkVersion: FlinkVersion, sql: String): SqlError = {
    val error = FlinkShimsProxy.proxy(flinkVersion, (classLoader: ClassLoader) => {
      try {
        val clazz = classLoader.loadClass("com.streamxhub.streamx.flink.core.FlinkSqlValidator")
        val method = clazz.getDeclaredMethod("verifySql", classOf[String])
        method.setAccessible(true)
        val sqlError = method.invoke(null, sql)
        if (sqlError == null) null
        else sqlError.toString
      } catch {
        case e: Throwable => logError("verifySql invocationTargetException", e)
          null
      }
    })
    SqlError.fromString(error)
  }

}
