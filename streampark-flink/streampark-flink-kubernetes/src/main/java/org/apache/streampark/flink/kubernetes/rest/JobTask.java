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

package org.apache.streampark.flink.kubernetes.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobTask {

    private int total;
    private int created;
    private int scheduled;
    private int deploying;
    private int running;
    private int finished;
    private int canceling;
    private int canceled;
    private int failed;
    private int reconciling;
    private int initializing;

    public int total() {
        return total;
    }

    public int created() {
        return created;
    }

    public int scheduled() {
        return scheduled;
    }

    public int deploying() {
        return deploying;
    }

    public int running() {
        return running;
    }

    public int finished() {
        return finished;
    }

    public int canceling() {
        return canceling;
    }

    public int canceled() {
        return canceled;
    }

    public int failed() {
        return failed;
    }

    public int reconciling() {
        return reconciling;
    }

    public int initializing() {
        return initializing;
    }
}
