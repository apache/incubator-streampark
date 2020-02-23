package com.streamxhub.flink.monitor.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.flink.monitor.base.utils.*;
import com.streamxhub.flink.monitor.base.utils.DateUtil;
import com.streamxhub.flink.monitor.system.dao.LoginLogMapper;
import com.streamxhub.flink.monitor.system.entity.User;
import com.streamxhub.flink.monitor.system.service.RedisService;
import com.streamxhub.flink.monitor.base.domain.ActiveUser;
import com.streamxhub.flink.monitor.base.domain.Constant;
import com.streamxhub.flink.monitor.base.domain.RestResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author benjobs
 */
@Validated
@RestController
public class VisitController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("index/{username}")
    public RestResponse index(@NotBlank(message = "{required}") @PathVariable String username) {
        Map<String, Object> data = new HashMap<>();
        // 获取系统访问记录
        Long totalVisitCount = loginLogMapper.findTotalVisitCount();
        data.put("totalVisitCount", totalVisitCount);
        Long todayVisitCount = loginLogMapper.findTodayVisitCount();
        data.put("todayVisitCount", todayVisitCount);
        Long todayIp = loginLogMapper.findTodayIp();
        data.put("todayIp", todayIp);
        // 获取近期系统访问记录
        List<Map<String, Object>> lastSevenVisitCount = loginLogMapper.findLastSevenDaysVisitCount(null);
        data.put("lastSevenVisitCount", lastSevenVisitCount);
        User param = new User();
        param.setUsername(username);
        List<Map<String, Object>> lastSevenUserVisitCount = loginLogMapper.findLastSevenDaysVisitCount(param);
        data.put("lastSevenUserVisitCount", lastSevenUserVisitCount);
        return new RestResponse().data(data);
    }

    @RequiresPermissions("user:online")
    @GetMapping("online")
    public RestResponse userOnline(String username) throws Exception {
        String now = DateUtil.formatFullTime(LocalDateTime.now());
        Set<String> userOnlineStringSet = redisService.zrangeByScore(Constant.ACTIVE_USERS_ZSET_PREFIX, now, "+inf");
        List<ActiveUser> activeUsers = new ArrayList<>();
        for (String userOnlineString : userOnlineStringSet) {
            ActiveUser activeUser = mapper.readValue(userOnlineString, ActiveUser.class);
            activeUser.setToken(null);
            if (StringUtils.isNotBlank(username)) {
                if (StringUtils.equalsIgnoreCase(username, activeUser.getUsername())) {
                    activeUsers.add(activeUser);
                }
            } else {
                activeUsers.add(activeUser);
            }
        }
        return new RestResponse().data(activeUsers);
    }

}
