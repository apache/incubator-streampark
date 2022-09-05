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

package org.apache.streampark.plugin.profiling.util;

import java.util.Arrays;

public class SparkAppCmdInfo {
    private String appClass;
    private String appJar;
    private String[] args = new String[0];

    public String getAppClass() {
        return appClass;
    }

    public void setAppClass(String appClass) {
        this.appClass = appClass;
    }

    public String getAppJar() {
        return appJar;
    }

    public void setAppJar(String appJar) {
        this.appJar = appJar;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        if (args == null) {
            this.args = new String[0];
        } else {
            this.args = Arrays.copyOf(args, args.length);
        }
    }

    @Override
    public String toString() {
        return "SparkAppCmdInfo{"
            + "appClass='"
            + appClass
            + '\''
            + ", appJar='"
            + appJar
            + '\''
            + ", args="
            + Arrays.toString(args)
            + '}';
    }
}
