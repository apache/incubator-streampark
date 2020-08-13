package com.streamxhub.flink.monitor.core.enums;

public enum AppExistsState {

    /**
     * 不存在
     */
    NO(0),

    /**
     * 表里存在
     */
    IN_DB(1),

    /**
     * 正在运行的yarn里存在
     */
    IN_YARN(2);

    int value;

    AppExistsState(int value) {
        this.value = value;
    }

}
