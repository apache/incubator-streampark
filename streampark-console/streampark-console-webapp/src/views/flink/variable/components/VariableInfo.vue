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
    :showOkBtn="false"
    @register="registerDrawer"
    showFooter
    width="40%"
    @ok="closeDrawer"
  >
    <template #title>
      <Icon icon="ant-design:code-outlined" />
      Variable Info
    </template>
    <Description class="variable-desc" :column="1" :data="variableInfo" :schema="roleColumn" />
  </BasicDrawer>
</template>
<script lang="ts">
  export default defineComponent({
    name: 'VariableInfo',
  });
</script>

<script setup lang="ts">
  import { defineComponent, h, ref } from 'vue';
  import { Description, DescItem } from '/@/components/Description';
  import Icon from '/@/components/Icon';
  import { useDrawerInner, BasicDrawer } from '/@/components/Drawer';
  import { fetchVariableInfo } from '/@/api/flink/variable';
  import { usePermission } from '/@/hooks/web/usePermission';

  const variableInfo = ref<Recordable>({});
  const showVariableDetail = ref(false);
  let realVariable = '';
  let desenVariable = '';
  const [registerDrawer, { closeDrawer }] = useDrawerInner((data: Recordable) => {
    data && onReceiveModalData(data);
  });

  function onReceiveModalData(data) {
    realVariable = '';
    variableInfo.value = Object.assign({}, data);
    showVariableDetail.value = false;
    desenVariable = data.variableValue;
  }
  async function handleVariableDetail() {
    if (!realVariable) {
      const res = await fetchVariableInfo({
        id: variableInfo.value.id,
      });
      realVariable = res.variableValue;
    }

    Object.assign(variableInfo.value, {
      variableValue: showVariableDetail.value ? desenVariable : realVariable,
    });
    showVariableDetail.value = !showVariableDetail.value;
  }
  // generated label
  const generatedLabelIcon = (icon: string, label: string) => {
    return h('div', null, [h(Icon, { icon }), h('span', { class: 'px-5px' }, label)]);
  };
  const { hasPermission } = usePermission();
  const roleColumn: DescItem[] = [
    {
      label: generatedLabelIcon('ant-design:code-outlined', 'Variable Code'),
      field: 'variableCode',
    },
    {
      label: generatedLabelIcon('ant-design:down-circle-outlined', 'Variable Value'),
      field: 'variableValue',
      render(value, data) {
        const renderIcon = () => {
          // need desensitization
          if (data.desensitization && hasPermission('variable:show_original')) {
            return h(Icon, {
              icon: `ant-design:${showVariableDetail.value ? 'eye' : 'eye-invisible'}-outlined`,
              color: showVariableDetail.value ? '#477de9' : '',
              onClick: handleVariableDetail,
              class: 'cursor-pointer',
            });
          } else {
            return null;
          }
        };
        return h('div', { class: 'flex items-center' }, [
          h('span', { class: 'pr-10px' }, value),
          renderIcon(),
        ]);
      },
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
</script>
<style lang="less">
  .variable-desc {
    .ant-descriptions-item-label {
      width: 160px;
    }
  }
</style>
