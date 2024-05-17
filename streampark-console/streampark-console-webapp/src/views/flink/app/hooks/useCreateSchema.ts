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
import { AppTypeEnum, ConfigTypeEnum, ExecModeEnum } from '/@/enums/flinkEnum';

import Icon, { SvgIcon } from '/@/components/Icon';
import { useCreateAndEditSchema } from './useCreateAndEditSchema';
import { fetchMain, fetchName } from '/@/api/flink/app/app';
import { modules, fetchListConf, fetchListJars } from '/@/api/flink/project';
import { RuleObject } from 'ant-design-vue/lib/form';
import { StoreValue } from 'ant-design-vue/lib/form/interface';
import { renderResourceFrom, renderYarnQueue } from './useFlinkRender';
import { filterOption, getAppConfType } from '../utils';
import { useI18n } from '/@/hooks/web/useI18n';
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

export const useCreateSchema = (dependencyRef: Ref, isDisabled? : boolean) => {
  const moduleList = ref<Array<{ name: string }>>([]);
  const jars = ref<Array<any>>([]);
  const {
    flinkEnvs,
    flinkClusters,
    projectList,
    getFlinkSqlSchema,
    getFlinkSqlOtherSchema,
    getFlinkClusterSchemas,
    getExecutionModeSchema,
    getFlinkFormOtherSchemas,
    getFlinkFormAttrOtherSchemas,
    getFlinkNameDetailSchema,
    suggestions,
    getFlinkFormConfigSchemas,
    // getFlinkTypeSchema
  } = useCreateAndEditSchema(dependencyRef, isDisabled);
  function handleCheckConfig(_rule: RuleObject, value: StoreValue) {
    if (value) {
      const confType = getAppConfType(value);
      if (confType === ConfigTypeEnum.UNKNOWN) {
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
                formModel.resourceFrom = 'cvs';
              }
            },
          };
        },
        defaultValue: 'sql',
        rules: [
          { required: true, message: t('flink.app.addAppTips.developmentModeIsRequiredMessage') },
        ],
      },
      ...getExecutionModeSchema.value,
      ...getFlinkClusterSchemas.value,
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
      ...getFlinkSqlSchema.value,
      {
        field: 'project',
        label: t('flink.app.project'),
        component: 'Select',
        componentProps: {
          showSearch: true,
          optionFilterProp: 'children',
          filterOption: (input: string, options: Recordable) => {
            return options.name.toLowerCase().indexOf(input.toLowerCase()) >= 0;
          },
          placeholder: t('flink.app.addAppTips.projectPlaceholder'),
          fieldNames: { label: 'name', value: 'id', options: 'options' },
          options: unref(projectList),
          onChange: (value: string) => {
            // When a valid value is entered, the module api needs to be sent
            if (value) {
              modules({
                id: value,
              }).then((res) => {
                moduleList.value = res.map((i: string) => ({ label: i, value: i }));
              });
            }
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
        field: 'dependency',
        label: t('flink.app.dependency'),
        component: 'Input',
        slot: 'dependency',
        ifShow: ({ values }) => {
          console.log("scheamValue", values)
          return values?.jobType == 'sql' ? true : values?.appType == AppTypeEnum.APACHE_FLINK;
        },
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

  // new Add@wangyao
  const getFirstCreateFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'jobType',
        label: t('flink.app.developmentMode'),
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: t('flink.app.addAppTips.developmentModePlaceholder'),
            options: getJobTypeOptions(),
            disabled: !!isDisabled ? isDisabled : false,
            onChange: (value: string) => {
              if (value === 'sql') {
                formModel.tableEnv = 1;
              } else {
                formModel.resourceFrom = 'cvs';
              }
            },
          };
        },
        defaultValue: 'sql',
        rules: [
          { required: true, message: t('flink.app.addAppTips.developmentModeIsRequiredMessage') },
        ],
      },
      ...getExecutionModeSchema.value, // 运行模式 执行模式
      ...getFlinkClusterSchemas.value, // flink版本
      ...getFlinkNameDetailSchema.value,
      {
        field: 'yarnQueue', // yarn队列 执行模式yarn
        label: t('flink.app.yarnQueue'),
        component: 'Input',
        ifShow: ({ values }) =>
          values.executionMode == ExecModeEnum.YARN_APPLICATION ||
          values.executionMode == ExecModeEnum.YARN_PER_JOB,
        render: (renderCallbackParams) => renderYarnQueue(renderCallbackParams),
      },
      
    ]
  })
  const getAttrCreateFormSchema = computed((): FormSchema[] => {
    return [
      ...getFirstCreateFormSchema.value,
      ...getFlinkSqlOtherSchema.value,
      {
        field: 'podTemplate', // Kubernetes Pod模板 执行模式为kubernetes application
        label: t('flink.app.podTemplate'),
        component: 'Input',
        slot: 'podTemplate',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
      },
      {
        field: 'dependency', // 作业依赖
        label: t('flink.app.dependency'),
        component: 'Input',
        slot: 'dependency',
        ifShow: ({ values }) => {
          return values?.jobType == 'sql' ? true : values?.appType == AppTypeEnum.APACHE_FLINK;
        },
      },
      {
        field: 'useSysHadoopConf', // 使用系统Hadoop配置
        label: t('flink.app.addAppTips.useSysHadoopConf'),
        component: 'Switch',
        slot: 'useSysHadoopConf',
        defaultValue: false,
        ifShow: ({ values }) => {
          console.log("hadoop", values)
          return values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION
        },
      },
      ...getFlinkFormAttrOtherSchemas.value,
    ]
  })
  // sql main
  const getMainCreateFormSchema = computed((): FormSchema[] => {
    return [
      // {
      //   field: 'jobType',
      //   label: t('flink.app.developmentMode'),
      //   component: 'Select',
      //   componentProps: ({ formModel }) => {
      //     return {
      //       placeholder: t('flink.app.addAppTips.developmentModePlaceholder'),
      //       options: getJobTypeOptions(),
      //       disabled: !!isDisabled ? isDisabled : false,
      //       onChange: (value: string) => {
      //         if (value === 'sql') {
      //           formModel.tableEnv = 1;
      //         } else {
      //           formModel.resourceFrom = 'cvs';
      //         }
      //       },
      //     };
      //   },
      //   defaultValue: 'sql',
      //   rules: [
      //     { required: true, message: t('flink.app.addAppTips.developmentModeIsRequiredMessage') },
      //   ],
      // },
      // ...getFlinkTypeSchema.value,
      ...getFlinkSqlSchema.value,
    ]
  })
  // custom code main
  const getCustomCreateFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'resourceFrom', // 资源来源
        label: t('flink.app.resourceFrom'),
        component: 'Select',
        render: ({ model }) => renderResourceFrom(model),
        rules: [{ required: true, message: t('flink.app.addAppTips.resourceFromMessage') }],
        show: ({ values }) => values?.jobType != 'sql',
      },
      {
        field: 'project', // 资源来源是CVS,项目
        label: t('flink.app.project'),
        component: 'Select',
        componentProps: {
          showSearch: true,
          optionFilterProp: 'children',
          filterOption: (input: string, options: Recordable) => {
            return options.name.toLowerCase().indexOf(input.toLowerCase()) >= 0;
          },
          placeholder: t('flink.app.addAppTips.projectPlaceholder'),
          fieldNames: { label: 'name', value: 'id', options: 'options' },
          options: unref(projectList),
          onChange: (value: string) => {
            // When a valid value is entered, the module api needs to be sent
            if (value) {
              modules({
                id: value,
              }).then((res) => {
                moduleList.value = res.map((i: string) => ({ label: i, value: i }));
              });
            }
          },
        },
        ifShow: ({ values }) => values?.jobType != 'sql' && values.resourceFrom != 'upload',
        rules: [{ required: true, message: t('flink.app.addAppTips.projectIsRequiredMessage') }],
      },
      {
        field: 'module', // 模块
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
        field: 'appType', // 作业类型
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
        field: 'jar', // 程序jar文件
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
        field: 'mainClass', // 程序入口类
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
        field: 'uploadJobJar', // 上传jar文件
        label: t('flink.app.uploadJobJar'),
        component: 'Select',
        slot: 'uploadJobJar',
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom == 'upload',
      },
      {
        field: 'mainClass', // 程序入口类
        label: t('flink.app.mainClass'),
        component: 'Input',
        componentProps: { placeholder: t('flink.app.addAppTips.mainClassPlaceholder') },
        ifShow: ({ values }) => values?.jobType !== 'sql' && values?.resourceFrom == 'upload',
        rules: [{ required: true, message: t('flink.app.addAppTips.mainClassIsRequiredMessage') }],
      },
      {
        field: 'config', // 作业配置
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
        field: 'args',
        label: t('flink.app.programArgs'),
        component: 'InputTextArea',
        defaultValue: '',
        slot: 'args',
        // ifShow: ({ values }) => (edit?.mode ? true : values.jobType == 'customcode'),
      },
    ]
  })
  // configuration drawer
  const getConfigFormSchema = computed((): FormSchema[] => {
    return [
      ...getFlinkFormConfigSchemas.value
    ]
  })
  return { flinkEnvs, flinkClusters, getCreateFormSchema, suggestions, getFirstCreateFormSchema, getAttrCreateFormSchema, getMainCreateFormSchema, getCustomCreateFormSchema, getConfigFormSchema };
};
