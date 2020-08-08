package com.streamxhub.flink.monitor.core.dao;

import com.streamxhub.flink.monitor.core.entity.Project;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface ProjectMapper extends BaseMapper<Project> {
    IPage<Project> findProject(Page<Project> page,@Param("project") Project project);

    void failureBuild(@Param("project") Project project);

    void successBuild(@Param("project") Project project);

    void startBuild(@Param("project") Project project);
}
