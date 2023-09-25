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

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

import java.util.LinkedHashMap;

@Configuration
public class ShiroConfig {

  private static final String ANON = "anon";

  private static final String JWT = "jwt";

  @Bean
  public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
    ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
    shiroFilterFactoryBean.setSecurityManager(securityManager);

    LinkedHashMap<String, Filter> filters = new LinkedHashMap<>();
    filters.put(JWT, new JWTFilter());
    shiroFilterFactoryBean.setFilters(filters);

    LinkedHashMap<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
    filterChainDefinitionMap.put("/actuator/**", ANON);

    filterChainDefinitionMap.put("/doc.html", ANON);
    filterChainDefinitionMap.put("/swagger-ui.html", ANON);
    filterChainDefinitionMap.put("/swagger-ui/**", ANON);
    filterChainDefinitionMap.put("/swagger-resources/**", ANON);
    filterChainDefinitionMap.put("/v3/api-docs/**", ANON);
    filterChainDefinitionMap.put("/webjars/**", ANON);

    filterChainDefinitionMap.put("/passport/**", ANON);
    filterChainDefinitionMap.put("/systemName", ANON);
    filterChainDefinitionMap.put("/member/teams", ANON);
    filterChainDefinitionMap.put("/user/check/**", ANON);
    filterChainDefinitionMap.put("/user/initTeam", ANON);
    filterChainDefinitionMap.put("/websocket/**", ANON);
    filterChainDefinitionMap.put("/metrics/**", ANON);

    filterChainDefinitionMap.put("/index.html", ANON);
    filterChainDefinitionMap.put("/assets/**", ANON);
    filterChainDefinitionMap.put("/resource/**/**", ANON);
    filterChainDefinitionMap.put("/css/**", ANON);
    filterChainDefinitionMap.put("/fonts/**", ANON);
    filterChainDefinitionMap.put("/img/**", ANON);
    filterChainDefinitionMap.put("/js/**", ANON);
    filterChainDefinitionMap.put("/loading/**", ANON);
    filterChainDefinitionMap.put("/*.js", ANON);
    filterChainDefinitionMap.put("/*.png", ANON);
    filterChainDefinitionMap.put("/*.jpg", ANON);
    filterChainDefinitionMap.put("/*.less", ANON);
    filterChainDefinitionMap.put("/*.ico", ANON);
    filterChainDefinitionMap.put("/", ANON);
    filterChainDefinitionMap.put("/**", JWT);

    shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

    return shiroFilterFactoryBean;
  }

  @Bean
  public SecurityManager securityManager() {
    DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
    securityManager.setRealm(shiroRealm());
    return securityManager;
  }

  @Bean
  public ShiroRealm shiroRealm() {
    return new ShiroRealm();
  }

  @Bean
  public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
      SecurityManager securityManager) {
    AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor =
        new AuthorizationAttributeSourceAdvisor();
    authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
    return authorizationAttributeSourceAdvisor;
  }
}
