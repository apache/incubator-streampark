import type { NScrollbar } from 'naive-ui'
import { ref, type Ref, watchEffect } from 'vue'
import { throttle } from 'radash'

export function useTabScroll(currentTabPath: Ref<string>) {
  const scrollbar = ref<InstanceType<typeof NScrollbar>>()
  const safeArea = ref(150)

  const handleTabSwitch = (distance: number) => {
    scrollbar.value?.scrollTo({
      left: distance,
      behavior: 'smooth',
    })
  }

  const scrollToCurrentTab = () => {
    nextTick(() => {
      const currentTabElement = document.querySelector(`[data-tab-path="${currentTabPath.value}"]`) as HTMLElement
      const tabBarScrollWrapper = document.querySelector('.tab-bar-scroller-wrapper .n-scrollbar-container')
      const tabBarScrollContent = document.querySelector('.tab-bar-scroller-content')

      if (currentTabElement && tabBarScrollContent && tabBarScrollWrapper) {
        const tabLeft = currentTabElement.offsetLeft
        const tabBarLeft = tabBarScrollWrapper.scrollLeft
        const wrapperWidth = tabBarScrollWrapper.getBoundingClientRect().width
        const tabWidth = currentTabElement.getBoundingClientRect().width
        const containerPR = Number.parseFloat(window.getComputedStyle(tabBarScrollContent).paddingRight)

        if (tabLeft + tabWidth + safeArea.value + containerPR > wrapperWidth + tabBarLeft) {
          handleTabSwitch(tabLeft + tabWidth + containerPR - wrapperWidth + safeArea.value)
        }
        else if (tabLeft - safeArea.value < tabBarLeft) {
          handleTabSwitch(tabLeft - safeArea.value)
        }
      }
    })
  }

  const handleScroll = throttle({ interval: 120 }, (step: number) => {
    scrollbar.value?.scrollBy({
      left: step * 400,
      behavior: 'smooth',
    })
  })

  const onWheel = (e: WheelEvent) => {
    e.preventDefault()
    if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
      handleScroll(e.deltaY > 0 ? 1 : -1)
    }
  }

  watchEffect(() => {
    if (currentTabPath.value) {
      scrollToCurrentTab()
    }
  })

  return {
    scrollbar,
    onWheel,
    safeArea,
    handleTabSwitch,
  }
}
