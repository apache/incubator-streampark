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
  import { EllipsisOutlined } from '@ant-design/icons-vue';
  import { isNil } from 'lodash-es';
  import { ref, computed, onMounted, onUnmounted } from 'vue';
  defineOptions({ name: 'AppResize' });
  const props = withDefaults(
    defineProps<{
      resizeMin?: number;
      left: number;
      tableContainer: HTMLElement | null;
    }>(),
    {
      resizeMin: 100,
    },
  );
  const emit = defineEmits(['resizeEnd', 'update:left']);
  const domRef = ref<HTMLElement | null>(null);
  const getStyle = computed(() => {
    return {
      left: `${props.left + 10}px`,
      top: '50px',
    };
  });
  const isMove = ref(false);

  function getResizeLeft(e: MouseEvent): number {
    const getOffsetLeft = (elem: HTMLElement | null): number => {
      let innerOffsetLeft = 0;
      let currentElem = elem;
      do {
        if (!isNil(currentElem!.offsetLeft)) innerOffsetLeft += currentElem!.offsetLeft;
      } while ((currentElem = currentElem!.offsetParent as HTMLElement));
      return Math.floor(innerOffsetLeft);
    };

    const offsetLeft = getOffsetLeft(props.tableContainer);
    console.log('offsetLeft', offsetLeft);
    const maxResize = props.tableContainer!.getBoundingClientRect().width - 510;
    let newLeft = e.pageX - offsetLeft;
    if (newLeft > maxResize) newLeft = maxResize;

    if (newLeft < props.resizeMin) newLeft = props.resizeMin;

    return newLeft;
  }

  function startMove() {
    isMove.value = true;
  }

  function move(e: MouseEvent) {
    if (!isMove.value) return;
    e.preventDefault();
    const left = getResizeLeft(e);
    emit('update:left', left);
  }

  function mouseup(e: MouseEvent) {
    isMove.value = false;
    if (e.target !== domRef.value) {
      e.stopPropagation();
      emit('resizeEnd', { left: props.left });
    }
  }

  onMounted(() => {
    window.addEventListener('mouseup', mouseup);
    window.addEventListener('mousemove', move);
  });
  onUnmounted(() => {
    window.removeEventListener('mouseup', mouseup);
    window.removeEventListener('mousemove', move);
  });
</script>

<template>
  <div ref="domRef" class="resize app-vertical" :style="getStyle" @mousedown="startMove">
    <div class="resize-handle app-handle-vertical">
      <EllipsisOutlined />
    </div>
  </div>
</template>

<style lang="less">
  :root {
    --resize-border-color: #f0f0f0;
    --resize-handle-border: #e8e8e8;
    --resize-background-color: #fbfbfb;
  }

  [data-theme='dark']:root {
    --resize-border-color: #303030;
    --resize-handle-border: #303030;
    --resize-background-color: #1d1d1d;
  }

  .resize {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 10;
    background: transparent;

    &.app-vertical {
      width: 7px;
      height: calc(100% - 95px);
      border-left: 1px solid var(--resize-border-color);
      cursor: col-resize;
    }
  }

  .resize-handle {
    position: relative;
    top: -2px;
    left: 50%;
    width: 30px;
    height: 10px;
    margin-right: 15px;
    border: 1px solid var(--resize-handle-border);
    border-radius: 2px;
    background: var(--resize-background-color);
    line-height: 10px;
    text-align: center;
    pointer-events: none;

    &.app-handle-vertical {
      top: 50%;
      left: auto;
      margin-top: -15px;
      margin-right: auto;
      margin-left: 5px;
      transform: rotate(90deg);
      transform-origin: left top;
    }

    span {
      position: relative;
      top: -2px;
    }
  }
</style>
