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
  <BasicModal :width="600" :show-cancel-btn="false" @register="registerModal" @ok="handleSubmit">
    <template #title>
      <Icon icon="ant-design:user-add-outlined" />
      {{t('system.user.userInfo')}}
    </template>
    <Description
      @register="registerDescription"
      :column="1"
      :data="userInfo"
      :schema="userColumn"
    />
  </BasicModal>
</template>
<script lang="ts">
  export default defineComponent({
    name: 'UserModal',
  });
</script>

<script setup lang="ts">
  import { Tag } from 'ant-design-vue';
  import { defineComponent, h, ref } from 'vue';
  import { useDescription, Description } from '/@/components/Description';
  import Icon from '/@/components/Icon';
  import { useModalInner, BasicModal } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  const userInfo = ref<Recordable>({});
  const { t } = useI18n();

  const [registerModal, { closeModal }] = useModalInner((data: Recordable) => {
    data && onReceiveModalData(data);
  });
  function onReceiveModalData(data) {
    userInfo.value = {};
    switch (data.sex) {
      case '0':
        data.sexText = t('system.user.userGender.male');
        break;
      case '1':
        data.sexText = t('system.user.userGender.female');
        break;
      case '2':
        data.sexText = t('system.user.userGender.secret');
        break;
      default:
        data.sexText = data.sex;
        break;
    }
    Object.assign(userInfo.value, data);
  }
  // Dynamically generate label icons
  const generatedLabelIcon = (icon: string, label: string) => {
    return h('div', null, [
      h(Icon, { icon: `ant-design:${icon}-outlined` }),
      h('span', { class: 'px-5px' }, label),
    ]);
  };
  const userColumn = [
    { label: generatedLabelIcon('user', t('system.user.table.userName')), field: 'username' },
    { label: generatedLabelIcon('star', t('system.user.table.userType')), field: 'userType' },
    {
      label: generatedLabelIcon('skin', t('system.user.table.gender')),
      field: 'sex',
      render: (curVal: string) => {
        const sexMap = {
          '0': t('system.user.userGender.male'),
          '1': t('system.user.userGender.female'),
          '2': t('system.user.userGender.secret'),
          [curVal]: curVal,
        };
        return sexMap[curVal];
      },
    },
    { label: generatedLabelIcon('mail', t('system.user.table.email')), field: 'email' },
    {
      label: generatedLabelIcon(`${userInfo.value?.status === '1' ? 'smile' : 'frown'}`, t('system.user.table.status')),
      field: 'status',
      render: (curVal) => {
        if (curVal === '0') {
          return h(Tag, { color: 'red' }, () => 'locked');
        } else if (curVal === '1') {
          return h(Tag, { color: 'green' }, () => 'effective');
        } else {
          return h('span', null, curVal);
        }
      },
    },
    {
      label: generatedLabelIcon(`clock-circle`, t('system.user.table.createTime')),
      field: 'createTime',
    },
    {
      label: generatedLabelIcon(`login`, t('system.user.table.recentLogin')),
      field: 'lastLoginTime',
    },
    {
      label: generatedLabelIcon(`message`, t('system.user.table.description')),
      field: 'description',
    },
  ];
  const [registerDescription] = useDescription();
  function handleSubmit() {
    closeModal();
  }
</script>
