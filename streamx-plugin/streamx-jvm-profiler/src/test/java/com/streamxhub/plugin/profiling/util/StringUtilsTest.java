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

package com.streamxhub.plugin.profiling.util;

import com.streamxhub.streamx.plugin.profiling.util.StringUtils;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class StringUtilsTest {
    @Test
    public void splitByLength() {
        List<String> list = StringUtils.splitByLength("a", 1);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("a", list.get(0));

        list = StringUtils.splitByLength("abc", 1);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("a", list.get(0));
        Assert.assertEquals("c", list.get(2));

        list = StringUtils.splitByLength("abcd", 2);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("ab", list.get(0));
        Assert.assertEquals("cd", list.get(1));

        list = StringUtils.splitByLength("abcde", 2);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("ab", list.get(0));
        Assert.assertEquals("cd", list.get(1));
        Assert.assertEquals("e", list.get(2));
    }

    @Test
    public void extractByRegex() {
        Assert.assertEquals(0, StringUtils.extractByRegex(null, null).size());
        Assert.assertEquals(1, StringUtils.extractByRegex("", "").size());
        Assert.assertEquals("", StringUtils.extractByRegex("", "").get(0));

        List<String> list =
            StringUtils.extractByRegex(
                "appcache/application_1498604172385_2751189/container_e241_1498604172385_2751189_01_000267",
                "application_[\\w_]+");
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("application_1498604172385_2751189", list.get(0));

        list =
            StringUtils.extractByRegex(
                "appcache/application_1498604172385_2751189/container_e241_1498604172385_2751189_01_000267,appcache/application_1498604172385_2751190/container_e241_1498604172385_2751189_01_000267",
                "application_[\\w_]+");
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("application_1498604172385_2751189", list.get(0));
        Assert.assertEquals("application_1498604172385_2751190", list.get(1));
    }

    @Test
    public void getValueAsBytes() {
        Assert.assertEquals(null, StringUtils.getBytesValueOrNull(null));
        Assert.assertEquals(null, StringUtils.getBytesValueOrNull(""));
        Assert.assertEquals(null, StringUtils.getBytesValueOrNull("xxx"));

        Assert.assertEquals(0L, StringUtils.getBytesValueOrNull("0").longValue());
        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123").longValue());
        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123.").longValue());
        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123.123").longValue());

        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123k").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123kb").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123 kb").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123K").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123KB").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123 KB").longValue());

        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123m").longValue());
        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123mb").longValue());
        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123 mb").longValue());
        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123M").longValue());

        Assert.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123g").longValue());
        Assert.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123gb").longValue());
        Assert.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123 gb").longValue());
        Assert.assertEquals(
            123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123G").longValue());

        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123bytes").longValue());
        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123 Bytes").longValue());
    }

    @Test
    public void getArgumentValue() {
        Assert.assertEquals(null, StringUtils.getArgumentValue(null, null));
        Assert.assertEquals(null, StringUtils.getArgumentValue(null, ""));
        Assert.assertEquals(null, StringUtils.getArgumentValue("", null));

        Assert.assertEquals(null, StringUtils.getArgumentValue("", ""));
        Assert.assertEquals(null, StringUtils.getArgumentValue("test", ""));
        Assert.assertEquals(null, StringUtils.getArgumentValue("", "test"));

        Assert.assertEquals(
            "com.foo.jobs.Abc", StringUtils.getArgumentValue("--class com.foo.jobs.Abc", "--class"));
        Assert.assertEquals(
            "com.foo.jobs.Abc", StringUtils.getArgumentValue(" --class  com.foo.jobs.Abc ", "--class"));
        Assert.assertEquals(
            "com.foo.jobs.Abc", StringUtils.getArgumentValue(" --class  com.foo.jobs.Abc ", "--class"));
        Assert.assertEquals(
            "com.foo.jobs.Abc",
            StringUtils.getArgumentValue(" --class  'com.foo.jobs.Abc' ", "--class"));
        Assert.assertEquals(
            "com.foo.jobs.Abc",
            StringUtils.getArgumentValue(" --class  \"com.foo.jobs.Abc\" ", "--class"));
        Assert.assertEquals(
            " com.foo.jobs.Abc ",
            StringUtils.getArgumentValue(" --class  ' com.foo.jobs.Abc ' ", "--class"));

        Assert.assertEquals(
            "com.foo.jobs.Abc",
            StringUtils.getArgumentValue(
                "xyz --class com.foo.jobs.Abc --jar file:/home/test/hi.jar --others world", "--class"));
    }

    @Test
    public void getArgumentValues() {
        Assert.assertArrayEquals(new String[0], StringUtils.getArgumentValues(null, null));
        Assert.assertArrayEquals(new String[0], StringUtils.getArgumentValues(null, ""));
        Assert.assertArrayEquals(new String[0], StringUtils.getArgumentValues("", null));

        Assert.assertArrayEquals(new String[0], StringUtils.getArgumentValues("", ""));
        Assert.assertArrayEquals(new String[0], StringUtils.getArgumentValues("test", ""));
        Assert.assertArrayEquals(new String[0], StringUtils.getArgumentValues("", "test"));

        Assert.assertArrayEquals(
            new String[]{"com.foo.jobs.Abc"},
            StringUtils.getArgumentValues("--class com.foo.jobs.Abc", "--class"));
        Assert.assertArrayEquals(
            new String[]{"com.foo.jobs.Abc", "com.foo.jobs.Abc", " com.foo.jobs.Abc "},
            StringUtils.getArgumentValues(
                " --class  \"com.foo.jobs.Abc\"  --class com.foo.jobs.Abc --class  ' com.foo.jobs.Abc ' ",
                "--class"));

        Assert.assertArrayEquals(
            new String[]{"com.foo.jobs.Abc"},
            StringUtils.getArgumentValues(
                "xyz --class com.foo.jobs.Abc --jar file:/home/test/hi.jar --others world", "--class"));
    }
}
