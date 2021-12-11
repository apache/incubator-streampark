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

package com.streamxhub.streamx.plugin.profiling.util;

import java.util.List;

/**
 * @author benjobs
 */
public class ClassAndMethodFilter {
    private static final String METHOD_NAME_WILDCARD = "*";

    private ClassAndMethod[] classAndMethods = new ClassAndMethod[0];

    public ClassAndMethodFilter(List<ClassAndMethod> classMethodNames) {
        if (classMethodNames != null) {
            this.classAndMethods = new ClassAndMethod[classMethodNames.size()];
            for (int i = 0; i < classMethodNames.size(); i++) {
                this.classAndMethods[i] = classMethodNames.get(i);
            }
        }
    }

    public boolean isEmpty() {
        return classAndMethods.length == 0;
    }

    public boolean matchClass(String className) {
        for (ClassAndMethod classAndMethod : classAndMethods) {
            if (className.startsWith(classAndMethod.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean matchMethod(String className, String methodName) {
        for (ClassAndMethod classAndMethod : classAndMethods) {
            if (className.startsWith(classAndMethod.getClassName())) {
                if (METHOD_NAME_WILDCARD.equals(classAndMethod.getMethodName())
                    || methodName.equals(classAndMethod.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
