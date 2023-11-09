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
