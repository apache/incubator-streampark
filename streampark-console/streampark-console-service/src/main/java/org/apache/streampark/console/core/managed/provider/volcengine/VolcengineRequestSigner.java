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

package org.apache.streampark.console.core.managed.provider.volcengine;

import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;

import lombok.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Volcengine HMAC-SHA256 request signer with deterministic canonicalization. */
@Component
class VolcengineRequestSigner {

    static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";
    static final String SERVICE = "flink";
    static final String SIGNED_HEADERS =
        "content-type;host;x-content-sha256;x-date;x-request-id";

    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withLocale(Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    SignedRequest sign(
                       String method,
                       URI endpoint,
                       Map<String, String> query,
                       byte[] body,
                       String region,
                       String requestId,
                       Instant now,
                       VolcengineCredentials credentials) {
        return sign(
            method,
            endpoint,
            query,
            body,
            CONTENT_TYPE,
            region,
            requestId,
            now,
            credentials);
    }

    SignedRequest sign(
                       String method,
                       URI endpoint,
                       Map<String, String> query,
                       byte[] body,
                       String contentType,
                       String region,
                       String requestId,
                       Instant now,
                       VolcengineCredentials credentials) {
        try {
            String canonicalQuery = canonicalQuery(query);
            String host = endpoint.getRawAuthority();
            String xDate = DATE_TIME.format(now);
            String shortDate = xDate.substring(0, 8);
            String payloadHash = sha256Hex(body);
            String canonicalHeaders =
                "content-type:" + contentType + "\n"
                    + "host:" + host + "\n"
                    + "x-content-sha256:" + payloadHash + "\n"
                    + "x-date:" + xDate + "\n"
                    + "x-request-id:" + requestId + "\n";
            String path =
                endpoint.getRawPath() == null || endpoint.getRawPath().isEmpty()
                    ? "/"
                    : endpoint.getRawPath();
            String canonicalRequest =
                method.toUpperCase(Locale.ROOT) + "\n"
                    + path + "\n"
                    + canonicalQuery + "\n"
                    + canonicalHeaders + "\n"
                    + SIGNED_HEADERS + "\n"
                    + payloadHash;
            String credentialScope = shortDate + "/" + region + "/" + SERVICE + "/request";
            String stringToSign =
                "HMAC-SHA256\n"
                    + xDate + "\n"
                    + credentialScope + "\n"
                    + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            byte[] signingKey = signingKey(credentials.secretKey(), shortDate, region, SERVICE);
            String signature;
            try {
                signature = hex(hmac(signingKey, stringToSign));
            } finally {
                Arrays.fill(signingKey, (byte) 0);
            }
            String accessKey = new String(credentials.accessKey());
            String authorization =
                "HMAC-SHA256 Credential="
                    + accessKey + "/" + credentialScope
                    + ", SignedHeaders=" + SIGNED_HEADERS
                    + ", Signature=" + signature;
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", contentType);
            headers.put("X-Content-Sha256", payloadHash);
            headers.put("X-Date", xDate);
            headers.put("X-Request-Id", requestId);
            headers.put("Authorization", authorization);
            return new SignedRequest(canonicalQuery, headers);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.UNKNOWN,
                "RequestSigningFailed",
                null,
                "Unable to sign Volcengine Flink request.");
        }
    }

    static String canonicalQuery(Map<String, String> query) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(query.entrySet());
        entries.sort(
            Comparator.comparing((Map.Entry<String, String> item) -> percentEncode(item.getKey()))
                .thenComparing(item -> percentEncode(item.getValue())));
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : entries) {
            if (result.length() > 0) {
                result.append('&');
            }
            result
                .append(percentEncode(entry.getKey()))
                .append('=')
                .append(percentEncode(entry.getValue()));
        }
        return result.toString();
    }

    private static String percentEncode(String value) {
        ByteBuffer bytes = StandardCharsets.UTF_8.encode(value == null ? "" : value);
        StringBuilder encoded = new StringBuilder();
        while (bytes.hasRemaining()) {
            int current = bytes.get() & 0xff;
            if ((current >= 'a' && current <= 'z')
                || (current >= 'A' && current <= 'Z')
                || (current >= '0' && current <= '9')
                || current == '-'
                || current == '_'
                || current == '.'
                || current == '~') {
                encoded.append((char) current);
            } else {
                encoded.append('%');
                encoded.append(Character.forDigit((current >>> 4) & 0xf, 16));
                encoded.append(Character.forDigit(current & 0xf, 16));
            }
        }
        return encoded.toString();
    }

    private static byte[] signingKey(
                                     char[] secretKey, String date, String region, String service) throws Exception {
        byte[] secret = new String(secretKey).getBytes(StandardCharsets.UTF_8);
        try {
            byte[] dateKey = hmac(secret, date);
            byte[] regionKey = hmac(dateKey, region);
            Arrays.fill(dateKey, (byte) 0);
            byte[] serviceKey = hmac(regionKey, service);
            Arrays.fill(regionKey, (byte) 0);
            byte[] signingKey = hmac(serviceKey, "request");
            Arrays.fill(serviceKey, (byte) 0);
            return signingKey;
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }

    private static byte[] hmac(byte[] key, String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] content) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private static String hex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            result[index * 2] = digits[value >>> 4];
            result[index * 2 + 1] = digits[value & 0x0f];
        }
        return new String(result);
    }

    @Value
    static class SignedRequest {

        String canonicalQuery;

        Map<String, String> headers;
    }
}
