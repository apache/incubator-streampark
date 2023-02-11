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

package org.apache.streampark.console.core.utils;

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.enums.ExecutionMode;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Util class for parsing and checking Yarn queue & Label */
public class YarnQueueLabelExpression {

  public static final String AT = "@";

  public static final String REGEX = "[a-zA-Z0-9_\\-]+";

  public static final String ERR_HINTS = "Yarn queue label format is invalid.";

  public static final Pattern QUEUE_LABEL_PATTERN =
      Pattern.compile(String.format("^(%s)(.%s)*(%s(%s)(,%s)*)?$", REGEX, REGEX, AT, REGEX, REGEX));

  public static final String QUEUE_LABEL_FORMAT = "%s" + AT + "%s";

  private final String queue;
  private @Nullable final String labelExpression;

  private YarnQueueLabelExpression(String queue, String labelExpression) {
    this.labelExpression = StringUtils.isEmpty(labelExpression) ? null : labelExpression;
    this.queue = queue;
  }

  public Optional<String> getLabelExpression() {
    return Optional.ofNullable(labelExpression);
  }

  public @Nonnull String getQueue() {
    return queue;
  }

  @Override
  public String toString() {
    return StringUtils.isEmpty(labelExpression)
        ? queue
        : String.format(QUEUE_LABEL_FORMAT, queue, labelExpression);
  }

  private static boolean isValid(String queueLabel, boolean ignoreEmpty) {
    if (StringUtils.isEmpty(queueLabel)) {
      return ignoreEmpty;
    }
    return QUEUE_LABEL_PATTERN.matcher(queueLabel).matches();
  }

  // Visible for test.
  public static boolean isValid(String queueLabel) {
    return isValid(queueLabel, false);
  }

  /**
   * Only check the validation of queue-labelExpression when using yarn application or yarn-session
   * mode.
   *
   * @param executionMode execution mode.
   * @param queueLabel queueLabel expression.
   */
  public static void checkQueueLabelIfNeed(int executionMode, String queueLabel) {
    if ((ExecutionMode.YARN_SESSION == ExecutionMode.of(executionMode)
            || ExecutionMode.YARN_APPLICATION == ExecutionMode.of(executionMode))
        && !YarnQueueLabelExpression.isValid(queueLabel, true)) {
      throw new IllegalArgumentException(ERR_HINTS);
    }
  }

  // Visible for test.
  public static YarnQueueLabelExpression of(@Nonnull String queueLabelExpr) {
    if (isValid(queueLabelExpr, false)) {
      String[] strs = queueLabelExpr.split(AT);
      if (strs.length == 2) {
        return new YarnQueueLabelExpression(strs[0], strs[1]);
      }
      return new YarnQueueLabelExpression(strs[0], null);
    }
    throw new IllegalArgumentException(ERR_HINTS);
  }

  public static YarnQueueLabelExpression of(
      @Nonnull String queue, @Nullable String labelExpression) {
    YarnQueueLabelExpression queueLabelExpression =
        new YarnQueueLabelExpression(queue, labelExpression);
    if (isValid(queueLabelExpression.toString(), false)) {
      return queueLabelExpression;
    }
    throw new IllegalArgumentException(ERR_HINTS);
  }

  /**
   * Add the queue and label expression into the map as the specified configuration items. For
   * {@link ConfigConst#KEY_YARN_APP_QUEUE} & {@link ConfigConst#KEY_YARN_APP_NODE_LABEL()}.
   *
   * @param queueLabelExp queue and label expression
   * @param map the target properties.
   */
  public static <V> void addQueueLabelExprInto(String queueLabelExp, @Nonnull Map<String, V> map) {
    if (StringUtils.isEmpty(queueLabelExp)) {
      return;
    }
    YarnQueueLabelExpression yarnQueueLabelExpression = of(queueLabelExp);
    yarnQueueLabelExpression
        .getLabelExpression()
        .ifPresent(labelExp -> map.put(ConfigConst.KEY_YARN_APP_NODE_LABEL(), (V) labelExp));
    map.put(ConfigConst.KEY_YARN_APP_QUEUE(), (V) yarnQueueLabelExpression.queue);
  }
}