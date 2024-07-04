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

package org.apache.streampark.console.system.authentication;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.AbstractShiroFilter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

import java.util.Map;

@Component
@Slf4j
/** Service used to change shiro filter and filterChains dynamically */
public class ShiroService {

    @Autowired
    private ShiroFilterFactoryBean shiroFilterFactoryBean;

    private DefaultFilterChainManager filterChainManager;
    private DefaultWebSecurityManager securityManager;

    @PostConstruct
    public void init() {
        AbstractShiroFilter shiroFilter;
        try {
            shiroFilter = shiroFilterFactoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException("Fail to get ShiroFilter from shiroFilterFactoryBean!");
        }
        securityManager = (DefaultWebSecurityManager) shiroFilter.getSecurityManager();
        PathMatchingFilterChainResolver filterChainResolver =
                (PathMatchingFilterChainResolver) shiroFilter.getFilterChainResolver();
        filterChainManager = (DefaultFilterChainManager) filterChainResolver.getFilterChainManager();
    }

    public void addRealm(Realm realm) {
        synchronized (this) {
            securityManager.getRealms().add(realm);
        }
    }

    public void addFilters(Map<String, Filter> filters) {
        synchronized (this) {
            filters.forEach(
                    (key, value) -> {
                        // The new filter can be appended after the existing map.
                        // As the sequence doesn't matter.
                        shiroFilterFactoryBean.getFilters().put(key, value);
                        filterChainManager.addFilter(key, value);
                    });
        }
    }

    public void addFilterChains(Map<String, String> filterChainDefinitionMap) {
        synchronized (this) {
            // Append the new filterchain onto the head of the current filterChainDefinitionMap.
            // To prevent being captured by filter chain ("/**", "jwt").
            filterChainDefinitionMap.putAll(shiroFilterFactoryBean.getFilterChainDefinitionMap());
            // Remove and re-insert the filterChainDefinitionMap
            filterChainManager.getFilterChains().clear();
            shiroFilterFactoryBean.getFilterChainDefinitionMap().clear();
            // Reset the filterChainDefinitionMap
            shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
            filterChainDefinitionMap.forEach(
                    (key, value) -> {
                        String chainDefinition = value.trim();
                        filterChainManager.createChain(key, chainDefinition);
                    });
        }
    }
}
