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

package org.apache.streampark.plugin.profiling;

import org.apache.streampark.plugin.profiling.reporter.ConsoleOutputReporter;
import org.apache.streampark.plugin.profiling.util.ClassAndMethod;
import org.apache.streampark.plugin.profiling.util.ClassMethodArgument;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ArgumentsTest {

    @Test
    void allArguments() {
        Arguments arguments =
            Arguments.parseArgs(
                "reporter=org.apache.streampark.plugin.profiling.ArgumentsTest$DummyReporter,durationProfiling=a.bc.foo,metricInterval=123,appIdVariable=APP_ID1,appIdRegex=app123,argumentProfiling=package1.class1.method1.1");
        Assertions.assertEquals(6, arguments.getRawArgValues().size());
        Assertions.assertFalse(arguments.isNoop());
        Assertions.assertEquals(DummyReporter.class, arguments.getReporter().getClass());
        Assertions.assertEquals(1, arguments.getDurationProfiling().size());
        Assertions.assertEquals(new ClassAndMethod("a.bc", "foo"), arguments.getDurationProfiling().get(0));
        Assertions.assertEquals(123, arguments.getMetricInterval());
        Assertions.assertEquals("APP_ID1", arguments.getAppIdVariable());
        Assertions.assertEquals("app123", arguments.getAppIdRegex());

        Assertions.assertEquals(1, arguments.getArgumentProfiling().size());
        Assertions.assertEquals(
            new ClassMethodArgument("package1.class1", "method1", 1),
            arguments.getArgumentProfiling().get(0));
    }

    @Test
    void emptyArguments() {
        Arguments arguments = Arguments.parseArgs("");
        Assertions.assertEquals(0, arguments.getRawArgValues().size());
        Assertions.assertFalse(arguments.isNoop());
        Assertions.assertEquals(ConsoleOutputReporter.class, arguments.getReporter().getClass());
        Assertions.assertEquals(0, arguments.getDurationProfiling().size());
        Assertions.assertEquals(60000, arguments.getMetricInterval());
        Assertions.assertEquals(0, arguments.getArgumentProfiling().size());
        Assertions.assertNull(arguments.getTag());
        Assertions.assertNull(arguments.getCluster());
        Assertions.assertNull(arguments.getAppIdVariable());
    }

    @Test()
    void emptyArgumentValue() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Arguments.parseArgs("reporter=,durationProfiling=,metricInterval=,appIdRegex=,"));
    }

    @Test
    void noop() {
        Arguments arguments =
            Arguments.parseArgs("durationProfiling=a.bc.foo,noop=true,durationProfiling=ab.c.d.test");
        Assertions.assertEquals(2, arguments.getRawArgValues().size());
        Assertions.assertTrue(arguments.isNoop());
        Assertions.assertEquals(2, arguments.getDurationProfiling().size());
    }

    @Test
    void durationProfiling() {
        Arguments arguments =
            Arguments.parseArgs("durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test");
        Assertions.assertEquals(2, arguments.getDurationProfiling().size());
        Assertions.assertEquals(new ClassAndMethod("a.bc", "foo"), arguments.getDurationProfiling().get(0));
        Assertions.assertEquals(
            new ClassAndMethod("ab.c.d", "test"), arguments.getDurationProfiling().get(1));
        Assertions.assertEquals(Arguments.DEFAULT_METRIC_INTERVAL, arguments.getMetricInterval());
        Assertions.assertEquals(Arguments.DEFAULT_APP_ID_REGEX, arguments.getAppIdRegex());
    }

    @Test
    void argumentProfiling() {
        Arguments arguments =
            Arguments.parseArgs("durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test");
        Assertions.assertEquals(2, arguments.getDurationProfiling().size());
        Assertions.assertEquals(new ClassAndMethod("a.bc", "foo"), arguments.getDurationProfiling().get(0));
        Assertions.assertEquals(
            new ClassAndMethod("ab.c.d", "test"), arguments.getDurationProfiling().get(1));
        Assertions.assertEquals(Arguments.DEFAULT_METRIC_INTERVAL, arguments.getMetricInterval());
        Assertions.assertEquals(Arguments.DEFAULT_APP_ID_REGEX, arguments.getAppIdRegex());
    }

    @Test
    void setReporter() {
        Arguments arguments = Arguments.parseArgs("");

        arguments.setReporter("org.apache.streampark.plugin.profiling.ArgumentsTest$DummyReporter");
        Reporter reporter = arguments.getReporter();
        Assertions.assertTrue(reporter instanceof DummyReporter);
    }

    @Test
    void setConfigProvider() {
        Arguments arguments = Arguments.parseArgs("");

        arguments.setConfigProvider(
            "org.apache.streampark.plugin.profiling.ArgumentsTest$DummyConfigProvider");
        ConfigProvider configProvider = arguments.getConfigProvider();
        Assertions.assertTrue(configProvider instanceof DummyConfigProvider);
    }

    @Test
    void processConfigProvider_DummyConfigProvider() {
        Arguments arguments =
            Arguments.parseArgs(
                "tag=tag1,cluster=cluster1,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=org.apache.streampark.plugin.profiling.ArgumentsTest$DummyConfigProvider");

        arguments.runConfigProvider();

        Assertions.assertEquals("tag1", arguments.getTag());
        Assertions.assertEquals("cluster1", arguments.getCluster());
        Assertions.assertEquals(1000L, arguments.getMetricInterval());
        Assertions.assertTrue(arguments.isIoProfiling());
        Assertions.assertArrayEquals(
            new ClassAndMethod[] {
                new ClassAndMethod("a.bc", "foo"), new ClassAndMethod("ab.c.d", "test")
            },
            arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));
    }

    @Test
    void processConfigProvider_SimpleConfigProvider() {
        Arguments arguments =
            Arguments.parseArgs(
                "tag=tag1,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=org.apache.streampark.plugin.profiling.ArgumentsTest$SimpleConfigProvider");

        arguments.runConfigProvider();

        Assertions.assertEquals("tag1", arguments.getTag());
        Assertions.assertEquals(9000L, arguments.getMetricInterval());
        Assertions.assertFalse(arguments.isIoProfiling());
        Assertions.assertArrayEquals(
            new ClassAndMethod[] {
                new ClassAndMethod("package.c900", "m900"), new ClassAndMethod("package.c901", "m901")
            },
            arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));
    }

    @Test
    void processConfigProvider_OverrideConfigProvider() {
        Arguments arguments =
            Arguments.parseArgs(
                "tag=tag1,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=org.apache.streampark.plugin.profiling.ArgumentsTest$OverrideConfigProvider");

        arguments.runConfigProvider();

        Assertions.assertEquals("tag1", arguments.getTag());
        Assertions.assertEquals(9001L, arguments.getMetricInterval());
        Assertions.assertFalse(arguments.isIoProfiling());
        Assertions.assertArrayEquals(
            new ClassAndMethod[] {
                new ClassAndMethod("package.c900", "m910"), new ClassAndMethod("package.c901", "m911")
            },
            arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));

        arguments =
            Arguments.parseArgs(
                "tag=tag2,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=org.apache.streampark.plugin.profiling.ArgumentsTest$OverrideConfigProvider");

        arguments.runConfigProvider();

        Assertions.assertEquals("tag2", arguments.getTag());
        Assertions.assertEquals(9002L, arguments.getMetricInterval());
        Assertions.assertTrue(arguments.isIoProfiling());
        Assertions.assertArrayEquals(
            new ClassAndMethod[] {
                new ClassAndMethod("package.c900", "m920"), new ClassAndMethod("package.c901", "m921")
            },
            arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));

        arguments =
            Arguments.parseArgs(
                "tag=tag3,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=org.apache.streampark.plugin.profiling.ArgumentsTest$OverrideConfigProvider");

        arguments.runConfigProvider();

        Assertions.assertEquals("tag3", arguments.getTag());
        Assertions.assertEquals(9000L, arguments.getMetricInterval());
        Assertions.assertTrue(arguments.isIoProfiling());
        Assertions.assertArrayEquals(
            new ClassAndMethod[] {
                new ClassAndMethod("package.c900", "m900"), new ClassAndMethod("package.c901", "m901")
            },
            arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));
    }

    public static class DummyReporter implements Reporter {
        @Override
        public void report(String profilerName, Map<String, Object> metrics) {
        }

        @Override
        public void close() {
        }
    }

    public static class DummyConfigProvider implements ConfigProvider {
        @Override
        public Map<String, Map<String, List<String>>> getConfig() {
            return new HashMap<>();
        }
    }

    public static class SimpleConfigProvider implements ConfigProvider {
        @Override
        public Map<String, Map<String, List<String>>> getConfig() {
            Map<String, Map<String, List<String>>> configMap = new HashMap<>();

            Map<String, List<String>> argMap = new HashMap<>();
            argMap.put("metricInterval", Collections.singletonList("9000"));
            argMap.put("ioProfiling", Collections.singletonList("false"));
            argMap.put("durationProfiling", Arrays.asList("package.c900.m900", "package.c901.m901"));

            configMap.put("", argMap);

            return configMap;
        }
    }

    public static class OverrideConfigProvider implements ConfigProvider {
        @Override
        public Map<String, Map<String, List<String>>> getConfig() {
            Map<String, Map<String, List<String>>> configMap = new HashMap<>();

            Map<String, List<String>> argMap = new HashMap<>();
            argMap.put("metricInterval", Collections.singletonList("9000"));
            argMap.put("ioProfiling", Collections.singletonList("true"));
            argMap.put("durationProfiling", Arrays.asList("package.c900.m900", "package.c901.m901"));

            configMap.put("", argMap);

            argMap = new HashMap<>();
            argMap.put("metricInterval", Collections.singletonList("9001"));
            argMap.put("ioProfiling", Collections.singletonList("false"));
            argMap.put("durationProfiling", Arrays.asList("package.c900.m910", "package.c901.m911"));

            configMap.put("tag1", argMap);

            argMap = new HashMap<>();
            argMap.put("metricInterval", Collections.singletonList("9002"));
            argMap.put("ioProfiling", Collections.singletonList("true"));
            argMap.put("durationProfiling", Arrays.asList("package.c900.m920", "package.c901.m921"));

            configMap.put("tag2", argMap);

            return configMap;
        }
    }
}
