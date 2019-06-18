package com.streamxhub.spark.monitor.system.service;

import com.streamxhub.spark.monitor.system.domain.LoginLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LoginLogService extends IService<LoginLog> {

    void saveLoginLog(LoginLog loginLog);
}
