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
  <BasicDrawer @register="registerDrawer" showFooter width="40%" @ok="handleSubmit">
    <template #title>
      <Icon icon="ant-design:code-outlined" />
      Variable Info
    </template>
    <Description
      @register="registerDescription"
      :column="1"
      :data="variableInfo"
      :schema="roleColumn"
    />
  </BasicDrawer>
</template>
<script lang="ts">
  export default defineComponent({
    name: 'VariableInfo',
  });
</script>

<script setup lang="ts">
  import { defineComponent, h, ref } from 'vue';
  import { useDescription, Description } from '/@/components/Description';
  import Icon from '/@/components/Icon';
  import { useDrawerInner, BasicDrawer } from '/@/components/Drawer';

  const variableInfo = ref<Recordable>({});

  const [registerDrawer, { closeDrawer }] = useDrawerInner((data: Recordable) => {
    data && onReceiveModalData(data);
  });

  async function onReceiveModalData(data) {
    variableInfo.value = Object.assign({}, data);
  }

  const generatedLabelIcon = (icon: string, label: string) => {
    return h('div', null, [h(Icon, { icon }), h('span', { class: 'px-5px' }, label)]);
  };
  const roleColumn = [
    {
      label: generatedLabelIcon('ant-design:code-outlined', 'Variable Code'),
      field: 'variableCode',
    },
    {
      label: generatedLabelIcon('ant-design:down-circle-outlined', 'Variable Value'),
      field: 'variableValue',
    },
    {
      label: generatedLabelIcon(`ant-design:user-outlined`, 'Create User'),
      field: 'creatorName',
    },
    {
      label: generatedLabelIcon(`ant-design:clock-circle-outlined`, 'Create Time'),
      field: 'createTime',
    },
    {
      label: generatedLabelIcon('ant-design:mail-outlined', 'Modify Time'),
      field: 'modifyTime',
    },
    {
      label: generatedLabelIcon('ant-design:message-outlined', 'Description'),
      field: 'description',
    },
  ];
  const [registerDescription] = useDescription();
  function handleSubmit() {
    closeDrawer();
  }
</script>
