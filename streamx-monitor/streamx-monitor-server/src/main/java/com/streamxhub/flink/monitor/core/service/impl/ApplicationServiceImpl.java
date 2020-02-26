package com.streamxhub.flink.monitor.core.service.impl;


import com.streamxhub.flink.monitor.base.domain.Constant;
import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.utils.SortUtil;
import com.streamxhub.flink.monitor.core.dao.ApplicationMapper;
import com.streamxhub.flink.monitor.core.entity.Application;
import com.streamxhub.flink.monitor.core.entity.Deployment;
import com.streamxhub.flink.monitor.core.entity.Project;
import com.streamxhub.flink.monitor.core.service.ApplicationService;
import com.streamxhub.flink.monitor.core.service.ProjectService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service("applicationService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Autowired
    private ProjectService projectService;

    @Override
    public IPage<Application> list(Application app, RestRequest request) {
        Page<Application> page = new Page<>();
        SortUtil.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        return this.baseMapper.findApplication(page, app);
    }

    @Override
    public boolean create(Application app) {
        if (app.getConfig() != null && app.getConfig().trim().length() > 0) {
            try {
                String config = URLDecoder.decode(app.getConfig(), "UTF-8");
                app.setConfig(config);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        app.setCreateTime(new Date());
        return save(app);
    }

    @Override
    public boolean startUp(String id) {
        Application application = getById(id);
        assert application != null;
        Project project = projectService.getById(application.getProjectId());
        assert project != null;

        JSONObject jsonConf = JSON.parseObject(application.getConfig());
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> objectEntry : jsonConf.entrySet()) {
            String k = objectEntry.getKey();
            Object v = objectEntry.getValue();
            builder.append("--").append(k).append(" ");
            if (!(v instanceof Boolean)) {
                builder.append(v).append(" ");
            }
        }
        String args = builder.toString().trim();
        Deployment deployment = new Deployment(project, application);
        deployment.setArgs(args);
        deployment.startUp();
        return true;
    }

}
