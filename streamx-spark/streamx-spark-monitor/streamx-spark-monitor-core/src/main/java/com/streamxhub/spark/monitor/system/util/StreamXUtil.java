package com.streamxhub.spark.monitor.system.util;

import com.streamxhub.spark.monitor.common.utils.SpringContextUtil;
import com.streamxhub.spark.monitor.system.authentication.JWTUtil;
import com.streamxhub.spark.monitor.system.domain.User;
import com.streamxhub.spark.monitor.system.service.CacheService;
import com.streamxhub.spark.monitor.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;

import static com.streamxhub.spark.monitor.common.utils.StreamXUtil.selectCacheByTemplate;

/**
 * STREAMX工具类
 */
@Slf4j
public class StreamXUtil {

    /**
     * 获取当前操作用户
     *
     * @return 用户信息
     */
    public static User getCurrentUser() {
        String token = (String) SecurityUtils.getSubject().getPrincipal();
        String username = JWTUtil.getUsername(token);
        UserService userService = SpringContextUtil.getBean(UserService.class);
        CacheService cacheService = SpringContextUtil.getBean(CacheService.class);

        return selectCacheByTemplate(() -> cacheService.getUser(username), () -> userService.findByName(username));
    }


}
