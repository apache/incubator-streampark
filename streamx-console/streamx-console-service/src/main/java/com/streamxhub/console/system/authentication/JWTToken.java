package com.streamxhub.console.system.authentication;

import org.apache.shiro.authc.AuthenticationToken;

import lombok.Data;

/**
 * JSON Web Token
 *
 * @author benjobs
 */
@Data
public class JWTToken implements AuthenticationToken {

    private static final long serialVersionUID = 1282057025599826155L;

    private String token;

    private String expireAt;

    public JWTToken(String token) {
        this.token = token;
    }

    public JWTToken(String token, String expireAt) {
        this.token = token;
        this.expireAt = expireAt;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
