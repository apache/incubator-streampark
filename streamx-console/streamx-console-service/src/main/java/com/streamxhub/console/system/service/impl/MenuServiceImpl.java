package com.streamxhub.console.system.service.impl;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.console.base.domain.Constant;
import com.streamxhub.console.base.domain.router.RouterMeta;
import com.streamxhub.console.base.domain.router.RouterTree;
import com.streamxhub.console.base.domain.router.VueRouter;
import com.streamxhub.console.base.utils.TreeUtil;
import com.streamxhub.console.system.dao.MenuMapper;
import com.streamxhub.console.system.entity.Menu;
import com.streamxhub.console.system.entity.User;
import com.streamxhub.console.system.service.MenuService;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<Menu> findUserPermissions(String username) {
        return this.baseMapper.findUserPermissions(username);
    }

    @Override
    public List<Menu> findUserMenus(String username) {
        return this.baseMapper.findUserMenus(username);
    }

    @Override
    public Map<String, Object> findMenus(Menu menu) {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
            findMenuCondition(queryWrapper, menu);
            List<Menu> menus = baseMapper.selectList(queryWrapper);

            List<RouterTree<Menu>> trees = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            buildTrees(trees, menus, ids);

            result.put("ids", ids);
            if (StringUtils.equals(menu.getType(), Constant.TYPE_BUTTON)) {
                result.put("rows", trees);
            } else {
                RouterTree<Menu> menuTree = TreeUtil.build(trees);
                result.put("rows", menuTree);
            }
            result.put("total", menus.size());
        } catch (NumberFormatException e) {
            log.info("查询菜单失败", e);
            result.put("rows", null);
            result.put("total", 0);
        }
        return result;
    }

    @Override
    public List<Menu> findMenuList(Menu menu) {
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        findMenuCondition(queryWrapper, menu);
        queryWrapper.orderByAsc(Menu::getMenuId);
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(Menu menu) {
        menu.setCreateTime(new Date());
        setMenu(menu);
        this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Menu menu) throws Exception {
        menu.setModifyTime(new Date());
        setMenu(menu);
        baseMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMeuns(String[] menuIds) throws Exception {
        for (String menuId : menuIds) {
            // 查找与这些菜单/按钮关联的用户
            List<String> userIds = this.baseMapper.findUserIdsByMenuId(String.valueOf(menuId));
            // 递归删除这些菜单/按钮
            this.baseMapper.deleteMenus(menuId);
        }
    }

    @Override
    public ArrayList<VueRouter<Menu>> getUserRouters(User user) {
        List<VueRouter<Menu>> routes = new ArrayList<>();
        // 只差type为菜单类型
        List<Menu> menus = this.findUserMenus(user.getUsername());
        menus.forEach(
                menu -> {
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

    private void buildTrees(List<RouterTree<Menu>> trees, List<Menu> menus, List<String> ids) {
        menus.forEach(
                menu -> {
                    ids.add(menu.getMenuId().toString());
                    RouterTree<Menu> tree = new RouterTree<>();
                    tree.setId(menu.getMenuId().toString());
                    tree.setKey(tree.getId());
                    tree.setParentId(menu.getParentId().toString());
                    tree.setText(menu.getMenuName());
                    tree.setTitle(tree.getText());
                    tree.setIcon(menu.getIcon());
                    tree.setComponent(menu.getComponent());
                    tree.setCreateTime(menu.getCreateTime());
                    tree.setModifyTime(menu.getModifyTime());
                    tree.setPath(menu.getPath());
                    tree.setOrder(menu.getOrderNum());
                    tree.setPermission(menu.getPerms());
                    tree.setType(menu.getType());
                    tree.setDisplay(menu.getDisplay());
                    trees.add(tree);
                });
    }

    private void setMenu(Menu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (Menu.TYPE_BUTTON.equals(menu.getType())) {
            menu.setPath(null);
            menu.setIcon(null);
            menu.setComponent(null);
        }
    }

    private void findMenuCondition(LambdaQueryWrapper<Menu> queryWrapper, Menu menu) {
        if (StringUtils.isNotBlank(menu.getMenuName())) {
            queryWrapper.eq(Menu::getMenuName, menu.getMenuName());
        }
        if (StringUtils.isNotBlank(menu.getType())) {
            queryWrapper.eq(Menu::getType, menu.getType());
        }
        if (StringUtils.isNotBlank(menu.getCreateTimeFrom())
                && StringUtils.isNotBlank(menu.getCreateTimeTo())) {
            queryWrapper
                    .ge(Menu::getCreateTime, menu.getCreateTimeFrom())
                    .le(Menu::getCreateTime, menu.getCreateTimeTo());
        }
    }
}
