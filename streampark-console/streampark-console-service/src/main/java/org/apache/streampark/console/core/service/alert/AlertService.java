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

import org.apache.streampark.console.core.bean.AlertTemplate;

/** The AlertService interface represents a service for sending alert. */
public interface AlertService {

    /**
     * Sends an alert based on the given alert configuration ID and alert template.
     *
     * @param alertConfigId the ID of the alert configuration
     * @param alertTemplate the alert template to use for generating the alert content
     * @return true if the alert is sent successfully, false otherwise
     */
    boolean alert(Long alertConfigId, AlertTemplate alertTemplate);
}
