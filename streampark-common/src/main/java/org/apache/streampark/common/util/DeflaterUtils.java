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

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class DeflaterUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(DeflaterUtils.class.getName());

    private DeflaterUtils() {
    }

    public static String zipString(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(text.getBytes());
        deflater.finish();
        byte[] bytes = new byte[256];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        while (!deflater.finished()) {
            int length = deflater.deflate(bytes);
            outputStream.write(bytes, 0, length);
        }
        deflater.end();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static String unzipString(String zipString) {
        byte[] decode;
        try {
            decode = Base64.getDecoder().decode(zipString);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Inflater inflater = new Inflater();
        inflater.setInput(decode);
        byte[] bytes = new byte[256];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        try {
            while (!inflater.finished()) {
                int length = inflater.inflate(bytes);
                outputStream.write(bytes, 0, length);
            }
        } catch (DataFormatException e) {
            LOG.warn("Failed to unzip string", e);
            return null;
        } finally {
            inflater.end();
        }
        return outputStream.toString();
    }

    /** Returns plain text, repeatedly decoding when the input is compressed multiple times. */
    public static String toPlainText(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String current = text;
        while (true) {
            String decoded = unzipString(current);
            if (decoded == null) {
                return current;
            }
            current = decoded;
        }
    }

    /** Stores SQL/conf text as a single compressed blob regardless of input encoding. */
    public static String compressForStorage(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        return zipString(toPlainText(text));
    }
}
