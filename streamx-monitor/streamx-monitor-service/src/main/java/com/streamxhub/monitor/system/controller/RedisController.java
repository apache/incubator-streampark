package com.streamxhub.monitor.system.controller;

import com.streamxhub.monitor.base.domain.RedisInfo;
import com.streamxhub.monitor.base.domain.RestResponse;
import com.streamxhub.monitor.system.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
@RestController
@RequestMapping("redis")
public class RedisController {

    @Autowired
    private RedisService redisService;

    @PostMapping("info")
    public RestResponse getRedisInfo() throws Exception {
        List<RedisInfo> infoList = this.redisService.getRedisInfo();
        return new RestResponse().data(infoList);
    }

    @PostMapping("keysSize")
    public Map<String, Object> getKeysSize() throws Exception {
        return redisService.getKeysSize();
    }

    @PostMapping("memoryInfo")
    public Map<String, Object> getMemoryInfo() throws Exception {
        return redisService.getMemoryInfo();
    }
}
