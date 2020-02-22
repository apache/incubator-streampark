/**
 * Copyright (c) 2015 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.streamxhub.spark.monitor.common.utils;

import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.Serializable;
import java.util.*;


public abstract class CommonUtils implements Serializable {

    private static final long serialVersionUID = 6458428317155311192L;

    private static String OS = System.getProperty("os.name").toLowerCase();

    /**
     * 非空判断
     *
     * @param objs 要判断,处理的对象
     * @return Boolean
     * @author <a href="mailto:benjobs@qq.com">Ben</a>
     * @see <b>对象为Null返回true,集合的大小为0也返回true,迭代器没有下一个也返回true..</b>
     * @since 1.0
     */
    public static Boolean isEmpty(Object... objs) {

        if (objs == null) {
            return true;
        }

        if (objs.length == 0) {
            return true;
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
                ((Iterable<?>) obj).iterator();
                if (!((Iterable<?>) obj).iterator().hasNext()) {
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
     * @author <a href="mailto:benjobs@qq.com">Ben</a>
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
                if (ObjectUtils.nullSafeEquals(candidate, element)) {
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
                if (ObjectUtils.nullSafeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether the given Collection contains the given element instance.
     * <p>Enforces the given instance to be present, rather than returning
     * <code>true</code> for an equal element as well.
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

            private EnumerationIterator(Enumeration<E> enumeration) {
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

    //获取系统名字
    public static String getOsName() {
        return OS;
    }

    public static boolean isLinux() {
        return OS.indexOf("linux") >= 0;
    }

    public static boolean isMacOS() {
        return OS.indexOf("mac") >= 0 && OS.indexOf("os") > 0 && OS.indexOf("x") < 0;
    }

    public static boolean isMacOSX() {
        return OS.indexOf("mac") >= 0 && OS.indexOf("os") > 0 && OS.indexOf("x") > 0;
    }

    public static boolean isWindows() {
        return OS.indexOf("windows") >= 0;
    }

    public static boolean isOS2() {
        return OS.indexOf("os/2") >= 0;
    }

    public static boolean isSolaris() {
        return OS.indexOf("solaris") >= 0;
    }

    public static boolean isSunOS() {
        return OS.indexOf("sunos") >= 0;
    }

    public static boolean isMPEiX() {
        return OS.indexOf("mpe/ix") >= 0;
    }

    public static boolean isHPUX() {
        return OS.indexOf("hp-ux") >= 0;
    }

    public static boolean isAix() {
        return OS.indexOf("aix") >= 0;
    }

    public static boolean isOS390() {
        return OS.indexOf("os/390") >= 0;
    }

    public static boolean isFreeBSD() {
        return OS.indexOf("freebsd") >= 0;
    }

    public static boolean isIrix() {
        return OS.indexOf("irix") >= 0;
    }

    public static boolean isDigitalUnix() {
        return OS.indexOf("digital") >= 0 && OS.indexOf("unix") > 0;
    }

    public static boolean isNetWare() {
        return OS.indexOf("netware") >= 0;
    }

    public static boolean isOSF1() {
        return OS.indexOf("osf1") >= 0;
    }

    public static boolean isOpenVMS() {
        return OS.indexOf("openvms") >= 0;
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
     * linux内核平台 1
     * window： 2
     * 其他平台 0
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

    /**
     * 将String数组转化为Long数组
     *
     * @param strArr String数组
     * @return Long数组
     * @Title: strArr2LongArr
     * @author: wanghajie 2012-12-13上午10:15:42
     */
    public static Long[] string2LongArray(String[] strArr) {
        if (CommonUtils.isEmpty(strArr)) {
            return null;
        }
        Long longArray[] = new Long[strArr.length];
        for (int i = 0; i < longArray.length; i++) {
            longArray[i] = StringUtils.parseLong(strArr[i]);
        }
        return longArray;
    }

    /**
     * 将将String数组转化为LongList
     *
     * @param strArr String数组
     * @return LongList
     * @Title: strArr2LongList
     * @author: wanghajie 2012-12-13上午11:09:10
     */
    public static List<Long> string2LongList(String[] strArr) {
        // 将String数组转化为Long数组
        Long[] longArr = string2LongArray(strArr);
        return longArr == null ? ((List<Long>) Collections.EMPTY_LIST) : Arrays.asList(longArr);
    }

    public static <T> T[] arrayRemoveElements(T[] array, T... elem) {
        assert array != null;
        List<T> arrayList = new ArrayList<T>(0);
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
        assert array != null;
        for (int j : index) {
            if (j < 0 || j > array.length - 1) {
                throw new IndexOutOfBoundsException("index error.@" + j);
            }
        }
        List<T> arrayList = new ArrayList<T>(0);
        Collections.addAll(arrayList, array);
        int i = 0;
        for (int j : index) {
            arrayList.remove(j - i);
            ++i;
        }
        return Arrays.copyOf(arrayList.toArray(array), arrayList.size());
    }


    public static <T> T[] arrayInsertIndex(T[] array, int index, T t) {
        assert array != null;
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


    public static boolean isNumber(String text) {
        try {
            Long.parseLong(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}


