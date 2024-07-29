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

import io.buji.pac4j.filter.CallbackFilter;
import io.buji.pac4j.filter.LogoutFilter;
import io.buji.pac4j.filter.SecurityFilter;
import io.buji.pac4j.realm.Pac4jRealm;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.core.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

import java.net.URI;
import java.util.LinkedHashMap;

@Component
@Configuration
@Slf4j
/** Plugin for {@link ShiroConfig.java} to load SSO config if enabled */
public class SsoShiroPlugin {

    @Autowired
    private Config ssoConfig;

    @Autowired
    private ShiroService shiroService;

    @Value("${sso.enable:#{false}}")
    private Boolean ssoEnable;

    @PostConstruct
    public void init() {
        // Make sso controller anon if it's not enabled
        if (!ssoEnable) {
            LinkedHashMap<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
            filterChainDefinitionMap.put("/sso/signin", "anon");
            filterChainDefinitionMap.put("/sso/token", "anon");
            shiroService.addFilterChains(filterChainDefinitionMap);
            return;
        }

        // Add Pac4jRealm into shiro
        shiroService.addRealm(new Pac4jRealm());

        // Construct the shiro filter for SSO
        constructShiroFilterForSSO();

        // Construct the filterChainDefinitionMap for SSO
        LinkedHashMap<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        filterChainDefinitionMap.put("/sso/signin", "ssoSecurityFilter");
        filterChainDefinitionMap.put("/sso/token", "ssoSecurityFilter");
        filterChainDefinitionMap.put("/pac4jLogout", "ssoLogoutFilter");
        // Get callback endpoint from callbackUrl
        String callbackEndpoint = URI.create(ssoConfig.getClients().getCallbackUrl()).getPath();
        filterChainDefinitionMap.put(callbackEndpoint, "ssoCallbackFilter");
        shiroService.addFilterChains(filterChainDefinitionMap);
    }

    private void constructShiroFilterForSSO() {
        SecurityFilter securityFilter = new SecurityFilter();
        CallbackFilter callbackFilter = new CallbackFilter();
        LogoutFilter logoutFilter = new LogoutFilter();
        securityFilter.setConfig(ssoConfig);
        callbackFilter.setConfig(ssoConfig);
        logoutFilter.setConfig(ssoConfig);
        logoutFilter.setDefaultUrl("/?defaulturlafterlogout");
        LinkedHashMap<String, Filter> filters = new LinkedHashMap<>();
        filters.put("ssoSecurityFilter", securityFilter);
        filters.put("ssoCallbackFilter", callbackFilter);
        filters.put("ssoLogoutFilter", logoutFilter);
        shiroService.addFilters(filters);
    }
}
