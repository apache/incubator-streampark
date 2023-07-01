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
<template>
  <BasicDrawer
    :okText="t('common.submitText')"
    @register="registerDrawer"
    showFooter
    width="650"
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:code-outlined" />
      {{ getTitle }}
    </template>
    <BasicForm @register="registerForm" :schemas="getResourceFormSchema">
      <template #resource="{ model, field }">
        <Resource ref="resourceRef" v-model:value="model[field]" :form-model="model" />
      </template>
    </BasicForm>
  </BasicDrawer>
</template>
<script lang="ts">
  export default {
    name: 'ResourceDrawer',
  };
</script>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { Icon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Resource from '/@/views/flink/resource/components/Resource.vue';
  import { fetchAddResource, fetchUpdateResource } from '/@/api/flink/resource';
  import { EngineTypeEnum } from '/@/views/flink/resource/resource.data';
  import {
    renderResourceType,
    renderStreamParkResourceGroup,
  } from '/@/views/flink/resource/useResourceRender';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success', 'register']);

  const props = defineProps({
    teamResource: {
      type: Object as Array<any>,
      required: true,
    },
  });

  const { t } = useI18n();
  const { Swal } = useMessage();

  const isUpdate = ref(false);
  const resourceId = ref<Nullable<number>>(null);
  const resourceRef = ref();

  const getResourceFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'resourceType',
        label: t('flink.resource.resourceType'),
        component: 'Select',
        render: ({ model }) => renderResourceType({ model }),
        rules: [
          { required: true, message: t('flink.resource.form.resourceTypeIsRequiredMessage') },
        ],
      },
      {
        field: 'engineType',
        label: t('flink.resource.engineType'),
        component: 'Select',
        defaultValue: EngineTypeEnum.FLINK,
        componentProps: {
          placeholder: t('flink.resource.engineTypePlaceholder'),
          options: [
            { label: 'apache flink', value: EngineTypeEnum.FLINK, disabled: false },
            { label: 'apache spark', value: EngineTypeEnum.SPARK, disabled: true },
          ],
        },
        rules: [{ required: true, message: t('flink.resource.form.engineTypeIsRequiredMessage') }],
      },
      {
        field: 'resourceName',
        label: t('flink.resource.groupName'),
        component: 'Input',
        componentProps: { placeholder: t('flink.resource.groupNamePlaceholder') },
        ifShow: ({ values }) => values?.resourceType == 'GROUP',
        rules: [{ required: true, message: t('flink.resource.groupNameIsRequiredMessage') }],
      },
      {
        field: 'resourceGroup',
        label: t('flink.resource.resourceGroup'),
        component: 'Select',
        render: ({ model }) =>
          renderStreamParkResourceGroup({ model, resources: unref(props.teamResource) }),
        ifShow: ({ values }) => values?.resourceType == 'GROUP',
      },
      {
        field: 'dependency',
        label: t('flink.resource.addResource'),
        component: 'Input',
        slot: 'resource',
        ifShow: ({ values }) => values?.resourceType !== 'GROUP',
      },
      {
        field: 'mainClass',
        label: t('flink.app.mainClass'),
        component: 'Input',
        componentProps: { placeholder: t('flink.app.addAppTips.mainClassPlaceholder') },
        ifShow: ({ values }) => values?.resourceType == 'FLINK_APP',
        rules: [{ required: true, message: t('flink.app.addAppTips.mainClassIsRequiredMessage') }],
      },
      {
        field: 'description',
        label: t('common.description'),
        component: 'InputTextArea',
        componentProps: { rows: 4 },
        rules: [{ max: 100, message: t('flink.resource.form.descriptionMessage') }],
      },
    ];
  });

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    name: 'ResourceForm',
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(
    async (data: Recordable) => {
      unref(resourceRef)?.setDefaultValue({});
      resetFields();
      setDrawerProps({ confirmLoading: false });
      isUpdate.value = !!data?.isUpdate;
      if (unref(isUpdate)) {
        resourceId.value = data.record.id;
        setFieldsValue(data.record);

        if (data.record?.resourceType == 'GROUP') {
          setFieldsValue({ resourceGroup: JSON.parse(data.record.resource || '[]') });
        } else {
          setFieldsValue({ dependency: data.record.resource });
          unref(resourceRef)?.setDefaultValue(JSON.parse(data.record.resource || '{}'));
        }
      }
    },
  );

  const getTitle = computed(() =>
    !unref(isUpdate) ? t('flink.resource.addResource') : t('flink.resource.modifyResource'),
  );

  // form submit
  async function handleSubmit() {
    try {
      const values = await validate();
      let resourceJson = '';

      if (values.resourceType == 'GROUP') {
        resourceJson = JSON.stringify(values.resourceGroup);
      } else {
        const resource: { pom?: string; jar?: string } = {};
        unref(resourceRef).handleApplyPom();
        const dependencyRecords = unref(resourceRef)?.dependencyRecords;
        const uploadJars = unref(resourceRef)?.uploadJars;

        if (unref(dependencyRecords) && unref(dependencyRecords).length > 0) {
          if (unref(dependencyRecords).length > 1) {
            Swal.fire('Failed', t('flink.resource.multiPomTip'), 'error');
            return;
          }
          Object.assign(resource, {
            pom: unref(dependencyRecords),
          });
        }

        if (uploadJars && unref(uploadJars).length > 0) {
          Object.assign(resource, {
            jar: unref(uploadJars),
          });
        }

        if (resource.pom === undefined && resource.jar === undefined) {
          Swal.fire('Failed', t('flink.resource.addResourceTip'), 'error');
          return;
        }

        if (resource.pom?.length > 0 && resource.jar?.length > 0) {
          Swal.fire('Failed', t('flink.resource.multiPomTip'), 'error');
          return;
        }

        resourceJson = JSON.stringify(resource);
      }

      setDrawerProps({ confirmLoading: true });
      await (isUpdate.value
        ? fetchUpdateResource({ id: resourceId.value, resource: resourceJson, ...values })
        : fetchAddResource({ resource: resourceJson, ...values }));
      unref(resourceRef)?.setDefaultValue({});
      resetFields();
      closeDrawer();
      emit('success', isUpdate.value);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less">
  .conf-switch {
    display: inline-block;
    margin-top: 10px;
    color: darkgrey;
  }
</style>
