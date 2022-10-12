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
    <BasicForm @register="registerForm">
      <template #menu="{ model, field }">
        <BasicTree
          v-model:value="model[field]"
          :treeData="treeData"
          :fieldNames="{ title: 'text', key: 'id' }"
          checkable
          toolbar
          title="menu assignment"
        />
      </template>
    </BasicForm>
  </BasicDrawer>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from './role.data';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicTree, TreeItem } from '/@/components/Tree';
  import { addRole, editRole } from '/@/api/sys/role';
  import { getMenuList, getRoleMenu } from '/@/api/demo/system';
  import { FormTypeEnum } from '/@/enums/formEnum';

  const handleTreeIcon = (treeData: TreeItem[]): TreeItem[] => {
    if (!treeData?.length) {
      return [];
    }
    treeData.forEach((v) => {
      v.icon = v.icon && !v.icon.includes('ant-design:') ? `ant-design:${v.icon}-outlined` : v.icon;
      v.children && handleTreeIcon(v.children);
    });
    return treeData;
  };

  export default defineComponent({
    name: 'RoleDrawer',
    components: { BasicDrawer, BasicForm, BasicTree },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const formType = ref(FormTypeEnum.Edit);
      const treeData = ref<TreeItem[]>([]);

      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 120,
        baseColProps: { span: 22 },
        schemas: formSchema,
        showActionButtonGroup: false,
      });

      const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
        resetFields();
        setDrawerProps({
          confirmLoading: false,
          showFooter: data.formType !== FormTypeEnum.View,
        });

        formType.value = data.formType;
        // You need to fill in treeData before setFieldsValue, otherwise the Tree component may report a key not exist warning
        if (!unref(isCreate)) {
          const res = await getRoleMenu({ roleId: data.record.roleId });
          data.record.menuId = res || [];
        }

        if (unref(treeData).length === 0) {
          const res = await getMenuList();
          treeData.value = handleTreeIcon(res?.rows?.children);
        }

        if (!unref(isCreate)) {
          setFieldsValue({ ...data.record });
        }
      });

      const getTitle = computed(() => {
        return {
          [FormTypeEnum.Create]: 'Add Role',
          [FormTypeEnum.Edit]: 'Edit Role',
          [FormTypeEnum.View]: 'View Role',
        }[unref(formType)];
      });

      const isCreate = computed(() => unref(formType) === FormTypeEnum.Create);

      async function handleSubmit() {
        try {
          const values = await validate();
          setDrawerProps({ confirmLoading: true });
          const params = { ...values };
          params.menuId = values.menuId.join(',');
          !unref(isCreate) ? await editRole(params) : await addRole(params);
          closeDrawer();
          emit('success');
        } finally {
          setDrawerProps({ confirmLoading: false });
        }
      }

      return {
        registerDrawer,
        registerForm,
        getTitle,
        handleSubmit,
        treeData,
      };
    },
  });
</script>
