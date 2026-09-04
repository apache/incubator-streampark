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

package org.apache.streampark.flink.client.request;

import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.enums.FlinkDeployMode;

import javax.annotation.Nullable;

import java.util.Map;

/** Request to cancel a Flink job, optionally creating a savepoint first. */
public final class CancelRequest extends AbstractSavepointRequest {

    private static final long serialVersionUID = 1L;

    private final boolean withSavepoint;
    private final boolean withDrain;
    private final String savepointPath;
    private final boolean nativeFormat;

    public CancelRequest(
                         long id,
                         FlinkVersion flinkVersion,
                         FlinkDeployMode deployMode,
                         @Nullable Map<String, Object> properties,
                         JobClientTarget target,
                         boolean withSavepoint,
                         boolean withDrain,
                         @Nullable String savepointPath,
                         boolean nativeFormat) {
        super(id, flinkVersion, deployMode, properties, target);
        this.withSavepoint = withSavepoint;
        this.withDrain = withDrain;
        this.savepointPath = savepointPath;
        this.nativeFormat = nativeFormat;
    }

    @Override
    public boolean withSavepoint() {
        return withSavepoint;
    }

    public boolean withDrain() {
        return withDrain;
    }

    @Override
    public String savepointPath() {
        return savepointPath;
    }

    @Override
    public boolean nativeFormat() {
        return nativeFormat;
    }
}
