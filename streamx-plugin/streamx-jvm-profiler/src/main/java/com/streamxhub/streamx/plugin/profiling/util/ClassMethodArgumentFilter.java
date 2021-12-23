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

import java.util.ArrayList;
import java.util.List;

/**
 * @author benjobs
 */
public class ClassMethodArgumentFilter {
    private static final String METHOD_NAME_WILDCARD = "*";

    private ClassMethodArgument[] classMethodArguments = new ClassMethodArgument[0];

    public ClassMethodArgumentFilter(List<ClassMethodArgument> classMethodArgumentToFilter) {
        if (classMethodArgumentToFilter != null) {
            this.classMethodArguments = new ClassMethodArgument[classMethodArgumentToFilter.size()];
            for (int i = 0; i < classMethodArgumentToFilter.size(); i++) {
                this.classMethodArguments[i] = classMethodArgumentToFilter.get(i);
            }
        }
    }

    public boolean isEmpty() {
        return classMethodArguments.length == 0;
    }

    public boolean matchClass(String className) {
        for (ClassMethodArgument classAndMethod : classMethodArguments) {
            if (className.startsWith(classAndMethod.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public List<Integer> matchMethod(String className, String methodName) {
        List<Integer> result = new ArrayList<>();

        for (ClassMethodArgument classMethodArgument : classMethodArguments) {
            if (className.startsWith(classMethodArgument.getClassName())) {
                if (METHOD_NAME_WILDCARD.equals(classMethodArgument.getMethodName())
                    || methodName.equals(classMethodArgument.getMethodName())) {
                    result.add(classMethodArgument.getArgumentIndex());
                }
            }
        }

        return result;
    }
}
