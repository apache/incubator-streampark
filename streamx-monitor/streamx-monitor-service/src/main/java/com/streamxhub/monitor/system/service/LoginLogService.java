package com.streamxhub.monitor.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.monitor.system.entity.LoginLog;

public interface LoginLogService extends IService<LoginLog> {

    void saveLoginLog(LoginLog loginLog);
}
