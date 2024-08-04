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

package org.apache.streampark.console.core.annotation;

import org.apache.streampark.console.core.aspect.AppChangeEventAspect;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In the controller({@link org.apache.streampark.console.core.controller}), If some method causes
 * application state update, need to add this annotation, This annotation marks which methods will
 * cause the application to be updated, Will work together with {@link
 * AppChangeEventAspect#appChangeEvent(ProceedingJoinPoint)}, The final purpose will be refresh {@link
 * FlinkAppHttpWatcher#WATCHING_APPS}, Make the state of the job consistent with the database
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AppChangeEvent {

    boolean value() default true;
}
