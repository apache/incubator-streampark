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

<script lang="ts">
  export default {
    name: 'UserTeam',
  };
</script>
<script setup lang="ts">
  import { ApiSelect } from '/@/components/Form';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { fetchUserTeam } from '/@/api/system/member';
  import { useI18n } from '/@/hooks/web/useI18n';
  const { t } = useI18n();
  const userStore = useUserStoreWithOut();

  function handleSetTeamId(value: string) {
    userStore.setTeamId({ teamId: value });
  }

  function handleOptionsChange(value: Recordable[]) {
    const hasIn = value.find((r: Recordable) => r.value == userStore.getTeamId);
    // select team not exist
    if (!hasIn) {
      handleSetTeamId(value[0].value);
    }
  }
</script>

<template>
  <div class="flex items-center min-w-160px">
    <span class="text-blue-500 pr-10px"> {{ t('system.team.team') }} : </span>
    <ApiSelect
      :api="fetchUserTeam as any"
      labelField="teamName"
      valueField="id"
      :params="{ userId: userStore.getUserInfo?.userId }"
      :alwaysLoad="true"
      :allow-clear="false"
      class="flex-1"
      :optionsData="userStore.getTeamList.map((t) => ({ teamName: t.label, id: t.value }))"
      @change="handleSetTeamId"
      :value="userStore.teamId"
      placeholder="Team"
      @options-change="handleOptionsChange"
      size="small"
    />
  </div>
</template>
