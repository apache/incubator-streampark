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
<template>
  <div :class="prefixCls">
    <template v-for="color in colorList || []" :key="color">
      <span
        @click="handleClick(color)"
        :class="[
          `${prefixCls}__item`,
          {
            [`${prefixCls}__item--active`]: def === color,
          },
        ]"
        :style="{ background: color }"
      >
        <CheckOutlined />
      </span>
    </template>
  </div>
</template>
<script lang="ts">
  import { defineComponent, PropType } from 'vue';
  import { CheckOutlined } from '@ant-design/icons-vue';

  import { useDesign } from '/@/hooks/web/useDesign';

  import { baseHandler } from '../handler';
  import { HandlerEnum } from '../enum';

  export default defineComponent({
    name: 'ThemeColorPicker',
    components: { CheckOutlined },
    props: {
      colorList: {
        type: Array as PropType<string[]>,
        defualt: [],
      },
      event: {
        type: Number as PropType<HandlerEnum>,
      },
      def: {
        type: String,
      },
    },
    setup(props) {
      const { prefixCls } = useDesign('setting-theme-picker');

      function handleClick(color: string) {
        props.event && baseHandler(props.event, color);
      }
      return {
        prefixCls,
        handleClick,
      };
    },
  });
</script>
<style lang="less">
  @prefix-cls: ~'@{namespace}-setting-theme-picker';

  .@{prefix-cls} {
    display: flex;
    flex-wrap: wrap;
    margin: 16px 0;
    justify-content: space-around;

    &__item {
      width: 20px;
      height: 20px;
      cursor: pointer;
      border: 1px solid #ddd;
      border-radius: 2px;

      svg {
        display: none;
      }

      &--active {
        border: 1px solid lighten(@primary-color, 10%);

        svg {
          display: inline-block;
          margin: 0 0 3px 3px;
          font-size: 12px;
          fill: @white !important;
        }
      }
    }
  }
</style>
