import type { UserInfo } from '/#/store';
import { defineStore } from 'pinia';
import { store } from '/@/store';
import { RoleEnum } from '/@/enums/roleEnum';
import { PageEnum } from '/@/enums/pageEnum';
import {
  APP_TEAMID_KEY_,
  EXPIRE_KEY,
  PERMISSION_KEY,
  ROLES_KEY,
  TOKEN_KEY,
  USER_INFO_KEY,
} from '/@/enums/cacheEnum';
import { getAuthCache, setAuthCache } from '/@/utils/auth';
import { signout } from '/@/api/system/passport';
import { fetchSetUserTeam } from '/@/api/system/user';

import { useI18n } from '/@/hooks/web/useI18n';
import { useMessage } from '/@/hooks/web/useMessage';
import { router } from '/@/router';
import { usePermissionStore } from '/@/store/modules/permission';
import { RouteRecordRaw } from 'vue-router';
import { PAGE_NOT_FOUND_ROUTE } from '/@/router/routes/basic';
import { h, unref } from 'vue';
import { getUserTeamId } from '/@/utils';
import { usePermission } from '/@/hooks/web/usePermission';

interface TeamListType {
  label: string;
  value: string;
}
interface UserState {
  userInfo: Nullable<UserInfo>;
  token?: string;
  expire?: string;
  roleList: RoleEnum[];
  permissions: string[];
  sessionTimeout?: boolean;
  lastUpdateTime: number;
  teamId: string;
  teamList: Array<TeamListType>;
}

export const useUserStore = defineStore({
  id: 'app-user',
  state: (): UserState => ({
    // user info
    userInfo: null,
    // token
    token: undefined,
    expire: undefined,
    // roleList
    roleList: [],
    permissions: [],
    // Whether the login expired
    sessionTimeout: false,
    // Last fetch time
    lastUpdateTime: 0,
    // user Team
    teamId: getUserTeamId(),
    // Maintain teamlist data
    teamList: [],
  }),
  getters: {
    getUserInfo(): UserInfo {
      return this.userInfo || getAuthCache<UserInfo>(USER_INFO_KEY) || {};
    },
    getToken(): string {
      return this.token || getAuthCache<string>(TOKEN_KEY);
    },
    getExpire(): string {
      return this.expire || getAuthCache<string>(EXPIRE_KEY);
    },
    getRoleList(): RoleEnum[] {
      return this.roleList?.length > 0 ? this.roleList : getAuthCache<RoleEnum[]>(ROLES_KEY);
    },
    getSessionTimeout(): boolean {
      return !!this.sessionTimeout;
    },
    getLastUpdateTime(): number {
      return this.lastUpdateTime;
    },
    getPermissions(): string[] {
      return this.permissions?.length > 0
        ? this.permissions
        : getAuthCache<string[]>(PERMISSION_KEY);
    },
    getTeamId(): string | undefined {
      return this.teamId;
    },
    getTeamList(): TeamListType[] {
      return this.teamList;
    },
  },
  actions: {
    setToken(info: string | undefined) {
      this.token = info ? info : ''; // for null or undefined value
      setAuthCache(TOKEN_KEY, info);
    },
    setExpire(info: string | undefined) {
      this.expire = info || '';
      setAuthCache(EXPIRE_KEY, info);
    },
    setRoleList(roleList: RoleEnum[]) {
      this.roleList = roleList || [];
      setAuthCache(ROLES_KEY, roleList);
    },
    setUserInfo(info: UserInfo | null) {
      this.userInfo = info;
      this.lastUpdateTime = new Date().getTime();
      setAuthCache(USER_INFO_KEY, info);
    },
    setSessionTimeout(flag: boolean) {
      this.sessionTimeout = flag;
    },
    resetState() {
      this.userInfo = null;
      this.token = '';
      this.roleList = [];
      this.sessionTimeout = false;
    },
    setPermissions(permissions: string[] = []) {
      this.permissions = permissions;
      setAuthCache(PERMISSION_KEY, permissions);
    },
    setData(data: Recordable) {
      const { token, expire, user, permissions, roles = [] } = data;

      this.setToken(token);
      this.setExpire(expire);
      this.setUserInfo(user);
      this.setRoleList(roles);
      this.setPermissions(permissions);
    },
    // set team
    async setTeamId(data: { teamId: string; userId?: string | number }): Promise<boolean> {
      try {
        const { refreshMenu } = usePermission();
        // The userId passed in is the binding operation at login
        const { permissions, roles = [], user } = await fetchSetUserTeam(data);
        this.setUserInfo(user as UserInfo);
        this.setRoleList(roles as RoleEnum[]);
        this.setPermissions(permissions);

        // If it returns success, it will be stored in the local cache
        this.teamId = data.teamId;
        sessionStorage.setItem(APP_TEAMID_KEY_, data.teamId);
        localStorage.setItem(APP_TEAMID_KEY_, data.teamId);
        await refreshMenu(unref(router.currentRoute)?.path);
        return Promise.resolve(true);
      } catch (error) {
        return Promise.reject(error);
      }
    },
    setTeamList(teamList: Array<TeamListType>) {
      this.teamList = teamList;
    },
    async afterLoginAction(goHome?: boolean): Promise<boolean> {
      if (!this.getToken) return false;
      const sessionTimeout = this.sessionTimeout;
      if (sessionTimeout) {
        this.setSessionTimeout(false);
      } else {
        try {
          const permissionStore = usePermissionStore();
          if (!permissionStore.isDynamicAddedRoute) {
            const [routes] = await permissionStore.buildRoutesAction();
            routes.forEach((route) => {
              router.addRoute(route as unknown as RouteRecordRaw);
            });
            router.addRoute(PAGE_NOT_FOUND_ROUTE as unknown as RouteRecordRaw);
            permissionStore.setDynamicAddedRoute(true);
          }
          goHome && (await router.replace(PageEnum.BASE_HOME));
          return true;
        } catch (error) {
          this.setToken(undefined);
          this.setSessionTimeout(false);
          this.setUserInfo(null);
          console.error(error, 'error');
        }
      }
      return false;
    },
    /**
     * @description: logout
     */
    async logout(goLogin = false) {
      if (this.getToken) {
        try {
          await signout();
        } catch {
          console.log('Token cancellation failed');
        }
      }
      this.setSessionTimeout(false);
      this.setUserInfo(null);
      sessionStorage.removeItem(APP_TEAMID_KEY_);
      sessionStorage.removeItem('appPageNo');
      localStorage.removeItem(APP_TEAMID_KEY_);
      this.setToken(undefined);
      goLogin && router.push(PageEnum.BASE_LOGIN);
    },

    /**
     * @description: Confirm before logging out
     */
    confirmLoginOut() {
      const { createConfirm } = useMessage();
      const { t } = useI18n();
      createConfirm({
        iconType: 'warning',
        title: () => h('span', t('sys.app.logoutTip')),
        content: () => h('span', { class: 'inline-block pt-20px' }, t('sys.app.logoutMessage')),
        onOk: async () => {
          await this.logout(true);
        },
      });
    },
  },
});

// Need to be used outside the setup
export function useUserStoreWithOut() {
  return useUserStore(store);
}
