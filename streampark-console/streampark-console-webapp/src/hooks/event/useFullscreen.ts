import { useFullscreen } from '@vueuse/core';
import { computed, ref, unref } from 'vue';
import { useI18n } from '../web/useI18n';

export const useFullscreenEvent = (gloabl = false) => {
  const fullscreenRef = ref<HTMLElement>();
  const { t } = useI18n();
  const { toggle, isFullscreen } = useFullscreen(gloabl ? null : fullscreenRef);

  const getTitle = computed(() => {
    return unref(isFullscreen)
      ? t('layout.header.tooltipExitFull')
      : t('layout.header.tooltipEntryFull');
  });

  return {
    fullscreenRef,
    toggle,
    isFullscreen,
    getTitle,
  };
};

export const useFullContent = () => {
  const fullScreenStatus = ref(false);
  const { t } = useI18n();

  const getTitle = computed(() => {
    return unref(fullScreenStatus)
      ? t('layout.header.tooltipExitFull')
      : t('layout.header.tooltipEntryFull');
  });
  function toggle() {
    fullScreenStatus.value = !fullScreenStatus.value;
  }
  const fullContentClass = computed(() => {
    return {
      [`box-content__full`]: fullScreenStatus.value,
    };
  });
  const fullEditorClass = computed(() => {
    return {
      [`h-[calc(100%-20px)]`]: !fullScreenStatus.value,
      [`h-[calc(100%-88px)]`]: fullScreenStatus.value,
    };
  });
  return {
    fullEditorClass,
    fullContentClass,
    fullScreenStatus,
    getTitle,
    toggle,
  };
};
