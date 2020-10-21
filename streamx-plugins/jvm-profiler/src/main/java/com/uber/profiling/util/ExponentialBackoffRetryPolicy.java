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

package com.uber.profiling.util;

import java.util.Random;
import java.util.concurrent.Callable;

public class ExponentialBackoffRetryPolicy<T> {
  private static final AgentLogger logger = AgentLogger.getLogger(ExponentialBackoffRetryPolicy.class.getName());

  private final int maxAttemptCount;
  private final long minSleepMillis;
  private final float scaleFactor;
  
  private Random random = new Random();

  public ExponentialBackoffRetryPolicy(int maxAttemptCount, long minSleepMillis) {
    this(maxAttemptCount, minSleepMillis, 2.0f);
  }

  public ExponentialBackoffRetryPolicy(int maxAttemptCount, long minSleepMillis, float scaleFactor) {
    this.maxAttemptCount = maxAttemptCount;
    this.minSleepMillis = minSleepMillis;
    this.scaleFactor = scaleFactor;
  }

  public T attempt(Callable<T> operation) {
    int remainingAttempts = maxAttemptCount - 1;
    long minSleepTime = minSleepMillis;
    long maxSleepTime = (long)(minSleepMillis * scaleFactor);

    Throwable previousException;

    try {
      return operation.call();
    } catch (Throwable ex) {
      if (remainingAttempts <= 0) {
        throw new RuntimeException("Failed with first try and no remaining retry", ex);
      }
      previousException = ex;
    }

    while (remainingAttempts > 0) {
      long sleepTime = minSleepTime + random.nextInt((int)(maxSleepTime - minSleepTime));
      logger.info(String.format("Retrying (after sleeping %s milliseconds) on exception: %s", sleepTime, previousException));
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException ex) {
        logger.warn("Sleep interrupted", ex);
      }
      try {
        return operation.call();
      } catch (Throwable ex) {
        previousException = ex;
      }
      
      remainingAttempts--;
      minSleepTime *= scaleFactor;
      maxSleepTime *= scaleFactor;
    }

    String msg = String.format("Failed after trying %s times", maxAttemptCount);
    throw new RuntimeException(msg, previousException);
  }

}
