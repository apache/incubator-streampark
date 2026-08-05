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

import org.apache.streampark.common.conf.ConfigKeys;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.SafeEncoder;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RedisEndpoint represents a redis connection endpoint info: host, port, auth password db number,
 * and timeout.
 */
public class RedisEndpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String host;
    private final int port;
    private final String auth;
    private final int db;
    private final int timeout;

    public RedisEndpoint() {
        this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT, null, Protocol.DEFAULT_DATABASE, Protocol.DEFAULT_TIMEOUT);
    }

    public RedisEndpoint(String host, int port, String auth, int db, int timeout) {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.db = db;
        this.timeout = timeout;
    }

    public RedisEndpoint(Properties conf) {
        this(
            conf.getProperty(ConfigKeys.KEY_HOST(), Protocol.DEFAULT_HOST),
            Integer.parseInt(
                conf.getProperty(ConfigKeys.KEY_PORT(), String.valueOf(Protocol.DEFAULT_PORT))),
            conf.getProperty(ConfigKeys.KEY_PASSWORD(), null),
            Integer.parseInt(
                conf.getProperty(ConfigKeys.KEY_DB(), String.valueOf(Protocol.DEFAULT_DATABASE))),
            Integer.parseInt(
                conf.getProperty(ConfigKeys.KEY_TIMEOUT(), String.valueOf(Protocol.DEFAULT_TIMEOUT))));
    }

    public RedisEndpoint(URI uri) {
        this(
            uri.getHost(),
            uri.getPort(),
            JedisURIHelper.getPassword(uri),
            JedisURIHelper.getDBIndex(uri),
            Protocol.DEFAULT_TIMEOUT);
    }

    public RedisEndpoint(String uri) {
        this(URI.create(uri));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAuth() {
        return auth;
    }

    public int getDb() {
        return db;
    }

    public int getTimeout() {
        return timeout;
    }

    public RedisEndpoint withAuth(String newAuth) {
        return new RedisEndpoint(host, port, newAuth, db, timeout);
    }

    public Jedis connect() {
        return RedisClient.connect(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedisEndpoint that = (RedisEndpoint) o;
        return port == that.port
            && db == that.db
            && timeout == that.timeout
            && Objects.equals(host, that.host)
            && Objects.equals(auth, that.auth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, auth, db, timeout);
    }

    @Override
    public String toString() {
        return "RedisEndpoint{host='"
            + host
            + "', port="
            + port
            + ", db="
            + db
            + ", timeout="
            + timeout
            + '}';
    }
}

class RedisNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private final RedisEndpoint endpoint;
    private final int startSlot;
    private final int endSlot;
    private final int idx;
    private final int total;

    RedisNode(RedisEndpoint endpoint, int startSlot, int endSlot, int idx, int total) {
        this.endpoint = endpoint;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.idx = idx;
        this.total = total;
    }

    public RedisEndpoint getEndpoint() {
        return endpoint;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }

    public int getIdx() {
        return idx;
    }

    public int getTotal() {
        return total;
    }

    public Jedis connect() {
        return endpoint.connect();
    }
}

/** RedisConfig holds the state of the cluster nodes, and uses consistent hashing to map keys to nodes. */
class RedisConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final RedisEndpoint initialHost;
    private final String initialAddr;
    private final RedisNode[] hosts;
    private final RedisNode[] nodes;

    RedisConfig(RedisEndpoint initialHost) {
        this.initialHost = initialHost;
        this.initialAddr = initialHost.getHost();
        this.nodes = getNodes(initialHost);
        this.hosts =
            Arrays.stream(nodes).filter(node -> node.getIdx() == 0).toArray(RedisNode[]::new);
    }

    public String getInitialAddr() {
        return initialAddr;
    }

    public String getAuth() {
        return initialHost.getAuth();
    }

    public int getDB() {
        return initialHost.getDb();
    }

    public RedisNode getRandomNode() {
        return hosts[ThreadLocalRandom.current().nextInt(hosts.length)];
    }

    public RedisNode[] getNodesBySlots(int sPos, int ePos) {
        return Arrays.stream(nodes)
            .filter(node -> intersect(sPos, ePos, node.getStartSlot(), node.getEndSlot()))
            .filter(node -> node.getIdx() == 0)
            .toArray(RedisNode[]::new);
    }

    public Jedis connectionForKey(String key) {
        return getHost(key).connect();
    }

    public RedisNode getHost(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        return Arrays.stream(hosts)
            .filter(host -> host.getStartSlot() <= slot && host.getEndSlot() >= slot)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("[StreamPark] No redis host for key: " + key));
    }

    public RedisNode[] getNodes(RedisEndpoint endpoint) {
        if (clusterEnabled(endpoint)) {
            return getClusterNodes(endpoint);
        }
        return getNonClusterNodes(endpoint);
    }

    private static boolean intersect(int sPos1, int ePos1, int sPos2, int ePos2) {
        if (sPos1 <= sPos2) {
            return ePos1 >= sPos2;
        }
        return ePos2 >= sPos1;
    }

    private boolean clusterEnabled(RedisEndpoint endpoint) {
        Jedis conn = endpoint.connect();
        try {
            String[] info = conn.info().split("\n");
            String version =
                Arrays.stream(info).filter(line -> line.contains("redis_version:")).findFirst().orElse("");
            String[] clusterEnable =
                Arrays.stream(info).filter(line -> line.contains("cluster_enabled:")).toArray(String[]::new);
            int mainVersion = Integer.parseInt(version.substring(14, version.indexOf('.')));
            return mainVersion > 2 && clusterEnable.length > 0 && clusterEnable[0].contains("1");
        } finally {
            conn.close();
        }
    }

    private RedisNode[] getNonClusterNodes(RedisEndpoint endpoint) {
        String masterHost = endpoint.getHost();
        int masterPort = endpoint.getPort();
        Jedis conn = endpoint.connect();
        String[] replinfo;
        try {
            replinfo = conn.info("Replication").split("\n");
        } finally {
            conn.close();
        }

        if (Arrays.stream(replinfo).anyMatch(line -> line.contains("role:slave"))) {
            String host =
                Arrays.stream(replinfo)
                    .filter(line -> line.contains("master_host:"))
                    .findFirst()
                    .orElse("")
                    .trim()
                    .substring(12);
            int port =
                Integer.parseInt(
                    Arrays.stream(replinfo)
                        .filter(line -> line.contains("master_port:"))
                        .findFirst()
                        .orElse("")
                        .trim()
                        .substring(12));
            return getNonClusterNodes(
                new RedisEndpoint(host, port, endpoint.getAuth(), endpoint.getDb(), endpoint.getTimeout()));
        }

        List<String[]> slaves = new ArrayList<>();
        for (String rl : replinfo) {
            if (rl.contains("slave") && rl.contains("online")) {
                String content = rl.substring(rl.indexOf(':') + 1);
                String[] parts = content.split(",");
                String ipPart = parts[0];
                String portPart = parts[1];
                slaves.add(
                    new String[]{
                            ipPart.substring(ipPart.indexOf('=') + 1),
                            portPart.substring(portPart.indexOf('=') + 1)
                    });
            }
        }

        List<RedisNode> nodeList = new ArrayList<>();
        nodeList.add(new RedisNode(endpoint, 0, 16383, 0, slaves.size() + 1));
        for (int i = 0; i < slaves.size(); i++) {
            String[] slave = slaves.get(i);
            nodeList.add(
                new RedisNode(
                    new RedisEndpoint(
                        slave[0],
                        Integer.parseInt(slave[1]),
                        endpoint.getAuth(),
                        endpoint.getDb(),
                        endpoint.getTimeout()),
                    0,
                    16383,
                    i + 1,
                    slaves.size() + 1));
        }
        return nodeList.toArray(new RedisNode[0]);
    }

    private RedisNode[] getClusterNodes(RedisEndpoint endpoint) {
        Jedis conn = endpoint.connect();
        try {
            List<Object> slots = conn.clusterSlots();
            List<RedisNode> result = new ArrayList<>();
            for (Object slotInfoObj : slots) {
                @SuppressWarnings("unchecked")
                List<Object> slotInfo = (List<Object>) slotInfoObj;
                int sPos = Integer.parseInt(slotInfo.get(0).toString());
                int ePos = Integer.parseInt(slotInfo.get(1).toString());
                int total = slotInfo.size() - 2;
                for (int i = 0; i < total; i++) {
                    @SuppressWarnings("unchecked")
                    List<Object> node = (List<Object>) slotInfo.get(i + 2);
                    String host = SafeEncoder.encode((byte[]) node.get(0));
                    int port = Integer.parseInt(node.get(1).toString());
                    result.add(
                        new RedisNode(
                            new RedisEndpoint(
                                host,
                                port,
                                endpoint.getAuth(),
                                endpoint.getDb(),
                                endpoint.getTimeout()),
                            sPos,
                            ePos,
                            i,
                            total));
                }
            }
            return result.toArray(new RedisNode[0]);
        } finally {
            conn.close();
        }
    }
}
