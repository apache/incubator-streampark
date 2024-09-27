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
import { computed, h, Ref, ref, unref } from 'vue';
import { DeployMode, JobTypeEnum, UseStrategyEnum } from '/@/enums/flinkEnum';
import { useCreateAndEditSchema } from './useCreateAndEditSchema';
import { renderSqlHistory } from './useFlinkRender';
import { Alert } from 'ant-design-vue';
import { decodeByBase64 } from '/@/utils/cipher';
import { fetchFlinkSql } from '/@/api/flink/flinkSql';
import { toPomString } from '../utils/Pom';
import { handleDependencyJsonToPom } from '../utils';
import { useDrawer } from '/@/components/Drawer';
import { useRoute } from 'vue-router';
import { useMessage } from '/@/hooks/web/useMessage';
import { useI18n } from '/@/hooks/web/useI18n';
const { t } = useI18n();

export const useEditStreamParkSchema = (
  configVersions: Ref,
  flinkSqlHistory: Ref,
  dependencyRef: Ref,
) => {
  const flinkSql = ref();
  const route = useRoute();
  const appId = route.query.appId as string;
  const {
    alerts,
    flinkEnvs,
    flinkClusters,
    getFlinkSqlSchema,
    getFlinkClusterSchemas,
    getFlinkFormOtherSchemas,
    getFlinkTypeSchema,
    getDeployModeSchema,
    suggestions,
  } = useCreateAndEditSchema(dependencyRef, {
    appId: appId,
    mode: 'streampark',
  });
  const { createMessage } = useMessage();
  const [registerDifferentDrawer, { openDrawer: openDiffDrawer }] = useDrawer();

  async function handleChangeSQL(v: string) {
    const res = await fetchFlinkSql({ id: v, appId: appId });
    flinkSql.value?.setContent(decodeByBase64(res.sql));
    console.log('res', flinkSql.value);
    unref(dependencyRef)?.setDefaultValue(JSON.parse(res.dependency || '{}'));
  }
  // start compare flinksql version
  async function handleCompareOk(compareSQL: Array<string>) {
    if (compareSQL.length != 2) {
      createMessage.warning('Two versions must be selected for comparison');
      return Promise.reject('error, compareSQL array length less thatn 2');
    }
    const res = await fetchFlinkSql({ appId: appId, id: compareSQL.join(',') });
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
      ...getDeployModeSchema.value,
      ...getFlinkClusterSchemas.value,
      {
        field: 'flinkSqlHistory',
        label: t('flink.app.historyVersion'),
        component: 'Select',
        render: ({ model }) =>
          renderSqlHistory(
            { model, flinkSqlHistory: unref(flinkSqlHistory) },
            { handleChangeSQL, handleCompareOk },
          ),
        ifShow: ({ values }) => {
          return values.jobType == JobTypeEnum.SQL && unref(flinkSqlHistory).length > 1;
        },
        required: true,
      },
      ...getFlinkSqlSchema.value,
      {
        field: 'projectName',
        label: 'Project',
        component: 'Input',
        render: ({ model }) => h(Alert, { message: model.projectName, type: 'info' }),
        ifShow: ({ model, values }) => values.jobType != JobTypeEnum.SQL && model.projectName,
      },
      { field: 'project', label: 'ProjectId', component: 'Input', show: false },

      {
        field: 'module',
        label: 'Application',
        component: 'Input',
        render: ({ model }) => h(Alert, { message: model.module, type: 'info' }),
        ifShow: ({ model, values }) => values.jobType != JobTypeEnum.SQL && model.module,
      },
      { field: 'configId', label: 'configId', component: 'Input', show: false },
      { field: 'config', label: '', component: 'Input', show: false },
      { field: 'strategy', label: '', component: 'Input', show: false },
      {
        field: 'appConf',
        label: 'Application conf',
        component: 'Input',
        slot: 'appConf',
        ifShow: ({ values }) => values.jobType != JobTypeEnum.SQL,
      },
      {
        field: 'compareConf',
        label: 'Compare conf',
        component: 'Input',
        slot: 'compareConf',
        defaultValue: [],
        ifShow: ({ values }) => {
          return (
            values.jobType == JobTypeEnum.JAR &&
            values.strategy == UseStrategyEnum.USE_EXIST &&
            unref(configVersions).length > 1
          );
        },
      },
      {
        field: 'useSysHadoopConf',
        label: 'Use System Hadoop Conf',
        component: 'Switch',
        slot: 'useSysHadoopConf',
        defaultValue: false,
        ifShow: ({ values }) => values.deployMode == DeployMode.KUBERNETES_APPLICATION,
      },
      ...getFlinkFormOtherSchemas.value,
    ];
  });
  return {
    alerts,
    flinkEnvs,
    flinkClusters,
    getEditStreamParkFormSchema,
    flinkSql,
    registerDifferentDrawer,
    suggestions,
  };
};
