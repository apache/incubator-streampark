package org.apache.streampark.flink.kubernetes;

/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.util.List;

public class IgsMeta implements Serializable {
    public IgsMeta() {
    }

    public IgsMeta(List<String> addresses, int port, String protocol, String serviceName, String ingressName, String hostname, String path, Boolean allNodes) {
        this.addresses = addresses;
        this.port = port;
        this.protocol = protocol;
        this.serviceName = serviceName;
        this.ingressName = ingressName;
        this.hostname = hostname;
        this.path = path;
        this.allNodes = allNodes;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIngressName() {
        return ingressName;
    }

    public void setIngressName(String ingressName) {
        this.ingressName = ingressName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getAllNodes() {
        return allNodes;
    }

    public void setAllNodes(Boolean allNodes) {
        this.allNodes = allNodes;
    }

    List<String> addresses;
    int port;
    String protocol;
    String serviceName;
    String ingressName;
    String hostname;
    String path;
    Boolean allNodes;
}
