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

package org.apache.steampark.common.utils;

import org.apache.streampark.common.exception.AbstractApiException;
import org.apache.streampark.common.exception.AlertException;
import org.apache.streampark.common.exception.ApiAlertException;
import org.apache.streampark.common.exception.ApiDetailException;
import org.apache.streampark.common.exception.ApplicationException;
import org.apache.streampark.common.exception.IllegalFileTypeException;
import org.apache.streampark.common.util.PremisesUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PremisesUtilsTest {

  @Test
  public void abstractApiExceptionCase() {

    /* throwIfNull */
    assertThrows(
        RuntimeException.class,
        () ->
            PremisesUtils.throwIfNull(
                null, "AbstractApiException can't be construction", AbstractApiException.class));

    /* throwIfFalse */
    assertThrows(
        RuntimeException.class,
        () ->
            PremisesUtils.throwIfFalse(
                false, "AbstractApiException can't be construction", AbstractApiException.class));

    /* throwIfTrue */
    assertThrows(
        RuntimeException.class,
        () ->
            PremisesUtils.throwIfTrue(
                true, "AbstractApiException can't be construction", AbstractApiException.class));
  }

  @Test
  public void alertExceptionCase() {

    /* throwIfNull */
    AlertException exception1 =
        assertThrows(
            AlertException.class,
            () -> PremisesUtils.throwIfNull(null, "object is null", AlertException.class));
    assertEquals(exception1.getMessage(), "object is null");

    /* throwIfFalse */
    AlertException exception2 =
        assertThrows(
            AlertException.class,
            () ->
                PremisesUtils.throwIfFalse(false, "expression return false", AlertException.class));
    assertEquals(exception2.getMessage(), "expression return false");

    /* throwIfTrue */
    AlertException exception3 =
        assertThrows(
            AlertException.class,
            () -> PremisesUtils.throwIfTrue(true, "expression return true", AlertException.class));
    assertEquals(exception3.getMessage(), "expression return true");
  }

  @Test
  public void apiAlertExceptionCase() {

    /* throwIfNull */
    ApiAlertException exception1 =
        assertThrows(
            ApiAlertException.class,
            () -> PremisesUtils.throwIfNull(null, "object is null", ApiAlertException.class));
    assertEquals(exception1.getMessage(), "object is null");

    /* throwIfFalse */
    AlertException exception2 =
        assertThrows(
            AlertException.class,
            () ->
                PremisesUtils.throwIfFalse(false, "expression return false", AlertException.class));
    assertEquals(exception2.getMessage(), "expression return false");

    /* throwIfTrue */
    AlertException exception3 =
        assertThrows(
            AlertException.class,
            () -> PremisesUtils.throwIfTrue(true, "expression return true", AlertException.class));
    assertEquals(exception3.getMessage(), "expression return true");
  }

  @Test
  public void apiDetailExceptionCase() {

    /* throwIfNull */
    ApiDetailException exception1 =
        assertThrows(
            ApiDetailException.class,
            () -> PremisesUtils.throwIfNull(null, "object is null", ApiDetailException.class));
    assertEquals(exception1.getMessage(), "Detail exception: \n" + "object is null");

    /* throwIfFalse */
    ApiDetailException exception2 =
        assertThrows(
            ApiDetailException.class,
            () ->
                PremisesUtils.throwIfFalse(
                    false, "expression return false", ApiDetailException.class));
    assertEquals(exception2.getMessage(), "Detail exception: \n" + "expression return false");

    /* throwIfTrue */
    ApiDetailException exception3 =
        assertThrows(
            ApiDetailException.class,
            () ->
                PremisesUtils.throwIfTrue(
                    true, "expression return true", ApiDetailException.class));
    assertEquals(exception3.getMessage(), "Detail exception: \n" + "expression return true");
  }

  @Test
  public void applicationExceptionCase() {

    /* throwIfNull */
    ApplicationException exception1 =
        assertThrows(
            ApplicationException.class,
            () -> PremisesUtils.throwIfNull(null, "object is null", ApplicationException.class));
    assertEquals(exception1.getMessage(), "object is null");

    /* throwIfFalse */
    ApplicationException exception2 =
        assertThrows(
            ApplicationException.class,
            () ->
                PremisesUtils.throwIfFalse(
                    false, "expression return false", ApplicationException.class));
    assertEquals(exception2.getMessage(), "expression return false");

    /* throwIfTrue */
    ApplicationException exception3 =
        assertThrows(
            ApplicationException.class,
            () ->
                PremisesUtils.throwIfTrue(
                    true, "expression return true", ApplicationException.class));
    assertEquals(exception3.getMessage(), "expression return true");
  }

  @Test
  public void illegalFileTypeExceptionCase() {

    /* throwIfNull */
    IllegalFileTypeException exception1 =
        assertThrows(
            IllegalFileTypeException.class,
            () ->
                PremisesUtils.throwIfNull(null, "object is null", IllegalFileTypeException.class));
    assertEquals(exception1.getMessage(), "object is null");

    /* throwIfFalse */
    IllegalFileTypeException exception2 =
        assertThrows(
            IllegalFileTypeException.class,
            () ->
                PremisesUtils.throwIfFalse(
                    false, "expression return false", IllegalFileTypeException.class));
    assertEquals(exception2.getMessage(), "expression return false");

    /* throwIfTrue */
    IllegalFileTypeException exception3 =
        assertThrows(
            IllegalFileTypeException.class,
            () ->
                PremisesUtils.throwIfTrue(
                    true, "expression return true", IllegalFileTypeException.class));
    assertEquals(exception3.getMessage(), "expression return true");
  }
}
