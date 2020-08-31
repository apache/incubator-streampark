package com.streamxhub.monitor.system.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.monitor.system.entity.Dept;
import com.streamxhub.monitor.base.domain.RestRequest;

import java.util.List;
import java.util.Map;

public interface DeptService extends IService<Dept> {

    Map<String, Object> findDepts(RestRequest request, Dept dept);

    List<Dept> findDepts(Dept dept, RestRequest request);

    void createDept(Dept dept);

    void updateDept(Dept dept);

    void deleteDepts(String[] deptIds);
}
