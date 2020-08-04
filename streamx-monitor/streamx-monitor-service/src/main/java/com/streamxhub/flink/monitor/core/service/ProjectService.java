package com.streamxhub.flink.monitor.core.service;

import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.domain.RestResponse;
import com.streamxhub.flink.monitor.core.entity.Project;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ProjectService extends IService<Project> {

    RestResponse create(Project project);

    boolean delete(String id);

    IPage<Project> page(Project project, RestRequest restRequest);

    List<Map<String,Object>> filelist(String id);

    RestResponse build(Long id) throws Exception;
}
