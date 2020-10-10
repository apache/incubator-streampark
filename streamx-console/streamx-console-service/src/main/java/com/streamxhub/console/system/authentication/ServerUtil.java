package com.streamxhub.console.system.authentication;

import com.streamxhub.console.system.entity.User;
import com.streamxhub.console.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServerUtil {


    @Autowired
    private UserService userService;

    public User getUser(){
        String token = (String) SecurityUtils.getSubject().getPrincipal();
        String username = JWTUtil.getUsername(token);
        User user = userService.findByName(username);
        return user;
    }

}
