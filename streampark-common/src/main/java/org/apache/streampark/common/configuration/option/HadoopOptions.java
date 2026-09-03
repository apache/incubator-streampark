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

package org.apache.streampark.common.configuration.option;

import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.ConfigOptions;

import java.time.Duration;

/**
 * Hadoop filesystem identity and Kerberos authentication options.
 *
 * <p>Credential-bearing options are marked sensitive at declaration time so conversion failures
 * never include their raw values.
 */
public final class HadoopOptions {

    /** JVM property consumed by Hadoop clients when no explicit UGI is supplied. */
    public static final String HADOOP_USER_NAME_PROPERTY = "HADOOP_USER_NAME";

    /** Hadoop user used by StreamPark services when Kerberos is disabled. */
    public static final ConfigOption<String> USER_NAME =
        ConfigOptions.key("streampark.hadoop-user-name")
            .stringType()
            .defaultValue("hdfs")
            .check(value -> !value.trim().isEmpty(), "Hadoop user name must not be blank")
            .withDescription("Hadoop user used by StreamPark services.")
            .build();

    /** Enables detailed Kerberos login diagnostics. */
    public static final ConfigOption<Boolean> KERBEROS_DEBUG =
        ConfigOptions.key("security.kerberos.login.debug")
            .booleanType()
            .defaultValue(false)
            .withDescription("Enables Kerberos login diagnostics.")
            .build();

    /** Enables Kerberos authentication for Hadoop clients. */
    public static final ConfigOption<Boolean> KERBEROS_ENABLED =
        ConfigOptions.key("security.kerberos.login.enable")
            .booleanType()
            .defaultValue(false)
            .withDescription("Enables Kerberos authentication.")
            .build();

    /** Kerberos principal used for login. */
    public static final ConfigOption<String> KERBEROS_PRINCIPAL =
        ConfigOptions.key("security.kerberos.login.principal")
            .stringType()
            .defaultValue("")
            .withDescription("Kerberos principal.")
            .build();

    /** Sensitive keytab path used for Kerberos login. */
    public static final ConfigOption<String> KERBEROS_KEYTAB =
        ConfigOptions.key("security.kerberos.login.keytab")
            .stringType()
            .defaultValue("")
            .sensitive()
            .withDescription("Kerberos keytab path.")
            .build();

    /** Path to the Kerberos client configuration file. */
    public static final ConfigOption<String> KERBEROS_KRB5 =
        ConfigOptions.key("security.kerberos.login.krb5")
            .stringType()
            .defaultValue("")
            .withDescription("Kerberos krb5.conf path.")
            .build();

    /** Interval used to renew Kerberos login tickets. */
    public static final ConfigOption<Duration> KERBEROS_TICKET_LIFETIME =
        ConfigOptions.key("security.kerberos.ttl")
            .durationType()
            .defaultValue(Duration.ofHours(2))
            .check(value -> !value.isZero(), "ticket lifetime must be greater than zero")
            .withDescription("Kerberos ticket renewal lifetime.")
            .build();

    private HadoopOptions() {
    }
}
