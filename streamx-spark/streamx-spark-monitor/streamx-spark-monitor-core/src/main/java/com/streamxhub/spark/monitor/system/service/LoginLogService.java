package com.streamxhub.spark.monitor.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.spark.monitor.system.domain.LoginLog;

public interface LoginLogService extends IService<LoginLog> {

    void saveLoginLog(LoginLog loginLog);
}
