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

package org.apache.streampark.console.base.exception;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.ResponseCode;

public class PermissionDeniedException extends AbstractApiException {

  public PermissionDeniedException(String message) {
    super(message, ResponseCode.CODE_UNAUTHORIZED);
  }

  public PermissionDeniedException(Throwable cause) {
    super(Utils.stringifyException(cause), ResponseCode.CODE_UNAUTHORIZED);
  }

  public PermissionDeniedException(String message, Throwable cause) {
    super(message + Utils.stringifyException(cause), ResponseCode.CODE_UNAUTHORIZED);
  }

  @Override
  public String getMessage() {
    return "Permission denied: \n" + super.getMessage();
  }
}
