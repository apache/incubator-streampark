package com.streamxhub.spark.monitor.system.service.impl;

import com.streamxhub.spark.monitor.common.utils.AddressUtil;
import com.streamxhub.spark.monitor.common.utils.HttpContextUtil;
import com.streamxhub.spark.monitor.common.utils.IPUtil;
import com.streamxhub.spark.monitor.system.dao.LoginLogMapper;
import com.streamxhub.spark.monitor.system.domain.LoginLog;
import com.streamxhub.spark.monitor.system.service.LoginLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lionsoul.ip2region.DbSearcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Service("loginLogService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    @Override
    @Transactional
    public void saveLoginLog(LoginLog loginLog) {
        loginLog.setLoginTime(new Date());
        HttpServletRequest request = HttpContextUtil.getHttpServletRequest();
        String ip = IPUtil.getIpAddr(request);
        loginLog.setIp(ip);
        loginLog.setLocation(AddressUtil.getCityInfo(DbSearcher.BTREE_ALGORITHM, ip));
        this.save(loginLog);
    }
}
