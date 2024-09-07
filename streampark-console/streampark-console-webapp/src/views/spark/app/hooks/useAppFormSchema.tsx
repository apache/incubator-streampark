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
import { computed, h, onMounted, ref, unref, type Ref } from 'vue';
import type { FormSchema } from '/@/components/Form';
import { useI18n } from '/@/hooks/web/useI18n';
import { AppExistsStateEnum, JobTypeEnum } from '/@/enums/sparkEnum';
import { ResourceFromEnum } from '/@/enums/flinkEnum';
import { SvgIcon } from '/@/components/Icon';
import type { SparkEnv } from '/@/api/spark/home.type';
import type { RuleObject } from 'ant-design-vue/lib/form';
import type { StoreValue } from 'ant-design-vue/lib/form/interface';
import { renderIsSetConfig, renderStreamParkResource, renderYarnQueue } from './useSparkRender';
import { executionModes } from '../data';
import { useDrawer } from '/@/components/Drawer';
import { fetchVariableAll } from '/@/api/resource/variable';
import { fetchTeamResource } from '/@/api/resource/upload';
import { fetchCheckSparkName } from '/@/api/spark/app';

export function useSparkSchema(sparkEnvs: Ref<SparkEnv[]>) {
  const { t } = useI18n();
  const teamResource = ref<Array<any>>([]);
  const suggestions = ref<Array<{ text: string; description: string; value: string }>>([]);

  const [registerConfDrawer, { openDrawer: openConfDrawer }] = useDrawer();
  /* Detect job name field */
  async function getJobNameCheck(_rule: RuleObject, value: StoreValue, _model: Recordable) {
    if (value === null || value === undefined || value === '') {
      return Promise.reject(t('spark.app.addAppTips.appNameIsRequiredMessage'));
    }
    const params = { appName: value };
    // if (edit?.appId) {
    //   Object.assign(params, { id: edit.appId });
    // }
    const res = await fetchCheckSparkName(params);
    switch (parseInt(res)) {
      case AppExistsStateEnum.NO:
        return Promise.resolve();
      case AppExistsStateEnum.IN_DB:
        return Promise.reject(t('spark.app.addAppTips.appNameNotUniqueMessage'));
      case AppExistsStateEnum.IN_YARN:
        return Promise.reject(t('spark.app.addAppTips.appNameExistsInYarnMessage'));
      case AppExistsStateEnum.IN_KUBERNETES:
        return Promise.reject(t('spark.app.addAppTips.appNameExistsInK8sMessage'));
      default:
        return Promise.reject(t('spark.app.addAppTips.appNameValid'));
    }
  }
  const getJobTypeOptions = () => {
    return [
      {
        label: h('div', {}, [
          h(SvgIcon, { name: 'code', color: '#108ee9' }, ''),
          h('span', { class: 'pl-10px' }, 'Spark Jar'),
        ]),
        value: String(JobTypeEnum.JAR),
      },
      {
        label: h('div', {}, [
          h(SvgIcon, { name: 'fql', color: '#108ee9' }, ''),
          h('span', { class: 'pl-10px' }, 'Spark SQL'),
        ]),
        value: String(JobTypeEnum.SQL),
      },
      {
        label: h('div', {}, [
          h(SvgIcon, { name: 'py', color: '#108ee9' }, ''),
          h('span', { class: 'pl-10px' }, 'PySpark'),
        ]),
        value: String(JobTypeEnum.PYSPARK),
      },
    ];
  };

  const formSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'jobType',
        label: t('spark.app.developmentMode'),
        component: 'Select',
        componentProps: ({ formModel }) => {
          return {
            placeholder: t('spark.app.addAppTips.developmentModePlaceholder'),
            options: getJobTypeOptions(),
            onChange: (value) => {
              if (value != JobTypeEnum.SQL) {
                formModel.resourceFrom = String(ResourceFromEnum.PROJECT);
              }
            },
          };
        },
        defaultValue: String(JobTypeEnum.SQL),
        rules: [
          { required: true, message: t('spark.app.addAppTips.developmentModeIsRequiredMessage') },
        ],
      },
      {
        field: 'executionMode',
        label: t('spark.app.executionMode'),
        component: 'Select',
        itemProps: {
          autoLink: false, //Resolve multiple trigger validators with null value ·
        },
        componentProps: {
          placeholder: t('spark.app.addAppTips.executionModePlaceholder'),
          options: executionModes,
        },
        rules: [
          {
            required: true,
            validator: async (_rule, value) => {
              if (value === null || value === undefined || value === '') {
                return Promise.reject(t('spark.app.addAppTips.executionModeIsRequiredMessage'));
              } else {
                return Promise.resolve();
              }
            },
          },
        ],
      },
      {
        field: 'versionId',
        label: 'Spark Version',
        component: 'Select',
        componentProps: {
          placeholder: 'Spark Version',
          options: unref(sparkEnvs),
          fieldNames: { label: 'sparkName', value: 'id', options: 'options' },
        },
        rules: [
          { required: true, message: t('spark.app.addAppTips.flinkVersionIsRequiredMessage') },
        ],
      },
      {
        field: 'sparkSql',
        label: 'Spark SQL',
        component: 'Input',
        slot: 'sparkSql',
        ifShow: ({ values }) => values?.jobType == JobTypeEnum.SQL,
        rules: [{ required: true, message: t('spark.app.addAppTips.flinkSqlIsRequiredMessage') }],
      },
      {
        field: 'teamResource',
        label: t('spark.app.resource'),
        component: 'Select',
        render: ({ model }) => renderStreamParkResource({ model, resources: unref(teamResource) }),
        ifShow: ({ values }) => values.jobType == JobTypeEnum.JAR,
      },
      {
        field: 'mainClass',
        label: t('spark.app.mainClass'),
        component: 'Input',
        componentProps: { placeholder: t('spark.app.addAppTips.mainClassPlaceholder') },
        ifShow: ({ values }) => values?.jobType == JobTypeEnum.JAR,
        rules: [{ required: true, message: t('spark.app.addAppTips.mainClassIsRequiredMessage') }],
      },
      {
        field: 'jobName',
        label: t('spark.app.appName'),
        component: 'Input',
        componentProps: { placeholder: t('spark.app.addAppTips.appNamePlaceholder') },
        dynamicRules: ({ model }) => {
          return [
            {
              required: true,
              trigger: 'blur',
              validator: (rule: RuleObject, value: StoreValue) =>
                getJobNameCheck(rule, value, model),
            },
          ];
        },
      },
      {
        field: 'tags',
        label: t('spark.app.tags'),
        component: 'Input',
        componentProps: {
          placeholder: t('spark.app.addAppTips.tagsPlaceholder'),
        },
      },
      {
        field: 'yarnQueue',
        label: t('spark.app.yarnQueue'),
        component: 'Input',
        render: (renderCallbackParams) => renderYarnQueue(renderCallbackParams),
      },
      {
        field: 'isSetConfig',
        label: t('spark.app.appConf'),
        component: 'Switch',
        render({ model, field }) {
          return renderIsSetConfig(model, field, registerConfDrawer, openConfDrawer);
        },
      },
      {
        field: 'appProperties',
        label: 'Application Properties',
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder:
            '$key=$value,If there are multiple parameters,you can new line enter them (-D <arg>)',
        },
      },
      {
        field: 'args',
        label: t('spark.app.programArgs'),
        component: 'InputTextArea',
        defaultValue: '',
        slot: 'args',
        ifShow: ({ values }) => [JobTypeEnum.JAR, JobTypeEnum.PYSPARK].includes(values?.jobType),
      },
      {
        field: 'hadoopUser',
        label: t('spark.app.hadoopUser'),
        component: 'Input',
      },
      {
        field: 'description',
        label: t('common.description'),
        component: 'InputTextArea',
        componentProps: { rows: 4, placeholder: t('spark.app.addAppTips.descriptionPlaceholder') },
      },
    ];
  });
  onMounted(async () => {
    /* Get team dependencies */
    fetchTeamResource({}).then((res) => {
      teamResource.value = res;
    });
    fetchVariableAll().then((res) => {
      suggestions.value = res.map((v) => {
        return {
          text: v.variableCode,
          description: v.description,
          value: v.variableValue,
        };
      });
    });
  });
  return {
    formSchema,
    suggestions,
  };
}
