package com.streamxhub.common.util

import java.util.UUID

object Utils {

  def uuid():String =   UUID.randomUUID().toString.replaceAll("-","")

}
