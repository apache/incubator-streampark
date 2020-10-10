package com.streamxhub.console.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.console.base.domain.Constant;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.base.utils.ShaHashUtil;
import com.streamxhub.console.base.utils.SortUtil;
import com.streamxhub.console.system.dao.UserMapper;
import com.streamxhub.console.system.dao.UserRoleMapper;
import com.streamxhub.console.system.entity.Menu;
import com.streamxhub.console.system.entity.User;
import com.streamxhub.console.system.entity.UserRole;
import com.streamxhub.console.system.service.MenuService;
import com.streamxhub.console.system.service.UserRoleService;
import com.streamxhub.console.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author benjobs
 */
@Slf4j
@Service("userService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private UserRoleMapper userRoleMapper;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private MenuService menuService;

    @Override
    public User findByName(String username) {
        return baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public IPage<User> findUserDetail(User user, RestRequest request) {
        try {
            Page<User> page = new Page<>();
            SortUtil.handlePageSort(request, page, "userId", Constant.ORDER_ASC, false);
            IPage<User> pages = this.baseMapper.findUserDetail(page, user);
            return pages;
        } catch (Exception e) {
            log.info("查询用户异常", e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLoginTime(String username) throws Exception {
        User user = new User();
        user.setLastLoginTime(new Date());
        this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(User user) throws Exception {
        // 创建用户
        user.setCreateTime(new Date());
        user.setAvatar(User.DEFAULT_AVATAR);
        String salt = ShaHashUtil.getRandomSalt(26);
        String password = ShaHashUtil.encrypt(salt, User.DEFAULT_PASSWORD);
        user.setSalt(salt);
        user.setPassword(password);
        save(user);
        // 保存用户角色
        String[] roles = user.getRoleId().split(StringPool.COMMA);
        setUserRoles(user, roles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(User user) throws Exception {
        // 更新用户
        user.setPassword(null);
        user.setModifyTime(new Date());
        updateById(user);

        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getUserId()));

        String[] roles = user.getRoleId().split(StringPool.COMMA);
        setUserRoles(user, roles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(String[] userIds) throws Exception {
        List<String> list = Arrays.asList(userIds);
        removeByIds(list);
        // 删除用户角色
        this.userRoleService.deleteUserRolesByUserId(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(User user) throws Exception {
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(String username, String avatar) throws Exception {
        User user = new User();
        user.setAvatar(avatar);
        this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String username, String password) throws Exception {
        User user = new User();
        String salt = ShaHashUtil.getRandomSalt(26);
        password = ShaHashUtil.encrypt(salt, password);
        user.setSalt(salt);
        user.setPassword(password);
        this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void regist(String username, String password) throws Exception {
        User user = new User();
        String salt = ShaHashUtil.getRandomSalt(26);
        password = ShaHashUtil.encrypt(salt, password);
        user.setSalt(salt);
        user.setPassword(password);
        user.setUsername(username);
        user.setCreateTime(new Date());
        user.setStatus(User.STATUS_VALID);
        user.setSex(User.SEX_UNKNOW);
        user.setAvatar(User.DEFAULT_AVATAR);
        user.setDescription("注册用户");
        this.save(user);

        UserRole ur = new UserRole();
        ur.setUserId(user.getUserId());
        /**
         * 注册用户角色 ID
         */
        ur.setRoleId(2L);
        this.userRoleMapper.insert(ur);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String[] usernames) throws Exception {
        for (String username : usernames) {
            User user = new User();
            String salt = ShaHashUtil.getRandomSalt(26);
            String password = ShaHashUtil.encrypt(salt, User.DEFAULT_PASSWORD);
            user.setSalt(salt);
            user.setPassword(password);
            this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        }

    }

    /**
     * 通过用户名获取用户权限集合
     *
     * @param username 用户名
     * @return 权限集合
     */
    @Override
    public Set<String> getPermissions(String username) {
        List<Menu> permissionList = this.menuService.findUserPermissions(username);
        return permissionList.stream().map(Menu::getPerms).collect(Collectors.toSet());
    }

    private void setUserRoles(User user, String[] roles) {
        Arrays.stream(roles).forEach(roleId -> {
            UserRole ur = new UserRole();
            ur.setUserId(user.getUserId());
            ur.setRoleId(Long.valueOf(roleId));
            this.userRoleMapper.insert(ur);
        });
    }
}
