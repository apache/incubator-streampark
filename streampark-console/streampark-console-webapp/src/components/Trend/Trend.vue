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
    :class="[prefixCls, reverseColor && 'reverse-color' ]">
    <span>
      <slot
        name="term" />
      <span
        class="item-text">
        <slot />
      </span>
    </span>
    <span
      :class="[flag]"><a-icon
      :type="`caret-${flag}`" /></span>
  </div>
</template>

<script>
export default {
  name: 'Trend',
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-trend'
    },
    /**
       * 上升下降标识：up|down
       */
    flag: {
      type: String,
      required: true
    },
    /**
       * 颜色反转
       */
    reverseColor: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style lang="less" scoped>
  @import "index";
</style>
