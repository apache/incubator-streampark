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
  import { Modal } from 'ant-design-vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { useUserStore } from '/@/store/modules/user';
  import { computed, ref } from 'vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Icon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';

  const emit = defineEmits(['success', 'update:visible']);
  const props = defineProps({
    userId: {
      type: String,
      default: '',
    },
    visible: {
      type: Boolean,
      default: false,
    },
  });
  const loading = ref(false);

  const { createMessage } = useMessage();
  const { t } = useI18n();
  const userStore = useUserStore();

  const formSchema = computed((): FormSchema[] => {
    return [
      {
        label: t('sys.login.selectTeam'),
        field: 'teamId',
        component: 'Select',
        componentProps: {
          options: userStore.getTeamList,
          placeholder: t('sys.login.selectTeam'),
          popupClassName: 'team-select-popup',
        },
        required: true,
      },
    ];
  });

  const [registerForm, { validate }] = useForm({
    colon: true,
    showActionButtonGroup: false,
    layout: 'vertical',
    baseColProps: { span: 24 },
  });
  // submit
  async function handleUserTeamSubmit() {
    try {
      const formValue = await validate();
      loading.value = true;
      const res = await userStore.setTeamId({ userId: props.userId, teamId: formValue.teamId });
      if (res) {
        loading.value = false;
        emit('success', res);
      } else {
        createMessage.error('set teamId failed');
      }
    } catch (error) {
      console.error(error);
    }
  }
</script>

<template>
  <Modal
    :visible="props.visible"
    centered
    @cancel="emit('update:visible', false)"
    @ok="handleUserTeamSubmit"
  >
    <template #title>
      <div>
        <Icon icon="ant-design:setting-outlined" color="green" />
        <span class="pl-10px">Select Team</span>
      </div>
    </template>
    <div class="p-4">
      <BasicForm @register="registerForm" :schemas="formSchema" />
    </div>
  </Modal>
</template>
