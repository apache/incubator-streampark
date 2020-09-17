package com.streamxhub.console.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.system.entity.LoginLog;

public interface LoginLogService extends IService<LoginLog> {

    void saveLoginLog(LoginLog loginLog);
}
