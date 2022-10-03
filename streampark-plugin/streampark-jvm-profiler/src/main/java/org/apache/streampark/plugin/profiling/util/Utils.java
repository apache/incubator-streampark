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

package org.apache.streampark.plugin.profiling.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Map;
import java.util.zip.Deflater;

public class Utils {

    protected static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                String.format("Failed to serialize %s (%s)", obj, obj.getClass()), e);
        }
    }

    public static byte[] toByteArray(InputStream input) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            int readByteCount;
            byte[] data = new byte[16 * 1024];
            while ((readByteCount = input.read(data, 0, data.length)) != -1) {
                byteArrayOutputStream.write(data, 0, readByteCount);
            }
            byteArrayOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String zipString(String text) {
        // Creates a new compressor with the specified compression level.
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        // Sets compressed input data.
        deflater.setInput(text.getBytes());
        // When called, indicates that compression should end with the current contents of the input buffer.
        deflater.finish();
        final byte[] bytes = new byte[256];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);

        while (!deflater.finished()) {
            // Compresses the input data and fills the specified buffer with the compressed data.
            int length = deflater.deflate(bytes);
            outputStream.write(bytes, 0, length);
        }
        // Turn off the compressor and discard any unprocessed input.
        deflater.end();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static String getLocalHostName() {
        try {
            Map<String, String> env = System.getenv();
            if (env.containsKey("COMPUTERNAME")) {
                return env.get("COMPUTERNAME");
            } else if (env.containsKey("HOSTNAME")) {
                return env.get("HOSTNAME");
            } else {
                return InetAddress.getLocalHost().getHostName();
            }
        } catch (Throwable e) {
            return "unknown_localhost_name";
        }
    }
}
