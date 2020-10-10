package com.streamxhub.console.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.base.domain.router.VueRouter;
import com.streamxhub.console.system.entity.Menu;
import com.streamxhub.console.system.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface MenuService extends IService<Menu> {

    List<Menu> findUserPermissions(String username);

    List<Menu> findUserMenus(String username);

    Map<String, Object> findMenus(Menu menu);

    List<Menu> findMenuList(Menu menu);

    void createMenu(Menu menu);

    void updateMenu(Menu menu) throws Exception;

    /**
     * 递归删除菜单/按钮
     *
     * @param menuIds menuIds
     */
    void deleteMeuns(String[] menuIds) throws Exception;

    /**
     *
     * @return
     */
    ArrayList<VueRouter<Menu>> getUserRouters(User user);
}
