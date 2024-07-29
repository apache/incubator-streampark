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

package org.apache.streampark.console.system.security.impl;

import org.apache.streampark.console.base.exception.ApiAlertException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import java.util.Properties;

@Component
@Configuration
@Slf4j
public class LdapService {

    @Value("${ldap.enable:#{false}}")
    private Boolean enable;

    @Value("${ldap.urls:#{null}}")
    private String ldapUrls;

    @Value("${ldap.base-dn:#{null}}")
    private String ldapBaseDn;

    @Value("${ldap.username:#{null}}")
    private String ldapSecurityPrincipal;

    @Value("${ldap.password:#{null}}")
    private String ldapPrincipalPassword;

    @Value("${ldap.user.identity-attribute:#{null}}")
    private String ldapUserIdentifyingAttribute;

    @Value("${ldap.user.email-attribute:#{null}}")
    private String ldapEmailAttribute;

    private Properties ldapEnv = null;

    /**
     * login by userId and return user email
     *
     * @param userId user identity id
     * @param userPwd user login password
     * @return boolean ldapLoginStatus
     */
    public boolean ldapLogin(String userId, String userPwd) {
        ApiAlertException.throwIfFalse(
            enable, "ldap is not enabled, Please check the configuration: ldap.enable");
        renderLdapEnv();
        try {
            NamingEnumeration<SearchResult> results = getSearchResults(userId);
            if (!results.hasMore()) {
                return false;
            }
            SearchResult result = results.next();
            NamingEnumeration<? extends Attribute> attrs = result.getAttributes().getAll();
            while (attrs.hasMore()) {
                ldapEnv.put(Context.SECURITY_PRINCIPAL, result.getNameInNamespace());
                ldapEnv.put(Context.SECURITY_CREDENTIALS, userPwd);
                try {
                    new InitialDirContext(ldapEnv);
                } catch (Exception e) {
                    log.warn("invalid ldap credentials or ldap search error", e);
                    return false;
                }
                Attribute attr = attrs.next();
                if (attr.getID().equals(ldapUserIdentifyingAttribute)) {
                    return true;
                }
            }
        } catch (NamingException e) {
            log.error("ldap search error", e);
        }
        return false;
    }

    private NamingEnumeration<SearchResult> getSearchResults(String userId) throws NamingException {
        LdapContext ctx = new InitialLdapContext(ldapEnv, null);
        SearchControls sc = new SearchControls();
        sc.setReturningAttributes(new String[]{ldapUserIdentifyingAttribute});
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        EqualsFilter filter = new EqualsFilter(ldapUserIdentifyingAttribute, userId);
        NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDn, filter.toString(), sc);
        return results;
    }

    private void renderLdapEnv() {
        if (ldapEnv == null) {
            ldapEnv = new Properties();
            ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            ldapEnv.put(Context.PROVIDER_URL, ldapUrls);
        }

        ldapEnv.put(Context.SECURITY_PRINCIPAL, ldapSecurityPrincipal);
        ldapEnv.put(Context.SECURITY_CREDENTIALS, ldapPrincipalPassword);
    }
}
