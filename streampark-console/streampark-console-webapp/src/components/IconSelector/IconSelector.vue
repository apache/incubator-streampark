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
  <div>
    <a-tabs>
      <a-tab-pane
        v-for="(v, i) in icons"
        :tab="v.title"
        :key="i">
        <ul>
          <li
            v-for="(icon, key) in v.icons"
            :key="`${v.title}-${key}`"
            :class="{ 'active': selectedIcon==icon }">
            <a-icon
              :type="icon"
              :style="{ fontSize: '36px' }"
              @click="handleSelectedIcon(icon)" />
          </li>
        </ul>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import icons from './icons'

export default {
  name: 'IconSelect',
  data () {
    return {
      selectedIcon: '',
      icons
    }
  },
  methods: {
    handleSelectedIcon (icon) {
      this.selectedIcon = icon
      this.$emit('change', icon)
    }
  }
}
</script>

<style lang="less" scoped>
  ul{
    list-style: none;
    padding: 0;
    overflow-y: scroll;
    height: 250px;
    li{
      display: inline-block;
      padding:5px;
      margin:5px;
      &:hover {
        cursor: pointer;
      }
      &.active{
        box-shadow: 0px 0px 5px 2px #888888;
      }
    }
  }
</style>
