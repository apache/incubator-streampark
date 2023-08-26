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

package org.apache.streampark.console.core.service.application;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.Application;

/**
 * This interface represents an Application Operation Service.
 * It extends the IService interface for handling Application entities.
 */
public interface ApplicationOpService extends IService<Application> {

    void starting(Application app);

    void start(Application app, boolean auto) throws Exception;

    void restart(Application application) throws Exception;

    void revoke(Application app) throws ApplicationException;

    void cancel(Application app) throws Exception;

    void forcedStop(Application app);

}
