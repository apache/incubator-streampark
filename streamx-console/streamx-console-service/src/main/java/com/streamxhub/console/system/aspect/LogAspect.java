package com.streamxhub.console.system.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.streamxhub.console.base.properties.StreamXProperties;
import com.streamxhub.console.base.utils.HttpContextUtil;
import com.streamxhub.console.base.utils.IPUtil;
import com.streamxhub.console.system.authentication.JWTUtil;
import com.streamxhub.console.system.entity.SysLog;
import com.streamxhub.console.system.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * AOP 记录用户操作日志
 *
 **/
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private StreamXProperties streamXProperties;

    @Autowired
    private LogService logService;

    @Pointcut("@annotation(com.streamxhub.console.base.annotation.Log)")
    public void pointcut() {
        // do nothing
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint point) throws JsonProcessingException {
        Object result = null;
        long beginTime = System.currentTimeMillis();
        try {
            // 执行方法
            result = point.proceed();
        } catch (Throwable e) {
            log.info(e.getMessage());
        }
        // 获取 request
        HttpServletRequest request = HttpContextUtil.getHttpServletRequest();
        // 设置 IP 地址
        String ip = IPUtil.getIpAddr(request);
        // 执行时长(毫秒)
        long time = System.currentTimeMillis() - beginTime;

        if (streamXProperties.isOpenAopLog()) {
            // 保存日志
            String token = (String) SecurityUtils.getSubject().getPrincipal();
            String username = JWTUtil.getUsername(token);

            SysLog log = new SysLog();
            log.setUsername(username);
            log.setIp(ip);
            log.setTime(time);
            logService.saveLog(point, log);
        }
        return result;
    }
}
