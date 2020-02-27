package com.streamxhub.flink.monitor.core.enums;

import lombok.Getter;

@Getter
public enum AppState {
    CREATED(0, "新建"),
    RUNNING(1, "运行中"),
    STOP(2, "停止"),
    ERROR(3, "错误");

    int value;
    String desc;
    
    AppState(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
