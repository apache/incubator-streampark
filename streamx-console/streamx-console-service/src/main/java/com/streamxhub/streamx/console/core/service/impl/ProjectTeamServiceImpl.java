package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.core.dao.ProjectTeamMapper;
import com.streamxhub.streamx.console.core.entity.ProjectTeam;
import com.streamxhub.streamx.console.core.service.ProjectTeamService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Map;

public class ProjectTeamServiceImpl extends ServiceImpl<ProjectTeamMapper, ProjectTeam>  implements ProjectTeamService {


    @Override
    public Map<String, Object> createProject(String name, String desc) {
        return null;
    }

    @Override
    public Map<String, Object> queryByCode(long projectCode) {
        return null;
    }

    @Override
    public Map<String, Object> queryByName(String projectName) {
        return null;
    }

    @Override
    public Map<String, Object> queryProjectListPaging(Integer pageSize, Integer pageNo, String searchVal) {
        return null;
    }

    @Override
    public Map<String, Object> deleteProject(Long projectCode) {
        return null;
    }

    @Override
    public Map<String, Object> update(Long projectCode, String projectName, String desc, String userName) {
        return null;
    }

    @Override
    public Map<String, Object> queryAllProjectList() {
        return null;
    }
}
