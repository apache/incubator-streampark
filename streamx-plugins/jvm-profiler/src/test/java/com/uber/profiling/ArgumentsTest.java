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

package com.uber.profiling;

import com.uber.profiling.reporters.ConsoleOutputReporter;
import com.uber.profiling.util.ClassAndMethod;
import com.uber.profiling.util.ClassMethodArgument;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgumentsTest {
    @Test
    public void allArguments() {
        Arguments arguments = Arguments.parseArgs("reporter=com.uber.profiling.ArgumentsTest$DummyReporter,durationProfiling=a.bc.foo,metricInterval=123,appIdVariable=APP_ID1,appIdRegex=app123,argumentProfiling=package1.class1.method1.1");
        Assert.assertEquals(6, arguments.getRawArgValues().size());
        Assert.assertFalse(arguments.isNoop());
        Assert.assertEquals(DummyReporter.class, arguments.getReporter().getClass());
        Assert.assertEquals(1, arguments.getDurationProfiling().size());
        Assert.assertEquals(new ClassAndMethod("a.bc", "foo"), arguments.getDurationProfiling().get(0));
        Assert.assertEquals(123, arguments.getMetricInterval());
        Assert.assertEquals("APP_ID1", arguments.getAppIdVariable());
        Assert.assertEquals("app123", arguments.getAppIdRegex());

        Assert.assertEquals(1, arguments.getArgumentProfiling().size());
        Assert.assertEquals(new ClassMethodArgument("package1.class1", "method1", 1), arguments.getArgumentProfiling().get(0));
    }

    @Test
    public void emptyArguments() {
        Arguments arguments = Arguments.parseArgs("");
        Assert.assertEquals(0, arguments.getRawArgValues().size());
        Assert.assertFalse(arguments.isNoop());
        Assert.assertEquals(ConsoleOutputReporter.class, arguments.getReporter().getClass());
        Assert.assertEquals(0, arguments.getDurationProfiling().size());
        Assert.assertEquals(60000, arguments.getMetricInterval());
        Assert.assertEquals(0, arguments.getArgumentProfiling().size());
        Assert.assertNull(arguments.getTag());
        Assert.assertNull(arguments.getCluster());
        Assert.assertNull(arguments.getAppIdVariable());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyArgumentValue() {
        Arguments.parseArgs("reporter=,durationProfiling=,metricInterval=,appIdRegex=,");
    }

    @Test
    public void noop() {
        Arguments arguments = Arguments.parseArgs("durationProfiling=a.bc.foo,noop=true,durationProfiling=ab.c.d.test");
        Assert.assertEquals(2, arguments.getRawArgValues().size());
        Assert.assertTrue(arguments.isNoop());
        Assert.assertEquals(2, arguments.getDurationProfiling().size());
    }
    
    @Test
    public void durationProfiling() {
        Arguments arguments = Arguments.parseArgs("durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test");
        Assert.assertEquals(2, arguments.getDurationProfiling().size());
        Assert.assertEquals(new ClassAndMethod("a.bc", "foo"), arguments.getDurationProfiling().get(0));
        Assert.assertEquals(new ClassAndMethod("ab.c.d", "test"), arguments.getDurationProfiling().get(1));
        Assert.assertEquals(Arguments.DEFAULT_METRIC_INTERVAL, arguments.getMetricInterval());
        Assert.assertEquals(Arguments.DEFAULT_APP_ID_REGEX, arguments.getAppIdRegex());
    }

    @Test
    public void argumentProfiling() {
        Arguments arguments = Arguments.parseArgs("durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test");
        Assert.assertEquals(2, arguments.getDurationProfiling().size());
        Assert.assertEquals(new ClassAndMethod("a.bc", "foo"), arguments.getDurationProfiling().get(0));
        Assert.assertEquals(new ClassAndMethod("ab.c.d", "test"), arguments.getDurationProfiling().get(1));
        Assert.assertEquals(Arguments.DEFAULT_METRIC_INTERVAL, arguments.getMetricInterval());
        Assert.assertEquals(Arguments.DEFAULT_APP_ID_REGEX, arguments.getAppIdRegex());
    }

    @Test
    public void setReporter() {
        Arguments arguments = Arguments.parseArgs("");

        arguments.setReporter("com.uber.profiling.ArgumentsTest$DummyReporter");
        Reporter reporter = arguments.getReporter();
        Assert.assertTrue(reporter instanceof com.uber.profiling.ArgumentsTest.DummyReporter);
    }

    @Test
    public void setConfigProvider() {
        Arguments arguments = Arguments.parseArgs("");

        arguments.setConfigProvider("com.uber.profiling.ArgumentsTest$DummyConfigProvider");
        ConfigProvider configProvider = arguments.getConfigProvider();
        Assert.assertTrue(configProvider instanceof com.uber.profiling.ArgumentsTest.DummyConfigProvider);
    }

    @Test
    public void processConfigProvider_DummyConfigProvider() {
        Arguments arguments = Arguments.parseArgs(
                "tag=tag1,cluster=cluster1,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=com.uber.profiling.ArgumentsTest$DummyConfigProvider");

        arguments.runConfigProvider();
        
        Assert.assertEquals("tag1", arguments.getTag());
        Assert.assertEquals("cluster1", arguments.getCluster());
        Assert.assertEquals(1000L, arguments.getMetricInterval());
        Assert.assertEquals(true, arguments.isIoProfiling());
        Assert.assertArrayEquals(
                new ClassAndMethod[]{new ClassAndMethod("a.bc", "foo"), new ClassAndMethod("ab.c.d", "test")}, 
                arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));
    }

    @Test
    public void processConfigProvider_SimpleConfigProvider() {
        Arguments arguments = Arguments.parseArgs(
                "tag=tag1,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=com.uber.profiling.ArgumentsTest$SimpleConfigProvider");
        
        arguments.runConfigProvider();
        
        Assert.assertEquals("tag1", arguments.getTag());
        Assert.assertEquals(9000L, arguments.getMetricInterval());
        Assert.assertEquals(false, arguments.isIoProfiling());
        Assert.assertArrayEquals(
                new ClassAndMethod[]{new ClassAndMethod("package.c900", "m900"), new ClassAndMethod("package.c901", "m901")},
                arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));
    }

    @Test
    public void processConfigProvider_OverrideConfigProvider() {
        Arguments arguments = Arguments.parseArgs(
                "tag=tag1,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=com.uber.profiling.ArgumentsTest$OverrideConfigProvider");
        
        arguments.runConfigProvider();
        
        Assert.assertEquals("tag1", arguments.getTag());
        Assert.assertEquals(9001L, arguments.getMetricInterval());
        Assert.assertEquals(false, arguments.isIoProfiling());
        Assert.assertArrayEquals(
                new ClassAndMethod[]{new ClassAndMethod("package.c900", "m910"), new ClassAndMethod("package.c901", "m911")},
                arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));

        arguments = Arguments.parseArgs(
                "tag=tag2,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=com.uber.profiling.ArgumentsTest$OverrideConfigProvider");

        arguments.runConfigProvider();
        
        Assert.assertEquals("tag2", arguments.getTag());
        Assert.assertEquals(9002L, arguments.getMetricInterval());
        Assert.assertEquals(true, arguments.isIoProfiling());
        Assert.assertArrayEquals(
                new ClassAndMethod[]{new ClassAndMethod("package.c900", "m920"), new ClassAndMethod("package.c901", "m921")},
                arguments.getDurationProfiling().toArray(new ClassAndMethod[2]));

        arguments = Arguments.parseArgs(
                "tag=tag3,metricInterval=1000,ioProfiling=true,durationProfiling=a.bc.foo,durationProfiling=ab.c.d.test,configProvider=com.uber.profiling.ArgumentsTest$OverrideConfigProvider");

        arguments.runConfigProvider();
        
        Assert.assertEquals("tag3", arguments.getTag());
        Assert.assertEquals(9000L, arguments.getMetricInterval());
        Assert.assertEquals(true, arguments.isIoProfiling());
        Assert.assertArrayEquals(
                new ClassAndMethod[]{new ClassAndMethod("package.c900", "m900"), new ClassAndMethod("package.c901", "m901")},
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
            argMap.put("metricInterval", Arrays.asList("9000"));
            argMap.put("ioProfiling", Arrays.asList("false"));
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
            argMap.put("metricInterval", Arrays.asList("9000"));
            argMap.put("ioProfiling", Arrays.asList("true"));
            argMap.put("durationProfiling", Arrays.asList("package.c900.m900", "package.c901.m901"));

            configMap.put("", argMap);

            argMap = new HashMap<>();
            argMap.put("metricInterval", Arrays.asList("9001"));
            argMap.put("ioProfiling", Arrays.asList("false"));
            argMap.put("durationProfiling", Arrays.asList("package.c900.m910", "package.c901.m911"));

            configMap.put("tag1", argMap);

            argMap = new HashMap<>();
            argMap.put("metricInterval", Arrays.asList("9002"));
            argMap.put("ioProfiling", Arrays.asList("true"));
            argMap.put("durationProfiling", Arrays.asList("package.c900.m920", "package.c901.m921"));

            configMap.put("tag2", argMap);

            return configMap;
        }
    }
}
