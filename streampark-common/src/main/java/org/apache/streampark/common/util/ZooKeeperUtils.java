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

package org.apache.streampark.common.util;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ZooKeeperUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(ZooKeeperUtils.class.getName());

    private static final String CONNECT = "localhost:2181";
    private static final Map<String, CuratorFramework> CLIENT_MAP = new ConcurrentHashMap<>();

    private ZooKeeperUtils() {
    }

    public static CuratorFramework getClient() {
        return getClient(CONNECT);
    }

    public static CuratorFramework getClient(String url) {
        CuratorFramework existing = CLIENT_MAP.get(url);
        if (existing != null) {
            return existing;
        }
        try {
            RetryPolicy retryPolicy = new RetryNTimes(5, 2000);
            CuratorFramework client =
                CuratorFrameworkFactory.builder()
                    .connectString(url)
                    .retryPolicy(retryPolicy)
                    .connectionTimeoutMs(2000)
                    .build();
            client.start();
            CLIENT_MAP.put(url, client);
            return client;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static void close(String url) {
        CuratorFramework client = getClient(url);
        if (client != null) {
            client.close();
            CLIENT_MAP.remove(url);
        }
    }

    public static List<String> listChildren(String path) throws Exception {
        return listChildren(path, CONNECT);
    }

    public static List<String> listChildren(String path, String url) throws Exception {
        CuratorFramework client = getClient(url);
        if (client.checkExists().forPath(path) == null) {
            return new ArrayList<>();
        }
        return client.getChildren().forPath(path);
    }

    public static boolean create(String path) throws Exception {
        return create(path, null, CONNECT, false);
    }

    public static boolean create(String path, String value, String url, boolean persistent) throws Exception {
        try {
            CuratorFramework client = getClient(url);
            if (client.checkExists().forPath(path) == null) {
                byte[] data =
                    value == null || value.isEmpty()
                        ? new byte[0]
                        : value.getBytes(StandardCharsets.UTF_8);
                CreateMode mode = persistent ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL;
                String opResult =
                    client.create().creatingParentsIfNeeded().withMode(mode).forPath(path, data);
                return path.equals(opResult);
            }
            return false;
        } catch (Exception e) {
            LOG.warn("Failed to create ZooKeeper path {}", path, e);
            return false;
        }
    }

    public static boolean update(String path, String value, String url, boolean persistent) throws Exception {
        try {
            CuratorFramework client = getClient(url);
            if (client.checkExists().forPath(path) == null) {
                CreateMode mode = persistent ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL;
                String opResult =
                    client.create()
                        .creatingParentsIfNeeded()
                        .withMode(mode)
                        .forPath(path, value.getBytes(StandardCharsets.UTF_8));
                return path.equals(opResult);
            }
            client.setData().forPath(path, value.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to update ZooKeeper path {}", path, e);
            return false;
        }
    }

    public static void delete(String path) throws Exception {
        delete(path, CONNECT);
    }

    public static void delete(String path, String url) throws Exception {
        try {
            CuratorFramework client = getClient(url);
            if (client.checkExists().forPath(path) != null) {
                client.delete().deletingChildrenIfNeeded().forPath(path);
            }
        } catch (Exception e) {
            LOG.warn("Failed to delete ZooKeeper path {}", path, e);
        }
    }

    public static String get(String path) throws Exception {
        return get(path, CONNECT);
    }

    public static String get(String path, String url) throws Exception {
        try {
            CuratorFramework client = getClient(url);
            if (client.checkExists().forPath(path) == null) {
                return null;
            }
            return new String(client.getData().forPath(path));
        } catch (Exception e) {
            LOG.warn("Failed to read ZooKeeper path {}", path, e);
            return null;
        }
    }
}
