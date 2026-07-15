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

package org.apache.streampark.spark.client;

import org.apache.streampark.common.conf.SparkVersion;
import org.apache.streampark.spark.client.bean.CancelRequest;
import org.apache.streampark.spark.client.bean.CancelResponse;
import org.apache.streampark.spark.client.bean.SubmitRequest;
import org.apache.streampark.spark.client.bean.SubmitResponse;
import org.apache.streampark.spark.client.proxy.SparkShimsProxy;

import java.lang.reflect.Method;

/** Spark client entry for cross-classloader proxy invocation. */
public final class SparkClient {

    private static final String SPARK_CLIENT_ENDPOINT_CLASS =
        "org.apache.streampark.spark.client.SparkClientEndpoint";

    private SparkClient() {
    }

    public static SubmitResponse submit(SubmitRequest submitRequest) {
        return proxy(
            submitRequest,
            submitRequest.getSparkVersion(),
            SubmitRequest.class.getName(),
            "submit");
    }

    public static CancelResponse cancel(CancelRequest cancelRequest) {
        return proxy(
            cancelRequest,
            cancelRequest.getSparkVersion(),
            CancelRequest.class.getName(),
            "cancel");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(
                               Object request, SparkVersion sparkVersion, String requestClassName, String methodName) {
        sparkVersion.checkVersion();
        return SparkShimsProxy.proxy(
            sparkVersion,
            classLoader -> {
                try {
                    Class<?> endpointClass = classLoader.loadClass(SPARK_CLIENT_ENDPOINT_CLASS);
                    Class<?> requestClass = classLoader.loadClass(requestClassName);
                    Method method = endpointClass.getDeclaredMethod(methodName, requestClass);
                    method.setAccessible(true);
                    Object obj =
                        method.invoke(null, SparkShimsProxy.getObject(classLoader, request));
                    if (obj == null) {
                        return null;
                    }
                    return (T) SparkShimsProxy.getObject(SparkClient.class.getClassLoader(), obj);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });
    }
}
