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

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReflectUtils {

    private static final Logger LOG = StreamParkLoggerFactory.loggerFactory().getLogger(ReflectUtils.class.getName());
    private ReflectUtils() {
    }

    public static Field getField(Class<?> beanClass, String name) {
        for (Field f : beanClass.getDeclaredFields()) {
            if (Objects.equals(name, f.getName()))
                return f;
        }
        return null;
    }

    public static Object getFieldValue(Object obj, String fieldName) throws IllegalAccessException {
        Field field = getField(obj.getClass(), fieldName);
        return getFieldValue(obj, field);
    }

    public static Object getFieldValue(Object obj, Field field) throws IllegalAccessException {
        if (obj == null || field == null)
            return null;
        field.setAccessible(true);
        return field.get(obj);
    }

    public static void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
        Field field = getAccessibleField(obj, fieldName);
        if (field == null)
            throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + "]");
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            LOG.error("[StreamPark] Failed to assign to the element.", e);
            throw new Exception(e.getMessage());
        }
    }

    private static Field getAccessibleField(Object obj, String fieldName) {
        AssertUtils.notNull(obj, "object can't be null");
        AssertUtils.required(StringUtils.isNotBlank(fieldName), "fieldName can't be blank");
        Class<?> superClass = obj.getClass();
        while (superClass != Object.class) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            } catch (NoSuchFieldException ignored) {
                superClass = superClass.getSuperclass();
            }
        }
        return null;
    }

    private static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
            || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    public static List<Method> getMethodsByAnnotation(Class<?> beanClass, Class<? extends Annotation> annotClazz) {
        List<Method> methods = new ArrayList<>();
        for (Method m : beanClass.getDeclaredMethods()) {
            if (m.getDeclaredAnnotation(annotClazz) != null)
                methods.add(m);
        }
        return methods;
    }
}
