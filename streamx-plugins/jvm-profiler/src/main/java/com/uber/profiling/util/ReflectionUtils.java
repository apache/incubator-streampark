/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {
    private static final AgentLogger logger = AgentLogger.getLogger(ReflectionUtils.class.getName());

    public static <T> Constructor<T> getConstructor(String implementaionClass, Class<T> interfaceClass) {
        Class<?> clazz = null;

        try {
            clazz = Class.forName(implementaionClass);
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to get class for %s", implementaionClass), e);
        }

        if (!interfaceClass.isAssignableFrom(clazz)) {
            throw new RuntimeException(String.format("Invalid class %s, please make sure it is an implementation of %s", clazz, interfaceClass.getName()));
        }

        try {
            Class<T> concretelass = (Class<T>) clazz;
            Constructor<T> constructor = concretelass.getConstructor();
            return constructor;
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to get constructor for %s", clazz.getName()), e);
        }
    }

    public static <T> T createInstance(String implementaionClass, Class<T> interfaceClass) {
        try {
            Constructor<T> constructor = getConstructor(implementaionClass, interfaceClass);
            T result = constructor.newInstance();
            logger.info(String.format("Created %s instance (%s) for interface %s", implementaionClass, result, interfaceClass));
            return result;
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to create %s instance for interface %s", implementaionClass, interfaceClass), e);
        }
    }
    
    public static Object executeStaticMethods(String className, String methods) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String[] methodArray = methods.split("\\.");
        Class clazz = Class.forName(className);
        Object clazzObject = null;
        Object result = null;
        for (String entry : methodArray) {
            Method method = clazz.getMethod(entry);
            if (method == null) {
                return null;
            }
            result = method.invoke(clazzObject);
            if (result == null) {
                return null;
            }

            clazz = result.getClass();
            clazzObject = result;
        }
        return result;
    }
}
