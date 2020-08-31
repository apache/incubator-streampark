package com.streamxhub.monitor.system.controller;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.streamxhub.monitor.base.annotation.Log;
import com.streamxhub.monitor.base.controller.BaseController;
import com.streamxhub.monitor.base.exception.AdminXException;
import com.streamxhub.monitor.system.entity.Role;
import com.streamxhub.monitor.system.entity.RoleMenu;
import com.streamxhub.monitor.system.service.RoleMenuServie;
import com.streamxhub.monitor.system.service.RoleService;
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
import java.util.stream.Collectors;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("role")
public class RoleController extends BaseController {

    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMenuServie roleMenuServie;

    private String message;

    @PostMapping("list")
    @RequiresPermissions("role:view")
    public Map<String, Object> roleList(RestRequest restRequest, Role role) {
        return getDataTable(roleService.findRoles(role, restRequest));
    }

    @PostMapping("check/name")
    public boolean checkRoleName(@NotBlank(message = "{required}") String roleName) {
        Role result = this.roleService.findByName(roleName);
        return result == null;
    }

    @PostMapping("menu")
    public List<String> getRoleMenus(@NotBlank(message = "{required}") String roleId) {
        List<RoleMenu> list = this.roleMenuServie.getRoleMenusByRoleId(roleId);
        return list.stream().map(roleMenu -> String.valueOf(roleMenu.getMenuId())).collect(Collectors.toList());
    }

    @Log("新增角色")
    @PostMapping("post")
    @RequiresPermissions("role:add")
    public void addRole(@Valid Role role) throws AdminXException {
        try {
            this.roleService.createRole(role);
        } catch (Exception e) {
            message = "新增角色失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @Log("删除角色")
    @DeleteMapping("delete")
    @RequiresPermissions("role:delete")
    public void deleteRoles(@NotBlank(message = "{required}") String roleIds) throws AdminXException {
        try {
            String[] ids = roleIds.split(StringPool.COMMA);
            this.roleService.deleteRoles(ids);
        } catch (Exception e) {
            message = "删除角色失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @Log("修改角色")
    @PutMapping("update")
    @RequiresPermissions("role:update")
    public void updateRole(Role role) throws AdminXException {
        try {
            this.roleService.updateRole(role);
        } catch (Exception e) {
            message = "修改角色失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @PostMapping("export")
    @RequiresPermissions("role:export")
    public void export(RestRequest restRequest, Role role, HttpServletResponse response) throws AdminXException {
        try {
            List<Role> roles = this.roleService.findRoles(role, restRequest).getRecords();
            ExcelKit.$Export(Role.class, response).downXlsx(roles, false);
        } catch (Exception e) {
            message = "导出Excel失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }
}
