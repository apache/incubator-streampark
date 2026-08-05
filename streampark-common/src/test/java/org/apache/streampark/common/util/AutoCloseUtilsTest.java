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

package org.apache.streampark.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutoCloseUtilsTest {

    @Test
    void usingShouldReturnResultAndCloseResource() throws Exception {
        TestResource resource = new TestResource();
        String result = AutoCloseUtils.using(resource, r -> "ok-" + r.name);
        assertThat(result).isEqualTo("ok-test");
        assertThat(resource.closed).isTrue();
    }

    @Test
    void usingShouldInvokeExceptionHandler() {
        TestResource resource = new TestResource();
        String result =
            AutoCloseUtils.using(
                resource,
                r -> {
                    throw new IllegalStateException("boom");
                },
                e -> "handled");
        assertThat(result).isEqualTo("handled");
        assertThat(resource.closed).isTrue();
    }

    @Test
    void usingShouldRethrowRuntimeExceptionWhenNoHandler() {
        TestResource resource = new TestResource();
        assertThatThrownBy(
            () -> AutoCloseUtils.using(
                resource,
                r -> {
                    throw new IllegalArgumentException("bad");
                }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("bad");
        assertThat(resource.closed).isTrue();
    }

    private static final class TestResource implements AutoCloseable {

        private final String name = "test";
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
