package com.streamxhub.flink.core.conf

import org.apache.flink.api.java.utils.ParameterTool

import com.streamxhub.flink.core.conf.ConfigConst.KEY_APP_NAME

import scala.collection.JavaConversions._

/**
 * TODO 过滤配置文件中的敏感信息,如数据库的密码等,在flink webUI上显示加密显示,在真正使用的时候获取的是原始的值...
 * 初步实现考虑: 发现key中有敏感的字符,则认为要加密,用 当前任务的yarnName+key的value Base64加密...
 */
class Parameter extends ParameterTool {

  /**
   * Key的末尾包含这些字符认为是敏感信息..需要加密....
   */
  val secret = List("password", "auth")

  private var parameterTool: ParameterTool = _

  private var appName: String = _

  def this(tool: ParameterTool) {
    this()
    this.parameterTool = ParameterTool.fromMap(Map.empty[String,String]).mergeWith(tool)
    this.appName = this.parameterTool.get(KEY_APP_NAME)
    /*
    secret.foreach(s=>{
      this.parameterTool.data.filter(x=>x._1.endsWith(s)).foreach(d=>{
        this.parameterTool.data.update(d._1,clear(d._2))
      })
      this.parameterTool.defaultData.filter(x=>x._1.endsWith(s)).foreach(d=>{
        this.parameterTool.defaultData.update(d._1,clear(d._2))
      })
    })
    */

  }

}

