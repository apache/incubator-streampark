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
    <span> {{ title }}</span>
    <InputNumber
      v-bind="$attrs"
      size="small"
      :class="`${prefixCls}-input-number`"
      @change="handleChange"
    />
  </div>
</template>
<script lang="ts">
  import { defineComponent, PropType } from 'vue';

  import { InputNumber } from 'ant-design-vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { baseHandler } from '../handler';
  import { HandlerEnum } from '../enum';

  export default defineComponent({
    name: 'InputNumberItem',
    components: { InputNumber },
    props: {
      event: {
        type: Number as PropType<HandlerEnum>,
      },
      title: {
        type: String,
      },
    },
    setup(props) {
      const { prefixCls } = useDesign('setting-input-number-item');

      function handleChange(e) {
        props.event && baseHandler(props.event, e);
      }
      return {
        prefixCls,
        handleChange,
      };
    },
  });
</script>
<style lang="less" scoped>
  @prefix-cls: ~'@{namespace}-setting-input-number-item';

  .@{prefix-cls} {
    display: flex;
    justify-content: space-between;
    margin: 16px 0;

    &-input-number {
      width: 126px;
    }
  }
</style>
