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

package org.apache.streampark.flink.kubernetes.watcher;

import java.util.concurrent.atomic.AtomicBoolean;

public interface FlinkWatcher extends AutoCloseable {

  AtomicBoolean STARTED = new AtomicBoolean(false);

  default void start() {
    synchronized (this) {
      if (!STARTED.getAndSet(true)) {
        doStart();
      }
    }
  }

  default void stop() {
    synchronized (this) {
      if (STARTED.getAndSet(false)) {
        doStop();
      }
    }
  }

  @Override
  default void close() throws Exception {
    if (STARTED.get()) {
      doStop();
    }
    doClose();
  }

  default void restart() {
    synchronized (this) {
      stop();
      start();
    }
  }

  void doStart();

  void doStop();

  void doClose();

  void doWatch();
}
