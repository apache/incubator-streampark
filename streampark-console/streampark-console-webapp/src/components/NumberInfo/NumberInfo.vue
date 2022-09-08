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
  <div
    :class="[prefixCls]">
    <slot
      name="subtitle">
      <div
        :class="[`${prefixCls}-subtitle`]">
        {{ typeof subTitle === 'string' ? subTitle : subTitle() }}
      </div>
    </slot>
    <div
      class="number-info-value">
      <span>{{ total }}</span>
      <span
        class="sub-total">
        {{ subTotal }}
        <icon
          :type="`caret-${status}`" />
      </span>
    </div>
  </div>
</template>

<script>
import Icon from 'ant-design-vue/es/icon'

export default {
  name: 'NumberInfo',
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-number-info'
    },
    total: {
      type: Number,
      required: true
    },
    subTotal: {
      type: Number,
      required: true
    },
    subTitle: {
      type: [String, Function],
      default: ''
    },
    status: {
      type: String,
      default: 'up'
    }
  },
  components: {
    Icon
  },
  data () {
    return {}
  }
}
</script>

<style lang="less" scoped>
  @import "index";
</style>
