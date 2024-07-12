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

package org.apache.streampark.console.base.util;

import org.apache.streampark.common.util.AssertUtils;

import org.apache.commons.lang3.StringUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import javax.annotation.Nonnull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import scala.Tuple2;

/** The util to throw a sub-exception of {@link RuntimeException} for the specified condition. */
public class Throws {

    private static final Cache<Tuple2<Class<?>, Class<?>>, Constructor<?>> CACHE = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.DAYS).maximumSize(32).build();

    private Throws() {
    }

    /**
     * Throw a runtime exception to the specified class type and condition.
     *
     * @param bool The condition.
     * @param exceptionClass The target runtime exception class type.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfTrue(
                                                                boolean bool, @Nonnull Class<T> exceptionClass) {
        if (bool) {
            throw createRuntimeException(exceptionClass);
        }
    }

    /**
     * Throw a runtime exception to the specified class type and condition.
     *
     * @param bool The condition.
     * @param exceptionClass The target runtime exception class type.
     * @param msgFormat The error message or message format.
     * @param args The real value of placeholders.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfTrue(
                                                                boolean bool, @Nonnull Class<T> exceptionClass,
                                                                String msgFormat, Object... args) {
        if (bool) {
            throw createRuntimeException(exceptionClass, getErrorMsg(msgFormat, args));
        }
    }

    /**
     * Throw a runtime exception to the specified class type and condition.
     *
     * @param bool The condition.
     * @param exceptionClass The target runtime exception class type.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfFalse(
                                                                 boolean bool, @Nonnull Class<T> exceptionClass) {
        throwIfTrue(!bool, exceptionClass);
    }

    /**
     * Throw a runtime exception to the specified class type and condition.
     *
     * @param bool The condition.
     * @param exceptionClass The target runtime exception class type.
     * @param msgFormat The error message or message format.
     * @param args The real value of placeholders.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfFalse(
                                                                 boolean bool, @Nonnull Class<T> exceptionClass,
                                                                 String msgFormat, Object... args) {
        throwIfTrue(!bool, exceptionClass, msgFormat, args);
    }

    /**
     * Throw a runtime exception to the specified class type and object.
     *
     * @param obj The object.
     * @param exceptionClass The target runtime exception class type.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfNull(
                                                                Object obj, @Nonnull Class<T> exceptionClass) {
        throwIfTrue(obj == null, exceptionClass);
    }

    /**
     * Throw a runtime exception to the specified class type and object.
     *
     * @param obj The object.
     * @param exceptionClass The target runtime exception class type.
     * @param msgFormat The error message or message format.
     * @param args The real value of placeholders.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfNull(
                                                                Object obj, @Nonnull Class<T> exceptionClass,
                                                                String msgFormat, Object... args) {
        throwIfTrue(obj == null, exceptionClass, msgFormat, args);
    }

    /**
     * Throw a runtime exception to the specified class type and object.
     *
     * @param obj The object.
     * @param exceptionClass The target runtime exception class type.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfNonnull(
                                                                   Object obj, @Nonnull Class<T> exceptionClass) {
        throwIfTrue(obj != null, exceptionClass);
    }

    /**
     * Throw a runtime exception to the specified class type and object.
     *
     * @param obj The object.
     * @param exceptionClass The target runtime exception class type.
     * @param msgFormat The error message or message format.
     * @param args The real value of placeholders.
     * @param <T> The target exception class.
     */
    public static <T extends RuntimeException> void throwIfNonnull(
                                                                   Object obj, @Nonnull Class<T> exceptionClass,
                                                                   String msgFormat, Object... args) {
        throwIfTrue(obj != null, exceptionClass, msgFormat, args);
    }

    private static <T extends RuntimeException> T createRuntimeException(
                                                                         @Nonnull Class<T> exceptionClass,
                                                                         String... errorMsgs) {
        try {
            return createRuntimeExceptionInternal(exceptionClass, errorMsgs);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends RuntimeException> T createRuntimeExceptionInternal(
                                                                                 @Nonnull Class<T> exceptionClass,
                                                                                 String... errorMsgs) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        AssertUtils.notNull(exceptionClass, "The target exception must be specified.");
        Tuple2<Class<?>, Class<?>> key = new Tuple2<>(exceptionClass, hasElements(errorMsgs) ? String.class : null);
        Constructor<?> constructor = CACHE.get(
            key,
            classAndParams -> {
                try {
                    return hasElements(errorMsgs)
                        ? exceptionClass.getConstructor(String.class)
                        : exceptionClass.getConstructor();
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            });
        AssertUtils.notNull(
            constructor,
            String.format("There's no a constructor for exception '%s'.", exceptionClass.getName()));
        return hasElements(errorMsgs)
            ? (T) constructor.newInstance(errorMsgs[0])
            : (T) constructor.newInstance();
    }

    private static boolean hasElements(Object[] objects) {
        return objects != null && objects.length > 0;
    }

    private static String getErrorMsg(String msgFormat, Object... args) {
        if (StringUtils.isBlank(msgFormat)) {
            return msgFormat;
        }
        if (args == null || args.length < 1) {
            return msgFormat;
        }
        return String.format(msgFormat, args);
    }
}
