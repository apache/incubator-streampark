package com.streamxhub.monitor.system.authentication;

import com.streamxhub.monitor.system.entity.User;
import com.streamxhub.monitor.system.manager.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServerUtil {


    @Autowired
    private UserManager userManager;

    public User getUser(){
        String token = (String) SecurityUtils.getSubject().getPrincipal();
        String username = JWTUtil.getUsername(token);
        User user = userManager.getUser(username);
        return user;
    }

}
