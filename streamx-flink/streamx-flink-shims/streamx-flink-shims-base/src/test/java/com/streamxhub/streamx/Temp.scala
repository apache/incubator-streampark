/*
a
 */
package com.streamxhub.streamx

/**
 * -
 *
 * @author ziqiang.wang
 * @date 2022-07-08 16:00
 * */
class Temp {

  /**
   * For collect and parse select statement result.
   */
  def select(): Unit = {
    // val result = context.sqlQuery(x.originSql).execute()
    // val schema = result.getResolvedSchema
    // val columnCount = schema.getColumnCount
    // val columnNames = schema.getColumnNames
    //
    // val resultStr = new mutable.StringBuilder
    // resultStr.append("\n查询到以下结果数据：\n\n")
    //
    // separatorAppend(resultStr, columnCount)
    // // Extract header information, such as: |id|name|age|gender|
    // columnNames.forEach((name: String) => {
    //   // Only the first 30 characters are needed, and if not enough, Spaces are used to fill in the right.
    //   val columnName = if (name.length > 30) name.substring(0, 30) else StringUtils.rightPad(name, 30, "")
    //   resultStr.append("|").append(columnName)
    // })
    // resultStr.append("|").append("\n")
    // separatorAppend(resultStr, columnCount)
    //
    // try {
    //   val it = result.collect
    //   try while (it.hasNext) {
    //     val row = it.next
    //     for (i <- 0 until columnCount) {
    //       var value = row.getField(i).toString
    //       if (value.length > 30) value = value.substring(0, 30)
    //       else value = StringUtils.rightPad(value, 30, "")
    //       resultStr.append("|").append(value)
    //     }
    //     resultStr.append("|").append("\n")
    //     separatorAppend(resultStr, columnCount)
    //
    //     logInfo(resultStr.toString())
    //   }
    //   catch {
    //     case e: Exception => logError(ExceptionUtils.stringifyException(e))
    //   } finally if (it != null) it.close()
    // }
    //
    // /**
    //  * Append separator symbol:  +----------+
    //  *
    //  * @param resultStr   To store all result string.
    //  * @param columnCount The count of column.
    //  */
    // def separatorAppend(resultStr: mutable.StringBuilder, columnCount: Int): Unit = {
    //   for (_ <- 0 until columnCount) {
    //     resultStr.append("+------------------------------")
    //   }
    //   resultStr.append("+").append("\n")
    // }
  }

}
