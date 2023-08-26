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

package org.apache.streampark.console.base.util;

import org.apache.streampark.console.base.exception.ApiDetailException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The file utils. */
public class FileUtils {

  private FileUtils() {}

  /**
   * Reads the last portion of a file as a byte array.
   *
   * @param file the file to read
   * @param maxSize the maximum number of bytes to read from the end of the file
   * @return the byte array containing the content read from the file
   * @throws IOException if an I/O error occurs
   */
  public static byte[] readEndOfFile(File file, long maxSize) throws IOException {
    long readSize = maxSize;
    byte[] fileContent;
    try (RandomAccessFile raFile = new RandomAccessFile(file, "r")) {
      if (raFile.length() > maxSize) {
        raFile.seek(raFile.length() - maxSize);
      } else if (raFile.length() < maxSize) {
        readSize = (int) raFile.length();
      }
      fileContent = new byte[(int) readSize];
      raFile.read(fileContent);
    }
    return fileContent;
  }

  /**
   * Read the content of a file from a specified offset.
   *
   * @param file The file to read from
   * @param startOffset The offset from where to start reading the file
   * @param maxSize The maximum size of the file to read
   * @return The content of the file as a byte array
   * @throws IOException if an I/O error occurs while reading the file
   * @throws IllegalArgumentException if the startOffset is greater than the file length
   */
  public static byte[] readFileFromOffset(File file, long startOffset, long maxSize)
      throws IOException {
    if (file.length() < startOffset) {
      throw new IllegalArgumentException(
          String.format(
              "The startOffset %s is great than the file length %s", startOffset, file.length()));
    }
    byte[] fileContent;
    try (RandomAccessFile raFile = new RandomAccessFile(file, "r")) {
      long readSize = Math.min(maxSize, file.length() - startOffset);
      raFile.seek(startOffset);
      fileContent = new byte[(int) readSize];
      raFile.read(fileContent);
    }
    return fileContent;
  }

  /**
   * Roll View Log.
   *
   * @param path The file path.
   * @param offset The offset.
   * @param limit The limit.
   * @return The content of the file.
   * @throws ApiDetailException if there's an error rolling the view log.
   */
  public static String rollViewLog(String path, int offset, int limit) {
    try {
      File file = new File(path);
      if (file.exists() && file.isFile()) {
        try (Stream<String> stream = Files.lines(Paths.get(path))) {
          List<String> lines = stream.skip(offset).limit(limit).collect(Collectors.toList());
          StringBuilder builder = new StringBuilder();
          lines.forEach(line -> builder.append(line).append("\r\n"));
          return builder.toString();
        }
      }
      return null;
    } catch (Exception e) {
      throw new ApiDetailException("roll view log error: " + e);
    }
  }
}
