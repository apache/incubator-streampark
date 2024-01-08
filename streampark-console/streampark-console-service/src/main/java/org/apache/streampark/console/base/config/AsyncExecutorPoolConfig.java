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

package org.apache.streampark.console.base.config;

import org.apache.streampark.common.util.ThreadUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AsyncExecutorPoolConfig extends AsyncConfigurerSupport {
  @Bean
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setKeepAliveSeconds(30);
    executor.setThreadNamePrefix("asyncTaskExecutor-");

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return executor;
  }

  @Bean("triggerSavepointExecutor")
  public Executor savepointExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 5);
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 10);
    executor.setQueueCapacity(1024);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("trigger-savepoint-executor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    return executor;
  }

  @Bean("flinkRestAPIWatchingExecutor")
  public Executor restAPIWatchingExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 5);
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 10);
    executor.setQueueCapacity(1024);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("flink-restapi-watching-executor-");
    return executor;
  }

  @Bean("flinkClusterWatchingExecutor")
  public Executor clusterWatchingExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 5);
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 10);
    executor.setQueueCapacity(1024);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("flink-cluster-watching-executor-");
    return executor;
  }

  @Bean("streamparkBuildPipelineExecutor")
  public ExecutorService pipelineExecutor() {
    return new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 5,
        Runtime.getRuntime().availableProcessors() * 10,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streampark-build-pipeline-executor"),
        new ThreadPoolExecutor.AbortPolicy());
  }

  @Bean("streamparkClusterExecutor")
  public ExecutorService clusterExecutor() {
    return new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 5,
        Runtime.getRuntime().availableProcessors() * 10,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streampark-cluster-executor"),
        new ThreadPoolExecutor.AbortPolicy());
  }

  @Bean("streamparkNotifyExecutor")
  public Executor notifyExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 5);
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 10);
    executor.setQueueCapacity(1024);
    executor.setKeepAliveSeconds(20);
    executor.setThreadNamePrefix("streampark-notify-executor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    return executor;
  }

  @Bean("streamparkDeployExecutor")
  public Executor deployExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 5);
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 10);
    executor.setQueueCapacity(1024);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("streampark-deploy-executor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    return executor;
  }

  @Bean("streamparkBuildExecutor")
  public Executor buildExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 5);
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 10);
    executor.setQueueCapacity(1024);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("streampark-build-executor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    return executor;
  }
}
