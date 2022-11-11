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
  <a-input v-bind="$attrs" :class="prefixCls" :size="size" :value="state">
    <template #addonAfter>
      <CountButton :size="size" :count="count" :value="state" :beforeStartFunc="sendCodeApi" />
    </template>
    <template #[item]="data" v-for="item in Object.keys($slots).filter((k) => k !== 'addonAfter')">
      <slot :name="item" v-bind="data || {}"></slot>
    </template>
  </a-input>
</template>
<script lang="ts">
  import { defineComponent, PropType } from 'vue';
  import CountButton from './CountButton.vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { useRuleFormItem } from '/@/hooks/component/useFormItem';

  const props = {
    value: { type: String },
    size: { type: String, validator: (v) => ['default', 'large', 'small'].includes(v) },
    count: { type: Number, default: 60 },
    sendCodeApi: {
      type: Function as PropType<() => Promise<boolean>>,
      default: null,
    },
  };

  export default defineComponent({
    name: 'CountDownInput',
    components: { CountButton },
    inheritAttrs: false,
    props,
    setup(props) {
      const { prefixCls } = useDesign('countdown-input');
      const [state] = useRuleFormItem(props);

      return { prefixCls, state };
    },
  });
</script>
<style lang="less">
  @prefix-cls: ~'@{namespace}-countdown-input';

  .@{prefix-cls} {
    .ant-input-group-addon {
      padding-right: 0;
      background-color: transparent;
      border: none;

      button {
        font-size: 14px;
      }
    }
  }
</style>
