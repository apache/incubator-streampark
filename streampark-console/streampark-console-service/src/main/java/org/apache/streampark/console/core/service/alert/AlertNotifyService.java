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
import org.apache.streampark.console.core.bean.AlertConfigParams;
import org.apache.streampark.console.core.bean.AlertTemplate;

/**
 * This interface defines a service for sending alert notifications, it has multiple
 * implementations.
 */
public interface AlertNotifyService {

    /**
     * Performs an alert with the given alert configuration parameters and alert template.
     *
     * @param alertConfig alert configuration parameters.
     * @param template alert template to use.
     * @return true if the alert was successfully triggered, false otherwise.
     * @throws AlertException if an error occurs while performing the alert.
     */
    boolean doAlert(AlertConfigParams alertConfig, AlertTemplate template) throws AlertException;
}
