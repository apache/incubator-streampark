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

import org.apache.streampark.gateway.service.SqlGatewayService;

import java.io.Serializable;
import java.util.Objects;

/** Info to describe the {@link SqlGatewayService}. */
public class GatewayInfo implements Serializable {

    /** Gateway service type. */
    public final String serviceType;

    /** Gateway service version. */
    public final String version;

    public GatewayInfo(String serviceType, String version) {
        this.serviceType = serviceType;
        this.version = version;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GatewayInfo that = (GatewayInfo) o;
        return Objects.equals(serviceType, that.serviceType) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceType, version);
    }

    @Override
    public String toString() {
        return "GatewayInfo{"
                + "serviceType='"
                + serviceType
                + '\''
                + ", version='"
                + version
                + '\''
                + '}';
    }
}
