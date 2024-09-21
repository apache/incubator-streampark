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
    :okButtonProps="{ class: 'e2e-upload-submit-btn' }"
    :cancelButtonProps="{ class: 'e2e-upload-cancel-btn' }"
    :okText="t('common.submitText')"
    @register="registerDrawer"
    showFooter
    width="700"
    @ok="handleSubmit"
  >
    <template #title>
      <SvgIcon name="resource" />
      {{ getTitle }}
    </template>
    <div class="mt-3">
      <BasicForm @register="registerForm" :schemas="getResourceFormSchema">
        <template #resource="{ model, field }">
          <Upload ref="resourceRef" v-model:value="model[field]" :form-model="model" />
        </template>
      </BasicForm>
    </div>
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
  import { useI18n } from '/@/hooks/web/useI18n';
  import Upload from './Upload.vue';
  import { fetchAddResource, fetchUpdateResource, checkResource } from '/@/api/resource/upload';
  import { EngineTypeEnum, ResourceTypeEnum } from '../upload.data';
  import {
    renderEngineType,
    renderResourceType,
    renderStreamParkResourceGroup,
  } from '../useUploadRender';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { exceptionPropWidth } from '/@/utils';
  import SvgIcon from '/@/components/Icon/src/SvgIcon.vue';

  const emit = defineEmits(['success', 'register']);

  const props = defineProps({
    teamResource: {
      type: Object as PropType<any>,
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
        field: 'engineType',
        label: t('flink.resource.engineType'),
        component: 'Select',
        render: ({ model }) => renderEngineType({ model }),
        defaultValue: EngineTypeEnum.FLINK,
        rules: [{ required: true, message: t('flink.resource.form.engineTypeIsRequiredMessage') }],
      },
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
        field: 'resourceName',
        label: t('flink.resource.resourceName'),
        component: 'Input',
        ifShow: ({ values }) =>
          values?.resourceType !== ResourceTypeEnum.CONNECTOR &&
          values?.resourceType !== ResourceTypeEnum.GROUP,
        componentProps: { placeholder: t('flink.resource.resourceNamePlaceholder') },
        rules: [
          { required: true, message: t('flink.resource.form.resourceNameIsRequiredMessage') },
        ],
      },
      {
        field: 'resourceName',
        label: t('flink.resource.groupName'),
        component: 'Input',
        componentProps: { placeholder: t('flink.resource.groupNamePlaceholder') },
        ifShow: ({ values }) => values?.resourceType === ResourceTypeEnum.GROUP,
        rules: [{ required: true, message: t('flink.resource.groupNameIsRequiredMessage') }],
      },
      {
        field: 'resourceGroup',
        label: t('flink.resource.resourceGroup'),
        component: 'Select',
        render: ({ model }) =>
          renderStreamParkResourceGroup({ model, resources: unref(props.teamResource) }),
        ifShow: ({ values }) => values?.resourceType === ResourceTypeEnum.GROUP,
      },
      {
        field: 'dependency',
        label: t('flink.resource.addResource'),
        component: 'Input',
        slot: 'resource',
        ifShow: ({ values }) => values?.resourceType !== ResourceTypeEnum.GROUP,
      },
      {
        field: 'mainClass',
        label: t('flink.app.mainClass'),
        component: 'Input',
        componentProps: { placeholder: t('flink.app.addAppTips.mainClassPlaceholder') },
        ifShow: ({ values }) => values?.resourceType === ResourceTypeEnum.APP,
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
    colon: true,
    layout: 'vertical',
    name: 'upload_form',
    baseColProps: { span: 22, offset: 1 },
    showActionButtonGroup: false,
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(
    async (data: Recordable) => {
      unref(resourceRef)?.setDefaultValue({});
      await resetFields();
      setDrawerProps({ confirmLoading: false });
      isUpdate.value = !!data?.isUpdate;
      if (unref(isUpdate)) {
        resourceId.value = data.record.id;
        await setFieldsValue(data.record);
        if (data.record?.resourceType === ResourceTypeEnum.GROUP) {
          await setFieldsValue({ resourceGroup: JSON.parse(data.record.resource || '[]') });
        } else {
          await setFieldsValue({ dependency: data.record.resource });
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
      const id = resourceId.value;
      resourceId.value = null;
      const values = await validate();
      let resourceJson = '';
      if (values.resourceType == ResourceTypeEnum.GROUP) {
        resourceJson = JSON.stringify(values.resourceGroup);
      } else {
        const resource: { pom?: string; jar?: string } = {};
        await unref(resourceRef).handleApplyPom();
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

        if (resource.pom && resource.pom.length > 0 && resource.jar && resource.jar.length > 0) {
          Swal.fire('Failed', t('flink.resource.multiPomTip'), 'error');
          return;
        }

        resourceJson = JSON.stringify(resource);
        // check resource
        const resp = await checkResource({
          id: id,
          resource: resourceJson,
          ...values,
        });
        const state = resp['state'];
        switch (state) {
          case 1:
            // download error
            if (resource.pom && resource.pom.length > 0) {
              Swal.fire({
                icon: 'error',
                title: t('sys.api.errorTip'),
                width: exceptionPropWidth(),
                html: '<pre class="api-exception">' + resp['exception'] + '</pre>',
                focusConfirm: false,
              });
            } else {
              Swal.fire('Failed', t('flink.resource.jarFileErrorTip'), 'error');
            }
            break;
          case 2:
            if (values.resourceType == ResourceTypeEnum.APP) {
              Swal.fire('Failed', t('flink.resource.mainNullTip'), 'error');
            }
            if (values.resourceType == ResourceTypeEnum.CONNECTOR) {
              Swal.fire('Failed', t('flink.resource.connectorInvalidTip'), 'error');
            }
            break;
          case 3:
            Swal.fire(
              'Failed',
              t('flink.resource.connectorInfoErrorTip').concat(': ').concat(resp['name']),
              'error',
            );
            break;
          case 4:
            Swal.fire('Failed', t('flink.resource.connectorExistsTip'), 'error');
            break;
          case 5:
            Swal.fire('Failed', t('flink.resource.connectorModifyTip'), 'error');
            break;
          case 0:
            const connector = resp['connector'] || null;
            setDrawerProps({ confirmLoading: true });
            await (isUpdate.value
              ? fetchUpdateResource({
                  id: id,
                  resource: resourceJson,
                  connector: connector,
                  ...values,
                })
              : fetchAddResource({ resource: resourceJson, connector: connector, ...values }));
            unref(resourceRef)?.setDefaultValue({});
            closeDrawer();
            emit('success', isUpdate.value);
            break;
          default:
            break;
        }
        await resetFields();
      }
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
