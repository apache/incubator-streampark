package com.streamxhub.console.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.console.base.domain.ActiveUser;
import com.streamxhub.console.base.domain.RestResponse;
import com.streamxhub.console.base.exception.AdminXException;
import com.streamxhub.console.base.properties.StreamXProperties;
import com.streamxhub.console.base.utils.*;
import com.streamxhub.console.system.authentication.JWTToken;
import com.streamxhub.console.system.authentication.JWTUtil;
import com.streamxhub.console.system.entity.LoginLog;
import com.streamxhub.console.system.entity.User;
import com.streamxhub.console.system.entity.UserConfig;
import com.streamxhub.console.system.manager.UserManager;
import com.streamxhub.console.system.service.LoginLogService;
import com.streamxhub.console.system.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.lionsoul.ip2region.DbSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author benjobs
 */
@Validated
@RestController
@RequestMapping("passport")
public class PassportController {

    @Autowired
    private UserManager userManager;
    @Autowired
    private UserService userService;
    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private StreamXProperties properties;

    @PostMapping("login")
    public RestResponse login(
            @NotBlank(message = "{required}") String username,
            @NotBlank(message = "{required}") String password,
            HttpServletRequest request) throws Exception {
        username = StringUtils.lowerCase(username);

        final String errorMessage = "用户名或密码错误";
        User user = this.userManager.getUser(username);

        if (user == null) {
            throw new AdminXException(errorMessage);
        }

        String salt = user.getSalt();
        password = ShaHashUtil.encrypt(salt, password);

        if (!StringUtils.equals(user.getPassword(), password)) {
            throw new AdminXException(errorMessage);
        }
        if (User.STATUS_LOCK.equals(user.getStatus())) {
            throw new AdminXException("账号已被锁定,请联系管理员！");
        }

        // 更新用户登录时间
        this.userService.updateLoginTime(username);
        // 保存登录记录
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        this.loginLogService.saveLoginLog(loginLog);

        String token = WebUtil.encryptToken(JWTUtil.sign(username, password));
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(properties.getShiro().getJwtTimeOut());
        String expireTimeStr = DateUtil.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr);

        String userId = this.saveTokenToRedis(user, jwtToken, request);
        user.setId(userId);

        Map<String, Object> userInfo = this.generateUserInfo(jwtToken, user);
        return new RestResponse().message("认证成功").data(userInfo);
    }

    @PostMapping("logout")
    public RestResponse logout() throws Exception {
        return new RestResponse().message("退出成功");
    }

    @DeleteMapping("kickout")
    @RequiresPermissions("user:kickout")
    public void kickout(@NotBlank(message = "{required}") String id) throws Exception {
        String now = DateUtil.formatFullTime(LocalDateTime.now());
    }


    private String saveTokenToRedis(User user, JWTToken token, HttpServletRequest request) throws Exception {
        String ip = IPUtil.getIpAddr(request);

        // 构建在线用户
        ActiveUser activeUser = new ActiveUser();
        activeUser.setUsername(user.getUsername());
        activeUser.setIp(ip);
        activeUser.setToken(token.getToken());
        activeUser.setLoginAddress(AddressUtil.getCityInfo(DbSearcher.BTREE_ALGORITHM, ip));
        return activeUser.getId();
    }

    /**
     * 生成前端需要的用户信息，包括：
     * 1. token
     * 2. Vue Router
     * 3. 用户角色
     * 4. 用户权限
     * 5. 前端系统个性化配置信息
     *
     * @param token token
     * @param user  用户信息
     * @return UserInfo
     */
    private Map<String, Object> generateUserInfo(JWTToken token, User user) {
        String username = user.getUsername();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("token", token.getToken());
        userInfo.put("expire", token.getExpireAt());

        Set<String> roles = this.userManager.getUserRoles(username);
        userInfo.put("roles", roles);

        Set<String> permissions = this.userManager.getUserPermissions(username);
        userInfo.put("permissions", permissions);

        UserConfig userConfig = this.userManager.getUserConfig(String.valueOf(user.getUserId()));
        userInfo.put("config", userConfig);

        user.setPassword("******");
        userInfo.put("user", user);
        return userInfo;
    }
}
