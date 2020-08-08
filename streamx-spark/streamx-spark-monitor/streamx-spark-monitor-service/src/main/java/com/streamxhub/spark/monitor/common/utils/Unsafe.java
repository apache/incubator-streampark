/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.spark.monitor.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import static com.streamxhub.spark.monitor.common.utils.ExceptionUtils.stackTrace;


/**
 * For the {@link sun.misc.Unsafe} access.
 */
@Slf4j
public final class Unsafe {

    private static final sun.misc.Unsafe UNSAFE;

    static {
        sun.misc.Unsafe unsafe;
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        } catch (Throwable t) {
            log.warn("sun.misc.Unsafe.theUnsafe: unavailable, {}.", stackTrace(t));
            unsafe = null;
        }

        UNSAFE = unsafe;
    }

    /**
     * Returns the {@link sun.misc.Unsafe}'s instance.
     */
    public static sun.misc.Unsafe getUnsafe() {
        return UNSAFE;
    }

    /**
     * Returns the system {@link ClassLoader}.
     */
    public static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                @Override
                public ClassLoader run() {
                    return ClassLoader.getSystemClassLoader();
                }
            });
        }
    }

    private Unsafe() {
    }
}
