package com.streamxhub.common.util

import java.util.Properties

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}
import org.apache.hadoop.security.UserGroupInformation
import com.streamxhub.common.conf.ConfigConst._

import scala.collection.JavaConversions._

class HBaseClient(fun: () => Connection) extends Serializable {
  lazy val connection: Connection = fun()
}

object HBaseClient {
  val conf: Configuration = HBaseConfiguration.create

  def apply(prop: Properties): HBaseClient = {
    val user = prop.remove(KEY_HBASE_AUTH_USER)
    prop.foreach(x => conf.set(x._1, x._2))
    val fun = () => {
      if (user != null) {
        UserGroupInformation.setConfiguration(conf)
        val remoteUser: UserGroupInformation = UserGroupInformation.createRemoteUser(user.toString)
        UserGroupInformation.setLoginUser(remoteUser)
      }
      val connection = ConnectionFactory.createConnection(conf)
      sys.addShutdownHook {
        connection.close()
      }
      connection
    }
    new HBaseClient(fun)
  }
}
