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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void notNullShouldThrowNullPointerExceptionIfArgumentIsNull() {
        NullPointerException ex =
            assertThrows(NullPointerException.class, () -> AssertUtils.notNull(null, "object can't be null"));
        assertEquals("object can't be null", ex.getMessage());
    }

    @Test
    void isNotEmptyShouldCheckIfArgumentIsNotEmpty() {
        assertFalse(Utils.isNotEmpty(null));
        assertTrue(Utils.isNotEmpty(new int[]{1}));
        assertTrue(Utils.isNotEmpty("string"));
        assertTrue(Utils.isNotEmpty(new String[]{"Seq"}));

        ArrayList<String> arrayList = new ArrayList<>(16);
        arrayList.add("arrayList");
        assertTrue(Utils.isNotEmpty(arrayList));

        HashMap<String, String> hashMap = new HashMap<>(16);
        hashMap.put("hash", "map");
        assertTrue(Utils.isNotEmpty(hashMap));
        assertTrue(Utils.isNotEmpty(new Object()));
    }

    @Test
    void requiredShouldThrowIllegalArgumentExceptionIfConditionIsFalse() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> AssertUtils.required(false));
        assertNull(ex.getMessage());
    }

    @Test
    void requireCheckJarFileShouldThrowIOExceptionIfJarFilePathIsInvalid() throws Exception {
        URL jar = new URL("http://host/file");
        IOException ex = assertThrows(IOException.class, () -> Utils.requireCheckJarFile(jar));
        assertTrue(ex.getMessage().contains("http://host/file"));
    }

    @Test
    void checkHttpURLShouldReturnFalseForUnreachableHosts() {
        assertFalse(Utils.checkHttpURL("http://local"));
        assertFalse(Utils.checkHttpURL("https://local"));
        assertFalse(Utils.checkHttpURL("http://127.0.0.1:1"));
    }
}
