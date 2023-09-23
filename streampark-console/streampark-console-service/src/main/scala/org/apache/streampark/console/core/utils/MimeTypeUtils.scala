/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.utils

import org.apache.commons.lang3.StringUtils
import org.apache.tika.metadata.{HttpHeaders, Metadata}
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.xml.sax.helpers.DefaultHandler

import java.io.InputStream

object MimeTypeUtils {

  def isPythonFileType(contentType: String, input: InputStream): Boolean = {
    if (StringUtils.isBlank(contentType) || input == null) {
      throw new RuntimeException("The contentType or inputStream can not be null")
    }
    getMimeType(input) == MediaType.TEXT_PLAIN.toString && contentType.contains("text/x-python")
  }

  def getMimeType(stream: InputStream): String = {
    val metadata: Metadata       = new Metadata
    val parser: AutoDetectParser = new AutoDetectParser
    parser.parse(stream, new DefaultHandler, metadata, new ParseContext)
    metadata.get(HttpHeaders.CONTENT_TYPE)
  }

}
