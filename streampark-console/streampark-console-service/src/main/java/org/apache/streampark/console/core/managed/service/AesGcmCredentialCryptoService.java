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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** AES-GCM credential envelope implementation with versioned master keys. */
@Service
class AesGcmCredentialCryptoService implements CredentialCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String ENVELOPE_VERSION = "v1";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final CredentialMasterKeyProvider keyProvider;
    private final SecureRandom secureRandom;

    @Autowired
    AesGcmCredentialCryptoService(CredentialMasterKeyProvider keyProvider) {
        this(keyProvider, new SecureRandom());
    }

    AesGcmCredentialCryptoService(
                                  CredentialMasterKeyProvider keyProvider, SecureRandom secureRandom) {
        this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider must not be null");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    @Override
    public EncryptedCredential encrypt(String plaintext, CredentialBinding binding) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("plaintext must not be empty");
        }
        Objects.requireNonNull(binding, "binding must not be null");

        int keyVersion = keyProvider.activeKeyVersion();
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                keyProvider.key(keyVersion),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad(binding));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv)
                .put(encrypted)
                .array();
            String envelope = ENVELOPE_VERSION
                + "."
                + keyVersion
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
            return new EncryptedCredential(keyVersion, envelope);
        } catch (GeneralSecurityException e) {
            throw cryptoFailure("Credential encryption failed", e);
        }
    }

    @Override
    public String decrypt(
                          EncryptedCredential encryptedCredential, CredentialBinding binding) {
        Objects.requireNonNull(encryptedCredential, "encryptedCredential must not be null");
        Objects.requireNonNull(binding, "binding must not be null");

        Envelope envelope = parseEnvelope(encryptedCredential);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                Cipher.DECRYPT_MODE,
                keyProvider.key(envelope.keyVersion),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, envelope.iv));
            cipher.updateAAD(aad(binding));
            return new String(cipher.doFinal(envelope.encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw cryptoFailure("Credential decryption failed", e);
        }
    }

    @Override
    public EncryptedCredential rotate(
                                      EncryptedCredential encryptedCredential,
                                      CredentialBinding binding) {
        String plaintext = decrypt(encryptedCredential, binding);
        return encrypt(plaintext, binding);
    }

    private static Envelope parseEnvelope(EncryptedCredential credential) {
        String[] parts = credential.getCiphertext().split("\\.", -1);
        if (parts.length != 3 || !ENVELOPE_VERSION.equals(parts[0])) {
            throw invalidEnvelope("Credential envelope format is invalid");
        }

        int embeddedKeyVersion;
        try {
            embeddedKeyVersion = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new CredentialCryptoException(
                CredentialCryptoException.Reason.INVALID_ENVELOPE,
                "Credential envelope key version is invalid",
                e);
        }
        if (embeddedKeyVersion != credential.getKeyVersion()) {
            throw invalidEnvelope("Credential envelope key version does not match stored metadata");
        }

        byte[] payload;
        try {
            payload = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new CredentialCryptoException(
                CredentialCryptoException.Reason.INVALID_ENVELOPE,
                "Credential envelope payload is invalid",
                e);
        }
        if (payload.length <= GCM_IV_LENGTH_BYTES + GCM_TAG_LENGTH_BITS / Byte.SIZE) {
            throw invalidEnvelope("Credential envelope payload is too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);
        return new Envelope(embeddedKeyVersion, iv, encrypted);
    }

    private static byte[] aad(CredentialBinding binding) {
        String value = binding.getProviderType()
            + '\0'
            + binding.getAccountId()
            + '\0'
            + binding.getField().aadName();
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static CredentialCryptoException invalidEnvelope(String message) {
        return new CredentialCryptoException(
            CredentialCryptoException.Reason.INVALID_ENVELOPE, message);
    }

    private static CredentialCryptoException cryptoFailure(String message, Throwable cause) {
        return new CredentialCryptoException(
            CredentialCryptoException.Reason.CRYPTO_FAILURE, message, cause);
    }

    private static final class Envelope {

        private final int keyVersion;
        private final byte[] iv;
        private final byte[] encrypted;

        private Envelope(int keyVersion, byte[] iv, byte[] encrypted) {
            this.keyVersion = keyVersion;
            this.iv = iv;
            this.encrypted = encrypted;
        }
    }
}
