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
import { FormSchema } from '/@/components/Table';
import { computed, h, reactive, Ref, ref, unref } from 'vue';
import { executionModes } from '../data';

import { useCreateAndEditSchema } from './useCreateAndEditSchema';
import { renderSqlHistory } from './useFlinkRender';
import { Alert } from 'ant-design-vue';
import { fetchGetVer } from '/@/api/flink/config';
import { decodeByBase64 } from '/@/utils/cipher';
import { fetchFlinkSql } from '/@/api/flink/app/flinkSql';
import { toPomString } from '../utils/Pom';
import { handleDependencyJsonToPom } from '../utils';
import { useDrawer } from '/@/components/Drawer';
import { AppListRecord } from '/@/api/flink/app/app.type';
import { useRoute } from 'vue-router';

export const useEditStreamParkSchema = (
  configVersions: Ref,
  flinkSqlHistory: Ref,
  dependencyRef: Ref,
) => {
  const flinkSql = ref();
  const app = reactive<Partial<AppListRecord>>({});
  const route = useRoute();
  const {
    getFlinkSqlSchema,
    getFlinkClusterSchemas,
    getFlinkFormOtherSchemas,
    getFlinkTypeSchema,
    flinkEnvs,
    flinkClusters,
    alerts,
  } = useCreateAndEditSchema(dependencyRef, {
    appId: route.query.appId as string,
    mode: 'streamx',
  });

  const [registerDifferentDrawer, { openDrawer: openDiffDrawer }] = useDrawer();

  async function handleChangeSQL(v) {
    const res = await fetchGetVer({ id: v });
    flinkSql.value?.setContent(decodeByBase64(res.sql));
  }

  async function handleCompareOk(compareSQL) {
    const res = await fetchFlinkSql({ id: compareSQL.join(',') });
    const obj1 = res[0];
    const obj2 = res[1];
    const sql1 = decodeByBase64(obj1.sql);
    const sql2 = decodeByBase64(obj2.sql);

    const pomMap1 = new Map();
    const jarMap1 = new Map();
    handleDependencyJsonToPom(obj1.dependency, pomMap1, jarMap1);
    let pom1 = '';
    let jar1 = '';
    pomMap1.forEach((v) => (pom1 += toPomString(v) + '\n\n'));
    jarMap1.forEach((v) => (jar1 += v + '\n'));

    const pomMap2 = new Map();
    const jarMap2 = new Map();
    handleDependencyJsonToPom(obj2.dependency, pomMap2, jarMap2);
    let pom2 = '';
    let jar2 = '';
    pomMap2.forEach((v) => (pom2 += toPomString(v) + '\n\n'));
    jarMap2.forEach((v) => (jar2 += v + '\n'));
    openDiffDrawer(true, {
      immediate: true,
      param: [
        {
          name: 'Flink SQL',
          format: 'sql',
          original: sql1,
          modified: sql2,
        },
        {
          name: 'Dependency',
          format: 'xml',
          original: pom1,
          modified: pom2,
        },
        {
          name: ' Jar ',
          format: 'text',
          original: jar1,
          modified: jar2,
        },
      ],
      original: obj1.version,
      modified: obj2.version,
    });
  }
  const getEditStreamParkFormSchema = computed((): FormSchema[] => {
    return [
      ...getFlinkTypeSchema.value,
      {
        field: 'executionMode',
        label: 'Execution Mode',
        component: 'Select',
        componentProps: {
          placeholder: 'Execution Mode',
          options: executionModes,
        },
      },
      ...getFlinkClusterSchemas.value,
      {
        field: 'flinkSqlHistory',
        label: 'History Version',
        component: 'Select',
        render: ({ model }) =>
          renderSqlHistory(
            { model, flinkSqlHistory: unref(flinkSqlHistory) },
            { handleChangeSQL, handleCompareOk },
          ),
        ifShow: ({ values }) => {
          return values.jobType == 2 && unref(flinkSqlHistory).length > 1;
        },
      },
      ...getFlinkSqlSchema.value,
      {
        field: 'projectName',
        label: 'Project',
        component: 'Input',
        render: () => h(Alert, { message: app.projectName, type: 'info' }),
        ifShow: ({ values }) => values.jobType != 2,
      },
      {
        field: 'module',
        label: 'Application',
        component: 'Input',
        render: ({ values }) => h(Alert, { message: values.module, type: 'info' }),
        ifShow: ({ values }) => values.jobType != 2,
      },
      {
        field: 'appConf',
        label: 'Application conf',
        component: 'Input',
        slot: 'appConf',
        ifShow: ({ values }) => values.jobType != 2,
      },
      {
        field: 'compareConf',
        label: 'Compare conf',
        component: 'Input',
        slot: 'compareConf',
        defaultValue: [],
        ifShow: ({ values }) => {
          return values.jobType != 2 && values.strategy == 1 && unref(configVersions).length > 1;
        },
      },
      {
        field: 'useSysHadoopConf',
        label: 'Use System Hadoop Conf',
        component: 'Switch',
        slot: 'useSysHadoopConf',
        defaultValue: false,
        ifShow: ({ values }) => values.executionMode == 6,
      },
      ...getFlinkFormOtherSchemas.value,
    ];
  });
  return {
    getEditStreamParkFormSchema,
    flinkEnvs,
    flinkClusters,
    flinkSql,
    alerts,
    registerDifferentDrawer,
  };
};
