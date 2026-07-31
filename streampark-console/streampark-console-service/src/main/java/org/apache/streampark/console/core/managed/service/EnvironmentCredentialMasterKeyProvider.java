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

package org.apache.streampark.console.core.managed.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Master key provider backed by deployment-injected configuration. */
@Component
class EnvironmentCredentialMasterKeyProvider implements CredentialMasterKeyProvider {

    private final int activeKeyVersion;
    private final Map<Integer, SecretKey> keys;

    EnvironmentCredentialMasterKeyProvider(
                                           @Value("${streampark.managed-flink.credentials.active-key-version:0}") int activeKeyVersion,
                                           @Value("${streampark.managed-flink.credentials.master-keys:}") String encodedKeys) {
        this.activeKeyVersion = activeKeyVersion;
        this.keys = parseKeys(encodedKeys);
    }

    @Override
    public int activeKeyVersion() {
        if (activeKeyVersion <= 0 || !keys.containsKey(activeKeyVersion)) {
            throw configurationError("Active credential master key is not configured");
        }
        return activeKeyVersion;
    }

    @Override
    public SecretKey key(int version) {
        SecretKey key = keys.get(version);
        if (key == null) {
            throw configurationError("Credential master key version is not configured: " + version);
        }
        return key;
    }

    private static Map<Integer, SecretKey> parseKeys(String encodedKeys) {
        if (encodedKeys == null || encodedKeys.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, SecretKey> parsed = new HashMap<>();
        for (String entry : encodedKeys.split(",")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length != 2) {
                throw configurationError("Credential master keys must use version=base64 format");
            }
            int version = parseVersion(pair[0]);
            byte[] keyBytes = decodeKey(pair[1], version);
            if (parsed.put(version, new SecretKeySpec(keyBytes, "AES")) != null) {
                throw configurationError("Duplicate credential master key version: " + version);
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    private static int parseVersion(String value) {
        try {
            int version = Integer.parseInt(value.trim());
            if (version <= 0) {
                throw configurationError("Credential master key version must be greater than zero");
            }
            return version;
        } catch (NumberFormatException e) {
            throw new CredentialCryptoException(
                CredentialCryptoException.Reason.CONFIGURATION,
                "Credential master key version must be an integer",
                e);
        }
    }

    private static byte[] decodeKey(String value, int version) {
        try {
            byte[] key = Base64.getDecoder().decode(value.trim());
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw configurationError(
                    "Credential master key must decode to 128, 192, or 256 bits for version "
                        + version);
            }
            return key;
        } catch (IllegalArgumentException e) {
            throw new CredentialCryptoException(
                CredentialCryptoException.Reason.CONFIGURATION,
                "Credential master key is not valid Base64 for version " + version,
                e);
        }
    }

    private static CredentialCryptoException configurationError(String message) {
        return new CredentialCryptoException(
            CredentialCryptoException.Reason.CONFIGURATION, message);
    }
}
