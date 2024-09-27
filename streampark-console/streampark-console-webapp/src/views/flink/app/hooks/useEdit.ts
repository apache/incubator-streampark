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
import { reactive } from 'vue';
import { useRoute } from 'vue-router';
import { optionsValueMapping } from '../data/option';
import { fetchGet } from '/@/api/flink/app';
import { AppListRecord } from '/@/api/flink/app.type';
import { isString } from '/@/utils/is';

export const useEdit = () => {
  const route = useRoute();

  const app = reactive<Partial<AppListRecord>>({});
  const defaultOptions = reactive<any>({});
  const memoryItems = reactive<{
    totalItems: string[];
    jmMemoryItems: string[];
    tmMemoryItems: string[];
  }>({
    totalItems: [],
    jmMemoryItems: [],
    tmMemoryItems: [],
  });
  /* initialization information */
  async function handleGetApplication() {
    const returnData = {};
    const appId = route.query.appId;
    const res = await fetchGet({ id: appId as string });
    Object.assign(app, res);
    Object.assign(defaultOptions, JSON.parse(app.options || '{}'));
    Object.assign(returnData, {
      jobType: res.jobType,
      appType: res.appType,
      versionId: res.versionId,
      deployMode: res.deployMode,
      resourceFrom: res.resourceFrom,
    });
    return returnData;
  }
  /* Form reset */
  function handleResetApplication() {
    let parallelism: Nullable<number> = null;
    let slot: Nullable<number> = null;
    const fieldValueOptions = {
      totalItem: {},
      tmOptionsItem: {},
      jmOptionsItem: {},
    };
    for (const k in defaultOptions) {
      let v = defaultOptions[k];
      if (isString(v)) v = v.replace(/[k|m|g]b$/g, '');
      const key = optionsValueMapping.get(k);
      if (key) {
        if (
          k === 'jobmanager.memory.flink.size' ||
          k === 'taskmanager.memory.flink.size' ||
          k === 'jobmanager.memory.process.size' ||
          k === 'taskmanager.memory.process.size'
        ) {
          memoryItems.totalItems.push(key);
          fieldValueOptions.totalItem[key] = parseInt(v);
        } else {
          if (k.startsWith('jobmanager.memory.')) {
            memoryItems.jmMemoryItems.push(key);
            if (k === 'jobmanager.memory.jvm-overhead.fraction') {
              fieldValueOptions.jmOptionsItem[key] = parseFloat(v);
            } else {
              fieldValueOptions.jmOptionsItem[key] = parseInt(v);
            }
          }
          if (k.startsWith('taskmanager.memory.')) {
            memoryItems.tmMemoryItems.push(key);
            if (
              k === 'taskmanager.memory.managed.fraction' ||
              k === 'taskmanager.memory.jvm-overhead.fraction'
            ) {
              fieldValueOptions.tmOptionsItem[key] = parseFloat(v);
            } else {
              fieldValueOptions.tmOptionsItem[key] = parseInt(v);
            }
          }
        }
      } else {
        if (k === 'taskmanager.numberOfTaskSlots') {
          slot = parseInt(v);
        }
        if (k === 'parallelism.default') {
          parallelism = parseInt(v);
        }
      }
    }

    return {
      parallelism,
      slot,
      totalOptions: memoryItems.totalItems,
      jmOptions: memoryItems.jmMemoryItems,
      tmOptions: memoryItems.tmMemoryItems,
      ...fieldValueOptions,
    };
  }
  return {
    handleGetApplication,
    handleResetApplication,
    defaultOptions,
    app,
    memoryItems,
  };
};
