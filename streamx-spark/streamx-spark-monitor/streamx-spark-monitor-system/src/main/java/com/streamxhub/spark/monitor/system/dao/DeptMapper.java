package com.streamxhub.spark.monitor.system.dao;

import com.streamxhub.spark.monitor.system.domain.Dept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 递归删除部门
     *
     * @param deptId deptId
     */
    void deleteDepts(String deptId);
}
