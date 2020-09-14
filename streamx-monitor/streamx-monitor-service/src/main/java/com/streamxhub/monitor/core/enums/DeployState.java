package com.streamxhub.monitor.core.enums;

public enum DeployState {

    /**
     * 不需要重新发布
     */
    NONE(0),

    /**
     * 程序更新需要重新发布
     */
    APP_UPDATED(1),

    /**
     * 配置文件更新需要重新发布
     */
    CONF_UPDATED(2),

    /**
     * 程序发布完,需要重新启动.
     */
    NEED_START(3);

    int value;

    DeployState(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }
}
