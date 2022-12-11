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
import { executionModes } from '../data';
import { fetchCheckHadoop } from '/@/api/flink/setting';
import { ExecModeEnum } from '/@/enums/flinkEnum';

import Icon, { SvgIcon } from '/@/components/Icon';
import { useCreateAndEditSchema } from './useCreateAndEditSchema';
import { fetchMain, fetchName } from '/@/api/flink/app/app';
import { modules, fetchListConf, fetchListJars } from '/@/api/flink/project';
import { RuleObject } from 'ant-design-vue/lib/form';
import { StoreValue } from 'ant-design-vue/lib/form/interface';
import { renderResourceFrom } from './useFlinkRender';
import { checkAppConfType, filterOption, getAppConfType } from '../utils';
import { useI18n } from '/@/hooks/web/useI18n';
import { AppTypeEnum } from '/@/enums/flinkEnum';
const { t } = useI18n();

const getJobTypeOptions = () => {
  return [
    {
      label: h('div', {}, [
        h(Icon, { icon: 'ant-design:code-outlined', color: '#108ee9' }, ''),
        h('span', { class: 'pl-10px' }, 'Custom Code'),
      ]),
      value: 'customcode',
    },
    {
      label: h('div', {}, [
        h(SvgIcon, { name: 'fql', color: '#108ee9' }, ''),
        h('span', { class: 'pl-10px' }, 'Flink SQL'),
      ]),
      value: 'sql',
    },
  ];
};

export const useCreateSchema = (dependencyRef: Ref) => {
  const moduleList = ref<Array<{ name: string }>>([]);
  const jars = ref<Array<any>>([]);

  const {
    flinkEnvs,
    flinkClusters,
    projectList,
    getFlinkSqlSchema,
    getFlinkClusterSchemas,
    getFlinkFormOtherSchemas,
    suggestions,
  } = useCreateAndEditSchema(dependencyRef);

  // async function handleEditConfig(config: string) {
  //   console.log('config', config);
  //   const res = await fetchAppConf({ config });
  //   const conf = decodeByBase64(res);
  //   openConfDrawer(true, {
  //     configOverride: conf,
  //   });
  // }
  function handleCheckConfig(_rule: RuleObject, value: StoreValue) {
    if (value) {
      const confType = getAppConfType(value);
      if (!checkAppConfType(confType)) {
        return Promise.reject('The configuration file must be (.properties|.yaml|.yml |.conf)');
      } else {
        return Promise.resolve();
      }
    } else {
      return Promise.reject('Please select config');
    }
  }
  const getCreateFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'jobType',
        label: t('flink.app.developmentMode'),
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: t('flink.app.addAppTips.developmentModePlaceholder'),
            options: getJobTypeOptions(),
            onChange: (value: string) => {
              if (value === 'sql') {
                formModel.tableEnv = 1;
              } else {
                formModel.resourceFrom = 'csv';
              }
            },
          };
        },
        defaultValue: 'sql',
        rules: [
          { required: true, message: t('flink.app.addAppTips.developmentModeIsRequiredMessage') },
        ],
      },
      {
        field: 'executionMode',
        label: t('flink.app.executionMode'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.app.addAppTips.executionModePlaceholder'),
          options: executionModes,
        },
        dynamicRules: () => {
          return [
            {
              required: true,
              validator: async (_rule, value) => {
                if (value === null || value === undefined || value === '') {
                  return Promise.reject(t('flink.app.addAppTips.executionModeIsRequiredMessage'));
                } else {
                  if (
                    [
                      ExecModeEnum.YARN_PER_JOB,
                      ExecModeEnum.YARN_SESSION,
                      ExecModeEnum.YARN_APPLICATION,
                    ].includes(value)
                  ) {
                    const res = await fetchCheckHadoop();
                    if (res) {
                      return Promise.resolve();
                    } else {
                      return Promise.reject(t('flink.app.addAppTips.hadoopEnvInitMessage'));
                    }
                  }
                  return Promise.resolve();
                }
              },
            },
          ];
        },
      },
      ...getFlinkClusterSchemas.value,
      ...getFlinkSqlSchema.value,
      {
        field: 'resourceFrom',
        label: t('flink.app.resourceFrom'),
        component: 'Select',
        render: ({ model }) => renderResourceFrom(model),
        rules: [{ required: true, message: t('flink.app.addAppTips.resourceFromMessage') }],
        show: ({ values }) => values?.jobType != 'sql',
      },
      {
        field: 'uploadJobJar',
        label: t('flink.app.uploadJobJar'),
        component: 'Select',
        slot: 'uploadJobJar',
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom == 'upload',
      },
      {
        field: 'mainClass',
        label: t('flink.app.mainClass'),
        component: 'Input',
        componentProps: { placeholder: t('flink.app.addAppTips.mainClassPlaceholder') },
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom == 'upload',
        rules: [{ required: true, message: t('flink.app.addAppTips.mainClassIsRequiredMessage') }],
      },
      {
        field: 'project',
        label: t('flink.app.project'),
        component: 'Select',
        componentProps: {
          showSearch: true,
          optionFilterProp: 'children',
          filterOption,
          placeholder: t('flink.app.addAppTips.projectPlaceholder'),
          fieldNames: { label: 'name', value: 'id', options: 'options' },
          options: unref(projectList),
          onChange: (value: string) => {
            modules({
              id: value,
            }).then((res) => {
              moduleList.value = res.map((i: string) => ({ label: i, value: i }));
            });
          },
        },
        ifShow: ({ values }) => values?.jobType != 'sql' && values.resourceFrom != 'upload',
        rules: [{ required: true, message: t('flink.app.addAppTips.projectIsRequiredMessage') }],
      },
      {
        field: 'module',
        label: t('flink.app.module'),
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            showSearch: true,
            optionFilterProp: 'children',
            filterOption,
            placeholder: t('flink.app.addAppTips.projectModulePlaceholder'),
            options: unref(moduleList),
            onChange: () => {
              Object.assign(formModel, {
                appType: undefined,
                config: undefined,
                jobName: undefined,
              });
            },
          };
        },
        ifShow: ({ values }) => values?.jobType != 'sql' && values?.resourceFrom != 'upload',
        rules: [{ required: true, message: t('flink.app.addAppTips.projectIsRequiredMessage') }],
      },
      {
        field: 'appType',
        label: t('flink.app.appType'),
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: t('flink.app.addAppTips.appTypePlaceholder'),
            options: [
              { label: 'StreamPark Flink', value: String(AppTypeEnum.STREAMPARK_FLINK) },
              { label: 'Apache Flink', value: String(AppTypeEnum.APACHE_FLINK) },
            ],
            onChange: () => {
              Object.assign(formModel, {
                config: undefined,
                jobName: undefined,
                configOverride: null,
              });
              fetchListJars({
                id: formModel.project,
                module: formModel.module,
              }).then((res) => {
                jars.value = res;
              });
            },
          };
        },
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom !== 'upload',
        dynamicRules: () => [
          { required: true, message: t('flink.app.addAppTips.appTypeIsRequiredMessage') },
        ],
      },
      {
        field: 'jar',
        label: t('flink.app.programJar'),
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: t('flink.app.addAppTips.appTypePlaceholder'),
            options: unref(jars).map((i) => ({ label: i, value: i })),
            onChange: (value) => {
              fetchMain({
                projectId: formModel.project,
                module: formModel.module,
                jar: value,
              }).then((res) => {
                formModel.mainClass = res;
              });
            },
          };
        },
        ifShow: ({ values }) =>
          values?.jobType != 'sql' &&
          values?.resourceFrom != 'upload' &&
          values.appType == String(AppTypeEnum.APACHE_FLINK),
        rules: [{ required: true, message: t('flink.app.addAppTips.programJarIsRequiredMessage') }],
      },
      {
        field: 'mainClass',
        label: t('flink.app.mainClass'),
        component: 'Input',
        componentProps: { placeholder: t('flink.app.addAppTips.mainClassPlaceholder') },
        ifShow: ({ values }) =>
          values?.jobType != 'sql' &&
          values?.resourceFrom != 'upload' &&
          values.appType == String(AppTypeEnum.APACHE_FLINK),
        rules: [{ required: true, message: t('flink.app.addAppTips.mainClassIsRequiredMessage') }],
      },
      {
        field: 'config',
        label: t('flink.app.appConf'),
        component: 'ApiTreeSelect',
        componentProps: ({ formModel }) => {
          return {
            api: fetchListConf,
            params: { id: formModel.project, module: formModel.module },
            dropdownStyle: { maxHeight: '400px', overflow: 'auto' },
            placeholder: 'Please select config',
            treeDefaultExpandAll: true,
            fieldNames: { children: 'children', label: 'title', key: 'value', value: 'value' },
            onChange: (value: string) => {
              fetchName({
                config: value,
              }).then((resp) => {
                formModel.jobName = resp;
              });
            },
          };
        },
        ifShow: ({ values }) =>
          values?.jobType != 'sql' &&
          values?.resourceFrom != 'upload' &&
          values.appType == String(AppTypeEnum.STREAMPARK_FLINK),
        dynamicRules: () => [{ required: true, validator: handleCheckConfig }],
      },
      {
        field: 'useSysHadoopConf',
        label: t('flink.app.addAppTips.useSysHadoopConf'),
        component: 'Switch',
        slot: 'useSysHadoopConf',
        defaultValue: false,
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
      },
      ...getFlinkFormOtherSchemas.value,
    ];
  });

  return { flinkEnvs, flinkClusters, getCreateFormSchema, suggestions };
};
