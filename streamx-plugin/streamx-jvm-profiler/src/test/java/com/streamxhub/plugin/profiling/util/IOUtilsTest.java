/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.plugin.profiling.util;

import com.streamxhub.streamx.plugin.profiling.util.Utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class IOUtilsTest {
    @Test
    public void toByteArray() {
        byte[] bytes = new byte[]{1, 2, 3};
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        byte[] result = Utils.toByteArray(byteArrayInputStream);
        Assert.assertEquals(3, result.length);
        Assert.assertEquals(1, result[0]);
        Assert.assertEquals(3, result[result.length - 1]);
    }
}
