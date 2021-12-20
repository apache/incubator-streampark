package com.streamxhub.streamx.console.core.service;

import java.util.List;

/**
 * @author Whojohn
 * @time 2021.12.20
 */
public interface SqlComplete {
    /**
     * 功能：
     * 1. 传入一个完整 sql 语句(推荐传入截断的字符)，只对最后一词联想
     * 2. 最后一个词的定义是非空格字符
     *
     * @param sql 输入一个需要联想的 sql
     * @return 返回一个潜在词列表
     */
    public List<String> getComplete(String sql);
}
