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

package org.apache.streampark.flink.client.bean;

import java.io.Serializable;

/** Savepoint-related options for job cancel requests. */
public final class SavepointCancelOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean withSavepoint;
    private final boolean withDrain;
    private final String savepointPath;
    private final boolean nativeFormat;

    public SavepointCancelOptions(
                                  boolean withSavepoint,
                                  boolean withDrain,
                                  String savepointPath,
                                  boolean nativeFormat) {
        this.withSavepoint = withSavepoint;
        this.withDrain = withDrain;
        this.savepointPath = savepointPath;
        this.nativeFormat = nativeFormat;
    }

    public boolean withSavepoint() {
        return withSavepoint;
    }

    public boolean withDrain() {
        return withDrain;
    }

    public String savepointPath() {
        return savepointPath;
    }

    public boolean nativeFormat() {
        return nativeFormat;
    }
}
