/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { Router } from 'vue-router';
import { fetchSsoToken } from '/@/api/system/user';
import { APP_TEAMID_KEY_ } from '/@/enums/cacheEnum';
import { PageEnum } from '/@/enums/pageEnum';
import { useUserStoreWithOut } from '/@/store/modules/user';
import { useMessage } from '/@/hooks/web/useMessage';
import { useI18n } from '/@/hooks/web/useI18n';
import { computed } from 'vue';

const HOME_PATH = PageEnum.BASE_HOME;

const { createMessage } = useMessage();

export function createSsoGuard(router: Router) {
  const userStore = useUserStoreWithOut();
  const { t } = useI18n();
  router.beforeEach(async (to, _, next) => {
    const token = userStore.getToken;
    const isFromBESso = computed(() => to.path === HOME_PATH && to.query['from'] === 'sso');
    if (!token && isFromBESso.value) {
      try {
        const data = await fetchSsoToken();
        if (data?.user) {
          const { lastTeamId, nickName } = data.user;
          userStore.teamId = lastTeamId || '';
          sessionStorage.setItem(APP_TEAMID_KEY_, userStore.teamId);
          localStorage.setItem(APP_TEAMID_KEY_, userStore.teamId);
          userStore.setData(data);
          const ssoLoginSuccess = await userStore.afterLoginAction();
          if (ssoLoginSuccess) {
            let successText = t('sys.login.loginSuccessDesc');
            if (nickName) successText += `: ${nickName}`;
            createMessage.success(`${t('sys.login.loginSuccessTitle')} ${successText}`);
            next(userStore.getUserInfo.homePath || HOME_PATH);
            return;
          }
        }
      } catch {}
    }
    next();
  });
}
