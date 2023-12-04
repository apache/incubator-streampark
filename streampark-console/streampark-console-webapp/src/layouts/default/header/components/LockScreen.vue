<script lang="ts" setup>
  import { Tooltip } from 'ant-design-vue';
  import Icon from '/@/components/Icon';
  import { useModal } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';
  const LockAction = createAsyncComponent(() => import('./lock/LockModal.vue'));

  const [register, { openModal }] = useModal();
  const { t } = useI18n();
  function handleLock() {
    openModal(true);
  }
</script>
<template>
  <div>
    <Tooltip :title="t('layout.header.tooltipLock')" placement="bottom" :mouseEnterDelay="0.5">
      <Icon
        icon="ant-design:lock-outlined"
        class="!px-8px"
        style="font-size: 19px !important"
        @click="handleLock"
      />
    </Tooltip>
    <LockAction @register="register" />
  </div>
</template>
