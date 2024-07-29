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
package org.apache.streampark.gateway.results;

import org.apache.streampark.gateway.OperationStatusEnum;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

/** Information of the {@code Operation}. */
public class OperationInfo implements Serializable {

    private final OperationStatusEnum status;
    @Nullable
    private final Exception exception;

    public OperationInfo(OperationStatusEnum status, @Nullable Exception exception) {
        this.status = status;
        this.exception = exception;
    }

    public OperationStatusEnum getStatus() {
        return status;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OperationInfo that = (OperationInfo) o;
        return status == that.status && Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, exception);
    }

    @Override
    public String toString() {
        return "OperationInfo{" + "status=" + status + ", exception=" + exception + '}';
    }
}
