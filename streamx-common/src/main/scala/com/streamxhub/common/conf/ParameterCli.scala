package com.streamxhub.common.conf


import com.streamxhub.common.util.PropertiesUtils
import scala.collection.JavaConversions._


object ParameterCli {


  private[conf] val resourcePrefix = "flink.deployment.resource."
  private[conf] val dynamicPrefix = "flink.deployment.dynamic."

  def main(args: Array[String]): Unit = {
    val action = args(0)
    val conf = args(1)
    val map = if(conf.endsWith(".properties")) {
      PropertiesUtils.fromPropertiesFile(conf)
    }else {
      PropertiesUtils.fromYamlFile(conf)
    }
    val buffer = new StringBuilder
    action match {
      case "--resource" =>
        map.filter(x=>x._1.startsWith(resourcePrefix) && x._2.nonEmpty ).foreach(x=> buffer.append(s" --${x._1.replace(resourcePrefix,"")} ${x._2}"))
        System.out.println(buffer.toString.trim)
      case "--dynamic" =>
        map.filter(x=>x._1.startsWith(dynamicPrefix) && x._2.nonEmpty ).foreach(x=> buffer.append(s" -yD ${x._1.replace(dynamicPrefix,"")}=${x._2}"))
        System.out.println(buffer.toString.trim)
      case "--name" =>
        map.getOrDefault(ConfigConst.KEY_FLINK_APP_NAME, "").trim match {
          case yarnName if yarnName.nonEmpty => println(" --yarnname " + yarnName)
          case _ => println("")
        }
      case _ =>

    }
  }

}
