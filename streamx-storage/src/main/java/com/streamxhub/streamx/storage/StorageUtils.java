/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.storage;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.InvalidPathException;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class StorageUtils {

    private static final Set<Character> AVOID_CHARS_IN_FILENAME = new LinkedHashSet(Arrays.asList('#', '%', '&', '{', '}', '\\', '<', '>', '*', '?', ' ', '\t', '$', '!', '\'', '"', ':', '@'));

    static void validateName(String filename) throws InvalidPathException {
        if (StringUtils.isBlank(filename)) {
            throw new InvalidPathException(filename, "filename cannot be blank");
        } else {
            boolean containsInvalidChar = filename.chars().anyMatch(AVOID_CHARS_IN_FILENAME::contains);
            if (containsInvalidChar) {
                throw new InvalidPathException(filename, String.format("filename cannot include the following characters: %s", AVOID_CHARS_IN_FILENAME));
            }
        }
    }

    static Path getBasePath(URI baseUri, String storageGroup) {
        return Paths.get(baseUri).resolve(storageGroup);
    }

    static Path getFileNamePath(Path basePath, String filename) {
        Objects.requireNonNull(basePath);
        Objects.requireNonNull(filename);
        return basePath.resolve(filename);
    }

    public static String getFullBucketPath(String bucketPathPrefix, String filename){
        Objects.requireNonNull(bucketPathPrefix);
        Objects.requireNonNull(filename);
        return String.format("%s/%s/%s", bucketPathPrefix, filename);
    }
}
