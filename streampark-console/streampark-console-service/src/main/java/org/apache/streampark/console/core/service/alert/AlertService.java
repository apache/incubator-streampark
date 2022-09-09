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

package org.apache.streampark.console.core.service.alert;

import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.CheckPointStatus;
import org.apache.streampark.console.core.enums.FlinkAppState;

public interface AlertService {

    void alert(Application application, CheckPointStatus checkPointStatus);

    void alert(Application application, FlinkAppState appState);

    boolean alert(AlertConfigWithParams params, AlertTemplate alertTemplate) throws AlertException;
}
