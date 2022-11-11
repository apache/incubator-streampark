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
import type { AppRouteRecordRaw } from '/@/router/types';

import { computed, toRaw, unref } from 'vue';

import { useMultipleTabStore } from '/@/store/modules/multipleTab';

import { uniqBy } from 'lodash-es';

import { useMultipleTabSetting } from '/@/hooks/setting/useMultipleTabSetting';

import { useRouter } from 'vue-router';

export function useFrameKeepAlive() {
  const router = useRouter();
  const { currentRoute } = router;
  const { getShowMultipleTab } = useMultipleTabSetting();
  const tabStore = useMultipleTabStore();
  const getFramePages = computed(() => {
    const ret = getAllFramePages(toRaw(router.getRoutes()) as unknown as AppRouteRecordRaw[]) || [];
    return ret;
  });

  const getOpenTabList = computed((): string[] => {
    return tabStore.getTabList.reduce((prev: string[], next) => {
      if (next.meta && Reflect.has(next.meta, 'frameSrc')) {
        prev.push(next.name as string);
      }
      return prev;
    }, []);
  });

  function getAllFramePages(routes: AppRouteRecordRaw[]): AppRouteRecordRaw[] {
    let res: AppRouteRecordRaw[] = [];
    for (const route of routes) {
      const { meta: { frameSrc } = {}, children } = route;
      if (frameSrc) {
        res.push(route);
      }
      if (children && children.length) {
        res.push(...getAllFramePages(children));
      }
    }
    res = uniqBy(res, 'name');
    return res;
  }

  function showIframe(item: AppRouteRecordRaw) {
    return item.name === unref(currentRoute).name;
  }

  function hasRenderFrame(name: string) {
    if (!unref(getShowMultipleTab)) {
      return router.currentRoute.value.name === name;
    }
    return unref(getOpenTabList).includes(name);
  }

  return { hasRenderFrame, getFramePages, showIframe, getAllFramePages };
}
