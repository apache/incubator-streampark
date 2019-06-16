package com.streamxhub.spark.monitor.util

import java.io.File
import java.text.DecimalFormat
import java.util.{UUID, Map => Jmap}

import scala.collection.immutable.{Map => Imap}
import scala.collection.mutable
import scala.collection.mutable.{Map => Mmap}
import scala.util.control.Breaks._

object CommonUtils {

  private val OS: String = System.getProperty("os.name").toLowerCase


  /**
    * 全部为null返回true,反之返回false
    *
    * @param objs
    * @return
    */
  def allNoEmpty(objs: Any*): Boolean = !allEmpty(objs)

  def allEmpty(objs: Any*): Boolean = {

    if (objs == null || objs.isEmpty) return true

    var nullIndex: Int = 0

    for (obj <- objs) {
      breakable {
        if (obj == null) {
          nullIndex += 1
          break
        }
      }
      // 字符序列集
      breakable {
        if (obj.isInstanceOf[CharSequence] && "" == obj.toString.trim) {
          nullIndex += 1
          break
        }
      }
      breakable {
        obj match {
          case value: Imap[_, _] if value.isEmpty =>
            nullIndex += 1
            break
          case _ =>
        }
      }
      breakable {
        obj match {
          case value: Mmap[_, _] if value.isEmpty =>
            nullIndex += 1
            break
          case _ =>
        }
      }
      breakable {
        obj match {
          case value: Jmap[_, _] if value.isEmpty =>
            nullIndex += 1
            break
          case _ =>
        }
      }
      breakable {
        obj match {
          case value: List[_] if value.isEmpty =>
            nullIndex += 1
            break
          case _ =>
        }
      }
      breakable {
        obj match {
          case value: Set[_] if value.isEmpty =>
            nullIndex += 1
            break
          case _ =>
        }
      }
      breakable {
        obj match {
          case value: mutable.Set[_] if value.isEmpty =>
            nullIndex += 1
            break
          case _ =>
        }
      }
      // 文件类型
      breakable {
        obj match {
          case file: File if !file.exists =>
            nullIndex += 1
            break
          case _ =>
        }
      }
      breakable {
        obj match {
          case array: Array[AnyRef] if array.length == 0 =>
            nullIndex += 1
            break
          case _ =>
        }
      }
    }

    nullIndex == objs.length
  }

  def noEmpty(obj: Any): Boolean = !isEmpty(obj)

  /**
    * 元素为空,则返回true
    *
    * @param obj
    * @return
    */
  def isEmpty(obj: Any): Boolean = allEmpty(obj)


  //获取系统名字
  def getOsName: String = {
    OS
  }

  def isLinux: Boolean = {
    OS.indexOf("linux") >= 0
  }

  def isMacOS: Boolean = {
    OS.indexOf("mac") >= 0 && OS.indexOf("os") > 0 && OS.indexOf("x") < 0
  }

  def isMacOSX: Boolean = {
    OS.indexOf("mac") >= 0 && OS.indexOf("os") > 0 && OS.indexOf("x") > 0
  }

  def isWindows: Boolean = {
    OS.indexOf("windows") >= 0
  }

  def isOS2: Boolean = {
    OS.indexOf("os/2") >= 0
  }

  def isSolaris: Boolean = {
    OS.indexOf("solaris") >= 0
  }

  def isSunOS: Boolean = {
    OS.indexOf("sunos") >= 0
  }

  def isMPEiX: Boolean = {
    OS.indexOf("mpe/ix") >= 0
  }

  def isHPUX: Boolean = {
    OS.indexOf("hp-ux") >= 0
  }

  def isAix: Boolean = {
    OS.indexOf("aix") >= 0
  }

  def isOS390: Boolean = {
    OS.indexOf("os/390") >= 0
  }

  def isFreeBSD: Boolean = {
    OS.indexOf("freebsd") >= 0
  }

  def isIrix: Boolean = {
    OS.indexOf("irix") >= 0
  }

  def isDigitalUnix: Boolean = {
    OS.indexOf("digital") >= 0 && OS.indexOf("unix") > 0
  }

  def isNetWare: Boolean = {
    OS.indexOf("netware") >= 0
  }

  def isOSF1: Boolean = {
    OS.indexOf("osf1") >= 0
  }

  def isOpenVMS: Boolean = {
    OS.indexOf("openvms") >= 0
  }

  def isUnix: Boolean = {
    var isUnix: Boolean = isLinux
    if (!(isUnix)) {
      isUnix = isMacOS
    }
    if (!(isUnix)) {
      isUnix = isMacOSX
    }
    if (!(isUnix)) {
      isUnix = isLinux
    }
    if (!(isUnix)) {
      isUnix = isDigitalUnix
    }
    if (!(isUnix)) {
      isUnix = isAix
    }
    if (!(isUnix)) {
      isUnix = isFreeBSD
    }
    if (!(isUnix)) {
      isUnix = isHPUX
    }
    if (!(isUnix)) {
      isUnix = isIrix
    }
    if (!(isUnix)) {
      isUnix = isMPEiX
    }
    if (!(isUnix)) {
      isUnix = isNetWare
    }
    if (!(isUnix)) {
      isUnix = isOpenVMS
    }
    if (!(isUnix)) {
      isUnix = isOS2
    }
    if (!(isUnix)) {
      isUnix = isOS390
    }
    if (!(isUnix)) {
      isUnix = isOSF1
    }
    if (!(isUnix)) {
      isUnix = isSunOS
    }
    if (!(isUnix)) {
      isUnix = isSolaris
    }
    return isUnix
  }


  def uuid: String = {
    UUID.randomUUID.toString.replaceAll("-", "")
  }

  /**
    * 生成指定长度的uuid
    *
    * @param len
    * @return
    */
  def uuid(len: Int): String = {
    val sb: StringBuffer = new StringBuffer
    while ( {
      sb.length < len
    }) {
      sb.append(uuid)
    }
    sb.toString.substring(0, len)
  }


  def fixedNum(number: Number): Double = {
    if (number.intValue == 0) {
      return 0D
    }
    val df: DecimalFormat = new DecimalFormat("#.00")
    df.format(number).toDouble
  }
}
