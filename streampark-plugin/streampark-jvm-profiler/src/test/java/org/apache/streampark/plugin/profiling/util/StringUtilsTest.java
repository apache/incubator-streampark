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

package org.apache.streampark.plugin.profiling.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class StringUtilsTest {

    @Test
    void splitByLength() {
        List<String> list = StringUtils.splitByLength("a", 1);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("a", list.get(0));

        list = StringUtils.splitByLength("abc", 1);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("c", list.get(2));

        list = StringUtils.splitByLength("abcd", 2);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("ab", list.get(0));
        Assertions.assertEquals("cd", list.get(1));

        list = StringUtils.splitByLength("abcde", 2);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("ab", list.get(0));
        Assertions.assertEquals("cd", list.get(1));
        Assertions.assertEquals("e", list.get(2));
    }

    @Test
    void extractByRegex() {
        Assertions.assertEquals(0, StringUtils.extractByRegex(null, null).size());
        Assertions.assertEquals(1, StringUtils.extractByRegex("", "").size());
        Assertions.assertEquals("", StringUtils.extractByRegex("", "").get(0));

        List<String> list =
            StringUtils.extractByRegex(
                "appcache/application_1498604172385_2751189/container_e241_1498604172385_2751189_01_000267",
                "application_[\\w_]+");
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("application_1498604172385_2751189", list.get(0));

        list =
            StringUtils.extractByRegex(
                "appcache/application_1498604172385_2751189/container_e241_1498604172385_2751189_01_000267,appcache/application_1498604172385_2751190/container_e241_1498604172385_2751189_01_000267",
                "application_[\\w_]+");
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("application_1498604172385_2751189", list.get(0));
        Assertions.assertEquals("application_1498604172385_2751190", list.get(1));
    }

    @Test
    void getValueAsBytes() {
        Assertions.assertNull(StringUtils.getBytesValueOrNull(null));
        Assertions.assertNull(StringUtils.getBytesValueOrNull(""));
        Assertions.assertNull(StringUtils.getBytesValueOrNull("xxx"));

        Assertions.assertEquals(0L, StringUtils.getBytesValueOrNull("0").longValue());
        Assertions.assertEquals(123L, StringUtils.getBytesValueOrNull("123").longValue());
        Assertions.assertEquals(123L, StringUtils.getBytesValueOrNull("123.").longValue());
        Assertions.assertEquals(123L, StringUtils.getBytesValueOrNull("123.123").longValue());

        Assertions.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123k").longValue());
        Assertions.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123kb").longValue());
        Assertions.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123 kb").longValue());
        Assertions.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123K").longValue());
        Assertions.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123KB").longValue());
        Assertions.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123 KB").longValue());

        Assertions.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123m").longValue());
        Assertions.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123mb").longValue());
        Assertions.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123 mb").longValue());
        Assertions.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123M").longValue());

        Assertions.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123g").longValue());
        Assertions.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123gb").longValue());
        Assertions.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123 gb").longValue());
        Assertions.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123G").longValue());

        Assertions.assertEquals(123L, StringUtils.getBytesValueOrNull("123bytes").longValue());
        Assertions.assertEquals(123L, StringUtils.getBytesValueOrNull("123 Bytes").longValue());
    }

    @Test
    void getArgumentValue() {
        Assertions.assertNull(StringUtils.getArgumentValue(null, null));
        Assertions.assertNull(StringUtils.getArgumentValue(null, ""));
        Assertions.assertNull(StringUtils.getArgumentValue("", null));

        Assertions.assertNull(StringUtils.getArgumentValue("", ""));
        Assertions.assertNull(StringUtils.getArgumentValue("test", ""));
        Assertions.assertNull(StringUtils.getArgumentValue("", "test"));

        Assertions.assertEquals(
            "com.foo.jobs.Abc", StringUtils.getArgumentValue("--class com.foo.jobs.Abc", "--class"));
        Assertions.assertEquals(
            "com.foo.jobs.Abc", StringUtils.getArgumentValue(" --class  com.foo.jobs.Abc ", "--class"));
        Assertions.assertEquals(
            "com.foo.jobs.Abc", StringUtils.getArgumentValue(" --class  com.foo.jobs.Abc ", "--class"));
        Assertions.assertEquals(
            "com.foo.jobs.Abc",
            StringUtils.getArgumentValue(" --class  'com.foo.jobs.Abc' ", "--class"));
        Assertions.assertEquals(
            "com.foo.jobs.Abc",
            StringUtils.getArgumentValue(" --class  \"com.foo.jobs.Abc\" ", "--class"));
        Assertions.assertEquals(
            " com.foo.jobs.Abc ",
            StringUtils.getArgumentValue(" --class  ' com.foo.jobs.Abc ' ", "--class"));

        Assertions.assertEquals(
            "com.foo.jobs.Abc",
            StringUtils.getArgumentValue(
                "xyz --class com.foo.jobs.Abc --jar file:/home/test/hi.jar --others world", "--class"));
    }

    @Test
    void getArgumentValues() {
        Assertions.assertArrayEquals(new String[0], StringUtils.getArgumentValues(null, null));
        Assertions.assertArrayEquals(new String[0], StringUtils.getArgumentValues(null, ""));
        Assertions.assertArrayEquals(new String[0], StringUtils.getArgumentValues("", null));

        Assertions.assertArrayEquals(new String[0], StringUtils.getArgumentValues("", ""));
        Assertions.assertArrayEquals(new String[0], StringUtils.getArgumentValues("test", ""));
        Assertions.assertArrayEquals(new String[0], StringUtils.getArgumentValues("", "test"));

        Assertions.assertArrayEquals(
            new String[] {"com.foo.jobs.Abc"},
            StringUtils.getArgumentValues("--class com.foo.jobs.Abc", "--class"));
        Assertions.assertArrayEquals(
            new String[] {"com.foo.jobs.Abc", "com.foo.jobs.Abc", " com.foo.jobs.Abc "},
            StringUtils.getArgumentValues(
                " --class  \"com.foo.jobs.Abc\"  --class com.foo.jobs.Abc --class  ' com.foo.jobs.Abc ' ",
                "--class"));

        Assertions.assertArrayEquals(
            new String[] {"com.foo.jobs.Abc"},
            StringUtils.getArgumentValues(
                "xyz --class com.foo.jobs.Abc --jar file:/home/test/hi.jar --others world", "--class"));
    }
}
