package com.streamxhub.monitor.system.controller;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.streamxhub.monitor.base.annotation.Log;
import com.streamxhub.monitor.base.controller.BaseController;
import com.streamxhub.monitor.base.exception.AdminXException;
import com.streamxhub.monitor.system.entity.Dept;
import com.streamxhub.monitor.system.service.DeptService;
import com.streamxhub.monitor.base.domain.RestRequest;
import com.wuwenze.poi.ExcelKit;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("dept")
public class DeptController extends BaseController {

    private String message;

    @Autowired
    private DeptService deptService;

    @PostMapping("list")
    public Map<String, Object> deptList(RestRequest request, Dept dept) {
        return this.deptService.findDepts(request, dept);
    }

    @Log("新增部门")
    @PostMapping("post")
    @RequiresPermissions("dept:add")
    public void addDept(@Valid Dept dept) throws AdminXException {
        try {
            this.deptService.createDept(dept);
        } catch (Exception e) {
            message = "新增部门失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @Log("删除部门")
    @DeleteMapping("delete")
    @RequiresPermissions("dept:delete")
    public void deleteDepts(@NotBlank(message = "{required}") String deptIds) throws AdminXException {
        try {
            String[] ids = deptIds.split(StringPool.COMMA);
            this.deptService.deleteDepts(ids);
        } catch (Exception e) {
            message = "删除部门失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @Log("修改部门")
    @PutMapping("update")
    @RequiresPermissions("dept:update")
    public void updateDept(@Valid Dept dept) throws AdminXException {
        try {
            this.deptService.updateDept(dept);
        } catch (Exception e) {
            message = "修改部门失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @PostMapping("export")
    @RequiresPermissions("dept:export")
    public void export(Dept dept, RestRequest request, HttpServletResponse response) throws AdminXException {
        try {
            List<Dept> depts = this.deptService.findDepts(dept, request);
            ExcelKit.$Export(Dept.class, response).downXlsx(depts, false);
        } catch (Exception e) {
            message = "导出Excel失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }
}
