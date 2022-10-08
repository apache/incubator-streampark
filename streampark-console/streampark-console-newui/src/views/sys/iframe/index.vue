<template>
  <div :class="prefixCls" :style="getWrapStyle">
    <Spin :spinning="loading" size="large" :style="getWrapStyle">
      <iframe
        :src="frameSrc"
        :class="`${prefixCls}__main`"
        ref="frameRef"
        @load="hideLoading"
      ></iframe>
    </Spin>
  </div>
</template>
<script lang="ts" setup>
  import { CSSProperties, onMounted } from 'vue';
  import { ref, unref, computed } from 'vue';
  import { Spin, Modal, message, notification } from 'ant-design-vue';
  import { useWindowSizeFn } from '/@/hooks/event/useWindowSizeFn';
  import { propTypes } from '/@/utils/propTypes';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { useLayoutHeight } from '/@/layouts/default/content/useContentViewHeight';

  import { swal } from '/@/adapter';

  defineProps({
    frameSrc: propTypes.string.def(''),
    // TODO
    // onLoad: { type: Function as PropType<(...args) => any>, default: null },
  });

  const loading = ref(true);
  const topRef = ref(50);
  const heightRef = ref(window.innerHeight);
  const frameRef = ref<HTMLFrameElement>();
  const { headerHeightRef } = useLayoutHeight();

  const { prefixCls } = useDesign('iframe-page');
  useWindowSizeFn(calcHeight, 150, { immediate: true });

  const getWrapStyle = computed((): CSSProperties => {
    return {
      height: `${unref(heightRef)}px`,
    };
  });

  onMounted(() => {
    calcHeight();
  });

  function calcHeight() {
    const iframe = unref(frameRef);
    if (!iframe) {
      return;
    }
    const top = headerHeightRef.value;
    topRef.value = top;
    heightRef.value = window.innerHeight - top;
    const clientHeight = document.documentElement.clientHeight - top;
    iframe.style.height = `${clientHeight}px`;
  }

  function onLoad(iframe) {
    import('penpal/lib/parent/connectToChild').then(({ default: connectToChild }) => {
      connectToChild({
        iframe,
        methods: {
          // ant-design-vue
          $messageSuccess: message.success,
          $messageInfo: message.info,
          $messageWarning: message.warning,
          $messageError: message.error,
          $messageLoading: message.loading,

          $notificationSuccess: notification.success,
          $notificationWarn: notification.warn,
          $notificationError: notification.error,

          $confirm: Modal.confirm,
          $info: Modal.info,
          $success: Modal.success,
          $error: Modal.error,
          $warning: Modal.warning,
          // vue-sweetalert2
          $swal: swal,
        },
      });
    });
  }

  function hideLoading() {
    loading.value = false;
    onLoad(unref(frameRef));
  }
</script>
<style lang="less" scoped>
  @prefix-cls: ~'@{namespace}-iframe-page';

  .@{prefix-cls} {
    .ant-spin-nested-loading {
      position: relative;
      height: 100%;

      .ant-spin-container {
        width: 100%;
        height: 100%;
        padding: 10px;
      }
    }

    &__mask {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
    }

    &__main {
      width: 100%;
      height: 100%;
      overflow: hidden;
      background-color: @component-background;
      border: 0;
      box-sizing: border-box;
    }
  }
</style>
