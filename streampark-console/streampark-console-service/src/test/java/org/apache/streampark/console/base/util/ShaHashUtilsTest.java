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

package org.apache.streampark.console.base.util;

import org.apache.streampark.common.constants.Constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test for {@link ShaHashUtils} */
class ShaHashUtilsTest {

    @Test
    void testEncrypt() {
        String randomSalt = "rh8b1ojwog777yrg0daesf04gk";
        String encryptPassword = ShaHashUtils.encrypt(randomSalt, Constants.STREAM_PARK);
        Assertions.assertEquals(
            "2513f3748847298ea324dffbf67fe68681dd92315bda830065facd8efe08f54f", encryptPassword);
    }
}
