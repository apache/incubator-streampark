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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.Serializable;
import java.util.Properties;
import java.util.function.Supplier;

public class HBaseClient implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final Configuration CONF = HBaseConfiguration.create();
    private final Supplier<Connection> func;
    private transient Connection connection;

    public HBaseClient(Supplier<Connection> func) {
        this.func = func;
    }

    public Connection getConnection() {
        if (connection == null)
            connection = func.get();
        return connection;
    }

    public Table table(String table) throws Exception {
        return getConnection().getTable(TableName.valueOf(table));
    }

    public static HBaseClient apply(Properties prop) {
        Object user = prop.remove(ConfigKeys.KEY_HBASE_AUTH_USER());
        for (String key : prop.stringPropertyNames())
            CONF.set(key, prop.getProperty(key));
        return new HBaseClient(() -> {
            try {
                if (user != null) {
                    UserGroupInformation.setConfiguration(CONF);
                    UserGroupInformation remoteUser = UserGroupInformation.createRemoteUser(user.toString());
                    UserGroupInformation.setLoginUser(remoteUser);
                }
                Connection connection = ConnectionFactory.createConnection(CONF);
                Runtime.getRuntime()
                    .addShutdownHook(
                        new Thread(
                            () -> {
                                try {
                                    connection.close();
                                } catch (Exception ignored) {
                                    // ignore
                                }
                            }));
                return connection;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
