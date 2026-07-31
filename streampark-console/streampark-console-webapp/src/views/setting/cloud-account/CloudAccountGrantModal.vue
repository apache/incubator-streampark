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
  import { ref } from 'vue';
  import { Alert, Select } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { fetchCloudAccountGrant, fetchCloudAccountGrants } from '/@/api/setting/cloudAccount';
  import type { CloudAccount } from '/@/api/setting/cloudAccount.type';
  import { fetTeamList } from '/@/api/system/team';
  import { useI18n } from '/@/hooks/web/useI18n';

  defineOptions({ name: 'CloudAccountGrantModal' });
  const emit = defineEmits(['register', 'success']);
  const { t } = useI18n();
  const account = ref<CloudAccount>();
  const selectedTeamIds = ref<string[]>([]);
  const teamOptions = ref<{ label: string; value: string }[]>([]);

  const [registerModal, { closeModal, setModalProps }] = useModalInner(
    async (data: { record: CloudAccount }) => {
      account.value = data.record;
      selectedTeamIds.value = [];
      teamOptions.value = [];
      setModalProps({ confirmLoading: true });
      try {
        const [teamPage, grants] = await Promise.all([
          fetTeamList({ page: 1, pageSize: 1000 }),
          fetchCloudAccountGrants(data.record.id),
        ]);
        teamOptions.value = teamPage.records.map((team) => ({
          label: team.teamName,
          value: team.id,
        }));
        selectedTeamIds.value = grants.map((grant) => grant.teamId);
      } finally {
        setModalProps({ confirmLoading: false });
      }
    },
  );

  async function handleSubmit() {
    if (!account.value) return;
    try {
      setModalProps({ confirmLoading: true });
      await fetchCloudAccountGrant({
        accountId: account.value.id,
        accountVersion: account.value.version,
        teamIds: selectedTeamIds.value,
      });
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<template>
  <BasicModal
    v-bind="$attrs"
    centered
    :title="t('setting.cloudAccount.grantTitle')"
    :width="560"
    show-footer
    @register="registerModal"
    @ok="handleSubmit"
  >
    <Alert class="mb-4" type="info" show-icon :message="t('setting.cloudAccount.grantHint')" />
    <Select
      v-model:value="selectedTeamIds"
      class="w-full"
      mode="multiple"
      show-search
      allow-clear
      :options="teamOptions"
      :placeholder="t('setting.cloudAccount.noTeams')"
      :filter-option="
        (input, option) =>
          String(option?.label || '')
            .toLowerCase()
            .includes(input.toLowerCase())
      "
    />
  </BasicModal>
</template>
