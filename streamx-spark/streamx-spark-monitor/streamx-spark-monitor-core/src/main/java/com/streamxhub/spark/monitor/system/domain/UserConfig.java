package com.streamxhub.spark.monitor.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@TableName("t_user_config")
@Data
public class UserConfig {

    public static final String DEFAULT_THEME = "dark";
    public static final String DEFAULT_LAYOUT = "side";
    public static final String DEFAULT_MULTIPAGE = "0";
    public static final String DEFAULT_FIX_SIDERBAR = "1";
    public static final String DEFAULT_FIX_HEADER = "1";
    public static final String DEFAULT_COLOR = "rgb(66, 185, 131)";

    /**
     * 用户 ID
     */
    @TableId(value = "USER_ID")
    private Long userId;

    /**
     * 系统主题 dark暗色风格，light明亮风格
     */
    private String theme;

    /**
     * 系统布局 side侧边栏，head顶部栏
     */
    private String layout;

    /**
     * 页面风格 1多标签页 0单页
     */
    private String multiPage;

    /**
     * 页面滚动是否固定侧边栏 1固定 0不固定
     */
    private String fixSiderbar;

    /**
     * 页面滚动是否固定顶栏 1固定 0不固定
     */
    private String fixHeader;

    /**
     * 主题颜色 RGB值
     */
    private String color;

}
