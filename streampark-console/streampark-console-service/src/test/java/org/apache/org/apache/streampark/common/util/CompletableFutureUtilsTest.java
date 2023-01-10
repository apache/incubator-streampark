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

package org.apache.org.apache.streampark.common.util;

import org.apache.streampark.common.util.CompletableFutureUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompletableFutureUtilsTest {

  @Test
  void testStartJobNormally() throws Exception {
    // It takes 5 seconds to start job.
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> runStart(5));

    // Stop job after 8 seconds.
    CompletableFuture.runAsync(() -> runStop(future, 8));

    AtomicBoolean isStartNormal = new AtomicBoolean(false);
    AtomicBoolean isStartException = new AtomicBoolean(false);

    // Set the timeout is 10 seconds for start job.
    CompletableFutureUtils.runTimeout(
            future,
            10L,
            TimeUnit.SECONDS,
            r -> isStartNormal.set(true),
            e -> isStartException.set(true))
        .get();

    Assertions.assertTrue(future.isDone());
    Assertions.assertTrue(isStartNormal.get());
    Assertions.assertFalse(isStartException.get());
  }

  @Test
  void testStopJobEarly() throws Exception {
    // It takes 10 seconds to start job.
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> runStart(10));

    // Stop job after 5 seconds.
    CompletableFuture.runAsync(() -> runStop(future, 5));

    AtomicBoolean isStartNormal = new AtomicBoolean(false);
    AtomicBoolean isStartException = new AtomicBoolean(false);

    // Set the timeout is 15 seconds for start job.
    CompletableFutureUtils.runTimeout(
            future,
            15L,
            TimeUnit.SECONDS,
            r -> {
              isStartNormal.set(true);
              throw new IllegalStateException(
                  "It shouldn't be called due to the job is stopped before the timeout.");
            },
            e -> {
              isStartException.set(true);
              Assertions.assertTrue(future.isCancelled());
              Assertions.assertTrue(e.getCause() instanceof CancellationException);
              System.out.println("The future is cancelled.");
            })
        .get();
    Assertions.assertTrue(future.isCancelled());
    Assertions.assertFalse(isStartNormal.get());
    Assertions.assertTrue(isStartException.get());
  }

  @Test
  void testStartJobTimeout() throws Exception {

    // It takes 10 seconds to start job.
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> runStart(10));

    // Stop job after 15 seconds.
    CompletableFuture.runAsync(() -> runStop(future, 15));

    AtomicBoolean isStartNormal = new AtomicBoolean(false);
    AtomicBoolean isStartException = new AtomicBoolean(false);

    // Set the timeout is 5 seconds for start job.
    CompletableFutureUtils.runTimeout(
            future,
            5L,
            TimeUnit.SECONDS,
            r -> {
              isStartNormal.set(true);
              throw new IllegalStateException(
                  "It shouldn't be called due to the job is timed out.");
            },
            e -> {
              isStartException.set(true);
              Assertions.assertFalse(future.isDone());
              Assertions.assertTrue(e.getCause() instanceof TimeoutException);
              future.cancel(true);
              System.out.println("Future is timed out.");
            })
        .get();
    Assertions.assertTrue(future.isCancelled());
    Assertions.assertFalse(isStartNormal.get());
    Assertions.assertTrue(isStartException.get());
  }

  /** Cancel the future after sec seconds. */
  private void runStop(CompletableFuture<String> future, int sec) {
    try {
      Thread.sleep(sec * 1000L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (future.isDone()) {
      System.out.println("The future is done.");
    } else {
      System.out.println("Force cancel future.");
      future.cancel(true);
    }
  }

  /** Start job, it will take sec seconds. */
  private String runStart(int sec) {
    try {
      Thread.sleep(sec * 1000L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return "start successful";
  }

  @Test
  public void thenSupplyNormally() throws Exception {
    String successResult = "success";
    String exceptionResult = "error";

    String resp =
        CompletableFutureUtils.supplyTimeout(
                CompletableFuture.supplyAsync(() -> successResult),
                1,
                TimeUnit.SECONDS,
                success -> success,
                e -> exceptionResult)
            .thenApply(r -> r)
            .get();

    Assertions.assertEquals(resp, successResult);
  }

  @Test
  public void thenSupplyTimeout() throws Exception {
    String successResult = "success";
    String exceptionResult = "error";
    CompletableFuture<String> future =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              return successResult;
            });
    String resp =
        CompletableFutureUtils.supplyTimeout(
                future, 1, TimeUnit.SECONDS, success -> success, e -> exceptionResult)
            .thenApply(r -> r)
            .get();
    Assertions.assertEquals(resp, exceptionResult);
  }

  @Test
  public void thenSupplyException() {
    String expectedExceptionMessage = "Exception in exceptionally handler";
    int processTime = 5000;
    int timeOut = 1000;
    CompletableFuture<String> future =
        CompletableFutureUtils.supplyTimeout(
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    Thread.sleep(processTime);
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                  return "success";
                }),
            timeOut,
            TimeUnit.MILLISECONDS,
            success -> success,
            e -> {
              throw new RuntimeException(expectedExceptionMessage);
            });

    assertThatThrownBy(future::get)
        .hasMessageContaining(expectedExceptionMessage)
        .hasCauseInstanceOf(RuntimeException.class)
        .isInstanceOf(ExecutionException.class);
  }
}
