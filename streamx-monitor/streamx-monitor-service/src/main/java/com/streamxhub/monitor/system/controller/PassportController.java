package com.streamxhub.monitor.system.controller;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.monitor.base.annotation.Limit;
import com.streamxhub.monitor.base.domain.ActiveUser;
import com.streamxhub.monitor.base.domain.Constant;
import com.streamxhub.monitor.base.domain.RestResponse;
import com.streamxhub.monitor.base.exception.AdminXException;
import com.streamxhub.monitor.base.properties.AdminXProperties;
import com.streamxhub.monitor.base.utils.*;
import com.streamxhub.monitor.system.authentication.JWTToken;
import com.streamxhub.monitor.system.authentication.JWTUtil;
import com.streamxhub.monitor.system.entity.LoginLog;
import com.streamxhub.monitor.system.entity.User;
import com.streamxhub.monitor.system.entity.UserConfig;
import com.streamxhub.monitor.system.manager.UserManager;
import com.streamxhub.monitor.system.service.LoginLogService;
import com.streamxhub.monitor.system.service.RedisService;
import com.streamxhub.monitor.system.service.UserService;
import com.streamxhub.monitor.base.utils.*;
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
    private RedisService redisService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private UserService userService;
    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private AdminXProperties properties;

    @Autowired
    private ObjectMapper mapper;

    @PostMapping("login")
    @Limit(key = "login", period = 60, count = 20, name = "登录接口", prefix = "limit")
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
        Set<String> userOnlineStringSet = redisService.zrangeByScore(Constant.ACTIVE_USERS_ZSET_PREFIX, now, "+inf");
        ActiveUser kickoutUser = null;
        String kickoutUserString = "";
        for (String userOnlineString : userOnlineStringSet) {
            ActiveUser activeUser = mapper.readValue(userOnlineString, ActiveUser.class);
            if (StringUtils.equals(activeUser.getId(), id)) {
                kickoutUser = activeUser;
                kickoutUserString = userOnlineString;
            }
        }
        if (kickoutUser != null && StringUtils.isNotBlank(kickoutUserString)) {
            // 删除 zset中的记录
            redisService.zrem(Constant.ACTIVE_USERS_ZSET_PREFIX, kickoutUserString);
            // 删除对应的 token缓存
            redisService.del(Constant.TOKEN_CACHE_PREFIX + kickoutUser.getToken() + "." + kickoutUser.getIp());
        }
    }


    private String saveTokenToRedis(User user, JWTToken token, HttpServletRequest request) throws Exception {
        String ip = IPUtil.getIpAddr(request);

        // 构建在线用户
        ActiveUser activeUser = new ActiveUser();
        activeUser.setUsername(user.getUsername());
        activeUser.setIp(ip);
        activeUser.setToken(token.getToken());
        activeUser.setLoginAddress(AddressUtil.getCityInfo(DbSearcher.BTREE_ALGORITHM, ip));

        // zset 存储登录用户，score 为过期时间戳
        this.redisService.zadd(Constant.ACTIVE_USERS_ZSET_PREFIX, Double.valueOf(token.getExpireAt()), mapper.writeValueAsString(activeUser));
        // redis 中存储这个加密 token，key = 前缀 + 加密 token + .ip
        this.redisService.set(Constant.TOKEN_CACHE_PREFIX + token.getToken() + StringPool.DOT + ip, token.getToken(), properties.getShiro().getJwtTimeOut() * 1000);

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
