/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.base.util;

import com.streamxhub.streamx.common.util.AssertUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.beans.BeanMap;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public final class CommonUtils implements Serializable {

    private CommonUtils() {

    }

    private static final long serialVersionUID = 6458428317155311192L;

    private static String os = System.getProperty("os.name").toLowerCase();

    /**
     * 非空判断
     *
     * @param objs 要判断,处理的对象
     * @return Boolean
     * @see <b>对象为Null返回true,集合的大小为0也返回true,迭代器没有下一个也返回true..</b>
     * @since 1.0
     */
    public static Boolean isEmpty(Object... objs) {

        if (objs == null) {
            return Boolean.TRUE;
        }

        if (objs.length == 0) {
            return Boolean.TRUE;
        }

        for (Object obj : objs) {
            if (obj == null) {
                return true;
            }

            // 字符序列集
            if ((obj instanceof CharSequence) && "".equals(obj.toString().trim())) {
                return true;
            }
            // 单列集合
            if (obj instanceof Collection) {
                if (((Collection<?>) obj).isEmpty()) {
                    return true;
                }
            }
            // 双列集合
            if (obj instanceof Map) {
                if (((Map<?, ?>) obj).isEmpty()) {
                    return true;
                }
            }

            if (obj instanceof Iterable) {
                if (((Iterable<?>) obj).iterator() == null || !((Iterable<?>) obj).iterator().hasNext()) {
                    return true;
                }
            }

            // 迭代器
            if (obj instanceof Iterator) {
                if (!((Iterator<?>) obj).hasNext()) {
                    return true;
                }
            }

            // 文件类型
            if (obj instanceof File) {
                if (!((File) obj).exists()) {
                    return true;
                }
            }

            if ((obj instanceof Object[]) && ((Object[]) obj).length == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * 空判断
     *
     * @param obj 要判断,处理的对象
     * @return Boolean
     * @see <b>与非空相反</b>
     * @since 1.0
     */
    public static Boolean notEmpty(Object... obj) {
        return !isEmpty(obj);
    }

    public static Long toLong(Object val, Long defVal) {
        if (isEmpty(val)) {
            return defVal;
        }
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static Long toLong(Object val) {
        return toLong(val, null);
    }

    public static Integer toInt(Object val, Integer defVal) {
        if (isEmpty(val)) {
            return defVal;
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static float toFloat(Object val, float defVal) {
        if (isEmpty(val)) {
            return defVal;
        }
        try {
            return Float.parseFloat(val.toString());
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static Boolean toBoolean(String text, Boolean defVal) {
        if (isEmpty(text)) {
            return false;
        }
        try {
            return Boolean.parseBoolean(text);
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static Boolean toBoolean(String text) {
        return toBoolean(text, false);
    }

    public static Integer toInt(Object val) {
        return toInt(val, null);
    }

    public static Float toFloat(Object val) {
        return toFloat(val, 0f);
    }

    public static List arrayToList(Object source) {
        return Arrays.asList(ObjectUtils.toObjectArray(source));
    }

    public static boolean contains(Iterator iterator, Object element) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (ObjectUtils.safeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether the given Enumeration contains the given element.
     *
     * @param enumeration the Enumeration to check
     * @param element     the element to look for
     * @return <code>true</code> if found, <code>false</code> else
     */
    public static boolean contains(Enumeration enumeration, Object element) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Object candidate = enumeration.nextElement();
                if (ObjectUtils.safeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean deleteFile(File dir) {
        if (!dir.exists()) {
            return false;
        }
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!deleteFile(file)) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * Check whether the given Collection contains the given element instance.
     *
     * <p>Enforces the given instance to be present, rather than returning <code>true</code> for an
     * equal element as well.
     *
     * @param collection the Collection to check
     * @param element    the element to look for
     * @return <code>true</code> if found, <code>false</code> else
     */
    public static boolean containsInstance(Collection collection, Object element) {
        if (collection != null) {
            for (Object candidate : collection) {
                if (candidate == element) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
        ArrayList<A> elements = new ArrayList<A>();
        while (enumeration.hasMoreElements()) {
            elements.add(enumeration.nextElement());
        }
        return elements.toArray(array);
    }

    /**
     * Adapt an enumeration to an iterator.
     *
     * @param enumeration the enumeration
     * @return the iterator
     */
    public static <E> Iterator<E> toIterator(Enumeration<E> enumeration) {
        @SuppressWarnings("hiding")
        class EnumerationIterator<E> implements Iterator<E> {
            private Enumeration<E> enumeration;

            public EnumerationIterator(Enumeration<E> enumeration) {
                this.enumeration = enumeration;
            }

            @Override
            public boolean hasNext() {
                return this.enumeration.hasMoreElements();
            }

            @Override
            public E next() {
                return this.enumeration.nextElement();
            }

            @Override
            public void remove() throws UnsupportedOperationException {
                throw new UnsupportedOperationException("Not supported");
            }
        }

        return new EnumerationIterator<E>(enumeration);
    }

    // 获取系统名字
    public static String getOsName() {
        return os;
    }

    public static boolean isLinux() {
        return os.indexOf("linux") >= 0;
    }

    public static boolean isMacOS() {
        return os.indexOf("mac") >= 0 && os.indexOf("os") > 0 && os.indexOf("x") < 0;
    }

    public static boolean isMacOSX() {
        return os.indexOf("mac") >= 0 && os.indexOf("os") > 0 && os.indexOf("x") > 0;
    }

    public static boolean isWindows() {
        return os.indexOf("windows") >= 0;
    }

    public static boolean isOS2() {
        return os.indexOf("os/2") >= 0;
    }

    public static boolean isSolaris() {
        return os.indexOf("solaris") >= 0;
    }

    public static boolean isSunOS() {
        return os.indexOf("sunos") >= 0;
    }

    public static boolean isMPEiX() {
        return os.indexOf("mpe/ix") >= 0;
    }

    public static boolean isHPUX() {
        return os.indexOf("hp-ux") >= 0;
    }

    public static boolean isAix() {
        return os.indexOf("aix") >= 0;
    }

    public static boolean isOS390() {
        return os.indexOf("os/390") >= 0;
    }

    public static boolean isFreeBSD() {
        return os.indexOf("freebsd") >= 0;
    }

    public static boolean isIrix() {
        return os.indexOf("irix") >= 0;
    }

    public static boolean isDigitalUnix() {
        return os.indexOf("digital") >= 0 && os.indexOf("unix") > 0;
    }

    public static boolean isNetWare() {
        return os.indexOf("netware") >= 0;
    }

    public static boolean isOSF1() {
        return os.indexOf("osf1") >= 0;
    }

    public static boolean isOpenVMS() {
        return os.indexOf("openvms") >= 0;
    }

    public static boolean isUnix() {
        boolean isUnix = isLinux();
        if (!isUnix) {
            isUnix = isMacOS();
        }
        if (!isUnix) {
            isUnix = isMacOSX();
        }
        if (!isUnix) {
            isUnix = isLinux();
        }
        if (!isUnix) {
            isUnix = isDigitalUnix();
        }
        if (!isUnix) {
            isUnix = isAix();
        }
        if (!isUnix) {
            isUnix = isFreeBSD();
        }
        if (!isUnix) {
            isUnix = isHPUX();
        }
        if (!isUnix) {
            isUnix = isIrix();
        }
        if (!isUnix) {
            isUnix = isMPEiX();
        }
        if (!isUnix) {
            isUnix = isNetWare();
        }
        if (!isUnix) {
            isUnix = isOpenVMS();
        }
        if (!isUnix) {
            isUnix = isOS2();
        }
        if (!isUnix) {
            isUnix = isOS390();
        }
        if (!isUnix) {
            isUnix = isOSF1();
        }
        if (!isUnix) {
            isUnix = isSunOS();
        }
        if (!isUnix) {
            isUnix = isSolaris();
        }
        return isUnix;
    }

    /**
     * linux内核平台 1 window： 2 其他平台 0
     */
    public static int getPlatform() {
        int platform = 0;
        if (CommonUtils.isUnix()) {
            platform = 1;
        }
        if (CommonUtils.isWindows()) {
            platform = 2;
        }
        return platform;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, Comparator.comparing(Map.Entry::getValue));
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <T> T[] arrayRemoveElements(T[] array, T... elem) {
        AssertUtils.notNull(array);
        List<T> arrayList = new ArrayList<>(0);
        Collections.addAll(arrayList, array);
        if (isEmpty(elem)) {
            return array;
        }
        for (T el : elem) {
            arrayList.remove(el);
        }
        return Arrays.copyOf(arrayList.toArray(array), arrayList.size());
    }

    public static <T> T[] arrayRemoveIndex(T[] array, int... index) {
        AssertUtils.notNull(array);
        for (int j : index) {
            if (j < 0 || j > array.length - 1) {
                throw new IndexOutOfBoundsException("index error.@" + j);
            }
        }
        List<T> arrayList = new ArrayList<>(0);
        Collections.addAll(arrayList, array);
        int i = 0;
        for (int j : index) {
            arrayList.remove(j - i);
            ++i;
        }
        return Arrays.copyOf(arrayList.toArray(array), arrayList.size());
    }

    public static <T> T[] arrayInsertIndex(T[] array, int index, T t) {
        AssertUtils.notNull(array);
        List<T> arrayList = new ArrayList<T>(array.length + 1);
        if (index == 0) {
            arrayList.add(t);
            Collections.addAll(arrayList, array);

        } else {
            T[] before = Arrays.copyOfRange(array, 0, index);
            T[] after = Arrays.copyOfRange(array, index, array.length);
            Collections.addAll(arrayList, before);
            arrayList.add(t);
            Collections.addAll(arrayList, after);
        }
        return arrayList.toArray(array);
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 生成指定长度的uuid
     *
     * @param len
     * @return
     */
    public static String uuid(int len) {
        StringBuffer sb = new StringBuffer();
        while (sb.length() < len) {
            sb.append(uuid());
        }
        return sb.toString().substring(0, len);
    }

    public static Double fixedNum(Number number) {
        return fixedNum(number, 2);
    }

    public static Double fixedNum(Number number, int offset) {
        if (number.doubleValue() == 0.00) {
            return 0D;
        }
        String prefix = "";
        while (offset > 0) {
            prefix += "0";
            offset -= 1;
        }

        java.text.DecimalFormat df = new java.text.DecimalFormat("#." + prefix);
        try {
            return Double.parseDouble(df.format(number));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public static String toPercent(Number number) {
        return toPercent(number, 0);
    }

    public static String toPercent(Number number, int offset) {
        offset += 2;
        Double num = fixedNum(number, offset);
        return (num * 100) + "%";
    }

    /**
     * 将对象装换为map
     *
     * @param bean
     * @return
     */
    public static <T> Map<String, Object> beanToMap(T bean) {
        Map<String, Object> map = new HashMap<>();
        if (bean != null) {
            BeanMap beanMap = BeanMap.create(bean);
            for (Object key : beanMap.keySet()) {
                map.put(key + "", beanMap.get(key));
            }
        }
        return map;
    }

    /**
     * 将map装换为javabean对象
     *
     * @param map
     * @param bean
     * @return
     */
    public static <T> T mapToBean(Map<String, Object> map, T bean) {
        BeanMap beanMap = BeanMap.create(bean);
        beanMap.putAll(map);
        return bean;
    }

    /**
     * 将List<T>转换为List<Map<String, Object>>
     *
     * @param objList
     * @return
     * @throws IOException
     */
    public static <T> List<Map<String, Object>> objectsToMaps(List<T> objList) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (objList != null && objList.size() > 0) {
            Map<String, Object> map = null;
            T bean = null;
            for (T t : objList) {
                bean = t;
                map = beanToMap(bean);
                list.add(map);
            }
        }
        return list;
    }

    /**
     * 将List<Map<String,Object>>转换为List<T>
     *
     * @param maps
     * @param clazz
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> List<T> mapsToObjects(List<Map<String, Object>> maps, Class<T> clazz)
        throws InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        if (maps != null && maps.size() > 0) {
            Map<String, Object> map;
            T bean;
            for (Map<String, Object> stringObjectMap : maps) {
                map = stringObjectMap;
                bean = clazz.newInstance();
                mapToBean(map, bean);
                list.add(bean);
            }
        }
        return list;
    }
}
