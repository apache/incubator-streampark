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
  import { onMounted, ref } from 'vue';
  import { Select } from 'ant-design-vue';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { fetchUserTeam } from '/@/api/sys/member';
  const SelectOption = Select.Option;

  const userStore = useUserStoreWithOut();

  const teamList = ref<Array<{ id: string; teamName: string }>>([]);

  onMounted(async () => {
    const res = await fetchUserTeam({ userId: userStore?.getUserInfo?.userId });
    teamList.value = [...res];
  });
</script>

<template>
  <div class="flex items-center min-w-160px">
    <span class="text-blue-500 pr-10px"> Team : </span>
    <Select
      :allow-clear="false"
      class="flex-1"
      @change="userStore.setTeamId"
      :value="userStore.getTeamId"
      placeholder="Team"
    >
      <SelectOption v-for="t in teamList" :key="t.id">
        {{ t.teamName }}
      </SelectOption>
    </Select>
  </div>
</template>
