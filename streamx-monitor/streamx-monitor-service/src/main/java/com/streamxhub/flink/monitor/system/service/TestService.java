package com.streamxhub.flink.monitor.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.flink.monitor.system.entity.Test;

import java.util.List;

public interface TestService extends IService<Test> {

    List<Test> findTests();

    /**
     * 批量插入
     *
     * @param list List<Test>
     */
    void batchInsert(List<Test> list);
}
