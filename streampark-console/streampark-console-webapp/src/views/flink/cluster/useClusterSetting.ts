import { DeployMode } from '/@/enums/flinkEnum';
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
import { RuleObject } from 'ant-design-vue/lib/form';
import { StoreValue } from 'ant-design-vue/lib/form/interface';
import { computed, onMounted, reactive, ref, unref } from 'vue';
import { k8sRestExposedType, resolveOrder } from '../app/data';
import {
  renderDynamicProperties,
  renderInputDropdown,
  renderOptionsItems,
  renderTotalMemory,
  renderYarnQueue,
} from '../app/hooks/useFlinkRender';
import { fetchCheckHadoop } from '/@/api/setting';
import { fetchListFlinkEnv } from '/@/api/flink/flinkEnv';
import { FormSchema } from '/@/components/Table';
import optionData from '../app/data/option';
import {
  fetchFlinkBaseImages,
  fetchK8sNamespaces,
  fetchSessionClusterIds,
} from '/@/api/flink/flinkHistory';
import { handleFormValue } from '../app/utils';
import { useMessage } from '/@/hooks/web/useMessage';
import { useI18n } from '/@/hooks/web/useI18n';
import { AlertSetting } from '/@/api/setting/types/alert.type';
import { fetchAlertSetting } from '/@/api/setting/alert';
import {
  fetchAvailableCloudAccounts,
  fetchManagedDraftDirectories,
  fetchManagedProjects,
  fetchManagedResourcePools,
} from '/@/api/flink/managedFlink';
import type {
  ManagedCloudAccount,
  ManagedCloudProject,
  ManagedDraftDirectory,
  ManagedFlinkEnvironment,
  ManagedResourcePool,
} from '/@/api/flink/managedFlink.type';
import { useUserStore } from '/@/store/modules/user';

export const useClusterSetting = () => {
  const { createMessage } = useMessage();
  const { t } = useI18n();
  const userStore = useUserStore();

  const submitLoading = ref(false);
  const flinkEnvs = ref<any[]>([]);
  const alerts = ref<AlertSetting[]>([]);
  const managedAccounts = ref<ManagedCloudAccount[]>([]);
  const managedProjects = ref<ManagedCloudProject[]>([]);
  const managedResourcePools = ref<ManagedResourcePool[]>([]);
  const managedDraftDirectories = ref<ManagedDraftDirectory[]>([]);
  const accountsLoading = ref(false);
  const projectsLoading = ref(false);
  const resourcePoolsLoading = ref(false);
  const draftDirectoriesLoading = ref(false);
  let projectRequestSequence = 0;
  let resourcePoolRequestSequence = 0;
  let draftDirectoryRequestSequence = 0;
  const historyRecord = reactive<{
    k8sNamespace: string[];
    k8sSessionClusterId: string[];
    serviceAccount: string[];
    k8sConf: string[];
    flinkImage: string[];
  }>({
    k8sNamespace: [],
    k8sSessionClusterId: [],
    serviceAccount: [],
    k8sConf: [],
    flinkImage: [],
  });

  const changeLoading = (loading: boolean) => {
    submitLoading.value = loading;
  };
  const getLoading = computed(() => submitLoading.value);

  /* check */
  async function handleCheckDeployMode(_rule: RuleObject, value: StoreValue) {
    if (value === null || value === undefined || value === '') {
      return Promise.reject(t('setting.flinkCluster.required.deployMode'));
    } else {
      if (value === DeployMode.YARN_SESSION) {
        try {
          const res = await fetchCheckHadoop();
          if (res) {
            return Promise.resolve();
          } else {
            return Promise.reject(
              t('setting.flinkCluster.operateMessage.hadoopEnvInitializationFailed'),
            );
          }
        } catch (error) {
          return Promise.reject(
            t('setting.flinkCluster.operateMessage.hadoopEnvInitializationFailed'),
          );
        }
      } else {
        return Promise.resolve();
      }
    }
  }

  // session mode
  function isShowInSessionMode(value: Recordable): boolean {
    return (
      value.deployMode == DeployMode.YARN_SESSION ||
      value.deployMode == DeployMode.KUBERNETES_SESSION
    );
  }

  function isManagedMode(value: Recordable): boolean {
    return value.deployMode == DeployMode.MANAGED_APPLICATION;
  }

  function requireTeamId(): string {
    const teamId = userStore.getTeamId;
    if (!teamId) {
      throw new Error('The active Team is required.');
    }
    return teamId;
  }

  async function loadManagedAccounts() {
    accountsLoading.value = true;
    try {
      managedAccounts.value = await fetchAvailableCloudAccounts(requireTeamId());
    } finally {
      accountsLoading.value = false;
    }
  }

  async function loadManagedProjects(cloudAccountId?: string) {
    const sequence = ++projectRequestSequence;
    ++resourcePoolRequestSequence;
    ++draftDirectoryRequestSequence;
    managedProjects.value = [];
    managedResourcePools.value = [];
    managedDraftDirectories.value = [];
    if (!cloudAccountId) {
      projectsLoading.value = false;
      resourcePoolsLoading.value = false;
      draftDirectoriesLoading.value = false;
      return;
    }
    projectsLoading.value = true;
    try {
      const result = await fetchManagedProjects({
        teamId: requireTeamId(),
        cloudAccountId,
      });
      if (sequence === projectRequestSequence) {
        managedProjects.value = result;
      }
    } finally {
      if (sequence === projectRequestSequence) {
        projectsLoading.value = false;
      }
    }
  }

  async function loadManagedResourcePools(cloudAccountId?: string, projectId?: string) {
    const sequence = ++resourcePoolRequestSequence;
    managedResourcePools.value = [];
    if (!cloudAccountId || !projectId) {
      resourcePoolsLoading.value = false;
      return;
    }
    resourcePoolsLoading.value = true;
    try {
      const result = await fetchManagedResourcePools({
        teamId: requireTeamId(),
        cloudAccountId,
        projectId,
      });
      if (sequence === resourcePoolRequestSequence) {
        managedResourcePools.value = result;
      }
    } finally {
      if (sequence === resourcePoolRequestSequence) {
        resourcePoolsLoading.value = false;
      }
    }
  }

  async function loadManagedDraftDirectories(cloudAccountId?: string, projectId?: string) {
    const sequence = ++draftDirectoryRequestSequence;
    managedDraftDirectories.value = [];
    if (!cloudAccountId || !projectId) {
      draftDirectoriesLoading.value = false;
      return;
    }
    draftDirectoriesLoading.value = true;
    try {
      const result = await fetchManagedDraftDirectories({
        teamId: requireTeamId(),
        cloudAccountId,
        projectId,
      });
      if (sequence === draftDirectoryRequestSequence) {
        managedDraftDirectories.value = result;
      }
    } finally {
      if (sequence === draftDirectoryRequestSequence) {
        draftDirectoriesLoading.value = false;
      }
    }
  }

  async function prepareManagedEnvironment(environment: ManagedFlinkEnvironment) {
    await loadManagedProjects(environment.cloudAccountId);
    await Promise.all([
      loadManagedResourcePools(environment.cloudAccountId, environment.projectId),
      loadManagedDraftDirectories(environment.cloudAccountId, environment.projectId),
    ]);
  }

  const getClusterSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'clusterName',
        label: t('setting.flinkCluster.form.clusterName'),
        component: 'Input',
        componentProps: {
          placeholder: t('setting.flinkCluster.placeholder.clusterName'),
        },
        required: true,
      },
      {
        field: 'deployMode',
        label: t('setting.flinkCluster.form.deployMode'),
        component: 'Select',
        componentProps: {
          placeholder: t('setting.flinkCluster.placeholder.deployMode'),
          options: [
            {
              label: 'standalone',
              value: DeployMode.STANDALONE,
            },
            { label: 'yarn session', value: DeployMode.YARN_SESSION },
            { label: 'kubernetes session', value: DeployMode.KUBERNETES_SESSION },
            {
              label: t('setting.flinkCluster.managed.deployMode'),
              value: DeployMode.MANAGED_APPLICATION,
            },
          ],
        },
        dynamicRules: () => {
          return [{ required: true, validator: handleCheckDeployMode }];
        },
      },
      {
        field: 'versionId',
        label: t('setting.flinkCluster.form.versionId'),
        component: 'Select',
        componentProps: {
          placeholder: t('setting.flinkCluster.placeholder.versionId'),
          options: unref(flinkEnvs),
          fieldNames: { label: 'flinkName', value: 'id', options: 'options' },
        },
        ifShow: ({ values }) => !isManagedMode(values),
        rules: [{ required: true, message: t('setting.flinkCluster.required.versionId') }],
      },
      {
        field: 'cloudAccountId',
        label: t('setting.flinkCluster.managed.cloudAccount'),
        component: 'Select',
        ifShow: ({ values }) => isManagedMode(values),
        componentProps: ({ formModel }) => ({
          showSearch: true,
          allowClear: true,
          loading: unref(accountsLoading),
          options: unref(managedAccounts).map((account) => ({
            label: `${account.accountName} (${account.region})`,
            value: account.id,
          })),
          onChange: async (cloudAccountId?: string) => {
            formModel.region =
              unref(managedAccounts).find((account) => account.id === cloudAccountId)?.region || '';
            formModel.projectId = undefined;
            formModel.resourcePoolId = undefined;
            formModel.draftDirectoryId = undefined;
            await loadManagedProjects(cloudAccountId);
          },
        }),
        rules: [
          {
            required: true,
            message: t('setting.flinkCluster.managed.required.cloudAccount'),
          },
        ],
      },
      {
        field: 'region',
        label: t('setting.flinkCluster.managed.region'),
        component: 'Input',
        ifShow: ({ values }) => isManagedMode(values),
        componentProps: { disabled: true },
      },
      {
        field: 'projectId',
        label: t('setting.flinkCluster.managed.project'),
        component: 'Select',
        ifShow: ({ values }) => isManagedMode(values),
        componentProps: ({ formModel }) => ({
          showSearch: true,
          allowClear: true,
          loading: unref(projectsLoading),
          disabled: !formModel.cloudAccountId,
          options: unref(managedProjects).map((project) => ({
            label: project.name,
            value: project.id,
          })),
          onChange: async (projectId?: string) => {
            formModel.resourcePoolId = undefined;
            formModel.draftDirectoryId = undefined;
            await Promise.all([
              loadManagedResourcePools(formModel.cloudAccountId, projectId),
              loadManagedDraftDirectories(formModel.cloudAccountId, projectId),
            ]);
          },
        }),
        rules: [{ required: true, message: t('setting.flinkCluster.managed.required.project') }],
      },
      {
        field: 'resourcePoolId',
        label: t('setting.flinkCluster.managed.resourcePool'),
        component: 'Select',
        ifShow: ({ values }) => isManagedMode(values),
        componentProps: ({ formModel }) => ({
          showSearch: true,
          allowClear: true,
          loading: unref(resourcePoolsLoading),
          disabled: !formModel.projectId,
          options: unref(managedResourcePools).map((pool) => ({
            label: pool.name || pool.fullName,
            value: pool.id,
          })),
        }),
        rules: [
          {
            required: true,
            message: t('setting.flinkCluster.managed.required.resourcePool'),
          },
        ],
      },
      {
        field: 'draftDirectoryId',
        label: t('setting.flinkCluster.managed.draftDirectoryId'),
        component: 'Select',
        ifShow: ({ values }) => isManagedMode(values),
        componentProps: ({ formModel }) => ({
          showSearch: true,
          allowClear: true,
          loading: unref(draftDirectoriesLoading),
          disabled: !formModel.projectId,
          placeholder: t('setting.flinkCluster.managed.placeholder.draftDirectoryId'),
          options: unref(managedDraftDirectories).map((directory) => ({
            label:
              directory.path && directory.path !== directory.name
                ? `${directory.name} (${directory.path})`
                : directory.name,
            value: directory.id,
          })),
        }),
        rules: [
          {
            required: true,
            message: t('setting.flinkCluster.managed.required.draftDirectoryId'),
          },
        ],
      },
      {
        field: 'address',
        label: 'JobManager URL',
        component: 'Input',
        componentProps: {
          placeholder: t('setting.flinkCluster.placeholder.addressRemoteMode'),
        },
        ifShow: ({ values }) => values.deployMode == DeployMode.STANDALONE,
        rules: [{ required: true, message: t('setting.flinkCluster.required.address') }],
      },
      {
        field: 'yarnQueue',
        label: t('setting.flinkCluster.form.yarnQueue'),
        component: 'Input',
        ifShow: ({ values }) => values.deployMode == DeployMode.YARN_SESSION,
        render: (renderCallbackParams) => renderYarnQueue(renderCallbackParams),
      },
      {
        field: 'alertId',
        label: t('flink.app.faultAlertTemplate'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.app.addAppTips.alertTemplatePlaceholder'),
          options: unref(alerts),
          fieldNames: { label: 'alertName', value: 'id', options: 'options' },
        },
        ifShow: ({ values }) =>
          values.deployMode == DeployMode.YARN_SESSION ||
          values.deployMode == DeployMode.STANDALONE,
      },
      {
        field: 'clusterId',
        label: t('setting.flinkCluster.form.k8sClusterId'),
        ifShow: ({ values }) => values.deployMode == DeployMode.KUBERNETES_SESSION,
        component: 'Input',
        defaultValue: unref(flinkEnvs).filter((v) => v.isDefault)[0],
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.k8sSessionClusterId,
          }),
      },
      {
        field: 'k8sNamespace',
        label: t('setting.flinkCluster.form.k8sNamespace'),
        ifShow: ({ values }) => values.deployMode == DeployMode.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.k8sNamespace,
          }),
      },
      {
        field: 'serviceAccount',
        label: t('setting.flinkCluster.form.serviceAccount'),
        ifShow: ({ values }) => values.deployMode == DeployMode.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.serviceAccount,
          }),
      },
      {
        field: 'k8sConf',
        label: t('setting.flinkCluster.form.k8sConf'),
        ifShow: ({ values }) => values.deployMode == DeployMode.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: t('setting.flinkCluster.placeholder.k8sConf'),
            options: historyRecord.k8sConf,
          }),
      },
      {
        field: 'flinkImage',
        label: t('setting.flinkCluster.form.flinkImage'),
        ifShow: ({ values }) => values.deployMode == DeployMode.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: t('setting.flinkCluster.placeholder.flinkImage'),
            options: historyRecord.flinkImage,
          }),
        rules: [{ required: true, message: t('setting.flinkCluster.required.flinkImage') }],
      },
      {
        field: 'k8sRestExposedType',
        label: t('setting.flinkCluster.form.k8sRestExposedType'),
        ifShow: ({ values }) => values.deployMode == DeployMode.KUBERNETES_SESSION,
        component: 'Select',
        componentProps: {
          placeholder: t('setting.flinkCluster.placeholder.k8sRestExposedType'),
          options: k8sRestExposedType,
        },
      },
      {
        field: 'resolveOrder',
        label: t('setting.flinkCluster.form.resolveOrder'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        componentProps: {
          placeholder: t('setting.flinkCluster.placeholder.resolveOrder'),
          options: resolveOrder,
        },
        rules: [{ message: t('setting.flinkCluster.placeholder.resolveOrder'), type: 'number' }],
      },
      {
        field: 'slot',
        label: t('setting.flinkCluster.form.taskSlots'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'InputNumber',
        componentProps: {
          placeholder: t('setting.flinkCluster.placeholder.taskSlots'),
          min: 1,
          step: 1,
          class: '!w-full',
        },
      },
      {
        field: 'totalOptions',
        label: 'Total Memory Options',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        render: (renderCallbackParams) => renderTotalMemory(renderCallbackParams),
      },
      {
        field: 'totalItem',
        label: 'totalItem',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'totalOptions', field, '.memory', true),
      },
      {
        field: 'jmOptions',
        label: t('setting.flinkCluster.form.jmOptions'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        componentProps: {
          showSearch: true,
          allowClear: true,
          mode: 'multiple',
          maxTagCount: 2,
          placeholder: t('setting.flinkCluster.placeholder.jmOptions'),
          fieldNames: { label: 'name', value: 'key', options: 'options' },
          options: optionData.filter((x) => x.group === 'jobmanager-memory'),
        },
      },
      {
        field: 'jmOptionsItem',
        label: 'jmOptionsItem',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'jmOptions', field, 'jobmanager.memory.'),
      },
      {
        field: 'tmOptions',
        label: t('setting.flinkCluster.form.tmOptions'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        componentProps: {
          showSearch: true,
          allowClear: true,
          mode: 'multiple',
          maxTagCount: 2,
          placeholder: t('setting.flinkCluster.placeholder.tmOptions'),
          fieldNames: { label: 'name', value: 'key', options: 'options' },
          options: optionData.filter((x) => x.group === 'taskmanager-memory'),
        },
      },
      {
        field: 'tmOptionsItem',
        label: 'tmOptionsItem',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'tmOptions', field, 'taskmanager.memory.'),
      },
      {
        field: 'dynamicProperties',
        label: t('setting.flinkCluster.form.dynamicProperties'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Input',
        render: (renderCallbackParams) => renderDynamicProperties(renderCallbackParams),
      },
      {
        field: 'description',
        label: t('setting.flinkCluster.form.clusterDescription'),
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder: t('setting.flinkCluster.placeholder.clusterDescription'),
        },
      },
    ];
  });
  function handleSubmitParams(values: Recordable) {
    const options = handleFormValue(values);
    const params = {
      clusterName: values.clusterName,
      deployMode: values.deployMode,
      versionId: values.versionId,
      description: values.description,
      alertId: values.alertId,
    };

    switch (values.deployMode) {
      case DeployMode.STANDALONE:
        Object.assign(params, {
          address: values.address,
        });
        return params;
      case DeployMode.YARN_SESSION:
        Object.assign(params, {
          options: JSON.stringify(options),
          yarnQueue: values.yarnQueue || 'default',
          dynamicProperties: values.dynamicProperties,
          resolveOrder: values.resolveOrder,
        });
        return params;
      case DeployMode.KUBERNETES_SESSION:
        Object.assign(params, {
          clusterId: values.clusterId,
          options: JSON.stringify(options),
          dynamicProperties: values.dynamicProperties,
          resolveOrder: values.resolveOrder,
          k8sRestExposedType: values.k8sRestExposedType,
          k8sNamespace: values.k8sNamespace || null,
          serviceAccount: values.serviceAccount,
          k8sConf: values.k8sConf,
          flinkImage: values.flinkImage || null,
          address: values.address,
        });
        return params;
      case DeployMode.MANAGED_APPLICATION: {
        const project = unref(managedProjects).find((item) => item.id === values.projectId);
        const resourcePool = unref(managedResourcePools).find(
          (item) => item.id === values.resourcePoolId,
        );
        return {
          teamId: requireTeamId(),
          clusterName: values.clusterName,
          description: values.description,
          cloudAccountId: values.cloudAccountId,
          projectId: values.projectId,
          projectName: project?.name,
          resourcePoolId: values.resourcePoolId,
          resourcePoolName: resourcePool?.name || resourcePool?.fullName,
          draftDirectoryId: values.draftDirectoryId,
        };
      }
      default:
        createMessage.error('error deployMode.');
        return {};
    }
  }
  onMounted(() => {
    loadManagedAccounts();
    fetchListFlinkEnv().then((res) => {
      flinkEnvs.value = res;
    });
    fetchAlertSetting().then((res) => {
      alerts.value = res;
    });
    fetchK8sNamespaces().then((res) => {
      historyRecord.k8sNamespace = res;
    });
    fetchSessionClusterIds({
      deployMode: DeployMode.KUBERNETES_SESSION,
    }).then((res) => {
      historyRecord.k8sSessionClusterId = res;
    });
    fetchFlinkBaseImages().then((res) => {
      historyRecord.flinkImage = res;
    });
  });
  return {
    getClusterSchema,
    handleSubmitParams,
    prepareManagedEnvironment,
    changeLoading,
    getLoading,
  };
};
