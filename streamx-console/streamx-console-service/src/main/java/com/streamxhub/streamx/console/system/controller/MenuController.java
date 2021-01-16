package com.streamxhub.streamx.console.system.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.streamxhub.streamx.console.base.controller.BaseController;
import com.streamxhub.streamx.console.base.domain.router.VueRouter;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.system.authentication.ServerUtil;
import com.streamxhub.streamx.console.system.entity.Menu;
import com.streamxhub.streamx.console.system.service.MenuService;
import com.wuwenze.poi.ExcelKit;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/menu")
public class MenuController extends BaseController {

    private String message;

    @Autowired
    private MenuService menuService;

    @Autowired
    private ServerUtil serverUtil;

    @PostMapping("router")
    public ArrayList<VueRouter<Menu>> getUserRouters() {
        return this.menuService.getUserRouters(serverUtil.getUser());
    }

    @PostMapping("list")
    @RequiresPermissions("menu:view")
    public Map<String, Object> menuList(Menu menu) {
        return this.menuService.findMenus(menu);
    }

    @PostMapping("post")
    @RequiresPermissions("menu:add")
    public void addMenu(@Valid Menu menu) throws ServiceException {
        try {
            this.menuService.createMenu(menu);
        } catch (Exception e) {
            message = "新增菜单/按钮失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @DeleteMapping("delete")
    @RequiresPermissions("menu:delete")
    public void deleteMenus(@NotBlank(message = "{required}") String menuIds)
            throws ServiceException {
        try {
            String[] ids = menuIds.split(StringPool.COMMA);
            this.menuService.deleteMeuns(ids);
        } catch (Exception e) {
            message = "删除菜单/按钮失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PutMapping("update")
    @RequiresPermissions("menu:update")
    public void updateMenu(@Valid Menu menu) throws ServiceException {
        try {
            this.menuService.updateMenu(menu);
        } catch (Exception e) {
            message = "修改菜单/按钮失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PostMapping("export")
    @RequiresPermissions("menu:export")
    public void export(Menu menu, HttpServletResponse response) throws ServiceException {
        try {
            List<Menu> menus = this.menuService.findMenuList(menu);
            ExcelKit.$Export(Menu.class, response).downXlsx(menus, false);
        } catch (Exception e) {
            message = "导出Excel失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }
}
