package com.streamxhub.flink.monitor.base.utils;

import com.streamxhub.flink.monitor.base.function.CacheSelector;
import com.streamxhub.flink.monitor.base.domain.Constant;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.streamxhub.flink.monitor.system.authentication.JWTUtil;
import com.streamxhub.flink.monitor.system.entity.User;
import com.streamxhub.flink.monitor.system.service.CacheService;
import com.streamxhub.flink.monitor.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * WEBX工具类
 */
@Slf4j
public class AdminXUtil {

    public static User getCurrentUser() {
        String token = (String) SecurityUtils.getSubject().getPrincipal();
        String username = JWTUtil.getUsername(token);
        UserService userService = SpringContextUtil.getBean(UserService.class);
        CacheService cacheService = SpringContextUtil.getBean(CacheService.class);
        return selectCacheByTemplate(() -> cacheService.getUser(username), () -> userService.findByName(username));
    }

    /**
     * 缓存查询摸板，先查缓存，如果缓存查询失败再从数据库查询
     *
     * @param cacheSelector    查询缓存的方法
     * @param databaseSelector 数据库查询方法
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T selectCacheByTemplate(CacheSelector<?> cacheSelector, Supplier<?> databaseSelector) {
        try {
            // 先查 Redis缓存
            log.debug("query data from redis ······");
            return (T) cacheSelector.select();
        } catch (Exception e) {
            // 数据库查询
            log.info("redis error：", e);
            log.debug("query data from database ······");
            return (T) databaseSelector.get();
        }
    }


    /**
     * token 加密
     *
     * @param token token
     * @return 加密后的 token
     */
    public static String encryptToken(String token) {
        try {
            EncryptUtil encryptUtil = new EncryptUtil(Constant.TOKEN_CACHE_PREFIX);
            return encryptUtil.encrypt(token);
        } catch (Exception e) {
            log.info("token加密失败：", e);
            return null;
        }
    }

    /**
     * token 解密
     *
     * @param encryptToken 加密后的 token
     * @return 解密后的 token
     */
    public static String decryptToken(String encryptToken) {
        try {
            EncryptUtil encryptUtil = new EncryptUtil(Constant.TOKEN_CACHE_PREFIX);
            return encryptUtil.decrypt(encryptToken);
        } catch (Exception e) {
            log.info("token解密失败：", e);
            return null;
        }
    }

    /**
     * 驼峰转下划线
     *
     * @param value 待转换值
     * @return 结果
     */
    public static String camelToUnderscore(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String[] arr = StringUtils.splitByCharacterTypeCamelCase(value);
        if (arr.length == 0) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        IntStream.range(0, arr.length).forEach(i -> {
            if (i != arr.length - 1) {
                result.append(arr[i]).append(StringPool.UNDERSCORE);
            } else {
                result.append(arr[i]);
            }
        });
        return StringUtils.lowerCase(result.toString());
    }

    public static Map<String, String> getRequestMap(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>(0);
        request.getParameterMap().forEach((k, v) -> {
            map.put(k, v[0]);
        });
        return map;
    }
}
