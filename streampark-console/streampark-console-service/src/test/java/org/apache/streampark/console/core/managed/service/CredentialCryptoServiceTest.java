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

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CredentialCryptoServiceTest {

    private static final String PLAINTEXT = "credential-value-that-must-stay-secret";
    private static final CredentialBinding ACCESS_KEY_BINDING =
        new CredentialBinding("VOLCENGINE", 100L, CredentialField.ACCESS_KEY);

    @Test
    void shouldEncryptWithRandomIvAndDecryptWithBoundAad() {
        MutableMasterKeyProvider keyProvider = keyProvider();
        CredentialCryptoService cryptoService =
            new AesGcmCredentialCryptoService(keyProvider);

        EncryptedCredential first = cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);
        EncryptedCredential second = cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);

        assertThat(first.getKeyVersion()).isEqualTo(1);
        assertThat(first.getCiphertext())
            .startsWith("v1.1.")
            .doesNotContain(PLAINTEXT)
            .isNotEqualTo(second.getCiphertext());
        assertThat(cryptoService.decrypt(first, ACCESS_KEY_BINDING)).isEqualTo(PLAINTEXT);
        assertThat(cryptoService.decrypt(second, ACCESS_KEY_BINDING)).isEqualTo(PLAINTEXT);
    }

    @Test
    void shouldRejectCiphertextSwappedAcrossAccounts() {
        CredentialCryptoService cryptoService =
            new AesGcmCredentialCryptoService(keyProvider());
        EncryptedCredential encrypted =
            cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);
        CredentialBinding otherAccount =
            new CredentialBinding("VOLCENGINE", 101L, CredentialField.ACCESS_KEY);

        assertCryptoFailure(cryptoService, encrypted, otherAccount);
    }

    @Test
    void shouldRejectCiphertextSwappedAcrossFields() {
        CredentialCryptoService cryptoService =
            new AesGcmCredentialCryptoService(keyProvider());
        EncryptedCredential encrypted =
            cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);
        CredentialBinding secretKeyBinding =
            new CredentialBinding("VOLCENGINE", 100L, CredentialField.SECRET_KEY);

        assertCryptoFailure(cryptoService, encrypted, secretKeyBinding);
    }

    @Test
    void shouldRejectDifferentKeyMaterialForSameVersion() {
        CredentialCryptoService encryptor =
            new AesGcmCredentialCryptoService(keyProvider());
        EncryptedCredential encrypted =
            encryptor.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);
        Map<Integer, SecretKey> differentKeys = new HashMap<>();
        differentKeys.put(1, new SecretKeySpec(keyBytes("different-key"), "AES"));
        CredentialCryptoService decryptor =
            new AesGcmCredentialCryptoService(
                new MutableMasterKeyProvider(1, differentKeys));

        assertCryptoFailure(decryptor, encrypted, ACCESS_KEY_BINDING);
    }

    @Test
    void shouldRejectEnvelopeWithMismatchedStoredKeyVersion() {
        CredentialCryptoService cryptoService =
            new AesGcmCredentialCryptoService(keyProvider());
        EncryptedCredential encrypted =
            cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);
        EncryptedCredential inconsistent =
            new EncryptedCredential(2, encrypted.getCiphertext());

        assertThatExceptionOfType(CredentialCryptoException.class)
            .isThrownBy(() -> cryptoService.decrypt(inconsistent, ACCESS_KEY_BINDING))
            .satisfies(
                error -> assertThat(error.getReason())
                    .isEqualTo(CredentialCryptoException.Reason.INVALID_ENVELOPE));
    }

    @Test
    void shouldRotateFromHistoricalToActiveKey() {
        MutableMasterKeyProvider keyProvider = keyProvider();
        CredentialCryptoService cryptoService =
            new AesGcmCredentialCryptoService(keyProvider);
        EncryptedCredential oldCredential =
            cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);

        keyProvider.activeVersion = 2;
        EncryptedCredential rotated =
            cryptoService.rotate(oldCredential, ACCESS_KEY_BINDING);

        assertThat(oldCredential.getKeyVersion()).isEqualTo(1);
        assertThat(rotated.getKeyVersion()).isEqualTo(2);
        assertThat(rotated.getCiphertext()).startsWith("v1.2.");
        assertThat(cryptoService.decrypt(rotated, ACCESS_KEY_BINDING)).isEqualTo(PLAINTEXT);
    }

    @Test
    void shouldKeepCredentialOperationsUnavailableWithoutInjectedKey() {
        EnvironmentCredentialMasterKeyProvider keyProvider =
            new EnvironmentCredentialMasterKeyProvider(0, "");
        CredentialCryptoService cryptoService =
            new AesGcmCredentialCryptoService(keyProvider);

        assertThatExceptionOfType(CredentialCryptoException.class)
            .isThrownBy(() -> cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING))
            .satisfies(
                error -> assertThat(error.getReason())
                    .isEqualTo(CredentialCryptoException.Reason.CONFIGURATION));
    }

    @Test
    void shouldParseDeploymentInjectedVersionedKeys() {
        String first = Base64.getEncoder().encodeToString(keyBytes("first-key"));
        String second = Base64.getEncoder().encodeToString(keyBytes("second-key"));
        CredentialMasterKeyProvider keyProvider =
            new EnvironmentCredentialMasterKeyProvider(2, "1=" + first + ",2=" + second);
        CredentialCryptoService cryptoService =
            new AesGcmCredentialCryptoService(keyProvider);

        EncryptedCredential encrypted =
            cryptoService.encrypt(PLAINTEXT, ACCESS_KEY_BINDING);

        assertThat(encrypted.getKeyVersion()).isEqualTo(2);
        assertThat(cryptoService.decrypt(encrypted, ACCESS_KEY_BINDING)).isEqualTo(PLAINTEXT);
    }

    private static void assertCryptoFailure(
                                            CredentialCryptoService cryptoService,
                                            EncryptedCredential encrypted,
                                            CredentialBinding binding) {
        assertThatExceptionOfType(CredentialCryptoException.class)
            .isThrownBy(() -> cryptoService.decrypt(encrypted, binding))
            .satisfies(
                error -> {
                    assertThat(error.getReason())
                        .isEqualTo(CredentialCryptoException.Reason.CRYPTO_FAILURE);
                    assertThat(error.getMessage()).doesNotContain(PLAINTEXT);
                });
    }

    private static MutableMasterKeyProvider keyProvider() {
        Map<Integer, SecretKey> keys = new HashMap<>();
        keys.put(1, new SecretKeySpec(keyBytes("first-key"), "AES"));
        keys.put(2, new SecretKeySpec(keyBytes("second-key"), "AES"));
        return new MutableMasterKeyProvider(1, keys);
    }

    private static byte[] keyBytes(String seed) {
        byte[] source = seed.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        for (int index = 0; index < key.length; index++) {
            key[index] = source[index % source.length];
        }
        return key;
    }

    private static final class MutableMasterKeyProvider
        implements
            CredentialMasterKeyProvider {

        private int activeVersion;
        private final Map<Integer, SecretKey> keys;

        private MutableMasterKeyProvider(
                                         int activeVersion, Map<Integer, SecretKey> keys) {
            this.activeVersion = activeVersion;
            this.keys = keys;
        }

        @Override
        public int activeKeyVersion() {
            return activeVersion;
        }

        @Override
        public SecretKey key(int version) {
            return keys.get(version);
        }
    }
}
