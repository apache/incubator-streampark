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

package org.apache.streampark.common.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Utils to process exception message. */
public class ExceptionUtils {

  private ExceptionUtils() {}

  @Nonnull
  public static String stringifyException(@Nullable Throwable throwable) {
    if (throwable == null) {
      return "(null)";
    }
    try (StringWriter stm = new StringWriter();
        PrintWriter writer = new PrintWriter(stm)) {
      throwable.printStackTrace(writer);
      return stm.toString();
    } catch (IOException e) {
      return e.getClass().getName() + " (error while printing stack trace)";
    }
  }
}
