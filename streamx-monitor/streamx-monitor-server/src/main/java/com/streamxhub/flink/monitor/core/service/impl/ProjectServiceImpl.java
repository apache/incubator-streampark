package com.streamxhub.flink.monitor.core.service.impl;

import com.streamxhub.flink.monitor.base.domain.Constant;
import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.domain.RestResponse;
import com.streamxhub.flink.monitor.base.properties.StreamXProperties;
import com.streamxhub.flink.monitor.base.utils.GZipUtil;
import com.streamxhub.flink.monitor.base.utils.SortUtil;
import com.streamxhub.flink.monitor.core.dao.ProjectMapper;
import com.streamxhub.flink.monitor.core.entity.Project;
import com.streamxhub.flink.monitor.core.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

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
    public RestResponse upload(MultipartFile file) {
        String uploadSuffix = ".*tar\\.gz$|.*jar$";
        String tarSuffix = ".tar.gz";
        if (Objects.requireNonNull(file.getOriginalFilename()).matches(uploadSuffix)) {
            try {
                File saveFile = new File(streamXProperties.getUploadDir() + file.getOriginalFilename());
                // delete when exsit
                if (saveFile.exists()) {
                    saveFile.delete();
                }
                // save file to app.home
                FileUtils.writeByteArrayToFile(saveFile, file.getBytes());
                File project = null;
                if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(tarSuffix)) {
                    project = GZipUtil.decompress(saveFile.getAbsolutePath(), streamXProperties.getWorkSpace());
                }
                this.save(saveFile, project, file.getSize());
                return new RestResponse().message("上传成功");
            } catch (Exception e) {
                log.info(e.getMessage());
                return new RestResponse().message("上传失败");
            }
        }
        return new RestResponse().message("上传格式错误");
    }

    private void save(File jarFile, File projectFile, Long size) {
        File file;
        int type;
        if (projectFile == null) {
            file = jarFile;
            type = 1;
        } else {
            file = projectFile;
            type = 2;
        }

        String path = file.getParent();
        String name = file.getName();

        Project project = new Project();
        project.setName(name);
        project.setId(DigestUtils.md5DigestAsHex(name.getBytes()));
        //项目的home路径
        project.setHome(path);
        //保存上传文件的原始路径
        project.setPath(jarFile.getAbsolutePath());
        project.setType(type);
        project.setSize(size);
        project.setDate(new Date());
        this.saveOrUpdate(project);
    }

    @Override
    public boolean delete(String id) {
        String[] ids = id.split(",");
        Collection<Project> projects = super.listByIds(Arrays.asList(ids));
        projects.forEach((Project x) -> {
            super.removeById(x.getId());
            File file = new File(x.getHome() + File.separator + x.getName());
            deleteFile(file);
            file = new File(x.getPath());
            deleteFile(file);
        });
        return true;
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
    public List<Map<String,Object>> filelist(String id) {
        Project project = getById(id);
        if (project == null) {
            return null;
        }
        String path = project.getHome() + File.separator + project.getName();
        File file = new File(path);
        List<Map<String,Object>> list = new ArrayList<>();
        //只过滤conf这个目录
        File[] files = file.listFiles(item -> item.getName().equals("conf"));
        assert files != null;
        for (File item:files) {
            eachFile(item,list,true);
        }
        return list;
    }

    private void eachFile(File file,List<Map<String,Object>> list,Boolean isRoot) {
        if(file!=null && file.exists() && file.listFiles()!=null) {
            if (isRoot) {
                String title = file.getName();
                String value = file.getAbsolutePath();
                Map<String,Object> map = new HashMap<>(0);
                map.put("key",title);
                map.put("title",title);
                map.put("value",value);
                List<Map<String,Object>> children = new ArrayList<>();
                eachFile(file,children,false);
                if(!children.isEmpty()) {
                    map.put("children",children);
                }
                list.add(map);
            }else {
                for (File item: file.listFiles()) {
                    String title = item.getName();
                    String value = item.getAbsolutePath();
                    Map<String,Object> map = new HashMap<>(0);
                    map.put("key",title);
                    map.put("title",title);
                    map.put("value",value);
                    List<Map<String,Object>> children = new ArrayList<>();
                    eachFile(item,children,false);
                    if(!children.isEmpty()) {
                        map.put("children",children);
                    }
                    list.add(map);
                }
            }
        }
    }

}
