package com.streamxhub.flink.monitor.core.service.impl;


import com.streamxhub.common.conf.ParameterCli;
import com.streamxhub.common.util.YarnUtils;
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
import com.streamxhub.flink.monitor.core.enums.AppState;
import com.streamxhub.flink.monitor.system.authentication.ServerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service("applicationService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ServerUtil serverUtil;

    @Override
    public IPage<Application> list(Application app, RestRequest request) {
        Page<Application> page = new Page<>();
        SortUtil.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        return this.baseMapper.findApplication(page, app);
    }

    @Override
    public String getYarnName(Application app) {
        String[] args = new String[2];
        args[0] = "--name";
        args[1] = app.getConfigFile();
        return ParameterCli.read(args);
    }

    @Override
    public boolean checkExists(Application app) {
        return YarnUtils.isContains(app.getAppName());
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
        //配置文件中配置的yarnName..
        String yarnName = this.getYarnName(app);
        app.setYarnName(yarnName);
        app.setUserId(serverUtil.getUser().getUserId());
        app.setState(AppState.CREATED.getValue());
        app.setCreateTime(new Date());
        return save(app);
    }

    @Override
    public boolean startUp(String id) {
        final Application application = getById(id);
        assert application != null;
        Project project = projectService.getById(application.getProjectId());
        assert project != null;
        //deploying...
        application.setState(AppState.DEPLOYING.getValue());
        updateById(application);

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
        this.getAppId(application);
        return true;
    }

    /**
     * 2秒钟从yarn里获取一次当前任务的appId,总共获取10次,如10次都未获取到则获取失败.
     */
    private void getAppId(Application application) {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("Flink-GetAppId-%d").daemon(true).build());
        executorService.submit(() -> {
            int index = 0;
            Long lastTime = 0L;
            while (index <= 10) {
                Long now = System.currentTimeMillis();
                if (lastTime == 0 || (now - lastTime) >= 2000) {
                    lastTime = now;
                    index++;
                    List<ApplicationId> idList = YarnUtils.getAppId(application.getYarnName());
                    if (!idList.isEmpty()) {
                        if (idList.size() == 1) {
                            ApplicationId applicationId = idList.get(0);
                            application.setAppId(applicationId.toString());
                        } else {
                            //表示有多个重复的任务.
                            application.setState(5);
                        }
                        updateById(application);
                        break;
                    }
                }
            }
        });
    }


}
