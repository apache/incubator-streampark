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

package org.apache.streampark.console.core.runner;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.conf.InternalOption;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.util.WebUtils;

import org.apache.commons.lang3.StringUtils;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Optional;

public class EnvApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Optional<String> profile =
            Arrays.stream(context.getEnvironment().getActiveProfiles()).findFirst();
        if ("test".equals(profile.orElse(null))) {
            return;
        }

        String appHome = WebUtils.getAppHome();
        if (StringUtils.isBlank(appHome)) {
            throw new ExceptionInInitializerError(
                String.format(
                    "[StreamPark] System initialization check failed,"
                        + " The system initialization check failed. If started local for development and debugging,"
                        + " please ensure the -D%s parameter is clearly specified,"
                        + " more detail: https://streampark.apache.org/docs/user-guide/deployment",
                    ConfigKeys.KEY_APP_HOME()));
        }

        // init InternalConfig
        initInternalConfig(context.getEnvironment());
    }

    private void initInternalConfig(Environment springEnv) {
        // override config from spring application.yaml
        InternalConfigHolder.keys().stream()
            .filter(springEnv::containsProperty)
            .forEach(
                key -> {
                    InternalOption config = InternalConfigHolder.getConfig(key);
                    Utils.requireNotNull(config);
                    InternalConfigHolder.set(config, springEnv.getProperty(key, config.classType()));
                });
    }

}

