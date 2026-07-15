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

package org.apache.streampark.flink.packer.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true, value = {"exception"})
public class PipeError {

    private String summary;
    @Nullable
    private transient Throwable exception;
    @Nullable
    private String exceptionStack;

    public boolean nonEmpty() {
        return (summary != null && !summary.isEmpty()) || exception != null;
    }

    public boolean isEmpty() {
        return !nonEmpty();
    }

    public static PipeError empty() {
        return of("", null);
    }

    public static PipeError of(String summary, @Nullable Throwable exception) {
        String stack = exception == null ? "" : stackTraceToString(exception);
        return new PipeError(summary, exception, stack);
    }

    private static String stackTraceToString(Throwable exception) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append(element).append('\n');
        }
        return sb.toString();
    }

    public PipeError copy() {
        return new PipeError(summary, exception, exceptionStack);
    }
}
