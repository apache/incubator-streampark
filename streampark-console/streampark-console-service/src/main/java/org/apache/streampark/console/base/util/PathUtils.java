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

import java.io.File;
import java.io.IOException;

/**
 * Filesystem path containment utilities.
 *
 * <p>All checks resolve the canonical (absolute, normalized, symlink-resolved) paths before
 * comparison, so the supplied name is always confined to the expected directory.
 */
public final class PathUtils {

  private PathUtils() {}

  /**
   * Returns {@code true} only when the canonical path of {@code child} is strictly located
   * somewhere <em>inside</em> {@code parent} (at any depth). A {@code child} that equals {@code
   * parent} is NOT considered a descendant.
   *
   * @param parent the allowed root directory
   * @param child the path to check
   * @return {@code true} if {@code child} is strictly under {@code parent}
   * @throws IOException if canonical path resolution fails
   */
  public static boolean isDescendantPath(File parent, File child) throws IOException {
    String parentPath = parent.getCanonicalPath();
    String childPath = child.getCanonicalPath();
    return childPath.startsWith(parentPath.concat(File.separator));
  }

  /**
   * Returns {@code true} only when the canonical parent directory of {@code child} is exactly
   * {@code parent}, i.e. {@code child} is a <em>direct</em> (first-level) entry of {@code parent}.
   * This is stricter than {@link #isDescendantPath(File, File)} and is used to confine a supplied
   * name (e.g. a module archive) to a single level under the expected directory.
   *
   * @param parent the allowed root directory
   * @param child the path to check
   * @return {@code true} if {@code child} sits directly under {@code parent}
   * @throws IOException if canonical path resolution fails
   */
  public static boolean isDirectChildPath(File parent, File child) throws IOException {
    File childParent = child.getCanonicalFile().getParentFile();
    return childParent != null && parent.getCanonicalFile().equals(childParent.getCanonicalFile());
  }
}
