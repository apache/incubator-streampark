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
package org.apache.streampark.console.core.utils;

import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.base.util.Throws;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ThrowsTest {

    static final String PURE_MSG_TPL = "Pure test string.";
    static final String MSG_TPL = "Template string %s.";
    static final String ARG = "Hello";
    static final String RENDERED_STR = String.format(MSG_TPL, ARG);

    public static class TestRuntimeException extends RuntimeException {

        public TestRuntimeException() {
        }
    }

    @Test
    void testThrowIfTrue() {
        assertThatThrownBy(() -> Throws.throwIfTrue(true, null))
            .hasMessage("The target exception must be specified.");
        assertThatThrownBy(() -> Throws.throwIfTrue(true, AlertException.class))
            .hasCauseInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> Throws.throwIfTrue(true, AlertException.class, PURE_MSG_TPL))
            .isInstanceOf(AlertException.class)
            .hasMessage(PURE_MSG_TPL);
        assertThatThrownBy(() -> Throws.throwIfTrue(true, AlertException.class, MSG_TPL, ARG))
            .isInstanceOf(AlertException.class)
            .hasMessage(RENDERED_STR);
        assertThatThrownBy(() -> Throws.throwIfTrue(true, TestRuntimeException.class))
            .isInstanceOf(TestRuntimeException.class);

        Throws.throwIfTrue(false, null);
        Throws.throwIfTrue(false, AlertException.class);
        Throws.throwIfTrue(false, AlertException.class, PURE_MSG_TPL);
        Throws.throwIfTrue(false, AlertException.class, MSG_TPL, ARG);
        Throws.throwIfTrue(false, TestRuntimeException.class);
    }

    @Test
    void testThrowIfFalse() {
        assertThatThrownBy(() -> Throws.throwIfFalse(false, null))
            .hasMessage("The target exception must be specified.");
        assertThatThrownBy(() -> Throws.throwIfFalse(false, AlertException.class))
            .hasCauseInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> Throws.throwIfFalse(false, AlertException.class, PURE_MSG_TPL))
            .isInstanceOf(AlertException.class)
            .hasMessage(PURE_MSG_TPL);
        assertThatThrownBy(() -> Throws.throwIfFalse(false, AlertException.class, MSG_TPL, ARG))
            .isInstanceOf(AlertException.class)
            .hasMessage(RENDERED_STR);
        assertThatThrownBy(() -> Throws.throwIfFalse(false, TestRuntimeException.class))
            .isInstanceOf(TestRuntimeException.class);

        Throws.throwIfFalse(true, null);
        Throws.throwIfFalse(true, AlertException.class);
        Throws.throwIfFalse(true, AlertException.class, PURE_MSG_TPL);
        Throws.throwIfFalse(true, AlertException.class, MSG_TPL, ARG);
        Throws.throwIfFalse(true, TestRuntimeException.class);
    }

    @Test
    void testThrowIfNull() {
        assertThatThrownBy(() -> Throws.throwIfNull(null, null))
            .hasMessage("The target exception must be specified.");
        assertThatThrownBy(() -> Throws.throwIfNull(null, AlertException.class))
            .hasCauseInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> Throws.throwIfNull(null, AlertException.class, PURE_MSG_TPL))
            .isInstanceOf(AlertException.class)
            .hasMessage(PURE_MSG_TPL);
        assertThatThrownBy(() -> Throws.throwIfNull(null, AlertException.class, MSG_TPL, ARG))
            .isInstanceOf(AlertException.class)
            .hasMessage(RENDERED_STR);
        assertThatThrownBy(() -> Throws.throwIfNull(null, TestRuntimeException.class))
            .isInstanceOf(TestRuntimeException.class);

        Throws.throwIfNull(this, null);
        Throws.throwIfNull(this, AlertException.class);
        Throws.throwIfNull(this, AlertException.class, PURE_MSG_TPL);
        Throws.throwIfNull(this, AlertException.class, MSG_TPL, ARG);
        Throws.throwIfNull(this, TestRuntimeException.class);
    }

    @Test
    void testThrowIfNonnull() {
        assertThatThrownBy(() -> Throws.throwIfNonnull(this, null))
            .hasMessage("The target exception must be specified.");
        assertThatThrownBy(() -> Throws.throwIfNonnull(this, AlertException.class))
            .hasCauseInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> Throws.throwIfNonnull(this, AlertException.class, PURE_MSG_TPL))
            .isInstanceOf(AlertException.class)
            .hasMessage(PURE_MSG_TPL);
        assertThatThrownBy(() -> Throws.throwIfNonnull(this, AlertException.class, MSG_TPL, ARG))
            .isInstanceOf(AlertException.class)
            .hasMessage(RENDERED_STR);
        assertThatThrownBy(() -> Throws.throwIfNonnull(this, TestRuntimeException.class))
            .isInstanceOf(TestRuntimeException.class);

        Throws.throwIfNonnull(null, null);
        Throws.throwIfNonnull(null, AlertException.class);
        Throws.throwIfNonnull(null, AlertException.class, PURE_MSG_TPL);
        Throws.throwIfNonnull(null, AlertException.class, MSG_TPL, ARG);
        Throws.throwIfNonnull(null, TestRuntimeException.class);
    }
}
