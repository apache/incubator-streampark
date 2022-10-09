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

import Icon, { SvgIcon } from '/@/components/Icon';
import { useCreateAndEditSchema } from './useCreateAndEditSchema';
import { fetchAppConf, fetchMain } from '/@/api/flink/app/app';
import { modules, fetchListConf, fetchListJars } from '/@/api/flink/project';
import { decodeByBase64 } from '/@/utils/cipher';
import { RuleObject } from 'ant-design-vue/lib/form';
import { StoreValue } from 'ant-design-vue/lib/form/interface';
import { renderResourceFrom } from './useFlinkRender';
import { filterOption } from '../utils';
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
    getFlinkSqlSchema,
    getFlinkClusterSchemas,
    getFlinkFormOtherSchemas,
    flinkEnvs,
    flinkClusters,
    projectList,
    openConfDrawer,
  } = useCreateAndEditSchema(dependencyRef);

  async function handleEditConfig(config: string) {
    console.log('config', config);
    const res = await fetchAppConf({ config });
    const conf = decodeByBase64(res);
    openConfDrawer(true, {
      configOverride: conf,
    });
  }
  function handleCheckConfig(_rule: RuleObject, value: StoreValue) {
    if (value) {
      const isProp = value.endsWith('.properties');
      const isYaml = value.endsWith('.yaml') || value.endsWith('.yml');
      if (!isProp && !isYaml) {
        return Promise.reject('The configuration file must be (.properties|.yaml|.yml)');
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
        label: 'Development Mode',
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: 'Please select Development Mode',
            options: getJobTypeOptions(),
            onChange: (value) => {
              if (value === 'sql') {
                formModel.tableEnv = 1;
              } else {
                formModel.resourceFrom = 'csv';
              }
            },
          };
        },
        defaultValue: 'sql',
        rules: [{ required: true, message: 'Job Type is required' }],
      },
      {
        field: 'executionMode',
        label: 'Execution Mode',
        component: 'Select',
        componentProps: {
          placeholder: 'Please select Execution Mode',
          options: executionModes,
        },
        dynamicRules: () => {
          return [
            {
              required: true,
              validator: async (_rule, value) => {
                if (value === null || value === undefined || value === '') {
                  return Promise.reject('Execution Mode is required');
                } else {
                  if ([2, 3, 4].includes(value)) {
                    const res = await fetchCheckHadoop();
                    if (res) {
                      return Promise.resolve();
                    } else {
                      return Promise.reject(
                        'Hadoop environment initialization failed, please check the environment settings',
                      );
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
        label: 'Resource From',
        component: 'Select',
        render: ({ model }) => renderResourceFrom(model),
        rules: [{ required: true, message: 'resource from is required' }],
        ifShow: ({ values }) => values?.jobType != 'sql',
      },
      {
        field: 'uploadJobJar',
        label: 'Upload Job Jar',
        component: 'Select',
        slot: 'uploadJobJar',
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom == 'upload',
      },
      {
        field: 'mainClass',
        label: 'Program Main',
        component: 'Input',
        componentProps: { placeholder: 'Please enter Main class' },
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom == 'upload',
        rules: [{ required: true, message: 'Program Main is required' }],
      },
      {
        field: 'project',
        label: 'Project',
        component: 'Select',
        componentProps: {
          showSearch: true,
          optionFilterProp: 'children',
          filterOption,
          placeholder: 'Please select Project',
          fieldNames: { label: 'name', value: 'id', options: 'options' },
          options: unref(projectList),
          onChange: (value: string) => {
            modules({
              id: value,
            }).then((res) => {
              moduleList.value = res;
            });
          },
        },
        ifShow: ({ values }) => values?.jobType != 'sql' && values.resourceFrom != 'upload',
        rules: [{ required: true, message: 'Project is required' }],
      },
      {
        field: 'module',
        label: 'Module',
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            showSearch: true,
            optionFilterProp: 'children',
            filterOption,
            placeholder: 'Please select module of this project',
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
        rules: [{ required: true, message: 'Project is required' }],
      },
      {
        field: 'appType',
        label: 'Application Type',
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: 'Please select Application type',
            options: [
              { label: 'StreamPark Flink', value: 1 },
              { label: 'Apache Flink', value: 2 },
            ],
            onChange: () => {
              Object.assign(formModel, {
                config: undefined,
                jobName: undefined,
                configOverride: null,
              });
              fetchListJars({
                id: formModel.projectId,
                module: formModel.module,
              }).then((res) => {
                jars.value = res;
              });
            },
          };
        },
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom !== 'upload',
        rules: [{ required: true, message: 'Application Type is required' }],
      },
      {
        field: 'jar',
        label: 'Program Jar',
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: 'Please select Application type',
            options: unref(jars).map((i) => ({ label: i, value: i })),
            onChange: (value) => {
              fetchMain({
                projectId: formModel.projectId,
                module: formModel.module,
                jar: value,
              }).then((res) => {
                formModel.mainClass = res;
              });
            },
          };
        },
        ifShow: ({ values }) =>
          values?.jobType != 'sql' && values?.resourceFrom != 'upload' && values.appType == 2,
        rules: [{ required: true, message: 'Program Jar is required' }],
      },
      {
        field: 'mainClass',
        label: 'Program Main',
        component: 'Input',
        componentProps: { placeholder: 'Please enter Main class' },
        ifShow: ({ values }) =>
          values?.jobType != 'sql' && values?.resourceFrom != 'upload' && values.appType == 2,
        rules: [{ required: true, message: 'Program Main is required' }],
      },
      {
        field: 'config',
        label: 'Application conf',
        component: 'ApiTreeSelect',
        componentProps: ({ formModel }) => {
          const componentProps = {
            api: fetchListConf,
            beforeFetch: () => {
              return {
                id: formModel.projectId,
                module: formModel.module,
              };
            },
            dropdownStyle: { maxHeight: '400px', overflow: 'auto' },
            placeholder: 'Please select config',
            treeDefaultExpandAll: true,
          };
          if (formModel.config) {
            Object.assign(componentProps, {
              suffixIcon: h(Icon, {
                icon: 'ant-design:setting-twotone',
                twoToneColor: '#4a9ff5',
                title: 'edit config',
                onClick: handleEditConfig.bind(null, formModel.config),
              }),
            });
          }
          return componentProps;
        },
        ifShow: ({ values }) =>
          values?.jobType != 'sql' && values?.resourceFrom != 'upload' && values.appType == 1,
        dynamicRules: () => [{ required: true, validator: handleCheckConfig }],
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

  return { getCreateFormSchema, flinkEnvs, flinkClusters };
};
