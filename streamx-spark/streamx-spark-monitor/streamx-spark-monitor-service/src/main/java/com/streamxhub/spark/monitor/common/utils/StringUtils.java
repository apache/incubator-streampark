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

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class StringUtils {

    public static final String SEARCH_SEPERATOR = "[ ,;|　]";

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Pattern KVP_PATTERN = Pattern.compile("([_.a-zA-Z0-9][-_.a-zA-Z0-9]*)[=](.*)"); //key value pair pattern.

    private static final Pattern INT_PATTERN = Pattern.compile("^\\d+$");

    private StringUtils() {
    }


    public static String joinString(Object[] arrays, String separator) {
        return joinString(Arrays.asList(arrays), separator);
    }

    public static String joinString(Collection<?> collection, String separator) {
        assert collection != null;
        StringBuilder collStr = new StringBuilder();
        for (Object o : collection) {
            collStr.append(o).append(separator);
        }
        return collStr.substring(0, collStr.length() - separator.length());
    }

    public static String joinString(Object[] arrays) {
        return joinString(arrays, ",");
    }

    public static String joinString(Collection<?> collection) {
        return joinString(collection, ",");
    }

    /**
     * 将字符串转换成String[]
     *
     * @param str 要转换的目标字符串
     * @return
     */
    public static String[] stringToArray(String str) {
        return stringToArray(str, null, 0);
    }

    /**
     * 将字符串转换成String[]
     *
     * @param str   要转换的目标字符串
     * @param limit 要转换的目标字符串
     * @return
     */
    public static String[] stringToArray(String str, int limit) {
        return stringToArray(str, null, limit);
    }

    /**
     * 将字符串转换成String[]
     *
     * @param str   要转换的目标字符串
     * @param split 对字符串截取的分隔符
     * @return
     */
    public static String[] stringToArray(String str, String split) {
        return stringToArray(str, split, 0);
    }

    /**
     * 将字符串转换成String[]
     *
     * @param str   要转换的目标字符串
     * @param split 对字符串截取的分隔符
     * @param limit 长度
     * @return
     */
    public static String[] stringToArray(String str, String split, int limit) {
        List<String> list = stringToList(str, split, limit);
        if (list == null) {
            return null;
        }
        return list.toArray(new String[0]);
    }

    /**
     * 将字符串转换成List<String>
     *
     * @param str 要转换的目标字符串
     * @return
     */
    public static List<String> stringToList(String str) {
        return stringToList(str, null, 0);
    }

    /**
     * 将字符串转换成List<String>
     *
     * @param str   要转换的目标字符串
     * @param limit 对字符串截取的分隔符
     * @return
     */
    public static List<String> stringToList(String str, int limit) {
        return stringToList(str, null, limit);
    }

    /**
     * 将字符串转换成List<String>
     *
     * @param str   要转换的目标字符串
     * @param split 长度
     * @return
     */
    public static List<String> stringToList(String str, String split) {
        return stringToList(str, split, 0);
    }

    /**
     * 转换字符串成list
     *
     * @param str   字符串
     * @param split 分隔符
     * @param limit 长度
     * @return
     */
    public static List<String> stringToList(String str, String split, int limit) {
        if (str == null) {
            return null;
        }
        String[] ret = null;
        if (split == null) {
            split = SEARCH_SEPERATOR;
        }
        ret = str.split(split, limit);

        List<String> list = new ArrayList<String>();
        for (String aRet : ret) {
            String s = aRet;
            if (s == null) {
                continue;
            }
            s = s.trim();
            if (!"".equals(s)) {
                list.add(s);
            }
        }
        return list;
    }

    /**
     * 转换字符串为int 失败时返回默认值 -1
     *
     * @param str
     * @return
     */
    public static int parseInt(String str) {
        return parseInt(str, -1);
    }

    /**
     * 转换字符串为int 失败时返回默认值 defaultValue
     *
     * @param str
     * @param defaultValue
     * @return
     */
    public static int parseInt(String str, int defaultValue) {
        if (str == null || "".equals(str.trim())) {
            return defaultValue;
        }
        int num = 0;
        try {
            num = Integer.parseInt(str);
        } catch (Exception e) {
            num = defaultValue;
        }
        return num;
    }

    /**
     * 转换字符串为long类型，如果转换失败，则返回默认值 -1
     *
     * @param str 字符串
     * @return
     */
    public static long parseLong(String str) {
        return parseLong(str, -1);
    }

    /**
     * 转换字符串为long类型，如果转换失败，则返回默认值 defaultValue
     *
     * @param str          字符串
     * @param defaultValue 默认值
     * @return
     */
    public static long parseLong(String str, long defaultValue) {
        if (str == null || "".equals(str.trim())) {
            return defaultValue;
        }
        long num = 0;
        try {
            num = Long.parseLong(str);
        } catch (Exception e) {
            num = defaultValue;
        }
        return num;
    }

    /**
     * 判断字符串是否为null或者为""
     *
     * @param str
     * @return
     */
    public static boolean isNullString(String str) {
        if (str == null || "".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static boolean isNotNullString(String str) {
        return !isNullString(str);
    }

    public static String checkString(String str, String defaultValue) {
        if (str == null || "".equals(str.trim())) {
            return defaultValue;
        }
        return str;
    }

    public static String objectToString(Object o) {
        if (o == null) {
            return "";
        }
        if ("".equals(o)) {
            return "";
        }
        return o.toString();
    }

    /**
     * 过滤字符串内的所有script标签
     *
     * @param htmlStr
     * @return writer:<a href="mailto:benjobs@qq.com">benjobs</a> 2012.2.1
     */
    public static String escapeJavaScript(String htmlStr) {
        if (htmlStr == null || "".equals(htmlStr)) {
            return "";
        }
        String regEx_script = "<script[^>]*?>[\\s\\S]*?</script>"; // 定义script的正则表达式
        Pattern p_script = Pattern.compile(regEx_script,
                Pattern.CASE_INSENSITIVE);
        Matcher m_script = p_script.matcher(htmlStr);
        htmlStr = m_script.replaceAll(""); // 过滤script标签
        return htmlStr.trim(); // 返回文本字符串
    }

    /**
     * 过滤字符串内的所有html标签
     *
     * @param htmlStr
     * @return writer:<a href="mailto:benjobs@qq.com">benjobs</a> 2012.2.1
     */
    public static String escapeHtml(String htmlStr) {
        if (htmlStr == null || "".equals(htmlStr)) {
            return "";
        }
        String regEx_script = "<script[^>]*?>[\\s\\S]*?</script>"; // 定义script的正则表达式
        String regEx_style = "<style[^>]*?>[\\s\\S]*?</style>"; // 定义style的正则表达式
        String regEx_html = "<[^>]+>"; // 定义HTML标签的正则表达式

        Pattern p_script = Pattern.compile(regEx_script,
                Pattern.CASE_INSENSITIVE);
        Matcher m_script = p_script.matcher(htmlStr);
        htmlStr = m_script.replaceAll(""); // 过滤script标签

        Pattern p_style = Pattern
                .compile(regEx_style, Pattern.CASE_INSENSITIVE);
        Matcher m_style = p_style.matcher(htmlStr);
        htmlStr = m_style.replaceAll(""); // 过滤style标签

        Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_html.matcher(htmlStr);
        htmlStr = m_html.replaceAll(""); // 过滤html标签

        return htmlStr.trim(); // 返回文本字符串
    }

    public static String htmlEncode(String source) {
        if (source == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            switch (c) {
                case '<':
                    buffer.append("&lt;");
                    break;
                case '>':
                    buffer.append("&gt;");
                    break;
                case '&':
                    buffer.append("&amp;");
                    break;
                case '"':
                    buffer.append("&quot;");
                    break;
                case 10:
                case 13:
                    break;
                default:
                    buffer.append(c);
            }
        }
        return buffer.toString();

    }

    /**
     * 取字符串的前toCount个字符
     *
     * @param str     被处理字符串
     * @param toCount 截取长度
     * @param more    后缀字符串
     * @return String
     * @version 2004.11.24
     * @author zhulx
     */
    public static String subString(String str, int toCount, String more) {
        int reInt = 0;
        String reStr = "";
        if (str == null) {
            return "";
        }
        char[] tempChar = str.toCharArray();
        for (int kk = 0; (kk < tempChar.length && toCount > reInt); kk++) {
            String s1 = String.valueOf(tempChar[kk]);
            byte[] b = s1.getBytes();
            reInt += b.length;
            reStr += tempChar[kk];
        }
        if (toCount == reInt || (toCount == reInt - 1)) {
            reStr += more;
        }
        return reStr;
    }

    /**
     * 按字节截取字符串，并保证不会截取半个汉字
     *
     * @param str
     * @param byteLength
     * @return
     */
    public static String truncate(String str, int byteLength) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return str;
        }
        if (byteLength < 0) {
            throw new IllegalArgumentException(
                    "Parameter byteLength must be great than 0");
        }
        char[] chs = str.toCharArray();
        int i = 0;
        int len = 0;
        while ((len < byteLength) && (i < chs.length)) {
            len = (chs[i++] > 0xff) ? (len + 2) : (len + 1);
        }
        if (len > byteLength) {
            i--;
        }
        return new String(chs, 0, i);
    }

    /**
     * 取得字符串s的char长度
     *
     * @param s
     * @return
     */
    public static int getWordCount(String s) {
        int length = 0;
        for (int i = 0; i < s.length(); i++) {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <= 255)
                length++;
            else
                length += 2;
        }
        return length;
    }

    public static String changeCharset(String str, String newCharset) {
        if (str != null) {
            //用默认字符编码解码字符串。
            byte[] bs = str.getBytes();
            //用新的字符编码生成字符串
            try {
                return new String(bs, newCharset);
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return null;
    }

    public static <T> String join(T[] array, String limit) {
        if (array == null || array.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (T t : array) {
            sb.append(t.toString()).append(limit);
        }
        String str = sb.toString();
        return str.substring(0, str.length() - limit.length());
    }

    public static String toUpperCase(String str) {
        return toUpperCase(str, 1);
    }

    public static String toUpperCase(String str, int position) {
        if (CommonUtils.isEmpty(str)) {
            throw new NullPointerException("str can not be empty！！");
        }
        if (position <= 0 || position > str.length()) {
            throw new IndexOutOfBoundsException("Position must be greater than 0 and not less than the length of the string to be processed");
        }

        if (position == 1) {//将数个字母小写
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }

        return str.substring(0, position - 1) + str.substring(position - 1, position).toUpperCase() + str.substring(position);
    }

    public static String toUpperCase(String str, int index, int len) {
        if (CommonUtils.isEmpty(str))
            throw new NullPointerException("str can not be empty！！");
        if (index <= 0 || (index + len - 1) > str.length()) {
            throw new IndexOutOfBoundsException("Position must be greater than 0 and not less than the length of the string to be processed");
        }

        if (index == 1) {//将数个字母小写
            return str.substring(0, len).toUpperCase() + str.substring(len);
        }
        return str.substring(0, index - 1) + str.substring(index - 1, len + 1).toUpperCase() + str.substring(index + len - 1);
    }

    public static String toLowerCase(String str) {
        return toLowerCase(str, 1);
    }

    public static String toLowerCase(String str, int position) {
        assert str != null;
        if (position <= 0 || position > str.length()) {
            throw new IndexOutOfBoundsException("Position must be greater than 0 and not less than the length of the string to be processed");
        }

        if (position == 1) {//将数个字母小写
            return str.substring(0, 1).toLowerCase() + str.substring(1);
        }

        return str.substring(0, position - 1) + str.substring(position - 1, position).toLowerCase() + str.substring(position);
    }

    public static String toLowerCase(String str, int index, int len) {
        if (CommonUtils.isEmpty(str))
            throw new NullPointerException("str can not be empty！！");
        if (index <= 0 || (index + len - 1) > str.length()) {
            throw new IndexOutOfBoundsException("Position must be greater than 0 and not less than the length of the string to be processed");
        }

        if (index == 1) {//将数个字母小写
            return str.substring(0, len).toLowerCase() + str.substring(len);
        }
        return str.substring(0, index - 1) + str.substring(index - 1, len + 1).toLowerCase() + str.substring(index + len - 1);
    }

    public static String clearLine(String val) {
        assert val != null;
        return val.replaceAll("\\n|\\r", "");
    }

    public static String replaceBlank(String val) {
        assert val != null;
        return Pattern.compile("\\s*|\\t|\\r|\\n").matcher(val).replaceAll("");
    }

    public static String replace(Object obj, int start, int end, String s1) {
        if (CommonUtils.isEmpty(obj)) return "";
        if (start < 0 || end < 0) throw new IndexOutOfBoundsException("replace:startIndex and endIndex error");
        String str = obj.toString();
        String str1 = str.substring(0, start - 1);
        String str2 = str.substring(start + end - 1);
        String replStr = "";
        for (int j = 0; j < end; j++) {
            replStr += s1;
        }
        return str1 + replStr + str2;
    }

    /**
     * 生成计费ID bs+四位随机生成的
     */
    public static String generateString(int length) {
        String corpid = "";
        int value;
        for (int i = 0; i < length; i++) {
            value = (int) (Math.random() * 26); // 26 表示只用小写字母 52表示所有字母 61表示数字字母组合
            corpid += generateChar(value);
        }
        return corpid;
    }

    private static char generateChar(int value) {
        char temp = 't';
        if (value >= 0 && value < 26) {
            temp = (char) ('a' + value);
        } else if (value >= 26 && value < 52) {
            temp = (char) ('A' + value - 26);
        } else if (value >= 52 && value < 62) {
            temp = (char) ('0' + value - 52);
        }
        if (temp == 's') {
            temp = 'z';
        }
        return temp;
    }

    public static boolean isBlank(String str) {
        if (str == null || str.trim().length() == 0)
            return true;
        return false;
    }

    /**
     * is empty string.
     *
     * @param str source string.
     * @return is empty.
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.length() == 0)
            return true;
        return false;
    }

    /**
     * is not empty string.
     *
     * @param str source string.
     * @return is not empty.
     */
    public static boolean isNotEmpty(String str) {
        return str != null && str.length() > 0;
    }

    /**
     * @param s1
     * @param s2
     * @return equals
     */
    public static boolean isEquals(String s1, String s2) {
        if (s1 == null && s2 == null)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equals(s2);
    }

    /**
     * is integer string.
     *
     * @param str
     * @return is integer
     */
    public static boolean isInteger(String str) {
        if (str == null || str.length() == 0)
            return false;
        return INT_PATTERN.matcher(str).matches();
    }

    public static int parseInteger(String str) {
        if (!isInteger(str))
            return 0;
        return Integer.parseInt(str);
    }


    public static boolean isChinese(String string) {
        int n = 0;
        for (int i = 0; i < string.length(); i++) {
            n = (int) string.charAt(i);
            if ((19968 <= n && n < 40869)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if s is a legal Java identifier.<p>
     * <a href="http://www.exampledepot.com/egs/java.lang/IsJavaId.html">more info.</a>
     */
    public static boolean isJavaIdentifier(String s) {
        if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param values
     * @param value
     * @return contains
     */
    public static boolean isContains(String[] values, String value) {
        if (value != null && value.length() > 0 && values != null && values.length > 0) {
            for (String v : values) {
                if (value.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (Character.isDigit(str.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * translat.
     *
     * @param src  source string.
     * @param from src char table.
     * @param to   target char table.
     * @return String.
     */
    public static String translat(String src, String from, String to) {
        if (isEmpty(src)) return src;
        StringBuilder sb = null;
        int ix;
        char c;
        for (int i = 0, len = src.length(); i < len; i++) {
            c = src.charAt(i);
            ix = from.indexOf(c);
            if (ix == -1) {
                if (sb != null)
                    sb.append(c);
            } else {
                if (sb == null) {
                    sb = new StringBuilder(len);
                    sb.append(src, 0, i);
                }
                if (ix < to.length())
                    sb.append(to.charAt(ix));
            }
        }
        return sb == null ? src : sb.toString();
    }

    /**
     * split.
     *
     * @param ch char.
     * @return string array.
     */
    public static String[] split(String str, char ch) {
        List<String> list = null;
        char c;
        int ix = 0, len = str.length();
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            if (c == ch) {
                if (list == null)
                    list = new ArrayList<String>();
                list.add(str.substring(ix, i));
                ix = i + 1;
            }
        }
        if (ix > 0)
            list.add(str.substring(ix));
        return list == null ? EMPTY_STRING_ARRAY : (String[]) list.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * join string.
     *
     * @param array String array.
     * @return String.
     */
    public static String join(String[] array) {
        if (array.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : array)
            sb.append(s);
        return sb.toString();
    }

    /**
     * join string like javascript.
     *
     * @param array String array.
     * @param split split
     * @return String.
     */
    public static String join(String[] array, char split) {
        if (array.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(split);
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * join string like javascript.
     *
     * @param array String array.
     * @param split split
     * @return String.
     */
    public static String join(String[] array, String split) {
        if (array.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(split);
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static String join(Collection<String> coll, String split) {
        if (coll.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String s : coll) {
            if (isFirst) isFirst = false;
            else sb.append(split);
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * parse key-value pair.
     *
     * @param str           string.
     * @param itemSeparator item separator.
     * @return key-value map;
     */
    private static Map<String, String> parseKeyValuePair(String str, String itemSeparator) {
        String[] tmp = str.split(itemSeparator);
        Map<String, String> map = new ConcurrentHashMap<>(tmp.length);
        for (int i = 0; i < tmp.length; i++) {
            Matcher matcher = KVP_PATTERN.matcher(tmp[i]);
            if (matcher.matches() == false)
                continue;
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    public static String getQueryStringValue(String qs, String key) {
        Map<String, String> map = parseQueryString(qs);
        return map.get(key);
    }

    /**
     * parse query string to Parameters.
     *
     * @param qs query string.
     * @return Parameters instance.
     */
    public static Map<String, String> parseQueryString(String qs) {
        if (qs == null || qs.length() == 0)
            return new HashMap<String, String>();
        return parseKeyValuePair(qs, "\\&");
    }

    public static String toQueryString(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        if (ps != null && ps.size() > 0) {
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(ps).entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && key.length() > 0
                        && value != null && value.length() > 0) {
                    if (buf.length() > 0) {
                        buf.append("&");
                    }
                    buf.append(key);
                    buf.append("=");
                    buf.append(value);
                }
            }
        }
        return buf.toString();
    }

    public static String camelToSplitName(String camelName, String split) {
        if (camelName == null || camelName.length() == 0) {
            return camelName;
        }
        StringBuilder buf = null;
        for (int i = 0; i < camelName.length(); i++) {
            char ch = camelName.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                if (buf == null) {
                    buf = new StringBuilder();
                    if (i > 0) {
                        buf.append(camelName.substring(0, i));
                    }
                }
                if (i > 0) {
                    buf.append(split);
                }
                buf.append(Character.toLowerCase(ch));
            } else if (buf != null) {
                buf.append(ch);
            }
        }
        return buf == null ? camelName : buf.toString();
    }

    public static Long[] splitToLongArray(String agentIds, String s) {
        if (CommonUtils.isEmpty(agentIds)) {
            return new Long[0];
        }
        String array[] = stringToArray(agentIds, ",");
        if (CommonUtils.isEmpty(array)) return new Long[0];

        Long longArray[] = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            longArray[i] = Long.parseLong(array[i]);
        }
        return longArray;
    }

    public static Integer[] splitToIntArray(String agentIds, String s) {
        if (CommonUtils.isEmpty(agentIds)) {
            return new Integer[0];
        }
        String array[] = stringToArray(agentIds, ",");
        if (CommonUtils.isEmpty(array)) return new Integer[0];

        Integer intArray[] = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = Integer.parseInt(array[i]);
        }
        return intArray;
    }

    public static String line(int count) {
        String str = "";
        int index = 0;
        while (true) {
            if (index == count) break;
            str += "\n";
            ++index;
        }
        return str;
    }

    public static String line(String separator, int count) {
        String str = "";
        int index = 0;
        while (true) {
            if (index == count) break;
            str += separator;
            ++index;
        }
        return str;
    }


    public static String tab(int count) {
        String str = "";
        int index = 0;
        while (true) {
            if (index == count) break;
            str += "\t";
            ++index;
        }
        return str;
    }

    public static void main(String[] args) {
        System.out.println(camelToSplitName("getName", "_"));
    }

}

