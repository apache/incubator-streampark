package com.uber.profiling.profilers;

import com.uber.profiling.Reporter;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThreadInfoProfilerTest {
    @Test
    public void profile() {
        final List<String> nameList = new ArrayList<>();
        final List<Map<String, Object>> metricList = new ArrayList<>();

        // create a Profile Instance.
        ThreadInfoProfiler profiler = new ThreadInfoProfiler(new Reporter() {
            @Override
            public void report(String profilerName, Map<String, Object> metrics) {
                nameList.add(profilerName);
                metricList.add(metrics);
            }

            @Override
            public void close() {

            }
        });
        // Set interval
        profiler.setIntervalMillis(150);
        Assert.assertEquals(150L, profiler.getIntervalMillis());

        // run 2 cycles on the profile.
        profiler.profile();
        profiler.profile();

        //start assertion.
        Assert.assertEquals(2, nameList.size());
        Assert.assertEquals(ThreadInfoProfiler.PROFILER_NAME, nameList.get(0));

        Assert.assertEquals(2, metricList.size());
        Assert.assertTrue(metricList.get(0).containsKey("totalStartedThreadCount"));
        Assert.assertTrue(metricList.get(0).containsKey("newThreadCount"));
        Assert.assertTrue(metricList.get(0).containsKey("liveThreadCount"));
        Assert.assertTrue(metricList.get(0).containsKey("peakThreadCount"));
    }
}