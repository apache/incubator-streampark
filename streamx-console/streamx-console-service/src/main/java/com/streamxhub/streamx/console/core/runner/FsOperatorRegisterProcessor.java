/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.streamxhub.streamx.console.core.runner;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.fs.FsOperator;
import com.streamxhub.streamx.common.fs.UnifiledFsOperator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * provide FsOperator Bean in SpringBoot.
 * what more, you can use {@link UnifiledFsOperator} directly.
 */
@Configuration
public class FsOperatorRegisterProcessor implements BeanDefinitionRegistryPostProcessor {

    private final ApplicationContext context;

    public FsOperatorRegisterProcessor(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        System.getProperties().setProperty(
            ConfigConst.KEY_STREAMX_WORKSPACE(),
            context.getEnvironment().getProperty(ConfigConst.KEY_STREAMX_WORKSPACE(), ConfigConst.STREAMX_WORKSPACE_DEFAULT())
        );
        System.getProperties().setProperty(
            ConfigConst.KEY_STREAMX_WORKSPACE_TYPE(),
            context.getEnvironment().getProperty(ConfigConst.KEY_STREAMX_WORKSPACE_TYPE(), ConfigConst.STREAMX_WORKSPACE_TYPE_DEFAULT())
        );
        beanDefinitionRegistry.registerBeanDefinition("fsOperator",
            new RootBeanDefinition(FsOperator.class, UnifiledFsOperator::auto)
        );
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    }

}
