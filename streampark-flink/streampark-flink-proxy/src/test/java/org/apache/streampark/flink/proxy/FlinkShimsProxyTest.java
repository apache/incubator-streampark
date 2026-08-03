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

package org.apache.streampark.flink.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

class FlinkShimsProxyTest {

    @Test
    void getObjectShouldDeserializeAcrossClassLoaders() throws Exception {
        String payload = "streampark-proxy-test";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Serializable roundTripped = FlinkShimsProxy.getObject(loader, payload);
        Assertions.assertEquals(payload, roundTripped);
    }

    @Test
    void getObjectShouldPreserveCustomSerializableType() throws Exception {
        TestBean original = new TestBean("app-1", 42);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        TestBean roundTripped = FlinkShimsProxy.getObject(loader, original);
        Assertions.assertEquals(original, roundTripped);
    }

    private static final class TestBean implements Serializable {

        private static final long serialVersionUID = 1L;
        private final String name;
        private final int value;

        TestBean(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TestBean)) {
                return false;
            }
            TestBean testBean = (TestBean) o;
            return value == testBean.value && name.equals(testBean.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + value;
        }
    }
}
