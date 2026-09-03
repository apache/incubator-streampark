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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class DeflaterUtilsTest {

    @Test
    void shouldRoundTripUtf8Text() {
        String text = "SELECT '\u6D41\u5904\u7406' AS name;";

        assertThat(DeflaterUtils.unzipString(DeflaterUtils.zipString(text))).isEqualTo(text);
    }

    @Test
    void shouldPreserveWhitespaceOnlyText() {
        String text = " \n\t ";

        assertThat(DeflaterUtils.unzipString(DeflaterUtils.zipString(text))).isEqualTo(text);
    }

    @Test
    void returnNullForInvalidInput() {
        String compressed = DeflaterUtils.zipString("SELECT * FROM source;");
        String truncated = compressed.substring(0, compressed.length() - 4);

        assertThat(DeflaterUtils.unzipString("not-base64")).isNull();
        assertTimeoutPreemptively(
            Duration.ofSeconds(1), () -> assertThat(DeflaterUtils.unzipString(truncated)).isNull());
    }

    @Test
    void handleEmptyAndNullInput() {
        assertTimeoutPreemptively(
            Duration.ofSeconds(1), () -> assertThat(DeflaterUtils.unzipString("")).isEmpty());
        assertThat(DeflaterUtils.zipString(null)).isEmpty();
        assertThat(DeflaterUtils.unzipString(null)).isNull();
    }

    @Test
    void shouldNormalizeLegacyNestedCompression() {
        String sql = "CREATE TABLE sink (id BIGINT);";
        String nested = DeflaterUtils.zipString(DeflaterUtils.zipString(sql));

        assertThat(DeflaterUtils.toPlainText(sql)).isEqualTo(sql);
        assertThat(DeflaterUtils.toPlainText(nested)).isEqualTo(sql);
        assertThat(DeflaterUtils.unzipString(DeflaterUtils.compressForStorage(nested))).isEqualTo(sql);
    }
}
