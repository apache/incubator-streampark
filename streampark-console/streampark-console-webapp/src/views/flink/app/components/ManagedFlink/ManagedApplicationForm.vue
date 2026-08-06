<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<script setup lang="ts">
  import { computed, h, nextTick, onMounted, ref, unref } from 'vue';
  import { Divider, Select } from 'ant-design-vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { SvgIcon } from '/@/components/Icon';
  import type { FormSchema } from '/@/components/Form';
  import {
    fetchCreateManagedApplication,
    fetchManagedApplication,
    fetchManagedEnvironments,
    fetchUpdateManagedApplication,
  } from '/@/api/flink/managedFlink';
  import type {
    ManagedFlinkApplication,
    ManagedFlinkApplicationForm,
    ManagedFlinkCapability,
    ManagedFlinkEnvironment,
  } from '/@/api/flink/managedFlink.type';
  import { fetchTeamResource } from '/@/api/resource/upload';
  import type { ResourceListRecord } from '/@/api/resource/upload/model/resourceModel';
  import { EngineTypeEnum, ResourceTypeEnum } from '/@/views/resource/upload/upload.data';
  import { useUserStore } from '/@/store/modules/user';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useGo } from '/@/hooks/web/usePage';
  import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';

  const FlinkSqlEditor = createAsyncComponent(() => import('../FlinkSql.vue'), {
    loading: true,
  });
  const ManagedDependencyUpload = createAsyncComponent(
    () => import('./ManagedDependencyUpload.vue'),
    { loading: true },
  );
  const SelectOption = Select.Option;

  const props = defineProps<{
    appId?: string;
  }>();

  const { t } = useI18n();
  const { createMessage } = useMessage();
  const userStore = useUserStore();
  const go = useGo();
  const submitting = ref(false);
  const loadingMetadata = ref(false);
  const environments = ref<ManagedFlinkEnvironment[]>([]);
  const resources = ref<ResourceListRecord[]>([]);
  const selectedEnvironment = ref<ManagedFlinkEnvironment>();
  const capability = ref<ManagedFlinkCapability>();
  const application = ref<ManagedFlinkApplication>();

  const [registerForm, { getFieldsValue, setFieldsValue, submit }] = useForm({
    name: 'managed_flink_application',
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    baseColProps: { span: 24 },
    colon: true,
    showActionButtonGroup: false,
  });

  function requireTeamId(): string {
    const teamId = userStore.getTeamId;
    if (!teamId) {
      throw new Error(t('flink.app.managed.activeTeamRequired'));
    }
    return teamId;
  }

  function parseCapability(
    environment?: ManagedFlinkEnvironment,
  ): ManagedFlinkCapability | undefined {
    if (!environment?.capabilityJson) return undefined;
    try {
      const parsed = JSON.parse(environment.capabilityJson) as ManagedFlinkCapability;
      if (
        !Array.isArray(parsed.engineVersions) ||
        !Array.isArray(parsed.jobTypes) ||
        !parsed.minCpu ||
        !parsed.cpuStep ||
        !parsed.memoryPerCpuGiB
      ) {
        return undefined;
      }
      return parsed;
    } catch {
      return undefined;
    }
  }

  function environmentOptions() {
    return unref(environments).map((environment) => ({
      label: `${environment.clusterName} · ${environment.projectName || environment.projectId} · ${
        environment.resourcePoolName || environment.resourcePoolId
      }`,
      value: environment.clusterId,
      disabled: !environment.lastProbeTime || Boolean(environment.lastProbeError),
    }));
  }

  function applicationResourceOptions() {
    return unref(resources)
      .filter(
        (resource) =>
          resource.resourceType === ResourceTypeEnum.APP &&
          resource.engineType === EngineTypeEnum.FLINK,
      )
      .map((resource) => ({ label: resource.resourceName, value: resource.resourceName }));
  }

  async function reloadResources() {
    resources.value = await fetchTeamResource({});
  }

  function numberValue(value: unknown, fallback = 0): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  function minimumMemory(cpu: unknown): number {
    return numberValue(cpu) * numberValue(unref(capability)?.memoryPerCpuGiB, 4);
  }

  function estimateCu(values: Recordable): number {
    const parallelism = Math.max(1, numberValue(values.parallelism, 1));
    const slots = Math.max(1, numberValue(values.taskManagerSlots, 1));
    const taskManagerCount = Math.ceil(parallelism / slots);
    const totalCpu =
      numberValue(values.taskManagerCpu) * taskManagerCount + numberValue(values.jobManagerCpu);
    const totalMemory =
      numberValue(values.taskManagerMemoryGiB) * taskManagerCount +
      numberValue(values.jobManagerMemoryGiB);
    return Math.max(totalCpu, totalMemory / numberValue(unref(capability)?.memoryPerCpuGiB, 4));
  }

  function defaultEngineVersion(engineVersions: string[]): string | undefined {
    return engineVersions.includes('FLINK_VERSION_1_17') ? 'FLINK_VERSION_1_17' : engineVersions[0];
  }

  async function updateEstimate() {
    const values = getFieldsValue() as Recordable;
    await setFieldsValue({ estimatedCu: estimateCu(values) });
  }

  async function selectEnvironment(environmentId?: string, resetCapabilityValues = true) {
    const environment = unref(environments).find((item) => item.clusterId == environmentId);
    selectedEnvironment.value = environment;
    capability.value = parseCapability(environment);
    if (!resetCapabilityValues || !unref(capability)) return;
    const currentCapability = unref(capability)!;
    const minCpu = numberValue(currentCapability.minCpu, 0.5);
    const minMemory = minCpu * numberValue(currentCapability.memoryPerCpuGiB, 4);
    await setFieldsValue({
      engineVersion: defaultEngineVersion(currentCapability.engineVersions),
      jobType: currentCapability.jobTypes[0],
      schedulingStrategy: currentCapability.schedulingStrategies[0] || 'DEFAULT',
      taskManagerCpu: minCpu,
      taskManagerMemoryGiB: minMemory,
      jobManagerCpu: minCpu,
      jobManagerMemoryGiB: minMemory,
    });
    await updateEstimate();
  }

  function validateJobName(_rule: unknown, value?: string) {
    if (!value) return Promise.reject(t('flink.app.managed.required.jobName'));
    if (value.includes(' ')) return Promise.reject(t('flink.app.managed.invalidJobName'));
    return Promise.resolve();
  }

  function validateMemory(name: string, cpuField: string) {
    return async (_rule: unknown, value: unknown) => {
      const minimum = minimumMemory((getFieldsValue() as Recordable)[cpuField]);
      if (numberValue(value) < minimum) {
        return Promise.reject(t('flink.app.managed.minimumMemory', { name, minimum }));
      }
      return Promise.resolve();
    };
  }

  function validateCpu(_rule: unknown, value: unknown) {
    const currentCapability = unref(capability);
    const minimum = numberValue(currentCapability?.minCpu, 0.5);
    const step = numberValue(currentCapability?.cpuStep, 0.5);
    const cpu = numberValue(value);
    const quotient = cpu / step;
    if (cpu < minimum || Math.abs(quotient - Math.round(quotient)) > 1e-8) {
      return Promise.reject(t('flink.app.managed.cpuIncrement', { minimum, step }));
    }
    return Promise.resolve();
  }

  function validateCheckpointTimeout(_rule: unknown, value: unknown) {
    const values = getFieldsValue() as Recordable;
    if (numberValue(value) < numberValue(values.checkpointIntervalMs)) {
      return Promise.reject(t('flink.app.managed.checkpointTimeoutInvalid'));
    }
    return Promise.resolve();
  }

  function validatePositiveNumber(label: string) {
    return (_rule: unknown, value: unknown) => {
      if (value === undefined || value === null || value === '' || numberValue(value) < 1) {
        return Promise.reject(t('flink.app.managed.positiveNumberRequired', { label }));
      }
      return Promise.resolve();
    };
  }

  async function applyRestartStrategyDefaults(strategy: string) {
    if (strategy !== 'FIXED_DELAY') return;
    const values = getFieldsValue() as Recordable;
    await setFieldsValue({
      restartAttempts: values.restartAttempts || 10,
      restartDelaySeconds: values.restartDelaySeconds || 20,
    });
  }

  const formSchemas = computed<FormSchema[]>(() => {
    const currentCapability = unref(capability);
    const minCpu = numberValue(currentCapability?.minCpu, 0.5);
    const cpuStep = numberValue(currentCapability?.cpuStep, 0.5);
    return [
      {
        field: 'managedEnvironmentId',
        label: t('flink.app.managed.environment'),
        component: 'Select',
        componentProps: {
          loading: unref(loadingMetadata),
          showSearch: true,
          optionFilterProp: 'label',
          options: environmentOptions(),
          onChange: (value?: string) => selectEnvironment(value),
        },
        rules: [{ required: true, message: t('flink.app.managed.required.environment') }],
      },
      {
        field: 'jobType',
        label: t('flink.app.jobType'),
        component: 'Input',
        slot: 'jobType',
        rules: [{ required: true, message: t('flink.app.managed.required.jobType') }],
      },
      {
        field: 'engineVersion',
        label: t('flink.app.managed.engineVersion'),
        component: 'Select',
        componentProps: {
          disabled: !currentCapability,
          options: (currentCapability?.engineVersions || []).map((version) => ({
            label: version.replace('FLINK_VERSION_', 'Flink ').replaceAll('_', '.'),
            value: version,
          })),
        },
        rules: [{ required: true, message: t('flink.app.managed.required.engineVersion') }],
      },
      {
        field: 'jobName',
        label: t('flink.app.appName'),
        component: 'Input',
        componentProps: { maxlength: 255 },
        dynamicRules: () => [{ required: true, trigger: 'blur', validator: validateJobName }],
      },
      {
        field: 'sql',
        label: 'Flink SQL',
        component: 'Input',
        slot: 'sql',
        ifShow: ({ values }) => values.jobType === 'STREAMING_SQL',
        rules: [{ required: true, message: t('flink.app.managed.required.sql') }],
      },
      {
        field: 'jar',
        label: t('flink.app.managed.jar'),
        component: 'Select',
        componentProps: ({ formModel }) => ({
          showSearch: true,
          optionFilterProp: 'label',
          options: applicationResourceOptions(),
          onChange: (resourceName?: string) => {
            formModel.mainClass =
              unref(resources).find((resource) => resource.resourceName === resourceName)
                ?.mainClass || '';
          },
        }),
        ifShow: ({ values }) => values.jobType === 'STREAMING_JAR',
        rules: [{ required: true, message: t('flink.app.managed.required.jar') }],
      },
      {
        field: 'mainClass',
        label: t('flink.app.mainClass'),
        component: 'Input',
        ifShow: ({ values }) => values.jobType === 'STREAMING_JAR',
        rules: [{ required: true, message: t('flink.app.managed.required.mainClass') }],
      },
      {
        field: 'dependencyResourceNames',
        label: t('flink.app.dependency'),
        component: 'Input',
        slot: 'dependency',
        ifShow: ({ values }) =>
          values.jobType === 'STREAMING_SQL' || values.jobType === 'STREAMING_JAR',
      },
      {
        field: 'resourceDivider',
        label: '',
        component: 'Divider',
        render: () =>
          h(Divider, { orientation: 'left', plain: true }, () =>
            t('flink.app.managed.resourceConfig'),
          ),
      },
      {
        field: 'parallelism',
        label: t('flink.app.parallelism'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        rules: [{ required: true, type: 'number', min: 1 }],
      },
      {
        field: 'taskManagerSlots',
        label: t('flink.app.managed.taskManagerSlots'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        rules: [{ required: true, type: 'number', min: 1 }],
      },
      {
        field: 'taskManagerCpu',
        label: t('flink.app.managed.taskManagerCpu'),
        component: 'InputNumber',
        componentProps: { min: minCpu, step: cpuStep, class: '!w-full' },
        dynamicRules: () => [{ required: true, validator: validateCpu }],
      },
      {
        field: 'taskManagerMemoryGiB',
        label: t('flink.app.managed.taskManagerMemory'),
        component: 'InputNumber',
        componentProps: ({ formModel }) => ({
          min: minimumMemory(formModel.taskManagerCpu),
          step: 1,
          class: '!w-full',
        }),
        dynamicRules: () => [
          {
            required: true,
            validator: validateMemory(t('flink.app.managed.taskManagerMemory'), 'taskManagerCpu'),
          },
        ],
      },
      {
        field: 'jobManagerCpu',
        label: t('flink.app.managed.jobManagerCpu'),
        component: 'InputNumber',
        componentProps: { min: minCpu, step: cpuStep, class: '!w-full' },
        dynamicRules: () => [{ required: true, validator: validateCpu }],
      },
      {
        field: 'jobManagerMemoryGiB',
        label: t('flink.app.managed.jobManagerMemory'),
        component: 'InputNumber',
        componentProps: ({ formModel }) => ({
          min: minimumMemory(formModel.jobManagerCpu),
          step: 1,
          class: '!w-full',
        }),
        dynamicRules: () => [
          {
            required: true,
            validator: validateMemory(t('flink.app.managed.jobManagerMemory'), 'jobManagerCpu'),
          },
        ],
      },
      {
        field: 'estimatedCu',
        label: t('flink.app.managed.estimatedCu'),
        component: 'InputNumber',
        componentProps: { disabled: true, class: '!w-full' },
      },
      {
        field: 'checkpointDivider',
        label: '',
        component: 'Divider',
        render: () =>
          h(Divider, { orientation: 'left', plain: true }, () =>
            t('flink.app.managed.checkpointConfig'),
          ),
      },
      {
        field: 'checkpointEnabled',
        label: t('flink.app.managed.checkpointEnabled'),
        component: 'Switch',
      },
      {
        field: 'checkpointIntervalMs',
        label: t('flink.app.managed.checkpointInterval'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1000, class: '!w-full' },
        ifShow: ({ values }) => Boolean(values.checkpointEnabled),
        rules: [
          {
            required: true,
            validator: validatePositiveNumber(t('flink.app.managed.checkpointInterval')),
          },
        ],
      },
      {
        field: 'checkpointTimeoutMs',
        label: t('flink.app.managed.checkpointTimeout'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1000, class: '!w-full' },
        ifShow: ({ values }) => Boolean(values.checkpointEnabled),
        dynamicRules: () => [
          { required: true, validator: validateCheckpointTimeout, trigger: 'blur' },
        ],
      },
      {
        field: 'stateTtlMs',
        label: t('flink.app.managed.stateTtl'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1000, class: '!w-full' },
        ifShow: ({ values }) => Boolean(values.checkpointEnabled),
      },
      {
        field: 'checkpointBackend',
        label: t('flink.app.managed.checkpointBackend'),
        component: 'Input',
        ifShow: ({ values }) => Boolean(values.checkpointEnabled),
      },
      {
        field: 'restartDivider',
        label: '',
        component: 'Divider',
        render: () =>
          h(Divider, { orientation: 'left', plain: true }, () =>
            t('flink.app.managed.restartStrategy'),
          ),
      },
      {
        field: 'restartStrategyType',
        label: t('flink.app.managed.restartStrategy'),
        component: 'Select',
        componentProps: {
          options: [
            { label: t('flink.app.managed.restartNone'), value: 'NONE' },
            { label: t('flink.app.managed.restartFixedDelay'), value: 'FIXED_DELAY' },
            { label: t('flink.app.managed.restartFailureRate'), value: 'FAILURE_RATE' },
            { label: t('flink.app.managed.restartExponentialDelay'), value: 'EXPONENTIAL_DELAY' },
          ],
          onChange: applyRestartStrategyDefaults,
        },
        rules: [{ required: true, message: t('flink.app.managed.required.restartStrategy') }],
      },
      {
        field: 'restartAttempts',
        label: t('flink.app.managed.restartAttempts'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'FIXED_DELAY',
        rules: [{ required: true, type: 'number', min: 1 }],
      },
      {
        field: 'restartDelaySeconds',
        label: t('flink.app.managed.restartDelay'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        ifShow: ({ values }) =>
          ['FIXED_DELAY', 'FAILURE_RATE'].includes(values.restartStrategyType),
        rules: [{ required: true, type: 'number', min: 1 }],
      },
      {
        field: 'maxFailuresPerInterval',
        label: t('flink.app.managed.maxFailuresPerInterval'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'FAILURE_RATE',
        rules: [{ required: true, type: 'number', min: 1 }],
      },
      {
        field: 'failureRateIntervalSeconds',
        label: t('flink.app.managed.failureRateInterval'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'FAILURE_RATE',
        rules: [{ required: true, type: 'number', min: 1 }],
      },
      {
        field: 'exponentialInitialBackoffSeconds',
        label: t('flink.app.managed.initialBackoff'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'EXPONENTIAL_DELAY',
      },
      {
        field: 'exponentialMaxBackoffSeconds',
        label: t('flink.app.managed.maxBackoff'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'EXPONENTIAL_DELAY',
      },
      {
        field: 'exponentialBackoffMultiplier',
        label: t('flink.app.managed.backoffMultiplier'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 0.1, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'EXPONENTIAL_DELAY',
      },
      {
        field: 'exponentialResetThresholdSeconds',
        label: t('flink.app.managed.resetBackoffThreshold'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'EXPONENTIAL_DELAY',
      },
      {
        field: 'exponentialJitterFactor',
        label: t('flink.app.managed.jitterFactor'),
        component: 'InputNumber',
        componentProps: { min: 0, max: 1, step: 0.1, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'EXPONENTIAL_DELAY',
      },
      {
        field: 'exponentialAttemptsBeforeReset',
        label: t('flink.app.managed.attemptsBeforeReset'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        ifShow: ({ values }) => values.restartStrategyType === 'EXPONENTIAL_DELAY',
      },
      {
        field: 'retryOnFailure',
        label: t('flink.app.managed.retryOnFailure'),
        component: 'Switch',
      },
      {
        field: 'retryIntervalMin',
        label: t('flink.app.managed.retryIntervalMin'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        ifShow: ({ values }) => Boolean(values.retryOnFailure),
      },
      {
        field: 'retryMaxCount',
        label: t('flink.app.managed.retryMaxCount'),
        component: 'InputNumber',
        componentProps: { min: 1, step: 1, precision: 0, class: '!w-full' },
        ifShow: ({ values }) => Boolean(values.retryOnFailure),
      },
      {
        field: 'releaseDivider',
        label: '',
        component: 'Divider',
        render: () =>
          h(Divider, { orientation: 'left', plain: true }, () =>
            t('flink.app.managed.releaseConfig'),
          ),
      },
      {
        field: 'priority',
        label: t('flink.app.managed.priority'),
        component: 'InputNumber',
        componentProps: { min: 1, max: 100, step: 1, precision: 0, class: '!w-full' },
      },
      {
        field: 'schedulingStrategy',
        label: t('flink.app.managed.schedulingStrategy'),
        component: 'Select',
        componentProps: {
          options: (currentCapability?.schedulingStrategies || ['DEFAULT']).map((strategy) => ({
            label: strategy,
            value: strategy,
          })),
        },
        rules: [{ required: true, message: t('flink.app.managed.required.schedulingStrategy') }],
      },
      {
        field: 'runtimeCustomProperties',
        label: t('flink.app.managed.runtimeCustomProperties'),
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder: t('flink.app.managed.customPropertiesPlaceholder'),
        },
      },
      {
        field: 'releaseCustomProperties',
        label: t('flink.app.managed.releaseCustomProperties'),
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder: t('flink.app.managed.customPropertiesPlaceholder'),
        },
      },
      {
        field: 'args',
        label: t('flink.app.programArgs'),
        component: 'InputTextArea',
        componentProps: { rows: 3 },
      },
      {
        field: 'description',
        label: t('common.description'),
        component: 'InputTextArea',
        componentProps: { rows: 4 },
      },
    ];
  });

  function parseProperties(value: unknown): Record<string, string> {
    const result: Record<string, string> = {};
    String(value || '')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#'))
      .forEach((line) => {
        const separator = line.indexOf('=');
        if (separator <= 0) {
          throw new Error(t('flink.app.managed.invalidCustomProperty', { line }));
        }
        result[line.slice(0, separator).trim()] = line.slice(separator + 1).trim();
      });
    return result;
  }

  function stringifyProperties(properties?: Record<string, string>): string {
    return Object.entries(properties || {})
      .map(([key, value]) => `${key}=${value}`)
      .join('\n');
  }

  function durationSeconds(value?: string): number | undefined {
    if (!value) return undefined;
    const matched = value.match(/^([0-9]+(?:\.[0-9]+)?)(?:s|second)$/);
    return matched ? Number(matched[1]) : undefined;
  }

  function restartParameters(values: Recordable): Record<string, string> {
    if (values.restartStrategyType === 'FIXED_DELAY') {
      return {
        'restart-strategy.fixed-delay.attempts': String(values.restartAttempts),
        'restart-strategy.fixed-delay.delay': `${values.restartDelaySeconds}second`,
      };
    }
    if (values.restartStrategyType === 'FAILURE_RATE') {
      return {
        'restart-strategy.failure-rate.max-failures-per-interval': String(
          values.maxFailuresPerInterval,
        ),
        'restart-strategy.failure-rate.failure-rate-interval': `${values.failureRateIntervalSeconds}second`,
        'restart-strategy.failure-rate.delay': `${values.restartDelaySeconds}second`,
      };
    }
    if (values.restartStrategyType === 'EXPONENTIAL_DELAY') {
      return {
        'restart-strategy.exponential-delay.initial-backoff': `${values.exponentialInitialBackoffSeconds}s`,
        'restart-strategy.exponential-delay.max-backoff': `${values.exponentialMaxBackoffSeconds}s`,
        'restart-strategy.exponential-delay.backoff-multiplier': String(
          values.exponentialBackoffMultiplier,
        ),
        'restart-strategy.exponential-delay.reset-backoff-threshold': `${values.exponentialResetThresholdSeconds}s`,
        'restart-strategy.exponential-delay.jitter-factor': String(values.exponentialJitterFactor),
        'restart-strategy.exponential-delay.attempts-before-reset-backoff': String(
          values.exponentialAttemptsBeforeReset,
        ),
      };
    }
    return {};
  }

  function buildRequest(values: Recordable): ManagedFlinkApplicationForm {
    const environment = unref(selectedEnvironment);
    if (!environment || !unref(capability)) {
      throw new Error(t('flink.app.managed.capabilityUnavailable'));
    }
    const request: ManagedFlinkApplicationForm = {
      teamId: requireTeamId(),
      jobName: values.jobName,
      description: values.description,
      managedEnvironmentId: values.managedEnvironmentId,
      jobType: values.jobType,
      args: values.args,
      runtimeConfig: {
        engineVersion: values.engineVersion,
        executionMode: 'STREAMING',
        resource: {
          parallelism: values.parallelism,
          taskManagerCpu: values.taskManagerCpu,
          taskManagerMemoryGiB: values.taskManagerMemoryGiB,
          taskManagerSlots: values.taskManagerSlots,
          jobManagerCpu: values.jobManagerCpu,
          jobManagerMemoryGiB: values.jobManagerMemoryGiB,
        },
        checkpoint: {
          enabled: Boolean(values.checkpointEnabled),
          intervalMs: values.checkpointEnabled
            ? numberValue(values.checkpointIntervalMs)
            : undefined,
          timeoutMs: values.checkpointEnabled ? numberValue(values.checkpointTimeoutMs) : undefined,
          stateTtlMs:
            values.checkpointEnabled && values.stateTtlMs
              ? numberValue(values.stateTtlMs)
              : undefined,
          backend: values.checkpointEnabled ? values.checkpointBackend : undefined,
        },
        restartStrategy: {
          type: String(values.restartStrategyType).toLowerCase().replaceAll('_', '-'),
          parameters: restartParameters(values),
        },
        retryOnFailure: Boolean(values.retryOnFailure),
        retryIntervalMin: values.retryOnFailure ? values.retryIntervalMin : undefined,
        retryMaxCount: values.retryOnFailure ? values.retryMaxCount : undefined,
        customProperties: parseProperties(values.runtimeCustomProperties),
      },
      releaseConfig: {
        resourcePoolId: environment.resourcePoolId,
        priority: values.priority,
        schedulingStrategy: values.schedulingStrategy,
        dependencyResourceNames:
          values.jobType === 'STREAMING_SQL' || values.jobType === 'STREAMING_JAR'
            ? values.dependencyResourceNames || []
            : [],
        customProperties: parseProperties(values.releaseCustomProperties),
      },
    };
    if (values.jobType === 'STREAMING_SQL') {
      request.sql = values.sql;
    } else {
      request.jar = values.jar;
      request.mainClass = values.mainClass;
    }
    if (unref(application)) {
      request.appId = unref(application)!.appId;
      request.version = unref(application)!.version;
    }
    return request;
  }

  async function handleSubmit(values: Recordable) {
    submitting.value = true;
    try {
      const request = buildRequest(values);
      if (request.appId) {
        await fetchUpdateManagedApplication(request);
        createMessage.success(t('flink.app.managed.updateSuccess'));
      } else {
        await fetchCreateManagedApplication(request);
        createMessage.success(t('flink.app.managed.createSuccess'));
      }
      go('/flink/app');
    } catch (error) {
      if (error instanceof Error) createMessage.error(error.message);
    } finally {
      submitting.value = false;
    }
  }

  async function handleFieldValueChange(field: string) {
    if (
      [
        'parallelism',
        'taskManagerSlots',
        'taskManagerCpu',
        'taskManagerMemoryGiB',
        'jobManagerCpu',
        'jobManagerMemoryGiB',
      ].includes(field)
    ) {
      await updateEstimate();
    }
  }

  function formValues(app: ManagedFlinkApplication): Recordable {
    const restart = app.runtimeConfig.restartStrategy;
    return {
      managedEnvironmentId: app.managedEnvironmentId,
      jobType: app.jobType,
      engineVersion: app.runtimeConfig.engineVersion,
      jobName: app.jobName,
      sql: app.sql,
      jar: app.jar,
      mainClass: app.mainClass,
      dependencyResourceNames: app.releaseConfig.dependencyResourceNames,
      ...app.runtimeConfig.resource,
      estimatedCu: app.estimatedCu,
      checkpointEnabled: app.runtimeConfig.checkpoint?.enabled,
      checkpointIntervalMs: app.runtimeConfig.checkpoint?.intervalMs,
      checkpointTimeoutMs: app.runtimeConfig.checkpoint?.timeoutMs,
      stateTtlMs: app.runtimeConfig.checkpoint?.stateTtlMs,
      checkpointBackend: app.runtimeConfig.checkpoint?.backend,
      restartStrategyType: String(restart?.type || 'NONE')
        .toUpperCase()
        .replaceAll('-', '_'),
      restartAttempts: restart?.parameters?.['restart-strategy.fixed-delay.attempts']
        ? Number(restart.parameters['restart-strategy.fixed-delay.attempts'])
        : 10,
      restartDelaySeconds:
        durationSeconds(
          restart?.parameters?.['restart-strategy.fixed-delay.delay'] ||
            restart?.parameters?.['restart-strategy.failure-rate.delay'],
        ) || 20,
      maxFailuresPerInterval: restart?.parameters?.[
        'restart-strategy.failure-rate.max-failures-per-interval'
      ]
        ? Number(restart.parameters['restart-strategy.failure-rate.max-failures-per-interval'])
        : undefined,
      failureRateIntervalSeconds: durationSeconds(
        restart?.parameters?.['restart-strategy.failure-rate.failure-rate-interval'],
      ),
      exponentialInitialBackoffSeconds: durationSeconds(
        restart?.parameters?.['restart-strategy.exponential-delay.initial-backoff'],
      ),
      exponentialMaxBackoffSeconds: durationSeconds(
        restart?.parameters?.['restart-strategy.exponential-delay.max-backoff'],
      ),
      exponentialBackoffMultiplier: restart?.parameters?.[
        'restart-strategy.exponential-delay.backoff-multiplier'
      ]
        ? Number(restart.parameters['restart-strategy.exponential-delay.backoff-multiplier'])
        : undefined,
      exponentialResetThresholdSeconds: durationSeconds(
        restart?.parameters?.['restart-strategy.exponential-delay.reset-backoff-threshold'],
      ),
      exponentialJitterFactor: restart?.parameters?.[
        'restart-strategy.exponential-delay.jitter-factor'
      ]
        ? Number(restart.parameters['restart-strategy.exponential-delay.jitter-factor'])
        : undefined,
      exponentialAttemptsBeforeReset: restart?.parameters?.[
        'restart-strategy.exponential-delay.attempts-before-reset-backoff'
      ]
        ? Number(
            restart.parameters['restart-strategy.exponential-delay.attempts-before-reset-backoff'],
          )
        : undefined,
      retryOnFailure: app.runtimeConfig.retryOnFailure,
      retryIntervalMin: app.runtimeConfig.retryIntervalMin,
      retryMaxCount: app.runtimeConfig.retryMaxCount,
      priority: app.releaseConfig.priority,
      schedulingStrategy: app.releaseConfig.schedulingStrategy,
      runtimeCustomProperties: stringifyProperties(app.runtimeConfig.customProperties),
      releaseCustomProperties: stringifyProperties(app.releaseConfig.customProperties),
      args: app.args,
      description: app.description,
    };
  }

  async function load() {
    loadingMetadata.value = true;
    try {
      const teamId = requireTeamId();
      const [environmentList, resourceList] = await Promise.all([
        fetchManagedEnvironments({ teamId }),
        fetchTeamResource({}),
      ]);
      environments.value = environmentList;
      resources.value = resourceList;
      if (props.appId) {
        const current = await fetchManagedApplication({ teamId, appId: props.appId });
        application.value = current;
        await selectEnvironment(current.managedEnvironmentId, false);
        const values = formValues(current);
        await setFieldsValue({ checkpointEnabled: values.checkpointEnabled });
        await nextTick();
        await setFieldsValue(values);
      } else {
        await setFieldsValue({
          parallelism: 1,
          taskManagerSlots: 1,
          checkpointEnabled: false,
          checkpointIntervalMs: 60000,
          checkpointTimeoutMs: 600000,
          restartStrategyType: 'FIXED_DELAY',
          restartAttempts: 10,
          restartDelaySeconds: 20,
          maxFailuresPerInterval: 20,
          failureRateIntervalSeconds: 600,
          exponentialInitialBackoffSeconds: 1,
          exponentialMaxBackoffSeconds: 60,
          exponentialBackoffMultiplier: 1.5,
          exponentialResetThresholdSeconds: 3600,
          exponentialJitterFactor: 0.1,
          exponentialAttemptsBeforeReset: 120,
          retryOnFailure: false,
          retryIntervalMin: 1,
          retryMaxCount: 3,
          priority: 50,
          schedulingStrategy: 'DEFAULT',
        });
      }
    } catch (error) {
      if (error instanceof Error) createMessage.error(error.message);
    } finally {
      loadingMetadata.value = false;
    }
  }

  onMounted(load);
</script>

<template>
  <BasicForm
    @register="registerForm"
    @submit="handleSubmit"
    @field-value-change="handleFieldValueChange"
    :schemas="formSchemas"
    class="!my-20px"
  >
    <template #jobType="{ model, field }">
      <Select v-model:value="model[field]" class="w-full" :disabled="!capability">
        <SelectOption v-for="jobType in capability?.jobTypes || []" :key="jobType" :value="jobType">
          <div class="flex items-center">
            <SvgIcon :name="jobType === 'STREAMING_SQL' ? 'fql' : 'fjar'" color="#108ee9" />
            <span class="pl-10px">
              {{ jobType === 'STREAMING_SQL' ? 'Flink SQL' : 'Flink JAR' }}
            </span>
          </div>
        </SelectOption>
      </Select>
    </template>
    <template #sql="{ model, field }">
      <FlinkSqlEditor v-model:value="model[field]" :show-verify="false" :show-preview="false" />
    </template>
    <template #dependency="{ model, field }">
      <ManagedDependencyUpload
        v-model:value="model[field]"
        :resources="resources"
        @uploaded="reloadResources"
      />
    </template>
    <template #formFooter>
      <div class="flex items-center w-full justify-center">
        <a-button @click="go('/flink/app')">
          {{ t('common.cancelText') }}
        </a-button>
        <a-button class="ml-4" :loading="submitting" type="primary" @click="submit()">
          {{ t('common.submitText') }}
        </a-button>
      </div>
    </template>
  </BasicForm>
</template>
