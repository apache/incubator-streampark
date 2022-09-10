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

import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.AccessTokenService;
import org.apache.streampark.console.system.service.RoleService;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

/**
 * Implementation of ShiroRealm, including two modules: authentication and authorization
 */
public class ShiroRealm extends AuthorizingRealm {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AccessTokenService accessTokenService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JWTToken;
    }

    /**
     * ` 授权模块，获取用户角色和权限
     *
     * @param token token
     * @return AuthorizationInfo 权限信息
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection token) {
        String username = JWTUtil.getUsername(token.toString());

        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();

        // 获取用户角色集
        Set<String> roleSet = roleService.getUserRoleName(username);
        simpleAuthorizationInfo.setRoles(roleSet);

        // 获取用户权限集
        Set<String> permissionSet = userService.getPermissions(username);
        simpleAuthorizationInfo.setStringPermissions(permissionSet);
        return simpleAuthorizationInfo;
    }

    /**
     * 用户认证
     *
     * @param authenticationToken 身份认证 token
     * @return AuthenticationInfo 身份认证信息
     * @throws AuthenticationException 认证相关异常
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        // 这里的 token是从 JWTFilter 的 executeLogin 方法传递过来的，已经经过了解密
        String token = (String) authenticationToken.getCredentials();
        String username = JWTUtil.getUsername(token);
        if (StringUtils.isBlank(username)) {
            throw new AuthenticationException("Token verification failed");
        }
        // 通过用户名查询用户信息
        User user = userService.findByName(username);

        if (user == null) {
            throw new AuthenticationException("ERROR Incorrect username or password!");
        }

        if (!JWTUtil.verify(token, username, user.getPassword())) {
            //校验是否属于api的token，权限是否有效
            String tokenDb = WebUtils.encryptToken(token);
            boolean effective = accessTokenService.checkTokenEffective(user.getUserId(), tokenDb);
            if (!effective) {
                throw new AuthenticationException("Token checked failed: 1-[Browser Request] please check the username or password; 2-[Api Request] please check the user status or accessToken status");
            }
            SecurityUtils.getSubject().getSession().setAttribute(AccessToken.IS_API_TOKEN, true);
        }
        return new SimpleAuthenticationInfo(token, token, "streampark_shiro_realm");
    }
}
