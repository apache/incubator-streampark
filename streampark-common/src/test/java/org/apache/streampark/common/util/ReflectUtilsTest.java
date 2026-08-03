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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectUtilsTest {

    static class TestObject {

        String value;

        TestObject(String value) {
            this.value = value;
        }
    }

    @Test
    void getFieldShouldReturnCorrectField() {
        java.lang.reflect.Field field = ReflectUtils.getField(TestObject.class, "value");
        assertEquals("value", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void getFieldShouldHandleNonExistentFieldsGracefully() {
        assertNull(ReflectUtils.getField(TestObject.class, "nonExistentField"));
    }

    @Test
    void getFieldValueShouldHandleNonExistentFieldGracefully() throws Exception {
        TestObject obj = new TestObject("test");
        assertNull(ReflectUtils.getFieldValue(obj, "nonExistentField"));
    }

    @Test
    void setFieldValueShouldThrowForNonExistentField() {
        TestObject obj = new TestObject("test");
        assertThrows(IllegalArgumentException.class,
            () -> ReflectUtils.setFieldValue(obj, "nonExistentField", "value"));
    }
}
