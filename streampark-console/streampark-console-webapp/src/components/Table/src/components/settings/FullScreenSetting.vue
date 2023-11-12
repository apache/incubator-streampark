<template>
  <Tooltip placement="top">
    <template #title>
      <span>{{ t('component.table.settingFullScreen') }}</span>
    </template>
    <FullscreenOutlined @click="toggle" v-if="!isFullscreen" />
    <FullscreenExitOutlined @click="toggle" v-else />
  </Tooltip>
</template>
<script lang="ts">
  import { computed, defineComponent } from 'vue';
  import { Tooltip } from 'ant-design-vue';
  import { FullscreenOutlined, FullscreenExitOutlined } from '@ant-design/icons-vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useTableContext } from '../../hooks/useTableContext';

  export default defineComponent({
    name: 'FullScreenSetting',
    components: {
      FullscreenExitOutlined,
      FullscreenOutlined,
      Tooltip,
    },

    setup() {
      const table = useTableContext();
      const { t } = useI18n();
      // const { toggle, isFullscreen } = useFullscreen(table.wrapRef);
      const isFullscreen = computed(() => {
        return table.tableFullScreen.value;
      });
      function toggle() {
        table.tableFullScreen.value = !isFullscreen.value;
      }
      return {
        toggle,
        isFullscreen,
        t,
      };
    },
  });
</script>
