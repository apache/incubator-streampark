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

import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.common.util.ThreadUtils;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Building pipeline abstract class. */
public abstract class BuildPipeline extends LoggerSupport
    implements
        BuildPipelineProcess,
        BuildPipelineExpose {

    private static final ThreadPoolExecutor EXEC_POOL =
        new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            300,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2048),
            ThreadUtils.threadFactory("streampark-pipeline-watcher-executor"),
            new ThreadPoolExecutor.AbortPolicy());

    protected PipelineStatusEnum pipeStatus = PipelineStatusEnum.pending;

    protected PipeError error = PipeError.empty();

    protected int curStep = 0;

    protected final Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> stepsStatus =
        new LinkedHashMap<>();

    private PipeWatcher watcher = new SilentPipeWatcher();

    protected BuildPipeline() {
        pipeType()
            .getSteps()
            .forEach(
                (seq, desc) -> stepsStatus.put(
                    seq,
                    new AbstractMap.SimpleEntry<>(
                        PipelineStepStatusEnum.waiting, System.currentTimeMillis())));
    }

    /** use to identify the log record that belongs to which pipeline instance */
    private String logSuffix() {
        return "appName=" + offerBuildParam().appName();
    }

    public BuildPipeline registerWatcher(PipeWatcher watcher) {
        this.watcher = watcher;
        return this;
    }

    private void notifyStart() {
        try {
            watcher.onStart(snapshot());
        } catch (Exception e) {
            logError("Pipeline watcher onStart callback failed", e);
        }
    }

    private void notifyStepChange() {
        try {
            watcher.onStepStateChange(snapshot());
        } catch (Exception e) {
            logError("Pipeline watcher onStepStateChange callback failed", e);
        }
    }

    private void notifyFinish(BuildResult result) {
        try {
            watcher.onFinish(snapshot(), result);
        } catch (Exception e) {
            logError("Pipeline watcher onFinish callback failed", e);
        }
    }

    protected <R> java.util.Optional<R> execStep(int seq, Callable<R> process) {
        try {
            curStep = seq;
            stepsStatus.put(
                seq,
                new AbstractMap.SimpleEntry<>(
                    PipelineStepStatusEnum.running, System.currentTimeMillis()));
            logInfo(
                "Building pipeline step["
                    + seq
                    + "/"
                    + allSteps()
                    + "] running => "
                    + pipeType().getSteps().get(seq));
            notifyStepChange();
            R result = process.call();
            stepsStatus.put(
                seq,
                new AbstractMap.SimpleEntry<>(
                    PipelineStepStatusEnum.success, System.currentTimeMillis()));
            logInfo("Building pipeline step[" + seq + "/" + allSteps() + "] success");
            notifyStepChange();
            return java.util.Optional.of(result);
        } catch (Exception cause) {
            stepsStatus.put(
                seq,
                new AbstractMap.SimpleEntry<>(
                    PipelineStepStatusEnum.failure, System.currentTimeMillis()));
            pipeStatus = PipelineStatusEnum.failure;
            error = PipeError.of(cause.getMessage(), cause);
            logInfo(
                "Building pipeline step["
                    + seq
                    + "/"
                    + allSteps()
                    + "] failure => "
                    + pipeType().getSteps().get(seq));
            notifyStepChange();
            return java.util.Optional.empty();
        }
    }

    protected void skipStep(int step) {
        curStep = step;
        stepsStatus.put(
            step,
            new AbstractMap.SimpleEntry<>(
                PipelineStepStatusEnum.skipped, System.currentTimeMillis()));
        logInfo(
            "Building pipeline step["
                + step
                + "/"
                + allSteps()
                + "] skipped => "
                + pipeType().getSteps().get(step));
        notifyStepChange();
    }

    /** Launch the building pipeline. */
    @Override
    public BuildResult launch() {
        pipeStatus = PipelineStatusEnum.running;
        try {
            notifyStart();
            logInfo("Building pipeline is launching, params=" + offerBuildParam());
            BuildResult result =
                EXEC_POOL.submit(this::buildProcess).get(20, TimeUnit.MINUTES);
            pipeStatus = PipelineStatusEnum.success;
            logInfo("Building pipeline has finished successfully.");
            notifyFinish(result);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pipeStatus = PipelineStatusEnum.failure;
            error = PipeError.of(e.getMessage(), e);
            logError("Building pipeline has failed.", e);
            BuildResult result = new ErrorResult();
            notifyFinish(result);
            return result;
        } catch (ExecutionException e) {
            pipeStatus = PipelineStatusEnum.failure;
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e;
            }
            error = PipeError.of(cause.getMessage(), cause);
            logError("Building pipeline has failed.", cause);
            BuildResult result = new ErrorResult();
            notifyFinish(result);
            return result;
        } catch (TimeoutException e) {
            pipeStatus = PipelineStatusEnum.failure;
            error = PipeError.of(e.getMessage(), e);
            logError("Building pipeline has failed.", e);
            BuildResult result = new ErrorResult();
            notifyFinish(result);
            return result;
        }
    }

    @Override
    public PipelineStatusEnum getPipeStatus() {
        return pipeStatus;
    }

    @Override
    public PipeError getError() {
        return error.copy();
    }

    @Override
    public Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> getStepsStatus() {
        return new LinkedHashMap<>(stepsStatus);
    }

    @Override
    public int getCurStep() {
        return curStep;
    }

    @Override
    public int allSteps() {
        return pipeType().getSteps().size();
    }

    @Override
    protected void logInfo(String msg) {
        super.logInfo("[streampark-packer] " + msg + " | " + logSuffix());
    }

    @Override
    protected void logError(String msg) {
        super.logError("[streampark-packer] " + msg + " | " + logSuffix());
    }

    @Override
    protected void logError(String msg, Throwable throwable) {
        super.logError("[streampark-packer] " + msg + " | " + logSuffix(), throwable);
    }

    protected RuntimeException pipelineException() {
        Throwable ex = getError().exception();
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        if (ex != null) {
            return new RuntimeException(ex);
        }
        return new RuntimeException(getError().summary());
    }

    /** intercept snapshot */
    public PipelineSnapshot snapshot() {
        return new PipelineSnapshot(
            offerBuildParam().appName(),
            pipeType(),
            getPipeStatus(),
            getCurStep(),
            allSteps(),
            getStepsStatus(),
            getError(),
            System.currentTimeMillis());
    }
}
