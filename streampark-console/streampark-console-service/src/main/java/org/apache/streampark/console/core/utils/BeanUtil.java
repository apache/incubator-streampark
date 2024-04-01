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

package org.apache.streampark.console.core.utils;

import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.func.Func1;

/** Util class for bean */
public class BeanUtil {

  /**
   * bean copy ignore null field
   *
   * @param source the source object for copy
   * @param target the target object for copy
   */
  @SafeVarargs
  public static <P, R> void copyIgnoreNull(
      Object source, Object target, Func1<P, R>... ignoreProperties) {
    cn.hutool.core.bean.BeanUtil.copyProperties(
        source,
        target,
        CopyOptions.create().ignoreNullValue().setIgnoreProperties(ignoreProperties));
  }
}
