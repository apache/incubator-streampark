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

package org.apache.streampark.spark.client.bean;

import java.io.Serializable;

/** HDFS workspace paths for Spark deployment. */
public class HdfsWorkspace implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sparkName;
    private final String sparkHome;
    private final String sparkLib;
    private final String sparkPlugins;
    private final String appJars;

    public HdfsWorkspace(
                         String sparkName,
                         String sparkHome,
                         String sparkLib,
                         String sparkPlugins,
                         String appJars) {
        this.sparkName = sparkName;
        this.sparkHome = sparkHome;
        this.sparkLib = sparkLib;
        this.sparkPlugins = sparkPlugins;
        this.appJars = appJars;
    }

    public String sparkName() {
        return sparkName;
    }

    public String getSparkName() {
        return sparkName;
    }

    public String sparkHome() {
        return sparkHome;
    }

    public String getSparkHome() {
        return sparkHome;
    }

    public String sparkLib() {
        return sparkLib;
    }

    public String getSparkLib() {
        return sparkLib;
    }

    public String sparkPlugins() {
        return sparkPlugins;
    }

    public String getSparkPlugins() {
        return sparkPlugins;
    }

    public String appJars() {
        return appJars;
    }

    public String getAppJars() {
        return appJars;
    }
}
