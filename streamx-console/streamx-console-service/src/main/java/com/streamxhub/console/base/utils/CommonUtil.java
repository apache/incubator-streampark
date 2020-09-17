/**
 * Copyright (c) 2019 The StreamX Project
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
package com.streamxhub.console.base.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.beans.BeanMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author <a href="mailto:benjobs@qq.com">benjobs@qq.com</a>
 * @name:CommonUtil
 * @version: 1.0.0
 * @company: design.adminx
 * @description: 常用工具类
 * @date: 2012-10-9 pa 18:03 于大麦<br/><br/>
 * <p>
 * <b style="color:RED">呵呵</b><br/><br/>
 * 我说我喜欢你,你说呵呵,<br/>
 * 我问你喜欢我不,你说呵呵,<br/>
 * 然后你便悄然离线,<br/>
 * 灰色头像不再跳闪，<br/>
 * 空留我傻傻的感叹。<br/>
 * 聊天就这样止步于呵呵,<br/>
 * 我们能不能不说呵呵,<br/>
 * 我已经怕了这个词了, <br/>
 * 请你不要敷衍我,<br/>
 * 我爱你那么多, <br/>
 * 你也喜欢下我好么？<br/><br/>
 * <p>
 * 记得上次我们一起唱的歌,<br/>
 * 我小声唱,你也跟着合,<br/>
 * 看着你脸上醉人的酒窝,<br/>
 * 我心里有说不出的快乐.<br/>
 * 我问你喜欢和我在一起么？<br/>
 * 你低头不语,笑着不说。<br/>
 * 原来只是我一厢情愿，<br/>
 * 回想起以往的缠绵,<br/>
 * 痛苦在我身上蔓延,<br/>
 * 眼泪慢慢模糊视线, <br/>
 * 你的影子在我脑海盘旋,<br/>
 * 是我对你太依恋，<br/>
 * 我只想陪你走完这一段, <br/>
 * 只要你在我身边, <br/>
 * 沿途风景,即便再美,我也不会留恋，<br/>
 * 为什么我们不能把这场恋爱谈完？<br/><br/>
 * <p>
 * 往日温柔的你现在何处,<br/>
 * 我的心只想向你倾诉，<br/>
 * 你怎忍心一人远走, <br/>
 * 让我承受思念之苦, <br/>
 * 你可知我在为你守候, <br/>
 * 我多想为你分担痛苦, <br/>
 * 时刻陪伴在你的左右, <br/>
 * 感受你的感受,去除你的烦忧,<br/>
 * 分享你的快乐,擦干你的泪珠。<br/>
 * 梦想着能和你在未来的旅途, <br/>
 * 一起嬉戏,没了忧愁, <br/>
 * 执子之手,相约白头! <br/><br/>
 * <p>
 * 后来你打电话说已经有了男朋友,<br/>
 * 一起过的很幸福。  <br/>
 * 我说了声:呵呵,祝你幸福! <br/>
 * 你问:你怎么也说呵呵呢? <br/>
 * 我还想说啥已说不出, <br/>
 * 眼泪已经止不住的流。<br/>
 * 你决定要走,    <br/>
 * 寻找属于你的归宿,  <br/>
 * 我也不能挽留，    <br/>
 * 只能真心的祝你幸福! <br/><br/>
 * <p>
 * 我用尽一生的思念，<br/>
 * 只为等待你的出现,<br/>
 * 如今你已越走越远,  <br/>
 * 而我只能独自感叹,    <br/>
 * 情话给谁听,眉毛为谁画? <br/>
 * 翘首企盼谁,携谁走天涯?  <br/>
 * 愿得谁之心,白首不相离？<br/>
 * 该执谁之手,相守到白发? <br/>
 * <hr style="color:RED"/>
 */

@Slf4j
public abstract class CommonUtil implements Serializable {


    private static final long serialVersionUID = 6458428317155311192L;


    private static String OS = System.getProperty("os.name").toLowerCase();

    public static final Map<Serializable,Serializable> localCache = new ConcurrentHashMap<>();


    private static Properties properties;

    public static String getProperties(String key) {
        if (properties == null) {
            properties = new Properties();
            InputStream in = null;
            try {
                String propDir = System.getProperty("nutz.boot.configure.properties.dir");
                if (propDir == null) {
                    in = CommonUtil.class.getClassLoader().getResourceAsStream("application.properties");
                } else {
                    in = new FileInputStream(new File(propDir + File.separator + "application.properties"));
                }
                properties.load(in);
            } catch (IOException e) {
                throw new RuntimeException("web.properties load error");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return properties.getProperty(key);
    }

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

    /**
     * 对Null作预处理
     *
     * @param obj   待处理的对象
     * @param clazz 该对象的类型
     * @return T 返回处理后的不为Null的该对象
     * @author <a href="mailto:benjobs@qq.com">Ben</a>
     * @see <b>对Null作预处理,有效避免NullPointerException</b>
     * @since 1.0
     */
    public static <T> T preparedNull(T obj, Class<?> clazz) {

        if (notEmpty(obj)) {
            return obj;
        }

        AssertUtil.notNull(clazz, "this class must be not null!");

        Object val = null;

        // 单列集合
        if (List.class.isAssignableFrom(clazz)) {
            val = new ArrayList<Object>(0);
        } else if (Set.class.isAssignableFrom(clazz)) {
            val = new HashSet<Object>(0);
        } else if (Map.class.isAssignableFrom(clazz)) {
            val = new HashMap<Object, Object>(0);
        } else {
            try {
                val = clazz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (T) val;
    }


    public static List arrayToList(Object source) {
        return Arrays.asList(ObjectUtil.toObjectArray(source));
    }

    public static boolean contains(Iterator iterator, Object element) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (ObjectUtil.safeEquals(candidate, element)) {
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
                if (ObjectUtil.safeEquals(candidate, element)) {
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
        if (CommonUtil.isUnix()) {
            platform = 1;
        }
        if (CommonUtil.isWindows()) {
            platform = 2;
        }
        return platform;
    }


    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {

            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <T> T[] arrayRemoveElements(T[] array, T... elem) {
        AssertUtil.notNull(array);
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
        AssertUtil.notNull(array);
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
        AssertUtil.notNull(array);
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

    //org.apache.httpcomponents:httpcore:jar
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
        Map<String, Object> map = Maps.newHashMap();
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
        List<Map<String, Object>> list = Lists.newArrayList();
        if (objList != null && objList.size() > 0) {
            Map<String, Object> map = null;
            T bean = null;
            for (int i = 0, size = objList.size(); i < size; i++) {
                bean = objList.get(i);
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
    public static <T> List<T> mapsToObjects(List<Map<String, Object>> maps, Class<T> clazz) throws InstantiationException, IllegalAccessException {
        List<T> list = Lists.newArrayList();
        if (maps != null && maps.size() > 0) {
            Map<String, Object> map = null;
            T bean = null;
            for (int i = 0, size = maps.size(); i < size; i++) {
                map = maps.get(i);
                bean = clazz.newInstance();
                mapToBean(map, bean);
                list.add(bean);
            }
        }
        return list;
    }

}


