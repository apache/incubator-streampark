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
package org.apache.streampark.common.util

import javax.annotation.Nonnull
import javax.annotation.Nullable

import java.io.PrintWriter
import java.io.StringWriter

object ExceptionUtils {

  /**
   * Stringify the exception object.
   *
   * @param throwable
   *   the target exception to stringify.
   * @return
   *   the result of string-exception.
   */
  @Nonnull def stringifyException(@Nullable throwable: Throwable): String = {
    if (throwable == null) {
      return "(null)"
    }
    val stm = new StringWriter()
    val writer = new PrintWriter(stm)
    try {
      throwable.printStackTrace(writer);
      stm.toString
    } catch {
      case e: Exception => s"${e.getClass.getName} (error while printing stack trace)";
      case _: Throwable => null
    } finally {
      Utils.close(writer, stm)
    }
  }

  @FunctionalInterface trait WrapperRuntimeExceptionHandler[I, O] {
    @throws[Exception]
    def handle(input: I): O
  }

  def wrapRuntimeException[I, O](input: I, handler: WrapperRuntimeExceptionHandler[I, O]): O = {
    try handler.handle(input)
    catch {
      case e: Exception =>
        throw new RuntimeException(e)
    }
  }
}
