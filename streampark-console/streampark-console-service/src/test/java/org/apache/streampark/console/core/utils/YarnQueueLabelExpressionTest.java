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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.util.YarnQueueLabelExpression;

import org.junit.jupiter.api.Test;

import static org.apache.streampark.console.core.util.YarnQueueLabelExpression.ERR_FORMAT_HINTS;
import static org.apache.streampark.console.core.util.YarnQueueLabelExpression.isValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Test class for {@link YarnQueueLabelExpression}. */
class YarnQueueLabelExpressionTest {

    @Test
    void testIsValid() {
        // Test 'default' queue
        assertThat(isValid("default")).isTrue();
        // Test default queue default label
        assertThat(isValid("default@default")).isTrue();
        // Test queue1, with label1,label2
        assertThat(isValid("queue1@label1,label2")).isTrue();
        // test 'default ' queue
        assertThat(isValid("default ")).isFalse();
        // test 'root.default' queue
        assertThat(isValid("root.default")).isTrue();
        // test 'root.default@' queue
        assertThat(isValid("root.default@")).isFalse();
        // test 'root.default@label1'
        assertThat(isValid("root.default@label1")).isTrue();
        // test 'root.default@label1,label2'
        assertThat(isValid("root.default@label1,label2")).isTrue();
        // test 'root.default@label1,' queue
        assertThat(isValid("root.default@label1,")).isFalse();
        // test 'default@'
        assertThat(isValid("default@")).isFalse();
        // test 'default.@label1,label2'
        assertThat(isValid("default.@label1,label2")).isFalse();
        // test 'queue1@label2, '
        assertThat(isValid("queue1@label2, ")).isFalse();
    }

    @Test
    void testOf() {
        assertThat(YarnQueueLabelExpression.of("a").getLabelExpression()).isEmpty();
        assertThat(YarnQueueLabelExpression.of("a").getQueue()).isEqualTo("a");
        assertThatThrownBy(() -> YarnQueueLabelExpression.of("a@"))
            .isInstanceOf(ApiAlertException.class)
            .hasMessageContaining(ERR_FORMAT_HINTS);
    }
}
