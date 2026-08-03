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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/** Error details of building pipeline. */
@JsonIgnoreProperties(ignoreUnknown = true, value = "exception")
public class PipeError {

    private String summary;
    @JsonIgnore
    @Nullable
    private transient Throwable exception;
    @Nullable
    private String exceptionStack;

    public PipeError() {
    }

    public PipeError(String summary, @Nullable Throwable exception, @Nullable String exceptionStack) {
        this.summary = summary;
        this.exception = exception;
        this.exceptionStack = exceptionStack;
    }

    public String summary() {
        return summary;
    }

    @Nullable
    public Throwable exception() {
        return exception;
    }

    @Nullable
    public String exceptionStack() {
        return exceptionStack;
    }

    public boolean nonEmpty() {
        return (summary != null && !summary.isEmpty()) || exception != null;
    }

    public boolean isEmpty() {
        return !nonEmpty();
    }

    public PipeError copy() {
        return new PipeError(summary, exception, exceptionStack);
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
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(element.toString());
        }
        return sb.toString();
    }

    @JsonProperty("summary")
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @JsonProperty("exceptionStack")
    public void setExceptionStack(String exceptionStack) {
        this.exceptionStack = exceptionStack;
    }
}
