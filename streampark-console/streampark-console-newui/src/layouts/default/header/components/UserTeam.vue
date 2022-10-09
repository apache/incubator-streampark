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
