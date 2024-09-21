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
  <BasicModal :width="600" :showOkBtn="false" @register="registerModal" showFooter @ok="closeModal">
    <template #title>
      <Icon icon="ant-design:code-outlined" />
      {{ t('flink.variable.variableInfoTitle') }}
    </template>
    <Description class="variable-desc" :column="1" :data="variableInfo" :schema="roleColumn" />
  </BasicModal>
</template>
<script setup lang="ts">
  import { h, ref } from 'vue';
  import { Description, DescItem } from '/@/components/Description';
  import Icon from '/@/components/Icon';
  import { useModalInner, BasicModal } from '/@/components/Modal';
  import { fetchVariableInfo } from '/@/api/resource/variable';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useI18n } from '/@/hooks/web/useI18n';

  const variableInfo = ref<Recordable>({});
  const showVariableDetail = ref(false);
  let realVariable = '';
  let desenVariable = '';
  const [registerModal, { closeModal }] = useModalInner((data: Recordable) => {
    data && onReceiveModalData(data);
  });
  const { t } = useI18n();

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
      label: generatedLabelIcon('ant-design:code-outlined', t('flink.variable.table.variableCode')),
      field: 'variableCode',
    },
    {
      label: generatedLabelIcon(
        'ant-design:down-circle-outlined',
        t('flink.variable.table.variableValue'),
      ),
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
      label: generatedLabelIcon(`ant-design:user-outlined`, t('flink.variable.table.createUser')),
      field: 'creatorName',
    },
    {
      label: generatedLabelIcon(
        `ant-design:clock-circle-outlined`,
        t('flink.variable.table.createTime'),
      ),
      field: 'createTime',
    },
    {
      label: generatedLabelIcon('ant-design:mail-outlined', t('flink.variable.table.modifyTime')),
      field: 'modifyTime',
    },
    {
      label: generatedLabelIcon(
        'ant-design:message-outlined',
        t('flink.variable.table.description'),
      ),
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
