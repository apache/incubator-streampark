package com.streamxhub.streamx.common.domain

import java.util.regex.Pattern

/**
 * author: Al-assad
 *
 * @param version   Actual flink version number, like "1.13.2", "1.14.0"
 * @param flinkHome Autual flink home that must be a readable local path
 */
case class FlinkVersion(version: String, flinkHome: String) {

  private[this] lazy val FLINK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$")

  // flink major version, like "1.13", "1.14"
  lazy val majorVersion: String = {
    val matcher = FLINK_VER_PATTERN.matcher(version)
    matcher.matches()
    matcher.group(1)
  }

  // streamx flink shims version, like "streamx-flink-shims_flink-1.13"
  lazy val shimsVersion: String = s"streamx-flink-shims_flink-$majorVersion"

  override def toString: String =
    s"""
       |-------------------------<<flinkVersion>>------------------------
       | flinkVersion : $version
       | flinkHome    : $flinkHome
       | majorVersion : $majorVersion
       | shimsVersion : $shimsVersion
       |__________________________________________________________________
       |""".stripMargin

}
