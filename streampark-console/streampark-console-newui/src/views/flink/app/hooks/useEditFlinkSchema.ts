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
import { getAlertSvgIcon } from './useFlinkRender';
import { Alert } from 'ant-design-vue';
import { AppListRecord } from '/@/api/flink/app/app.type';
import { useRoute } from 'vue-router';
import { fetchMain } from '/@/api/flink/app/app';

export const useEditFlinkSchema = (jars: Ref) => {
  const flinkSql = ref();
  const app = reactive<Partial<AppListRecord>>({});
  const resourceFrom = ref<Nullable<number>>(null);
  const route = useRoute();
  const {
    getFlinkClusterSchemas,
    getFlinkFormOtherSchemas,
    getFlinkTypeSchema,
    flinkEnvs,
    flinkClusters,
    alerts,
  } = useCreateAndEditSchema(null, { appId: route.query.appId as string, mode: 'streamx' });

  const getEditFlinkFormSchema = computed((): FormSchema[] => {
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
      {
        field: 'resourceFrom',
        label: 'Resource From',
        component: 'Input',
        render: () => {
          if (unref(resourceFrom) === 1) return getAlertSvgIcon('github', 'CICD (build from CSV)');
          else if (unref(resourceFrom) === 2)
            return getAlertSvgIcon('upload', 'Upload (upload local job)');
          else return '';
        },
      },
      ...getFlinkClusterSchemas.value,
      {
        field: 'projectName',
        label: 'Project',
        component: 'Input',
        render: () => h(Alert, { message: app.projectName, type: 'info' }),
        ifShow: unref(resourceFrom) === 1,
      },
      {
        field: 'module',
        label: 'Module',
        component: 'Input',
        render: () => h(Alert, { message: app.module, type: 'info' }),
        ifShow: unref(resourceFrom) === 1,
      },
      {
        field: 'jar',
        label: 'Program Jar',
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: 'Please select jar',
            options: unref(jars).map((i) => ({ label: i, value: i })),
            onChange: (value) => {
              fetchMain({
                projectId: app.projectId,
                module: app.module,
                jar: value,
              }).then((res) => {
                formModel.mainClass = res;
              });
            },
          };
        },
        ifShow: unref(resourceFrom) === 1,
        rules: [{ required: true, message: 'Please select jar' }],
      },
      {
        field: 'uploadJobJar',
        label: 'Upload Job Jar',
        component: 'Select',
        slot: 'uploadJobJar',
        ifShow: unref(resourceFrom) !== 1,
      },
      {
        field: 'jar',
        label: 'Program Jar',
        component: 'Input',
        dynamicDisabled: true,
        ifShow: unref(resourceFrom) !== 1,
      },
      {
        field: 'mainClass',
        label: 'Program Main',
        component: 'Input',
        componentProps: {
          allowClear: true,
          placeholder: 'Please enter Main class',
        },
        rules: [{ required: true, message: 'Program Main is required' }],
      },
      ...getFlinkFormOtherSchemas.value,
    ];
  });
  return {
    getEditFlinkFormSchema,
    flinkEnvs,
    flinkClusters,
    flinkSql,
    alerts,
  };
};
