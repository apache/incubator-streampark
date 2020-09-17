package com.streamxhub.console.system.manager;

import com.streamxhub.console.system.authentication.ServerUtil;
import com.streamxhub.console.system.entity.Menu;
import com.streamxhub.console.system.entity.Role;
import com.streamxhub.console.system.entity.User;
import com.streamxhub.console.system.entity.UserConfig;
import com.streamxhub.console.base.domain.router.RouterMeta;
import com.streamxhub.console.base.domain.router.VueRouter;
import com.streamxhub.console.base.utils.TreeUtil;
import com.streamxhub.console.system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 封装一些和 User相关的业务操作
 */
@Service
public class UserManager {

    @Autowired
    private RoleService roleService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserConfigService userConfigService;

    @Autowired
    private ServerUtil serverUtil;


    /**
     * 通过用户名获取用户基本信息
     *
     * @param username 用户名
     * @return 用户基本信息
     */
    public User getUser(String username) {
        return this.userService.findByName(username);
    }

    /**
     * 通过用户名获取用户角色集合
     *
     * @param username 用户名
     * @return 角色集合
     */
    public Set<String> getUserRoles(String username) {
        List<Role> roleList = this.roleService.findUserRole(username);
        return roleList.stream().map(Role::getRoleName).collect(Collectors.toSet());
    }

    /**
     * 通过用户名获取用户权限集合
     *
     * @param username 用户名
     * @return 权限集合
     */
    public Set<String> getUserPermissions(String username) {
        List<Menu> permissionList =  this.menuService.findUserPermissions(username);
        return permissionList.stream().map(Menu::getPerms).collect(Collectors.toSet());
    }

    /**
     * 通过用户名构建 Vue路由
     *
     * @return 路由集合
     */
    public ArrayList<VueRouter<Menu>> getUserRouters() {

        List<VueRouter<Menu>> routes = new ArrayList<>();
        //只差type为菜单类型
        List<Menu> menus = this.menuService.findUserMenus(serverUtil.getUser().getUsername());
        menus.forEach(menu -> {
            VueRouter<Menu> route = new VueRouter<>();
            route.setId(menu.getMenuId().toString());
            route.setParentId(menu.getParentId().toString());
            route.setPath(menu.getPath());
            route.setComponent(menu.getComponent());
            route.setName(menu.getMenuName());
            boolean hidden = menu.getDisplay().equals(Menu.DISPLAY_NONE);
            route.setMeta(new RouterMeta(true, hidden, true, menu.getIcon()));
            routes.add(route);
        });
        return TreeUtil.buildVueRouter(routes);
    }

    /**
     * 通过用户 ID获取前端系统个性化配置
     *
     * @param userId 用户 ID
     * @return 前端系统个性化配置
     */
    public UserConfig getUserConfig(String userId) {
        return this.userConfigService.findByUserId(userId);
    }

}
