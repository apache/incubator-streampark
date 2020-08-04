package com.streamxhub.flink.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.streamxhub.flink.monitor.base.domain.Constant;
import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.domain.RestResponse;
import com.streamxhub.flink.monitor.base.properties.StreamXProperties;
import com.streamxhub.flink.monitor.base.utils.SortUtil;
import com.streamxhub.flink.monitor.core.dao.ProjectMapper;
import com.streamxhub.flink.monitor.core.entity.Project;
import com.streamxhub.flink.monitor.core.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

@Slf4j
@Service("projectService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Resource
    private StreamXProperties streamXProperties;

    @Override
    public RestResponse create(Project project) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Project::getName, project.getName());
        int saved = count(queryWrapper);
        RestResponse response = RestResponse.create();
        if (saved == 0) {
            project.setDate(new Date());
            project.setProtocol(project.getUrl().split(":")[0]);
            boolean status = save(project);
            if (status) {
                return response.message("添加任务成功");
            } else {
                return response.message("添加任务失败");
            }
        } else {
            return response.message("该名称的项目已存在,添加任务失败");
        }
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public IPage<Project> page(Project project, RestRequest request) {
        Page<Project> page = new Page<>();
        SortUtil.handlePageSort(request, page, "date", Constant.ORDER_DESC, false);
        return this.baseMapper.findProject(page, project);
    }

    private void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public List<Map<String, Object>> filelist(String id) {
       /* Project project = getById(id);
        if (project == null) {
            return null;
        }
        File file = new File(path);
        List<Map<String, Object>> list = new ArrayList<>();
        //只过滤conf这个目录
        File[] files = file.listFiles(item -> item.getName().equals("conf"));
        assert files != null;
        for (File item : files) {
            eachFile(item, list, true);
        }
        return list;*/
        return null;
    }

    @Override
    public RestResponse build(Long id) throws Exception {
        boolean success = cloneOrPull(id);
        if (success) {
            return RestResponse.create();
        } else {
            return RestResponse.create().message("clone or pull error.");
        }
    }

    private boolean cloneOrPull(Long id) {
        Project project = getById(id);
        try {
            if (!project.isCloned()) {
                Git git = Git.cloneRepository()
                        .setURI(project.getUrl())
                        .setDirectory(project.getHome(streamXProperties.getWorkSpace()))
                        .setBranch(project.getBranches())
                        .setCredentialsProvider(project.getCredentialsProvider())
                        .call();
                git.close();
                this.baseMapper.cloned(project);
            } else {
                Git git = new Git(project.getRepository(streamXProperties.getWorkSpace()));
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(project.getBranches()).call();

                log.info("[StreamX] pull starting...");
                git.pull()
                        .setRemote("origin")
                        .setRemoteBranchName(project.getBranches())
                        .setCredentialsProvider(project.getCredentialsProvider())
                        .setProgressMonitor(new ProgressMonitor() {
                            @Override
                            public void start(int totalTasks) {
                                System.out.println("[StreamX] start pull...s");
                            }

                            @Override
                            public void beginTask(String title, int totalWork) {
                                System.out.println("[StreamX] beginTask,title:" + title + ",totalWork:" + totalWork);
                            }

                            @Override
                            public void update(int completed) {
                                System.out.println("[StreamX] update, Progress :" + completed + "%");
                            }

                            @Override
                            public void endTask() {
                                System.out.println("[StreamX] end pull...s");
                            }

                            @Override
                            public boolean isCancelled() {
                                return false;
                            }
                        }).call();
                git.close();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void eachFile(File file, List<Map<String, Object>> list, Boolean isRoot) {
        if (file != null && file.exists() && file.listFiles() != null) {
            if (isRoot) {
                String title = file.getName();
                String value = file.getAbsolutePath();
                Map<String, Object> map = new HashMap<>(0);
                map.put("key", title);
                map.put("title", title);
                map.put("value", value);
                List<Map<String, Object>> children = new ArrayList<>();
                eachFile(file, children, false);
                if (!children.isEmpty()) {
                    map.put("children", children);
                }
                list.add(map);
            } else {
                for (File item : file.listFiles()) {
                    String title = item.getName();
                    String value = item.getAbsolutePath();
                    Map<String, Object> map = new HashMap<>(0);
                    map.put("key", title);
                    map.put("title", title);
                    map.put("value", value);
                    List<Map<String, Object>> children = new ArrayList<>();
                    eachFile(item, children, false);
                    if (!children.isEmpty()) {
                        map.put("children", children);
                    }
                    list.add(map);
                }
            }
        }
    }

}
