<template>
  <Tooltip :title="getTitle" placement="bottom" :mouseEnterDelay="0.5">
    <span @click="toggle">
      <FullscreenOutlined v-if="!isFullscreen" />
      <FullscreenExitOutlined v-else />
    </span>
  </Tooltip>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import { Tooltip } from 'ant-design-vue';

  import { FullscreenExitOutlined, FullscreenOutlined } from '@ant-design/icons-vue';
  import { useFullscreenEvent } from '/@/hooks/event/useFullscreen';
  export default defineComponent({
    name: 'FullScreen',
    components: { FullscreenExitOutlined, FullscreenOutlined, Tooltip },

    setup() {
      const { toggle, isFullscreen, getTitle } = useFullscreenEvent(true);

      return {
        getTitle,
        isFullscreen,
        toggle,
      };
    },
  });
</script>
