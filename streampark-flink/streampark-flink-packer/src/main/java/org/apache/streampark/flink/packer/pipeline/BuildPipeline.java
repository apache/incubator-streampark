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

package org.apache.streampark.flink.packer.pipeline;

import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.common.util.ThreadUtils;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class BuildPipeline {

    protected static final Logger log =
        StreamParkLoggerFactory.loggerFactory().getLogger(BuildPipeline.class.getName());

    protected static final ThreadPoolExecutor EXEC_POOL = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 2,
        300,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(2048),
        ThreadUtils.threadFactory("streampark-pipeline-watcher-executor"),
        new ThreadPoolExecutor.AbortPolicy());

    protected PipelineStatusEnum pipeStatus = PipelineStatusEnum.pending;
    protected PipeError error = PipeError.empty();
    protected int curStep = 0;
    protected final Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> stepsStatus = new HashMap<>();
    protected PipeWatcher watcher = new SilentPipeWatcher();

    protected BuildPipeline() {
        for (Map.Entry<Integer, String> step : getPipeType().getSteps().entrySet()) {
            stepsStatus.put(step.getKey(), Map.entry(PipelineStepStatusEnum.waiting, System.currentTimeMillis()));
        }
    }

    public abstract PipelineTypeEnum getPipeType();
    protected abstract BuildParam offerBuildParam();
    protected abstract BuildResult buildProcess() throws Throwable;

    public BuildPipeline registerWatcher(PipeWatcher watcher) {
        this.watcher = watcher;
        return this;
    }

    protected <R> java.util.Optional<R> execStep(int seq, Callable<R> process) {
        try {
            curStep = seq;
            stepsStatus.put(seq, Map.entry(PipelineStepStatusEnum.running, System.currentTimeMillis()));
            log.info("[streampark-packer] Building pipeline step[{}/{}] running => {} | appName={}",
                seq, getAllSteps(), getPipeType().getSteps().get(seq), offerBuildParam().appName());
            watcher.onStepStateChange(snapshot());
            R result = process.call();
            stepsStatus.put(seq, Map.entry(PipelineStepStatusEnum.success, System.currentTimeMillis()));
            log.info("[streampark-packer] Building pipeline step[{}/{}] success | appName={}",
                seq, getAllSteps(), offerBuildParam().appName());
            watcher.onStepStateChange(snapshot());
            return java.util.Optional.of(result);
        } catch (Throwable cause) {
            stepsStatus.put(seq, Map.entry(PipelineStepStatusEnum.failure, System.currentTimeMillis()));
            pipeStatus = PipelineStatusEnum.failure;
            error = PipeError.of(cause.getMessage(), cause);
            log.info("[streampark-packer] Building pipeline step[{}/{}] failure => {} | appName={}",
                seq, getAllSteps(), getPipeType().getSteps().get(seq), offerBuildParam().appName());
            try {
                watcher.onStepStateChange(snapshot());
            } catch (Exception ignored) {
            }
            return java.util.Optional.empty();
        }
    }

    protected void skipStep(int step) {
        curStep = step;
        stepsStatus.put(step, Map.entry(PipelineStepStatusEnum.skipped, System.currentTimeMillis()));
        log.info("[streampark-packer] Building pipeline step[{}/{}] skipped => {} | appName={}",
            step, getAllSteps(), getPipeType().getSteps().get(step), offerBuildParam().appName());
        try {
            watcher.onStepStateChange(snapshot());
        } catch (Exception ignored) {
        }
    }

    public BuildResult launch() {
        pipeStatus = PipelineStatusEnum.running;
        try {
            watcher.onStart(snapshot());
            log.info("[streampark-packer] Building pipeline is launching, params={} | appName={}",
                offerBuildParam(), offerBuildParam().appName());
            BuildResult result =
                EXEC_POOL
                    .submit(
                        (Callable<BuildResult>) () -> {
                            try {
                                return buildProcess();
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        })
                    .get(20, TimeUnit.MINUTES);
            pipeStatus = PipelineStatusEnum.success;
            log.info("[streampark-packer] Building pipeline has finished successfully. | appName={}",
                offerBuildParam().appName());
            watcher.onFinish(snapshot(), result);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pipeStatus = PipelineStatusEnum.failure;
            error = PipeError.of(e.getMessage(), e);
            log.error("[streampark-packer] Building pipeline interrupted. | appName={}",
                offerBuildParam().appName(), e);
            BuildResult result = new ErrorResult();
            try {
                watcher.onFinish(snapshot(), result);
            } catch (Exception ignored) {
            }
            return result;
        } catch (Throwable cause) {
            pipeStatus = PipelineStatusEnum.failure;
            error = PipeError.of(cause.getMessage(), cause);
            log.error("[streampark-packer] Building pipeline has failed. | appName={}",
                offerBuildParam().appName(), cause);
            BuildResult result = new ErrorResult();
            try {
                watcher.onFinish(snapshot(), result);
            } catch (Exception ignored) {
            }
            return result;
        }
    }

    public PipelineStatusEnum getPipeStatus() {
        return pipeStatus;
    }
    public PipeError getError() {
        return error.copy();
    }
    public Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> getStepsStatus() {
        return stepsStatus;
    }
    public int getCurStep() {
        return curStep;
    }
    public int getAllSteps() {
        return getPipeType().getSteps().size();
    }

    public PipelineTypeEnum pipeType() {
        return getPipeType();
    }

    public PipelineSnapshot snapshot() {
        return new PipelineSnapshot(
            offerBuildParam().appName(),
            getPipeType(),
            getPipeStatus(),
            getCurStep(),
            getAllSteps(),
            getStepsStatus(),
            getError(),
            System.currentTimeMillis());
    }

    @SuppressWarnings("unchecked")
    public <T extends BuildPipeline> T as(Class<T> clz) {
        return (T) this;
    }
}
