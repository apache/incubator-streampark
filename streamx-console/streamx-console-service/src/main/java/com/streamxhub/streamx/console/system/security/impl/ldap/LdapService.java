/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.system.security.impl.ldap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
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
    @Value("${ldap.urls:null}")
    private String ldapUrls;

    @Value("${ldap.embedded.base-dn:null}")
    private String ldapBaseDn;

    @Value("${ldap.username:null}")
    private String ldapSecurityPrincipal;

    @Value("${ldap.password:null}")
    private String ldapPrincipalPassword;

    @Value("${ldap.user.identity.attribute:null}")
    private String ldapUserIdentifyingAttribute;

    @Value("${ldap.user.email.attribute:null}")
    private String ldapEmailAttribute;

    /**
     * login by userId and return user email
     *
     * @param userId  user identity id
     * @param userPwd user login password
     * @return user email
     */
    public String ldapLogin(String userId, String userPwd) {
        Properties searchEnv = getManagerLdapEnv();
        try {
            //Connect to the LDAP server and Authenticate with a service user of whom we know the DN and credentials
            LdapContext ctx = new InitialLdapContext(searchEnv, null);
            SearchControls sc = new SearchControls();
            sc.setReturningAttributes(new String[]{ldapEmailAttribute});
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String searchFilter = String.format("(%s=%s)", ldapUserIdentifyingAttribute, userId);

            //Search for the user you want to authenticate, search him with some attribute
            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDn, searchFilter, sc);
            // NamingEnumeration answer = ctx.search(usersContainer, "     (objectclass=group)", ctls);
            if (results.hasMore()) {
                // get the users DN (distinguishedName) from the result
                SearchResult result = results.next();
                NamingEnumeration attrs = result.getAttributes().getAll();
                while (attrs.hasMore()) {
                    //Open another connection to the LDAP server with the found DN and the password
                    searchEnv.put(Context.SECURITY_PRINCIPAL, result.getNameInNamespace());
                    searchEnv.put(Context.SECURITY_CREDENTIALS, userPwd);
                    try {
                        new InitialDirContext(searchEnv);
                    } catch (Exception e) {
                        log.warn("invalid ldap credentials or ldap search error", e);
                        return null;
                    }
                    Attribute attr = (Attribute) attrs.next();
                    if (attr.getID().equals(ldapEmailAttribute)) {
                        return (String) attr.get();
                    }
                }
            }
        } catch (NamingException e) {
            log.error("ldap search error", e);
            return null;
        }
        return null;
    }

    /***
     * get ldap env fot ldap server search
     * @return Properties
     */
    Properties getManagerLdapEnv() {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldapSecurityPrincipal);
        env.put(Context.SECURITY_CREDENTIALS, ldapPrincipalPassword);
        env.put(Context.PROVIDER_URL, ldapUrls);
        return env;
    }
}
