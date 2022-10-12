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
    v-bind="$attrs"
    @register="registerDrawer"
    showFooter
    :title="getTitle"
    width="40%"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
  </BasicDrawer>
</template>
<script lang="ts">
  import { computed, defineComponent, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from './user.data';
  import { FormTypeEnum } from '/@/enums/formEnum';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';

  import { addUser, updateUser } from '/@/api/sys/user';

  export default defineComponent({
    name: 'MenuDrawer',
    components: { BasicDrawer, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const formType = ref(FormTypeEnum.Edit);

      const [registerForm, { resetFields, setFieldsValue, updateSchema, validate, clearValidate }] =
        useForm({
          labelWidth: 120,
          schemas: formSchema(unref(formType)),
          showActionButtonGroup: false,
          baseColProps: { lg: 22, md: 22 },
        });

      const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
        resetFields();
        setDrawerProps({
          confirmLoading: false,
          showFooter: data.formType !== FormTypeEnum.View,
        });
        formType.value = data.formType;

        updateSchema(formSchema(unref(formType)));

        if (unref(formType) !== FormTypeEnum.Create) {
          const roleIds = data.record?.roleId ?? [];
          data.record.roleId = Array.isArray(roleIds) ? roleIds : roleIds.split(',');
          setFieldsValue({
            ...data.record,
          });
          clearValidate('username');
          clearValidate('nickname');
        }
      });

      const getTitle = computed(() => {
        return {
          [FormTypeEnum.Create]: 'Add User',
          [FormTypeEnum.Edit]: 'Edit User',
          [FormTypeEnum.View]: 'View User',
        }[unref(formType)];
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          setDrawerProps({ confirmLoading: true });
          unref(formType) === FormTypeEnum.Edit ? await updateUser(values) : await addUser(values);
          closeDrawer();
          emit('success');
        } finally {
          setDrawerProps({ confirmLoading: false });
        }
      }

      return { registerDrawer, registerForm, getTitle, handleSubmit };
    },
  });
</script>
