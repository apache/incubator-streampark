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
      class="app_controller"
      :showCancelBtn="false"
      :okText="t('common.closeText')"
      @register="registerDrawer"
      showFooter
      width="50%"
      @ok="handleSubmit"
    >
      <template #title>
        <Icon icon="ant-design:user-add-outlined" />
        {{ getTitle }}
      </template>
      <BasicForm ref="form" @register="registerForm" :schemas="getConfigFormSchema">
      </BasicForm>
    </BasicDrawer>
  </template>
  <script lang="ts">
    import { computed, defineComponent, ref, unref, reactive } from 'vue';
    import { BasicForm, useForm } from '/@/components/Form';
    // import { formSchema } from '../user.data';
    import { FormTypeEnum } from '/@/enums/formEnum';
    import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
    import { useCreateSchema } from '../hooks/useCreateSchema';
    // import { addUser, updateUser } from '/@/api/system/user';
    import Icon from '/@/components/Icon';
    import { useI18n } from '/@/hooks/web/useI18n';
    import { AppListRecord } from '/@/api/flink/app/app.type';
    import { useMessage } from '/@/hooks/web/useMessage';
  
    export default defineComponent({
      name: 'MenuDrawer',
      components: { BasicDrawer, Icon, BasicForm },
      emits: ['addConfigsuccess', 'register', 'addConfigFailed'],
      setup(_, {emit}) {
        const { t } = useI18n();
        const formType = ref(FormTypeEnum.Edit);
        const dependencyRef = ref();
        const app = reactive<Partial<AppListRecord>>({}); // 属性提交的参数
        const { createMessage } = useMessage();
  
        const [registerForm, { resetFields, setFieldsValue, clearValidate, validateFields }] = useForm({
          name: 'MemberForm',
          colon: true,
          showActionButtonGroup: false,
          baseColProps: { span: 24 },
          labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
          wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
        });
  
        const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
          console.log("data:", data)
          // formType.value = data.formType;
          resetFields();
          clearValidate();
          // updateSchema(formSchema(unref(formType)));
          setDrawerProps({
            confirmLoading: false
          });
          Object.assign(app, data);
          setFieldsValue(data);
          // if (unref(formType) !== FormTypeEnum.Create) {
          //   const roleIds = data.record?.roleId ?? [];
          //   data.record.roleId = Array.isArray(roleIds) ? roleIds : roleIds.split(',');
          //   setFieldsValue(data);
          // }
        });
        const { getConfigFormSchema } = useCreateSchema(dependencyRef, true);

        const getTitle = computed(() => {
          return {
            // [FormTypeEnum.Create]: t('system.user.form.create'),
            [FormTypeEnum.Edit]: t('flink.app.addDrawerMenu.editConfig'),
            // [FormTypeEnum.View]: t('system.user.form.view'),
          }[unref(formType)];
        });
        let isSubmitConfig = ref(false)
        async function handleSubmit() {
          try {
            const values = await validateFields();
            // const values = formValidate()
            setDrawerProps({ confirmLoading: true });
            // const oldValues = JSON.parse(sessionStorage.getItem('AddJobModalParams') || '{}') as {[key: string]: any}
            const oldValues = JSON.parse(sessionStorage.getItem('AddJobModalParams') || '{}');
            const params = {...oldValues, ...values}
            sessionStorage.setItem('AddJobModalParams', JSON.stringify(params));
            isSubmitConfig.value = true
            closeDrawer();
            emit('addConfigsuccess');
          } catch (e) {
            createMessage.warning(t('flink.app.addDrawerMenu.configValidateTips'))
            isSubmitConfig.value = false
            emit('addConfigFailed', 'config');
          } finally {
            setDrawerProps({ confirmLoading: false });
          }
        }
  
        return { t, registerDrawer, registerForm, getTitle, getConfigFormSchema, handleSubmit, isSubmitConfig };
      },
    });
  </script>
  <style lang="less">
    @import url('../styles/Add.less');
  </style>
  