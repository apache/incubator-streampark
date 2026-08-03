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

import java.util.function.Function;

/** AutoCloseable resource management utilities. */
public final class AutoCloseUtils {

    private AutoCloseUtils() {
    }

    public static <T extends AutoCloseable, R> R using(T autoCloseable, Function<T, R> func) {
        return using(autoCloseable, func, null);
    }

    public static <T extends AutoCloseable, R> R using(
                                                       T autoCloseable, Function<T, R> func,
                                                       Function<Throwable, R> excFunc) {
        try (T resource = autoCloseable) {
            return func.apply(resource);
        } catch (Throwable e) {
            if (excFunc != null) {
                return excFunc.apply(e);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
            throw new IllegalStateException(e);
        }
    }
}
